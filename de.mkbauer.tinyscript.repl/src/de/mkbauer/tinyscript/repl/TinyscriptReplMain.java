package de.mkbauer.tinyscript.repl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.DumbTerminal;

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
import de.mkbauer.tinyscript.TinyscriptModelUtil;
import de.mkbauer.tinyscript.validation.TinyscriptSyntaxErrorMessageProvider;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.generator.TinyscriptGenerator;
import de.mkbauer.tinyscript.interpreter.FileSystemModuleLoader;
import de.mkbauer.tinyscript.interpreter.StandardInput;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptInterruption;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.scoping.SharedGlobals;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccess;
import de.mkbauer.tinyscript.ts.CallOrPropertyAccessSuffix;
import de.mkbauer.tinyscript.ts.CallSuffix;
import de.mkbauer.tinyscript.ts.ExpressionStatement;
import de.mkbauer.tinyscript.ts.FunctionDeclaration;
import de.mkbauer.tinyscript.ts.Identifier;
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
			  --node                 run the files with node instead of the interpreter
			                         (experimental: the generated Javascript does not yet
			                         find all errors the interpreter finds)
			  --emit-js              write the Javascript for node to the standard output
			                         instead of running the files
			  --module-path <dirs>   where imports of bare paths ("x", "lib/y") are searched,
			                         separated by ":" (default: $TINYSCRIPT_PATH, otherwise
			                         the directory of each file / the working directory)
			  -h, --help             show this help
			  --version              show the version

			Exit codes: 0 success, 1 syntax or runtime error, 2 wrong usage (also: no
			node 20 or later on the PATH for --node)
			""";

	private static final String INTERACTIVE_HELP = """
			Enter tinyscript statements, each ending with ';' as in a file; an incomplete
			statement continues on the next line. The value of an expression is shown.
			  !load file.tys run a file in the session: its functions, classes and
			                 variables can then be used (again after a change)
			  !names         the names the session has defined, with their values
			  !reset         start a new session (forgets all names)
			  !help          show this help
			  !quit, !bye    leave (or Ctrl-D)
			Tab completes names (after a dot: the properties of the object); a statement
			with an open bracket goes on in the next line and is edited as a whole.
			Ctrl-C stops a running statement, or clears the input at the prompt.
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

	/** How files run: with the interpreter, with node (--node), or only generated (--emit-js). */
	private enum Backend { INTERPRETER, NODE, EMIT_JS }

	private Backend backend = Backend.INTERPRETER;

	private final Injector injector;

	// The interactive session: one engine, its lines see the earlier lines
	private TinyscriptEngine sessionEngine;

	private XtextResourceSet sessionResourceSet;

	private Resource currentResource;

	/** How std:io reads in the interactive session: with line editing (null: from in). */
	private StandardInput sessionInput;

	/** The inputs and files the session has run, for !names. */
	private final List<Tinyscript> sessionScripts = new ArrayList<>();

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
		if (sessionInput != null)
			engine.defineStdIn(sessionInput);
		else
			engine.defineStdIn(in);
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
				case "--node", "--emit-js" -> {
					Backend chosen = arg.equals("--node") ? Backend.NODE : Backend.EMIT_JS;
					if (backend != Backend.INTERPRETER && backend != chosen) {
						err.println("--node and --emit-js cannot be combined");
						return EXIT_USAGE;
					}
					backend = chosen;
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
		if (files.isEmpty() && backend != Backend.INTERPRETER) {
			err.println((backend == Backend.NODE ? "--node" : "--emit-js") + " needs a file");
			err.print(USAGE);
			return EXIT_USAGE;
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
		if (backend == Backend.NODE) {
			String problem = NodeRunner.checkNode();
			if (problem != null) {
				err.println("tinyscript --node: " + problem);
				return EXIT_USAGE;
			}
			err.println("tinyscript --node is experimental: the generated Javascript does not yet find all errors the interpreter finds");
		}
		// Each file runs on its own (fresh engine); files share names only via import / export
		for (Path file : files) {
			int exitCode = backend == Backend.INTERPRETER ? executeFile(file) : generateFile(file);
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
			err.print(describeSyntaxErrors(errors, file));
			return EXIT_ERROR;
		}
		err.print(describeWarnings(issues, file));
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

	/**
	 * --node and --emit-js: the same checks before the run as the interpreter (syntax, validator,
	 * the imported modules), then the Javascript for node, run or written to the standard output.
	 */
	private int generateFile(Path file) {
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
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
			err.print(describeSyntaxErrors(errors, file));
			return EXIT_ERROR;
		}
		err.print(describeWarnings(issues, file));
		String javascript;
		try {
			TinyscriptGenerator generator = injector.getInstance(TinyscriptGenerator.class);
			javascript = generator.generate((Tinyscript) resource.getContents().get(0), true,
					new FileSystemModuleLoader(modulePath(file.toAbsolutePath().getParent()))).toString();
		}
		catch (TinyscriptRuntimeException e) {
			// An import that is not found, cyclic or not exported, as the interpreter reports it
			err.println(e.getClass().getSimpleName() + ": " + e.getMessage());
			err.print(TinyscriptDiagnostics.excerpt(e));
			return EXIT_ERROR;
		}
		if (backend == Backend.EMIT_JS) {
			out.print(javascript);
			out.flush();
			return EXIT_OK;
		}
		try {
			return NodeRunner.run(javascript, scriptArguments, in, out, err, in == System.in && out == System.out && err == System.err)
					== 0 ? EXIT_OK : EXIT_ERROR;
		}
		catch (IOException e) {
			err.println("tinyscript --node: cannot run node: " + e.getMessage());
			return EXIT_USAGE;
		}
	}

	/**
	 * The syntax errors of a file, each with its place and the source line with a marker.
	 */
	private static String describeSyntaxErrors(List<Issue> errors, Path file) {
		String source = readSource(file);
		StringBuilder result = new StringBuilder();
		for (Issue error : errors) {
			int column = TinyscriptDiagnostics.column(error);
			result.append("TinyscriptSyntaxError: " + error.getMessage() + " in " + file + ":" + error.getLineNumber() + ":" + column + System.lineSeparator());
			result.append(TinyscriptDiagnostics.excerpt(source, error.getLineNumber(), column));
		}
		return result.toString();
	}

	/**
	 * The warnings of a file, one per line, each with its place.
	 */
	private static String describeWarnings(List<Issue> issues, Path file) {
		StringBuilder result = new StringBuilder();
		for (String warning : TinyscriptDiagnostics.warnings(issues))
			result.append(warning.replaceFirst("\\(line (\\d+)\\)$", "in " + Matcher.quoteReplacement(file.toString()) + ":$1") + System.lineSeparator());
		return result.toString();
	}

	private static String readSource(Path file) {
		try {
			return Files.readString(file);
		}
		catch (IOException e) {
			return "";
		}
	}

	/**
	 * Starts a new interactive session: an engine without any names of its own.
	 */
	private void newSession() {
		sessionResourceSet = injector.getInstance(XtextResourceSet.class);
		sessionEngine = newEngine(sessionResourceSet, Path.of("").toAbsolutePath());
		currentResource = null;
		sessionScripts.clear();
	}

	private int interactive() {
		newSession();
		Terminal terminal;
		try {
			terminal = newTerminal();
		}
		catch (IOException e) {
			err.println("Error initializing console: " + e.getMessage());
			return EXIT_USAGE;
		}
		LineReaderBuilder builder = LineReaderBuilder.builder()
				.terminal(terminal)
				.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
				// Plain lines, as the history of jline 2 had them
				.option(LineReader.Option.HISTORY_TIMESTAMPED, false)
				// An open statement goes on in the next line of the same input
				.parser(new TinyscriptLineParser())
				.variable(LineReader.SECONDARY_PROMPT_PATTERN, "... ")
				.completer(new TinyscriptCompleter(() -> sessionEngine))
				.highlighter(new TinyscriptHighlighter());
		if (historyFile != null)
			builder.variable(LineReader.HISTORY_FILE, historyFile);
		LineReader reader = builder.build();
		sessionInput = scriptInput(terminal);
		sessionEngine.defineStdIn(sessionInput);
		PrintWriter console = terminal.writer();
		// Ctrl-C while a statement runs stops it (at the prompt, jline reports it instead)
		terminal.handle(Terminal.Signal.INT, signal -> sessionEngine.interrupt());
		console.println("tinyscript " + version() + " - !help for help, !quit to leave");
		String script = "";
		boolean multiline = false;
		try {
			while (true) {
				String line;
				try {
					console.flush();
					line = reader.readLine(multiline ? "... " : "> ");
				}
				catch (UserInterruptException e) {
					// Ctrl-C at the prompt: forget the input so far
					Thread.interrupted();
					multiline = false;
					script = "";
					continue;
				}
				catch (EndOfFileException e) {
					// jline's Ctrl-C handler interrupts the reading thread, which may end the
					// read as if the input had ended: that is Ctrl-C too, not the end
					if (Thread.interrupted()) {
						console.println();
						multiline = false;
						script = "";
						continue;
					}
					break;
				}
				Matcher command = COMMAND.matcher(line.trim());
				if (command.matches() && (command.group(1).equals("quit") || command.group(1).equals("bye")))
					break;
				try {
					if (command.matches()) {
						// A command ends an unfinished statement
						multiline = false;
						script = "";
						runCommand(command.group(1), command.group(2).trim(), console);
						continue;
					}
					Tinyscript ast = parse(script + line, multiline);
					if (ast != null) {
						for (String warning : lastWarnings)
							console.println(warning.replaceFirst(" \\(line \\d+\\)$", ""));
						console.flush();
						sessionScripts.add(ast);
						TSValue result = sessionEngine.execute(ast);
						out.flush();
						if (showsResult(ast, result))
							console.println(result.toString());
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
					console.println("TinyscriptSyntaxError: " + e.getMessage()
							+ (input.contains("\n") ? " (line " + e.getAffectedLine() + " of the input)" : ""));
					console.print(TinyscriptDiagnostics.excerpt(input, e.getAffectedLine(), e.getAffectedColumn()));
					multiline = false;
					script = "";
				}
				catch (TinyscriptRuntimeException e) {
					out.flush();
					console.print(describeRuntimeError(e));
					multiline = false;
					script = "";
				}
				catch (RuntimeException e) {
					// An interpreter bug must not end the session
					console.println("InternalError: " + e);
					multiline = false;
					script = "";
				}
			}
			console.println("Bye!");
			console.flush();
		}
		finally {
			saveHistory(reader);
			closeQuietly(terminal);
		}
		return EXIT_OK;
	}

	/**
	 * A command of the interactive session: ! and a word, maybe with an argument; not a
	 * statement such as !done; (which ends with ';').
	 */
	private static final Pattern COMMAND = Pattern.compile("!([a-z]+)(\\s+[^;]*|)");

	/**
	 * The longest value !names shows; longer ones are cut.
	 */
	private static final int MAX_VALUE_LENGTH = 70;

	private void runCommand(String name, String argument, PrintWriter console) {
		switch (name) {
			case "help" -> console.print(INTERACTIVE_HELP);
			case "load" -> {
				if (argument.isEmpty())
					console.println("!load needs a file: !load file.tys");
				else
					load(Path.of(unquote(argument)), console);
			}
			case "names" -> {
				if (sessionNames().isEmpty())
					console.println("No names yet: the session has not defined any");
				for (String sessionName : sessionNames())
					console.println(sessionName + " = " + shorten(sessionEngine.getGlobalContext().lookup(sessionName).toString()));
			}
			case "reset" -> {
				newSession();
				console.println("A new session: all names are forgotten");
			}
			default -> console.println("Unknown command !" + name + " (!help lists the commands)");
		}
	}

	/**
	 * Runs a file in the session: later inputs see its top-level names. Loading it again (after
	 * a change) replaces them.
	 */
	private void load(Path file, PrintWriter console) {
		if (!Files.isReadable(file)) {
			console.println("File not found: " + file);
			return;
		}
		if (!file.getFileName().toString().endsWith("." + FILE_EXTENSION)) {
			console.println("Not a tinyscript file (expected ." + FILE_EXTENSION + "): " + file);
			return;
		}
		URI uri = URI.createFileURI(file.toAbsolutePath().normalize().toString());
		Resource previous = sessionResourceSet.getResource(uri, false);
		if (previous != null)
			sessionResourceSet.getResources().remove(previous);
		Resource resource = sessionResourceSet.createResource(uri);
		SharedGlobals.share(resource);
		try {
			resource.load(null);
		}
		catch (IOException e) {
			sessionResourceSet.getResources().remove(resource);
			console.println("Cannot read " + file + ": " + e.getMessage());
			return;
		}
		List<Issue> issues = getValidator(resource).validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		List<Issue> errors = TinyscriptDiagnostics.relevantErrors(issues);
		if (!errors.isEmpty()) {
			// A file with errors must not leave its declarations behind for later inputs
			sessionResourceSet.getResources().remove(resource);
			console.print(describeSyntaxErrors(errors, file));
			return;
		}
		console.print(describeWarnings(issues, file));
		console.flush();
		Tinyscript script = (Tinyscript) resource.getContents().get(0);
		sessionScripts.add(script);
		sessionEngine.execute(script);
		out.flush();
		List<String> names = namesOf(script);
		console.println("Loaded " + file + (names.isEmpty() ? "" : ": " + String.join(", ", names)));
	}

	private static String unquote(String argument) {
		boolean quoted = argument.length() >= 2
				&& (argument.startsWith("\"") && argument.endsWith("\"") || argument.startsWith("'") && argument.endsWith("'"));
		return quoted ? argument.substring(1, argument.length() - 1) : argument;
	}

	private static String shorten(String text) {
		String line = text.replace("\n", " ");
		return line.length() <= MAX_VALUE_LENGTH ? line : line.substring(0, MAX_VALUE_LENGTH - 3) + "...";
	}

	/**
	 * The names the session has defined so far (in the order of their first definition), as far
	 * as their declarations have run.
	 */
	private List<String> sessionNames() {
		Set<String> names = new LinkedHashSet<>();
		for (Tinyscript script : sessionScripts)
			names.addAll(namesOf(script));
		return names.stream().filter(name -> sessionEngine.getGlobalContext().isDeclared(name)).toList();
	}

	/**
	 * The top-level names a script declares: imports, functions, classes, let and const.
	 */
	private static List<String> namesOf(Tinyscript script) {
		Set<String> names = new LinkedHashSet<>();
		for (Identifier name : TinyscriptModelUtil.importedIdentifiers(script))
			names.add(name.getName());
		if (script.getGlobal() != null) {
			for (Statement statement : script.getGlobal().getStatements()) {
				if (statement instanceof FunctionDeclaration function && function.getId() != null)
					names.add(function.getId().getName());
				for (Identifier name : TinyscriptModelUtil.declaredIdentifiers(statement))
					names.add(name.getName());
			}
		}
		names.remove(null);
		return List.copyOf(names);
	}

	/**
	 * What std:io reads in the session: lines from the terminal, with line editing but
	 * without the session's completion, highlighting and history. Ctrl-C stops the
	 * statement, Ctrl-D is the end of the input.
	 */
	private StandardInput scriptInput(Terminal terminal) {
		DefaultParser parser = new DefaultParser();
		// Plain lines: quotes and backslashes need not be closed
		parser.setEofOnUnclosedQuote(false);
		parser.setEofOnEscapedNewLine(false);
		LineReader reader = LineReaderBuilder.builder()
				.terminal(terminal)
				.parser(parser)
				.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true)
				.build();
		return prompt -> {
			out.flush();
			try {
				return reader.readLine(prompt);
			}
			catch (UserInterruptException e) {
				Thread.interrupted();
				throw new TinyscriptInterruption();
			}
			catch (EndOfFileException e) {
				// jline's Ctrl-C handler interrupts the reading thread, which may end the read
				// as if the input had ended
				if (Thread.interrupted())
					throw new TinyscriptInterruption();
				return null;
			}
		};
	}

	/**
	 * The terminal of the session: the system's if the session reads System.in (with line
	 * editing where it is a real terminal), otherwise a plain one on the given streams.
	 */
	private Terminal newTerminal() throws IOException {
		// jline logs hints (e.g. on the permissions of the history file) that do not concern the session
		Logger.getLogger("org.jline").setLevel(Level.SEVERE);
		if (in == System.in && out == System.out)
			return TerminalBuilder.builder().encoding(StandardCharsets.UTF_8).system(true).dumb(true).build();
		Terminal terminal = new DumbTerminal("tinyscript", Terminal.TYPE_DUMB, in, out, StandardCharsets.UTF_8);
		// Like a pipe: no echo of the input, no \r before each \n
		Attributes attributes = terminal.getAttributes();
		attributes.setLocalFlag(Attributes.LocalFlag.ECHO, false);
		attributes.setOutputFlag(Attributes.OutputFlag.OPOST, false);
		terminal.setAttributes(attributes);
		return terminal;
	}

	/**
	 * A runtime error as when running a file: the message and the source line with a marker,
	 * then the calls that led there if the error is inside a function. The lines of the
	 * session are called REPL1, REPL2, ... in the calls, as in node.
	 */
	static String describeRuntimeError(TinyscriptRuntimeException e) {
		String nl = System.lineSeparator();
		StringBuilder result = new StringBuilder(e.getClass().getSimpleName() + ": " + e.getMessage() + nl);
		result.append(TinyscriptDiagnostics.excerpt(e));
		if (e.getTinyscriptStacktrace() != null && e.getTinyscriptStacktrace().length > 1)
			result.append(e.getTinyscriptStacktraceAsString().replaceAll("__repl(\\d+)\\." + FILE_EXTENSION, "REPL$1"));
		return result.toString();
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

	private static void saveHistory(LineReader reader) {
		try {
			reader.getHistory().save();
		}
		catch (IOException e) {
			// Losing the history is not worth an error
		}
	}

	private static void closeQuietly(Terminal terminal) {
		try {
			terminal.close();
		}
		catch (IOException e) {
			// Nothing left to do at the end of the session
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
		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			URI syntheticUri = URI.createURI(name + i + "." + FILE_EXTENSION);
			if (sessionResourceSet.getResource(syntheticUri, false) == null)
				return syntheticUri;
		}
		throw new IllegalStateException();
	}

}
