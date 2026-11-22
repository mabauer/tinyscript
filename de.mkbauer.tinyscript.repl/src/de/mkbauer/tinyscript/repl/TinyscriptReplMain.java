package de.mkbauer.tinyscript.repl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

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

import de.mkbauer.tinyscript.TinyscriptDiagnostics;
import de.mkbauer.tinyscript.validation.TinyscriptSyntaxErrorMessageProvider;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.interpreter.FileSystemModuleLoader;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.scoping.SharedGlobals;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccess;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.CallSuffix;
import de.mkbauer.tinyscript.ts.ExpressionStatement;
import de.mkbauer.tinyscript.ts.Statement;
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

	/**
	 * Each tinyscript call nests many Java calls; with the default stack, a recursion
	 * ends after a few thousand calls. The interpreter runs in a thread with this stack
	 * (reserved, not allocated up front), which allows a recursion depth of about 500,000
	 * and still ends an endless recursion within a few seconds.
	 */
	private static final long STACK_SIZE = 256L << 20;

	private static final String USAGE = """
			Usage: tinyscript [options] [file.tys ...] [-- arguments ...]

			Runs the given tinyscript files in order and stops at the first error.
			Without files, starts an interactive session. The arguments after --
			are the scripts' System.args.

			Options:
			  --module-path <dirs>   where imports of bare paths ("x", "lib/y") are searched,
			                         separated by ":" (default: $TINYSCRIPT_PATH, otherwise
			                         the directory of each file / the working directory)
			  -h, --help             show this help
			  --version              show the version

			Exit codes: 0 success, 1 syntax or runtime error, 2 wrong usage
			""";

	private static final String INTERACTIVE_HELP = """
			Enter tinyscript statements, each ending with ';' as in a file; an incomplete
			statement continues on the next line. The value of an expression is shown.
			  !help          show this help
			  !quit, !bye    leave (or Ctrl-D)
			""";

	private final InputStream in;

	private final PrintStream out;

	private final PrintStream err;

	/** Where the interactive session keeps its history (null: no history). */
	private final Path historyFile;

	/** The module path from the environment (TINYSCRIPT_PATH), or null. */
	private final String environmentModulePath;

	/** The module path from --module-path, or null. */
	private String moduleOption;

	/** The arguments after --, the scripts' System.args. */
	private List<String> scriptArguments = List.of();

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
		System.exit(new TinyscriptReplMain(System.in, System.out, System.err, history, System.getenv("TINYSCRIPT_PATH")).run(args));
	}

	TinyscriptReplMain(InputStream in, PrintStream out, PrintStream err, Path historyFile, String environmentModulePath) {
		this.in = in;
		this.out = out;
		this.err = err;
		this.historyFile = historyFile;
		this.environmentModulePath = environmentModulePath;
		injector = new TinyscriptStandaloneSetup().createInjectorAndDoEMFRegistration();
	}

	/**
	 * A new engine; imports of bare paths are searched in the module path, by default
	 * in {@code defaultDirectory}.
	 */
	private TinyscriptEngine newEngine(XtextResourceSet resourceSet, Path defaultDirectory) {
		TinyscriptEngine engine = new TinyscriptEngine(resourceSet);
		engine.setSandboxed(false);
		engine.defineStdOut(out);
		engine.setModuleLoader(new FileSystemModuleLoader(modulePath(defaultDirectory)));
		engine.setScriptArguments(scriptArguments);
		return engine;
	}

	private List<Path> modulePath(Path defaultDirectory) {
		String dirs = moduleOption != null ? moduleOption : environmentModulePath;
		if (dirs == null || dirs.isBlank())
			return List.of(defaultDirectory);
		return Arrays.stream(dirs.split(File.pathSeparator))
				.filter(dir -> !dir.isBlank())
				.map(Path::of)
				.toList();
	}

	/**
	 * Runs the command line and returns the exit code.
	 */
	int run(String[] args) {
		FutureTask<Integer> task = new FutureTask<>(() -> runWithLargeStack(args));
		new Thread(null, task, "tinyscript", STACK_SIZE).start();
		try {
			return task.get();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return EXIT_ERROR;
		}
		catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException runtimeException)
				throw runtimeException;
			if (e.getCause() instanceof Error error)
				throw error;
			throw new IllegalStateException(e.getCause());
		}
	}

	private int runWithLargeStack(String[] args) {
		List<Path> files = new ArrayList<>();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			switch (arg) {
				case "--module-path" -> {
					if (i + 1 >= args.length) {
						err.println("--module-path needs a value");
						err.print(USAGE);
						return EXIT_USAGE;
					}
					moduleOption = args[++i];
				}
				case "-h", "--help" -> {
					out.print(USAGE);
					return EXIT_OK;
				}
				case "--version" -> {
					out.println("tinyscript " + version());
					return EXIT_OK;
				}
				case "--" -> {
					scriptArguments = List.of(args).subList(i + 1, args.length);
					i = args.length;
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
		if (files.isEmpty())
			return interactive();
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
		TinyscriptEngine engine = newEngine(resourceSet, file.toAbsolutePath().getParent());
		Resource resource = resourceSet.createResource(URI.createFileURI(file.toAbsolutePath().toString()));
		try {
			resource.load(null);
		}
		catch (IOException e) {
			err.println("Cannot read " + file + ": " + e.getMessage());
			return EXIT_USAGE;
		}
		List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		List<Issue> errors = TinyscriptDiagnostics.relevantErrors(issues);
		if (!errors.isEmpty()) {
			String source = readSource(file);
			for (Issue error : errors) {
				int column = TinyscriptDiagnostics.column(error);
				err.println("TinyscriptSyntaxError: " + error.getMessage() + " in " + file + ":" + error.getLineNumber() + ":" + column);
				err.print(TinyscriptDiagnostics.excerpt(source, error.getLineNumber(), column));
			}
			return EXIT_ERROR;
		}
		for (String warning : TinyscriptDiagnostics.warnings(issues))
			err.println(warning.replaceFirst("\\(line (\\d+)\\)$", "in " + java.util.regex.Matcher.quoteReplacement(file.toString()) + ":$1"));
		try {
			engine.execute((Tinyscript) resource.getContents().get(0));
		}
		catch (TinyscriptRuntimeException e) {
			err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
			err.print(TinyscriptDiagnostics.excerpt(e));
			err.print(e.getTinyscriptStacktraceAsString());
			return EXIT_ERROR;
		}
		return EXIT_OK;
	}

	private static String readSource(Path file) {
		try {
			return Files.readString(file);
		}
		catch (IOException e) {
			return "";
		}
	}

	private int interactive() {
		sessionResourceSet = injector.getInstance(XtextResourceSet.class);
		sessionEngine = newEngine(sessionResourceSet, Path.of("").toAbsolutePath());
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
						for (String warning : lastWarnings)
							device.println(warning.replaceFirst(" \\(line \\d+\\)$", ""));
						TSValue result = sessionEngine.execute(ast);
						if (showsResult(ast, result))
							device.println(result.toString());
						multiline = false;
						script = "";
					}
					else {
						multiline = true;
						script = script + line + "\n";
					}
				}
				catch (TinyscriptSyntaxError e) {
					// As for a file; the line is counted from the first line of the statement
					String input = script + line;
					device.println("TinyscriptSyntaxError: " + e.getMessage()
							+ (input.contains("\n") ? " (line " + e.getAffectedLine() + " of the input)" : ""));
					device.print(TinyscriptDiagnostics.excerpt(input, e.getAffectedLine(), e.getAffectedColumn()));
					multiline = false;
					script = "";
				}
				catch (TinyscriptRuntimeException e) {
					device.println(e.getClass().getSimpleName() + ": " + e.getMessage());
					multiline = false;
					script = "";
				}
				catch (RuntimeException e) {
					// An interpreter bug must not end the session
					device.println("InternalError: " + e);
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
	 * Whether the session shows the result: only for an expression statement (x * 2; or
	 * x = 5;), not for declarations or loops, and not the undefined of a call like print(x);.
	 */
	private static boolean showsResult(Tinyscript ast, TSValue result) {
		List<Statement> statements = ast.getGlobal().getStatements();
		if (statements.isEmpty() || !(statements.get(statements.size() - 1) instanceof ExpressionStatement statement))
			return false;
		if (statement.getIncrement() != null)
			return false;
		return result != TSValue.UNDEFINED || !isCall(statement);
	}

	private static boolean isCall(ExpressionStatement statement) {
		return statement.getExpr() instanceof CallOrPropertyAccess access
				&& (access.getSuffix() instanceof CallSuffix
					|| access.getSuffix() instanceof CallOrPropertyAccessSuffix suffix && suffix.getCall() != null);
	}

	/**
	 * Parses a statement typed in the interactive session. Returns null if it is incomplete;
	 * a syntax error is a TinyscriptSyntaxError. As in a file, a statement needs its ';': if
	 * only that is missing at the end, that is an error (instead of waiting for the next line).
	 */
	private Tinyscript parse(String script, boolean reuseResource) {
		if (!reuseResource || currentResource == null) {
			currentResource = sessionResourceSet.createResource(newResourceUri());
			// The lines of a session see each other's top-level names
			SharedGlobals.share(currentResource);
		}
		List<Issue> errors = errors(script);
		if (!errors.isEmpty()) {
			Issue issue = errors.get(0);
			TinyscriptSyntaxError error = new TinyscriptSyntaxError(issue.getMessage(), null, issue.getLineNumber(), TinyscriptDiagnostics.column(issue));
			if (issue.getMessage().startsWith(TinyscriptSyntaxErrorMessageProvider.UNEXPECTED_END)) {
				// The statement is not complete yet: continue on the next line
				if (!errors(script + ";").isEmpty())
					return null;
				// Complete but for the final ';' (x * 2): say so instead of waiting for more
				error = missingSemicolon(script);
			}
			// A rejected line must not leave its declarations behind for later lines
			sessionResourceSet.getResources().remove(currentResource);
			currentResource = null;
			throw error;
		}
		lastWarnings = TinyscriptDiagnostics.warnings(lastIssues);
		return currentResource.getContents().isEmpty() ? null : (Tinyscript) currentResource.getContents().get(0);
	}

	// The issues of the last validation and the warnings of the last complete statement
	private List<Issue> lastIssues = List.of();
	private List<String> lastWarnings = List.of();

	// The errors of the script, loaded into the current resource
	private List<Issue> errors(String script) {
		if (currentResource.isLoaded())
			currentResource.unload();
		try {
			currentResource.load(new StringInputStream(script), null);
		}
		catch (IOException e) {
			throw new WrappedException(e);
		}
		List<Issue> issues = getValidator(currentResource).validate(currentResource, CheckMode.ALL, CancelIndicator.NullImpl);
		lastIssues = issues;
		return TinyscriptDiagnostics.relevantErrors(issues);
	}

	// Marks the place right after the end of the statement
	private static TinyscriptSyntaxError missingSemicolon(String script) {
		String[] lines = script.stripTrailing().split("\n", -1);
		String last = lines[lines.length - 1];
		return new TinyscriptSyntaxError("Missing ';' at the end of the statement", null, lines.length, last.length() + 1);
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
