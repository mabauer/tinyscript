package de.mkbauer.tinyscript.runtime.json;

import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptJsonError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;
import de.mkbauer.tinyscript.runtime.object.ObjectConstructor;

/**
 * The global JSON with parse(text), as in Javascript: objects, arrays, strings, numbers, true,
 * false and null. Unlike Javascript, the text must be a string (JSON.parse(5) is a TypeError), and
 * there is no reviver. A text that is no JSON is a TinyscriptJsonError, which catch (e) gets as an
 * Error named SyntaxError.
 */
public class JsonObject extends TSObject {

	// Deeper nesting is an error instead of a stack overflow of the interpreter
	static final int MAX_DEPTH = 1000;

	public JsonObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		defineBuiltinMethod("parse", (self, args) -> parse(args[0]), 1);
	}

	private TSValue parse(TSValue text) {
		if (!text.isString())
			throw new TinyscriptTypeError("JSON.parse() needs a string, not " + text.typeOf());
		return new Parser(engine, text.asString()).parseText();
	}

	private static final class Parser {
		private final TinyscriptEngine engine;
		private final String text;
		private int pos = 0;

		Parser(TinyscriptEngine engine, String text) {
			this.engine = engine;
			this.text = text;
		}

		TSValue parseText() {
			TSValue value = value(0);
			skipWhitespace();
			if (pos < text.length())
				throw error("unexpected " + describe(pos) + " after the value");
			return value;
		}

		private TSValue value(int depth) {
			skipWhitespace();
			if (pos >= text.length())
				throw error("the text ends where a value is expected");
			char c = text.charAt(pos);
			switch (c) {
				case '{': return object(depth + 1);
				case '[': return array(depth + 1);
				case '"': return TSValue.of(string());
				case 't': return literal("true", TSValue.of(true));
				case 'f': return literal("false", TSValue.of(false));
				case 'n': return literal("null", TSValue.NULL);
				default:
					if (c == '-' || (c >= '0' && c <= '9'))
						return number();
					throw error("unexpected " + describe(pos) + " where a value is expected");
			}
		}

		private TSValue object(int depth) {
			checkDepth(depth);
			pos++;
			TSObject object = engine.getConstructor(ObjectConstructor.NAME).createObject();
			skipWhitespace();
			if (peek() == '}') {
				pos++;
				return object;
			}
			while (true) {
				skipWhitespace();
				if (peek() != '"')
					throw error("expected a key in double quotes, not " + describe(pos));
				String key = string();
				skipWhitespace();
				expect(':');
				object.put(key, value(depth));
				skipWhitespace();
				if (peek() == ',') {
					pos++;
					continue;
				}
				expect('}');
				return object;
			}
		}

		private TSValue array(int depth) {
			checkDepth(depth);
			pos++;
			ArrayObject array = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
			skipWhitespace();
			if (peek() == ']') {
				pos++;
				return array;
			}
			while (true) {
				array.push(value(depth));
				skipWhitespace();
				if (peek() == ',') {
					pos++;
					continue;
				}
				expect(']');
				return array;
			}
		}

		private String string() {
			int start = pos;
			pos++;
			StringBuilder result = new StringBuilder();
			while (true) {
				if (pos >= text.length())
					throw error("the string at position " + start + " has no closing \"");
				char c = text.charAt(pos++);
				if (c == '"')
					return result.toString();
				if (c < 0x20)
					throw error("a string cannot contain a line break or another control character (position " + (pos - 1) + "): write \\n etc.");
				if (c != '\\') {
					result.append(c);
					continue;
				}
				char escaped = pos < text.length() ? text.charAt(pos++) : '\0';
				switch (escaped) {
					case '"': result.append('"'); break;
					case '\\': result.append('\\'); break;
					case '/': result.append('/'); break;
					case 'b': result.append('\b'); break;
					case 'f': result.append('\f'); break;
					case 'n': result.append('\n'); break;
					case 'r': result.append('\r'); break;
					case 't': result.append('\t'); break;
					case 'u':
						if (pos + 4 > text.length() || !text.substring(pos, pos + 4).matches("[0-9a-fA-F]{4}"))
							throw error("\\u needs four hexadecimal digits (position " + (pos - 2) + ")");
						result.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
						pos += 4;
						break;
					default:
						throw error("unknown escape \\" + escaped + " at position " + (pos - 2));
				}
			}
		}

		// -?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?
		private TSValue number() {
			int start = pos;
			if (peek() == '-')
				pos++;
			if (peek() == '0')
				pos++;
			else if (!digits())
				throw error("expected a digit at position " + pos + ", not " + describe(pos));
			if (peek() == '.') {
				pos++;
				if (!digits())
					throw error("expected a digit after the . at position " + (pos - 1));
			}
			if (peek() == 'e' || peek() == 'E') {
				pos++;
				if (peek() == '+' || peek() == '-')
					pos++;
				if (!digits())
					throw error("expected a digit in the exponent at position " + pos);
			}
			return TSValue.of(Double.parseDouble(text.substring(start, pos)));
		}

		private boolean digits() {
			int start = pos;
			while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9')
				pos++;
			return pos > start;
		}

		private TSValue literal(String word, TSValue value) {
			if (!text.startsWith(word, pos))
				throw error("unexpected " + describe(pos) + " where a value is expected");
			pos += word.length();
			return value;
		}

		private void expect(char c) {
			if (peek() != c)
				throw error("expected " + c + " at position " + pos + ", not " + describe(pos));
			pos++;
		}

		private char peek() {
			return pos < text.length() ? text.charAt(pos) : '\0';
		}

		private void skipWhitespace() {
			while (pos < text.length() && " \t\n\r".indexOf(text.charAt(pos)) >= 0)
				pos++;
		}

		private void checkDepth(int depth) {
			if (depth > MAX_DEPTH)
				throw error("nested more than " + MAX_DEPTH + " levels deep");
		}

		// "'}' at position 5" or "the end"
		private String describe(int position) {
			return position >= text.length() ? "the end" : "'" + text.charAt(position) + "' at position " + position;
		}

		private TinyscriptJsonError error(String message) {
			return new TinyscriptJsonError("JSON.parse: " + message);
		}
	}
}
