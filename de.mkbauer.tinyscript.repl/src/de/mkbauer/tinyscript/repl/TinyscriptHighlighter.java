package de.mkbauer.tinyscript.repl;

import java.util.regex.Pattern;

import org.jline.reader.Highlighter;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

/**
 * Colors the input as the web demo does: keywords, literals, strings, numbers, comments, and
 * the commands of the session. A terminal without colors shows the plain text.
 */
final class TinyscriptHighlighter implements Highlighter {

	static final AttributedStyle KEYWORD = AttributedStyle.BOLD.foreground(AttributedStyle.BLUE);
	static final AttributedStyle ATOM = AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA);
	static final AttributedStyle STRING = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
	static final AttributedStyle NUMBER = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN);
	static final AttributedStyle COMMENT = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT + AttributedStyle.BLACK);
	static final AttributedStyle COMMAND = AttributedStyle.BOLD.foreground(AttributedStyle.YELLOW);

	@Override
	public AttributedString highlight(LineReader reader, String buffer) {
		return highlight(buffer);
	}

	static AttributedString highlight(String buffer) {
		AttributedStringBuilder result = new AttributedStringBuilder();
		if (buffer.stripLeading().startsWith("!")) {
			int start = buffer.indexOf('!');
			int end = start + 1;
			while (end < buffer.length() && Character.isLetter(buffer.charAt(end)))
				end++;
			if (TinyscriptSyntax.COMMANDS.contains(buffer.substring(start, end))) {
				result.append(buffer.substring(0, start));
				result.styled(COMMAND, buffer.substring(start, end));
				result.append(buffer.substring(end));
				return result.toAttributedString();
			}
		}
		int i = 0;
		while (i < buffer.length()) {
			char c = buffer.charAt(i);
			int end;
			AttributedStyle style = null;
			if (c == '/' && i + 1 < buffer.length() && buffer.charAt(i + 1) == '/') {
				end = buffer.indexOf('\n', i);
				end = end < 0 ? buffer.length() : end;
				style = COMMENT;
			}
			else if (c == '/' && i + 1 < buffer.length() && buffer.charAt(i + 1) == '*') {
				end = buffer.indexOf("*/", i + 2);
				end = end < 0 ? buffer.length() : end + 2;
				style = COMMENT;
			}
			else if (c == '"' || c == '\'') {
				end = TinyscriptSyntax.endOfString(buffer, i);
				style = STRING;
			}
			else if (Character.isDigit(c)) {
				end = i + 1;
				while (end < buffer.length() && (Character.isLetterOrDigit(buffer.charAt(end)) || buffer.charAt(end) == '.' || buffer.charAt(end) == '_'))
					end++;
				style = NUMBER;
			}
			else if (Character.isJavaIdentifierStart(c)) {
				end = i + 1;
				while (end < buffer.length() && Character.isJavaIdentifierPart(buffer.charAt(end)))
					end++;
				String word = buffer.substring(i, end);
				// A property (o.from) is no keyword
				boolean property = i > 0 && buffer.charAt(i - 1) == '.';
				if (!property && TinyscriptSyntax.KEYWORDS.contains(word))
					style = KEYWORD;
				else if (!property && TinyscriptSyntax.ATOMS.contains(word))
					style = ATOM;
			}
			else {
				end = i + 1;
			}
			if (style != null)
				result.styled(style, buffer.substring(i, end));
			else
				result.append(buffer.substring(i, end));
			i = end;
		}
		return result.toAttributedString();
	}

	@Override
	public void setErrorPattern(Pattern errorPattern) {
		// Errors are reported after Enter, not while typing
	}

	@Override
	public void setErrorIndex(int errorIndex) {
		// Errors are reported after Enter, not while typing
	}

}
