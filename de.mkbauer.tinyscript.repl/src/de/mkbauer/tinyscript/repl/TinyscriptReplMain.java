package de.mkbauer.tinyscript.repl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jline.console.ConsoleReader;
import jline.console.history.FileHistory;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.util.StringInputStream;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.IResourceValidator;
import org.eclipse.xtext.validation.Issue;

import com.google.inject.Injector;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.scoping.SharedGlobals;
import de.mkbauer.tinyscript.ts.Tinyscript;

/**
 * The tinyscript command line: runs .tys files, or starts an interactive session.
 */
class TinyscriptReplMain  {

	static final int EXIT_OK = 0;
	/** A syntax or runtime error in a script. */
	static final int EXIT_ERROR = 1;
	/** Wrong usage: unknown option, missing file, not a .tys file. */
	static final int EXIT_USAGE = 2;

	private static final String FILE_EXTENSION = "tys";

	private static final String USAGE = """
			Usage: tinyscript [options] [file.tys ...]

			Runs the given tinyscript files in order and stops at the first error.
			Without files, starts an interactive session.

			Options:
			  -h, --help     show this help
			  --version      show the version

			Exit codes: 0 success, 1 syntax or runtime error, 2 wrong usage
			""";

	private static final String INTERACTIVE_HELP = """
			Enter tinyscript statements; an incomplete statement continues on the next line.
			  !help          show this help
			  !quit, !bye    leave (or Ctrl-D)
			""";

	private final InputStream in;

	private final PrintStream out;

	private final PrintStream err;

	/** Where the interactive session keeps its history (null: no history). */
	private final Path historyFile;

	private final Injector injector;

	// The interactive session: one engine, its lines see the earlier lines
	private TinyscriptEngine sessionEngine;

	private XtextResourceSet sessionResourceSet;

	private Resource currentResource;

	public static void main(String[] args) {
		// Guice's bytecode generation (AOP, unused by Xtext) calls sun.misc.Unsafe,
		// which prints a deprecation warning on Java 24+ and will be removed.
		if (System.getProperty("guice_bytecode_gen_option") == null)
			System.setProperty("guice_bytecode_gen_option", "DISABLED");

		Path history = Path.of(System.getProperty("user.home"), ".tinyscript_history");
		System.exit(new TinyscriptReplMain(System.in, System.out, System.err, history).run(args));
	}

	TinyscriptReplMain(InputStream in, PrintStream out, PrintStream err, Path historyFile) {
		this.in = in;
		this.out = out;
		this.err = err;
		this.historyFile = historyFile;
		injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

	private TinyscriptEngine newEngine(XtextResourceSet resourceSet) {
		TinyscriptEngine engine = new TinyscriptEngine(resourceSet);
		engine.setSandboxed(false);
		engine.defineStdOut(out);
		return engine;
	}

	/**
	 * Runs the command line and returns the exit code.
	 */
	int run(String[] args) {
		if (args.length == 0)
			return interactive();
		List<Path> files = new ArrayList<>();
		for (String arg : args) {
			switch (arg) {
				case "-h", "--help" -> {
					out.print(USAGE);
					return EXIT_OK;
				}
				case "--version" -> {
					out.println("tinyscript " + version());
					return EXIT_OK;
				}
				default -> {
					if (arg.startsWith("-")) {
						err.println("Unknown option: " + arg);
						err.print(USAGE);
						return EXIT_USAGE;
					}
					files.add(Path.of(arg));
				}
			}
		}
		// Check all files before running any of them
		for (Path file : files) {
			if (!Files.isReadable(file)) {
				err.println("File not found: " + file);
				return EXIT_USAGE;
			}
			if (!file.getFileName().toString().endsWith("." + FILE_EXTENSION)) {
				err.println("Not a tinyscript file (expected ." + FILE_EXTENSION + "): " + file);
				return EXIT_USAGE;
			}
		}
		// Each file runs on its own (fresh engine); files share names only via import / export
		for (Path file : files) {
			int exitCode = executeFile(file);
			if (exitCode != EXIT_OK)
				return exitCode;
		}
		return EXIT_OK;
	}

	private int executeFile(Path file) {
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		TinyscriptEngine engine = newEngine(resourceSet);
		Resource resource = resourceSet.createResource(URI.createFileURI(file.toAbsolutePath().toString()));
		try {
			resource.load(null);
		}
		catch (IOException e) {
			err.println("Cannot read " + file + ": " + e.getMessage());
			return EXIT_USAGE;
		}
		List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		if (!issues.isEmpty()) {
			for (Issue issue : issues)
				err.println("TinyscriptSyntaxError: " + issue.getMessage() + " in " + file + ":" + issue.getLineNumber());
			return EXIT_ERROR;
		}
		try {
			engine.execute((Tinyscript) resource.getContents().get(0));
		}
		catch (TinyscriptRuntimeException e) {
			err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
			err.print(e.getTinyscriptStacktraceAsString());
			return EXIT_ERROR;
		}
		return EXIT_OK;
	}

	private int interactive() {
		sessionResourceSet = injector.getInstance(XtextResourceSet.class);
		sessionEngine = newEngine(sessionResourceSet);
		ConsoleReader device;
		try {
			device = new ConsoleReader(in, out);
		}
		catch (IOException e) {
			err.println("Error initializing console: " + e.getMessage());
			return EXIT_USAGE;
		}
		device.setExpandEvents(false);
		FileHistory history = loadHistory(device);
		out.println("tinyscript " + version() + " - !help for help, !quit to leave");
		String script = "";
		boolean multiline = false;
		try {
			while (true) {
				device.setPrompt(multiline ? "... " : "> ");
				String line = device.readLine();
				if (line == null || line.trim().equals("!quit") || line.trim().equals("!bye"))
					break;
				if (line.trim().equals("!help")) {
					device.print(INTERACTIVE_HELP);
					continue;
				}
				try {
					Tinyscript ast = parse(script + line, multiline);
					if (ast != null) {
						TSValue result = sessionEngine.execute(ast);
						device.println(result.asString());
						multiline = false;
						script = "";
					}
					else {
						multiline = true;
						script = script + line + "\n";
					}
				}
				catch (TinyscriptRuntimeException e) {
					device.println(e.getClass().getSimpleName() + ": " + e.getMessage());
					multiline = false;
					script = "";
				}
			}
			device.println("Bye!");
			device.flush();
		}
		catch (IOException e) {
			err.println("Console error: " + e.getMessage());
			return EXIT_ERROR;
		}
		finally {
			saveHistory(history);
		}
		return EXIT_OK;
	}

	/**
	 * Parses a statement typed in the interactive session. Returns null if it is incomplete.
	 */
	private Tinyscript parse(String script, boolean reuseResource) {
		if (!reuseResource || currentResource == null) {
			currentResource = sessionResourceSet.createResource(newResourceUri());
			// The lines of a session see each other's top-level names
			SharedGlobals.share(currentResource);
		}
		else {
			currentResource.unload();
		}
		try {
			currentResource.load(new StringInputStream(script), null);
		}
		catch (IOException e) {
			throw new WrappedException(e);
		}
		List<Issue> issues = getValidator(currentResource).validate(currentResource, CheckMode.ALL, CancelIndicator.NullImpl);
		if (!issues.isEmpty()) {
			Issue issue = issues.get(0);
			if (issue.getMessage().contains("mismatched input '<EOF>'")) {
				if (issues.size() == 1)
					return null;
				issue = issues.get(1);
			}
			// A rejected line must not leave its declarations behind for later lines
			sessionResourceSet.getResources().remove(currentResource);
			currentResource = null;
			throw new TinyscriptRuntimeException("line " + issue.getLineNumber() + ": " + issue.getMessage());
		}
		return currentResource.getContents().isEmpty() ? null : (Tinyscript) currentResource.getContents().get(0);
	}

	private FileHistory loadHistory(ConsoleReader device) {
		if (historyFile == null)
			return null;
		try {
			FileHistory history = new FileHistory(historyFile.toFile());
			device.setHistory(history);
			return history;
		}
		catch (IOException e) {
			// The session works without history
			return null;
		}
	}

	private void saveHistory(FileHistory history) {
		if (history == null)
			return;
		try {
			history.flush();
		}
		catch (IOException e) {
			// Losing the history is not worth an error
		}
	}

	private static String version() {
		String version = TinyscriptReplMain.class.getPackage().getImplementationVersion();
		return version != null ? version : "(development build)";
	}

	private IResourceValidator getValidator(Resource resource) {
		return ((XtextResource) resource).getResourceServiceProvider().getResourceValidator();
	}

	private URI newResourceUri() {
		String name = "__repl";
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			URI syntheticUri = URI.createURI(name + i + "." + FILE_EXTENSION);
			if (sessionResourceSet.getResource(syntheticUri, false) == null)
				return syntheticUri;
		}
		throw new IllegalStateException();
	}

}
