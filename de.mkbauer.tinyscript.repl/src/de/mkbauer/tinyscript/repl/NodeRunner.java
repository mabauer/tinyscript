package de.mkbauer.tinyscript.repl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs generated Javascript with the node on the PATH (tinyscript --node).
 */
final class NodeRunner {

	/** The oldest node the generated Javascript is tested with. */
	static final int MINIMUM_VERSION = 20;

	private static final Pattern VERSION = Pattern.compile("v(\\d+)\\.");

	private NodeRunner() {
	}

	/**
	 * Null if a node of at least {@link #MINIMUM_VERSION} is on the PATH, otherwise what is wrong.
	 */
	static String checkNode() {
		String version;
		try {
			Process process = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			version = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
			if (!process.waitFor(30, TimeUnit.SECONDS) || process.exitValue() != 0)
				return "needs node " + MINIMUM_VERSION + " or later on the PATH; 'node --version' failed";
		}
		catch (IOException e) {
			return "needs node " + MINIMUM_VERSION + " or later on the PATH, found none";
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return "interrupted";
		}
		Matcher matcher = VERSION.matcher(version);
		if (!matcher.lookingAt() || Integer.parseInt(matcher.group(1)) < MINIMUM_VERSION)
			return "needs node " + MINIMUM_VERSION + " or later, found " + version;
		return null;
	}

	/**
	 * Runs {@code javascript} with node and returns its exit code. With {@code inherit}, node
	 * uses the standard input and output of this process directly (the command line); otherwise
	 * they are copied from and to the given streams (tests).
	 */
	static int run(String javascript, List<String> arguments, InputStream in, PrintStream out, PrintStream err, boolean inherit)
			throws IOException {
		// .cjs: the generated code uses require, also below a package.json with "type": "module"
		Path file = Files.createTempFile("tinyscript-", ".cjs");
		try {
			Files.writeString(file, javascript, StandardCharsets.UTF_8);
			List<String> command = new ArrayList<>(List.of("node", file.toString()));
			command.addAll(arguments);
			ProcessBuilder builder = new ProcessBuilder(command);
			if (inherit)
				builder.inheritIO();
			Process process = builder.start();
			List<Thread> copies = new ArrayList<>();
			if (!inherit) {
				copies.add(copy(process.getInputStream(), out));
				copies.add(copy(process.getErrorStream(), err));
				Thread input = new Thread(() -> {
					try (OutputStream stdin = process.getOutputStream()) {
						in.transferTo(stdin);
					}
					catch (IOException e) {
						// node ended without reading all of its input
					}
				});
				input.setDaemon(true);
				input.start();
			}
			int exitCode = process.waitFor();
			for (Thread copy : copies)
				copy.join();
			out.flush();
			err.flush();
			return exitCode;
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("interrupted", e);
		}
		finally {
			Files.deleteIfExists(file);
		}
	}

	private static Thread copy(InputStream from, PrintStream to) {
		Thread thread = new Thread(() -> {
			try {
				from.transferTo(to);
			}
			catch (IOException e) {
				// The stream of node ended
			}
		});
		thread.start();
		return thread;
	}
}
