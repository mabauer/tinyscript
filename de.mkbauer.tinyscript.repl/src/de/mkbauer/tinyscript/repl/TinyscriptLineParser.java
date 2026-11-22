package de.mkbauer.tinyscript.repl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jline.reader.EOFError;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;

/**
 * How jline splits the input: Enter in an open statement (an unclosed bracket or comment)
 * starts a new line of the same input, so the statement can be edited and recalled from the
 * history as a whole. For completion, the word at the cursor is a name or a path of names
 * ({@code Math.ma}), or a command ({@code !lo}).
 */
final class TinyscriptLineParser implements Parser {

	private static final Pattern NAME_PATH = Pattern.compile("[A-Za-z_$][\\w$]*(?:\\s*\\.\\s*[A-Za-z_$][\\w$]*)*(?:\\s*\\.)?$|\\.$");

	private static final Pattern COMMAND = Pattern.compile("^\\s*(![a-z]*)$");

	@Override
	public ParsedLine parse(String line, int cursor, ParseContext context) {
		if (context == ParseContext.ACCEPT_LINE && TinyscriptSyntax.isOpen(line))
			throw new EOFError(-1, -1, "The statement is not complete");
		return new Line(line, cursor, wordBefore(line.substring(0, cursor)));
	}

	/**
	 * The word that ends at the cursor, to be completed (may be empty).
	 */
	static String wordBefore(String text) {
		Matcher command = COMMAND.matcher(text);
		if (command.find())
			return command.group(1);
		if (insideStringOrComment(text))
			return "";
		Matcher path = NAME_PATH.matcher(text);
		if (!path.find() || path.group().equals("."))
			return "";
		// Not a name: the digits after a dot (1.5); a property of something that is not a
		// name ("a".len, f().x) is not completed
		int start = path.start();
		if (start > 0 && Character.isDigit(text.charAt(start - 1)))
			return "";
		if (text.substring(0, start).stripTrailing().endsWith("."))
			return "";
		return path.group();
	}

	private static boolean insideStringOrComment(String text) {
		int i = 0;
		while (i < text.length()) {
			char c = text.charAt(i);
			if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '/') {
				int end = text.indexOf('\n', i);
				if (end < 0)
					return true;
				i = end;
			}
			else if (c == '/' && i + 1 < text.length() && text.charAt(i + 1) == '*') {
				int end = text.indexOf("*/", i + 2);
				if (end < 0)
					return true;
				i = end + 2;
			}
			else if (c == '"' || c == '\'') {
				int end = TinyscriptSyntax.endOfString(text, i);
				if (end >= text.length() && (end - i < 2 || text.charAt(end - 1) != c || text.charAt(end - 2) == '\\'))
					return true;
				i = end;
			}
			else {
				i++;
			}
		}
		return false;
	}

	private record Line(String line, int cursor, String word) implements ParsedLine {

		@Override
		public int wordIndex() {
			return 0;
		}

		@Override
		public int wordCursor() {
			return word.length();
		}

		@Override
		public List<String> words() {
			return List.of(word);
		}
	}

}
