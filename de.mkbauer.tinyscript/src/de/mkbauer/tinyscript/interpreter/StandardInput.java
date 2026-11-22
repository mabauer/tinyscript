package de.mkbauer.tinyscript.interpreter;

import java.io.IOException;

/**
 * Where std:io reads its lines from: by default the engine's standard input
 * ({@link TinyscriptEngine#defineStdIn(java.io.InputStream)}); the interactive REPL reads
 * them with line editing.
 */
@FunctionalInterface
public interface StandardInput {

	/**
	 * Shows {@code prompt} (maybe empty) and reads a line.
	 *
	 * @return the line without its line break ({@code \n} or {@code \r\n}), or null at the
	 *         end of the input
	 */
	String readLine(String prompt) throws IOException;
}
