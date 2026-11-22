package de.mkbauer.tinyscript.repl;

import java.util.Set;

/**
 * What the interactive session needs to know about tinyscript's syntax for editing:
 * keywords, and whether a statement is still open (an unclosed bracket or comment).
 */
final class TinyscriptSyntax {

	/** The keywords of the grammar (Tinyscript.xtext), as the web demo highlights them. */
	static final Set<String> KEYWORDS = Set.of(
			"let", "const", "function", "class", "if", "else", "for", "while", "break", "continue",
			"throw", "try", "catch", "return", "new", "this", "typeof", "instanceof", "delete",
			"import", "export", "from", "as");

	/** Literals and assert, highlighted apart from the keywords. */
	static final Set<String> ATOMS = Set.of("assert", "true", "false", "null", "undefined");

	/** The commands of the interactive session. */
	static final Set<String> COMMANDS = Set.of("!load", "!names", "!reset", "!help", "!quit", "!bye");

	private TinyscriptSyntax() {
	}

	/**
	 * Whether the text ends inside a bracket ((, [, {) or a block comment, so that the statement
	 * goes on in the next line. Brackets in strings and comments do not count; a string that is
	 * not closed at the end of its line is left to the parser.
	 */
	static boolean isOpen(String text) {
		int depth = 0;
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
				int end = text.indexOf('\n', i);
				i = end < 0 ? text.length() : end;
			}
			else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
				int end = text.indexOf("*/", i + 2);
				if (end < 0)
					return true;
				i = end + 2;
			}
			else if (c == '"' || c == '\'') {
				i = endOfString(text, i);
			}
			else {
				if (c == '(' || c == '[' || c == '{')
					depth++;
				else if (c == ')' || c == ']' || c == '}')
					depth--;
				i++;
			}
		}
		return depth > 0;
	}

	/**
	 * The index after the string that starts at {@code start}: after its closing quote, or at
	 * the end of its line if it is not closed.
	 */
	static int endOfString(String text, int start) {
		char quote = text.charAt(start);
		int i = start + 1;
		while (i < text.length() && text.charAt(i) != '\n') {
			char c = text.charAt(i);
			if (c == '\\')
				i += 2;
			else if (c == quote)
				return i + 1;
			else
				i++;
		}
		return Math.min(i, text.length());
	}

}
