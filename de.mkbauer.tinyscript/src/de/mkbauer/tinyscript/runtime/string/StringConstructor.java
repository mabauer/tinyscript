package de.mkbauer.tinyscript.runtime.string;

import java.util.Locale;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSString;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

public class StringConstructor extends BuiltinConstructor {

	public final static String NAME = "String";

	public StringConstructor(TinyscriptEngine engine) {
		super(engine);
		defineBuiltinMethod("fromCharCode", this::fromCharCode, 0);

		TSObject stringPrototype = getPrototypeProperty().asObject();
		stringPrototype.defineBuiltinMethod("toString", this::stringToString, 0);
		stringPrototype.defineBuiltinMethod("at", this::at, 1);
		stringPrototype.defineBuiltinMethod("charAt", this::charAt, 1);
		stringPrototype.defineBuiltinMethod("substring", this::substring, 1);
		stringPrototype.defineBuiltinMethod("indexOf", this::indexOf, 1);
		stringPrototype.defineBuiltinMethod("lastIndexOf", this::lastIndexOf, 1);
		stringPrototype.defineBuiltinMethod("includes", this::includes, 1);
		stringPrototype.defineBuiltinMethod("startsWith", this::startsWith, 1);
		stringPrototype.defineBuiltinMethod("endsWith", this::endsWith, 1);
		stringPrototype.defineBuiltinMethod("slice", this::slice, 1);
		stringPrototype.defineBuiltinMethod("split", this::split, 1);
		stringPrototype.defineBuiltinMethod("trim", this::trim, 0);
		stringPrototype.defineBuiltinMethod("replace", this::replace, 2);
		stringPrototype.defineBuiltinMethod("replaceAll", this::replaceAll, 2);
		stringPrototype.defineBuiltinMethod("repeat", this::repeat, 1);
		stringPrototype.defineBuiltinMethod("padStart", this::padStart, 1);
		stringPrototype.defineBuiltinMethod("padEnd", this::padEnd, 1);
		stringPrototype.defineBuiltinMethod("toUpperCase", this::toUpperCase, 0);
		stringPrototype.defineBuiltinMethod("toLowerCase", this::toLowerCase, 0);
		stringPrototype.defineBuiltinMethod("charCodeAt", this::charCodeAt, 1);
	}

	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		if (args.length > 0) {
			String str = TSObject.stringify(args[0]);
			if (self instanceof StringObject) {
				((StringObject) self).setValue(str);
			}
			else {
				return TSValue.of(str);
			}
		}
		return self;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public TSObject createObject() {
		return createObject(null);
	}

	public StringObject createObject(TSValue value) {
		StringObject newObject = new StringObject(engine, value);
		newObject.setPrototype(getPrototypeProperty().asObject());
		newObject.defineDefaultProperty("constructor", this);
		return newObject;
	}

	// String.prototype methods

	private TSValue stringToString(TSObject self, TSValue[] args) {
		if (!(self instanceof StringObject))
			throw new TinyscriptTypeError("Function String.prototype.toString only works for String objects.");
		return TSValue.of(((StringObject) self).getValue());
	}

	// String.prototype.at(index): the character at index, a negative index counts from the end
	private TSValue at(TSObject self, TSValue[] args) {
		String str = TSObject.toString(engine, self);
		int index = TSObject.toRelativeIndex(args[0], str.length());
		return index < 0 ? TSValue.UNDEFINED : TSValue.of(str.substring(index, index + 1));
	}

	// Positions must be integers; like all builtins, no silent conversion
	private static int integer(TSValue value, String function) {
		if (!value.isMathematicalInteger())
			throw new TinyscriptTypeError(function + "() needs an integer, not " + (value.isNumber() ? value.asString() : value.typeOf()));
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value.asDouble()));
	}

	private TSValue charAt(TSObject self, TSValue[] args) {
		String result = "";
		if (self instanceof StringObject) {
			String str = TSObject.toString(engine, self);
			int pos = integer(args[0], "charAt");
			if (pos >= 0 && pos < str.length()) {
				result = str.substring(pos, pos + 1);
			}
		}
		return TSValue.of(result);
	}

	private TSValue substring(TSObject self, TSValue[] args) {
		String result = "";
		if (self instanceof StringObject) {
			String str = TSObject.toString(engine, self);
			int start = integer(args[0], "substring");
			int end = (args.length > 1) ? integer(args[1], "substring") : str.length();
			// Clamp both indices to the string and swap them if necessary (like JavaScript)
			start = Math.clamp(start, 0, str.length());
			end = Math.clamp(end, 0, str.length());
			result = str.substring(Math.min(start, end), Math.max(start, end));
		}
		engine.recordStringCreation(result);
		return TSValue.of(result);
	}

	// String.prototype.indexOf(search[, position]): like Javascript, a negative position counts as 0
	private TSValue indexOf(TSObject self, TSValue[] args) {
		String str = thisString(self, "indexOf");
		int position = Math.clamp(TSObject.toStartIndex(args, 1), 0, str.length());
		return TSValue.of(str.indexOf(string(args[0], "indexOf"), position));
	}

	// String.prototype.lastIndexOf(search[, position]): the last occurrence at or before position
	private TSValue lastIndexOf(TSObject self, TSValue[] args) {
		String str = thisString(self, "lastIndexOf");
		int position = args.length > 1 ? Math.clamp(integer(args[1], "lastIndexOf"), 0, str.length()) : str.length();
		return TSValue.of(str.lastIndexOf(string(args[0], "lastIndexOf"), position));
	}

	private TSValue includes(TSObject self, TSValue[] args) {
		return TSValue.of(thisString(self, "includes").contains(string(args[0], "includes")));
	}

	private TSValue startsWith(TSObject self, TSValue[] args) {
		return TSValue.of(thisString(self, "startsWith").startsWith(string(args[0], "startsWith")));
	}

	private TSValue endsWith(TSObject self, TSValue[] args) {
		return TSValue.of(thisString(self, "endsWith").endsWith(string(args[0], "endsWith")));
	}

	// String.prototype.slice(start[, end]): like substring, but a negative index counts from the end
	// and start > end gives ""
	private TSValue slice(TSObject self, TSValue[] args) {
		String str = thisString(self, "slice");
		int start = sliceIndex(integer(args[0], "slice"), str.length());
		int end = args.length > 1 ? sliceIndex(integer(args[1], "slice"), str.length()) : str.length();
		return TSValue.of(start < end ? str.substring(start, end) : "");
	}

	private static int sliceIndex(int index, int length) {
		return index < 0 ? Math.max(0, length + index) : Math.min(index, length);
	}

	// String.prototype.split(separator): the parts between the separators; "" splits into characters.
	// Unlike Java's String.split, the separator is no regular expression and empty parts are kept.
	private TSValue split(TSObject self, TSValue[] args) {
		String str = thisString(self, "split");
		String separator = string(args[0], "split");
		ArrayObject parts = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		if (separator.isEmpty()) {
			for (int i = 0; i < str.length(); i++)
				parts.push(TSValue.of(str.substring(i, i + 1)));
			return parts;
		}
		int start = 0;
		int next;
		while ((next = str.indexOf(separator, start)) >= 0) {
			parts.push(TSValue.of(str.substring(start, next)));
			start = next + separator.length();
		}
		parts.push(TSValue.of(str.substring(start)));
		return parts;
	}

	private TSValue trim(TSObject self, TSValue[] args) {
		return TSValue.of(trim(thisString(self, "trim")));
	}

	/**
	 * Removes leading and trailing whitespace, as Javascript's String.prototype.trim (Java's
	 * String.strip misses the no-break spaces and the byte order mark).
	 */
	public static String trim(String str) {
		int start = 0;
		int end = str.length();
		while (start < end && isWhitespace(str.charAt(start)))
			start++;
		while (end > start && isWhitespace(str.charAt(end - 1)))
			end--;
		return str.substring(start, end);
	}

	private static boolean isWhitespace(char c) {
		return Character.isWhitespace(c) || Character.isSpaceChar(c) || c == '\uFEFF';
	}

	// String.prototype.replace(search, replacement): replaces the first occurrence; search is a string,
	// not a regular expression. The replacement may contain the patterns $$, $&, $` and $' of Javascript.
	private TSValue replace(TSObject self, TSValue[] args) {
		return replace(thisString(self, "replace"), string(args[0], "replace"), string(args[1], "replace"), false);
	}

	private TSValue replaceAll(TSObject self, TSValue[] args) {
		return replace(thisString(self, "replaceAll"), string(args[0], "replaceAll"), string(args[1], "replaceAll"), true);
	}

	private TSValue replace(String str, String search, String replacement, boolean all) {
		StringBuilder result = new StringBuilder();
		int start = 0;
		int match = str.indexOf(search);
		while (match >= 0) {
			result.append(str, start, match);
			appendReplacement(result, replacement, str, match, match + search.length());
			start = match + search.length();
			if (!all)
				break;
			// An empty search matches before every character and at the end
			match = search.isEmpty() ? (match < str.length() ? match + 1 : -1) : str.indexOf(search, start);
		}
		result.append(str, start, str.length());
		engine.recordStringCreation(result.toString());
		return TSValue.of(result.toString());
	}

	private static void appendReplacement(StringBuilder result, String replacement, String str, int matchStart, int matchEnd) {
		for (int i = 0; i < replacement.length(); i++) {
			char c = replacement.charAt(i);
			char next = i + 1 < replacement.length() ? replacement.charAt(i + 1) : 0;
			if (c != '$' || "$&`'".indexOf(next) < 0 || next == 0) {
				result.append(c);
				continue;
			}
			switch (next) {
				case '$' -> result.append('$');
				case '&' -> result.append(str, matchStart, matchEnd);
				case '`' -> result.append(str, 0, matchStart);
				default -> result.append(str, matchEnd, str.length());
			}
			i++;
		}
	}

	// String.prototype.repeat(count): count must be a non-negative integer
	private TSValue repeat(TSObject self, TSValue[] args) {
		String str = thisString(self, "repeat");
		int count = integer(args[0], "repeat");
		if (count < 0)
			throw new TinyscriptTypeError("repeat() needs a non-negative count, not " + count);
		engine.checkStringLength((long) str.length() * count);
		String result = str.repeat(count);
		engine.recordStringCreation(result);
		return TSValue.of(result);
	}

	// String.prototype.padStart(length[, padding]), padEnd(length[, padding]): pads with spaces or
	// repetitions of padding up to length
	private TSValue padStart(TSObject self, TSValue[] args) {
		String str = thisString(self, "padStart");
		return TSValue.of(padding(str, args, "padStart") + str);
	}

	private TSValue padEnd(TSObject self, TSValue[] args) {
		String str = thisString(self, "padEnd");
		return TSValue.of(str + padding(str, args, "padEnd"));
	}

	private String padding(String str, TSValue[] args, String function) {
		int length = integer(args[0], function);
		String padding = args.length > 1 ? string(args[1], function) : " ";
		if (length <= str.length() || padding.isEmpty())
			return "";
		engine.checkStringLength(length);
		int missing = length - str.length();
		return padding.repeat(missing / padding.length() + 1).substring(0, missing);
	}

	private TSValue toUpperCase(TSObject self, TSValue[] args) {
		return TSValue.of(thisString(self, "toUpperCase").toUpperCase(Locale.ROOT));
	}

	private TSValue toLowerCase(TSObject self, TSValue[] args) {
		return TSValue.of(thisString(self, "toLowerCase").toLowerCase(Locale.ROOT));
	}

	// String.prototype.charCodeAt(index): the UTF-16 code of the character at index, NaN if there is none
	private TSValue charCodeAt(TSObject self, TSValue[] args) {
		String str = thisString(self, "charCodeAt");
		int index = integer(args[0], "charCodeAt");
		return TSValue.of(index >= 0 && index < str.length() ? str.charAt(index) : Double.NaN);
	}

	// String.fromCharCode(code, ...): the string of the given UTF-16 codes
	private TSValue fromCharCode(TSObject self, TSValue[] args) {
		StringBuilder result = new StringBuilder();
		for (TSValue arg : args) {
			int code = integer(arg, "fromCharCode");
			if (code < 0 || code > 0xFFFF)
				throw new TinyscriptTypeError("fromCharCode() needs character codes from 0 to 65535, not " + code);
			result.append((char) code);
		}
		return TSValue.of(result.toString());
	}

	/**
	 * The string a String.prototype method is called on.
	 */
	private static String thisString(TSObject self, String method) {
		if (!(self instanceof StringObject string))
			throw new TinyscriptTypeError("Function String.prototype." + method + " only works for strings.");
		return string.getValue();
	}

	// String arguments must be strings; like all builtins, no silent conversion
	private static String string(TSValue value, String function) {
		return switch (value) {
			case TSString s -> s.value();
			case StringObject s -> s.getValue();
			default -> throw new TinyscriptTypeError(function + "() needs a string, not " + value.typeOf());
		};
	}

}
