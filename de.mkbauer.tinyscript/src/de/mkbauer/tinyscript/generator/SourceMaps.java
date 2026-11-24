package de.mkbauer.tinyscript.generator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Source maps for the generated Javascript, with markers: the generator writes a marker with a
 * tinyscript position (source, line, column) before each statement, call and checked operator;
 * {@link #attach} removes the markers and appends a source map (version 3, inline as a data URL)
 * that maps the positions in the generated code to the tinyscript files, so that node (with
 * --enable-source-maps) shows them in stack traces.
 */
public final class SourceMaps {

	/**
	 * Delimits a marker. The generator never writes this character otherwise: string literals
	 * escape control characters, and the prelude has none.
	 */
	static final char MARK = '\u0001';

	private SourceMaps() {
	}

	/** The marker for a position in a source (line and column 0-based). */
	public static String marker(int source, int line, int column) {
		return MARK + "" + source + ":" + line + ":" + column + MARK;
	}

	/** The code without the markers. */
	public static String strip(String code) {
		return code.indexOf(MARK) < 0 ? code : code.replaceAll(MARK + "[^" + MARK + "]*" + MARK, "");
	}

	/**
	 * The code without the markers, with a source map appended that maps the positions of the
	 * markers; sources are the names of the tinyscript files (by the source number of the
	 * markers), contents their texts.
	 */
	public static String attach(String code, List<String> sources, List<String> contents) {
		StringBuilder result = new StringBuilder(code.length());
		List<List<int[]>> lines = new ArrayList<>();
		List<int[]> segments = new ArrayList<>();
		lines.add(segments);
		int column = 0;
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);
			if (c == MARK) {
				int end = code.indexOf(MARK, i + 1);
				String[] parts = code.substring(i + 1, end).split(":");
				segments.add(new int[] { column, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]) });
				i = end;
				continue;
			}
			result.append(c);
			if (c == '\n') {
				segments = new ArrayList<>();
				lines.add(segments);
				column = 0;
			}
			else
				column++;
		}
		String map = "{\"version\":3,\"sources\":" + jsonArray(sources) + ",\"sourcesContent\":" + jsonArray(contents)
				+ ",\"names\":[],\"mappings\":\"" + mappings(lines) + "\"}";
		if (result.length() > 0 && result.charAt(result.length() - 1) != '\n')
			result.append('\n');
		result.append("//# sourceMappingURL=data:application/json;charset=utf-8;base64,")
				.append(Base64.getEncoder().encodeToString(map.getBytes(StandardCharsets.UTF_8)))
				.append('\n');
		return result.toString();
	}

	// The mappings of version 3: lines separated by ;, segments by , and each segment the
	// generated column, source, line and column, relative to the segment before (the generated
	// column only within its line), in Base64 VLQ
	static String mappings(List<List<int[]>> lines) {
		StringBuilder result = new StringBuilder();
		int source = 0, line = 0, column = 0;
		for (int l = 0; l < lines.size(); l++) {
			if (l > 0)
				result.append(';');
			int generatedColumn = 0;
			List<int[]> segments = lines.get(l);
			for (int s = 0; s < segments.size(); s++) {
				int[] segment = segments.get(s);
				if (s > 0)
					result.append(',');
				result.append(vlq(segment[0] - generatedColumn)).append(vlq(segment[1] - source))
						.append(vlq(segment[2] - line)).append(vlq(segment[3] - column));
				generatedColumn = segment[0];
				source = segment[1];
				line = segment[2];
				column = segment[3];
			}
		}
		return result.toString();
	}

	private static final String BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	// Base64 VLQ: the sign in the lowest bit, then groups of 5 bits, the lowest first, with a
	// continuation bit
	public static String vlq(int value) {
		int rest = value < 0 ? ((-value) << 1) | 1 : value << 1;
		StringBuilder result = new StringBuilder();
		do {
			int digit = rest & 31;
			rest >>>= 5;
			if (rest > 0)
				digit |= 32;
			result.append(BASE64.charAt(digit));
		} while (rest > 0);
		return result.toString();
	}

	private static String jsonArray(List<String> values) {
		StringBuilder result = new StringBuilder("[");
		for (int i = 0; i < values.size(); i++) {
			if (i > 0)
				result.append(',');
			result.append(jsonString(values.get(i)));
		}
		return result.append(']').toString();
	}

	static String jsonString(String value) {
		if (value == null)
			return "null";
		StringBuilder result = new StringBuilder("\"");
		for (char c : value.toCharArray()) {
			switch (c) {
				case '"' -> result.append("\\\"");
				case '\\' -> result.append("\\\\");
				case '\n' -> result.append("\\n");
				case '\r' -> result.append("\\r");
				case '\t' -> result.append("\\t");
				default -> {
					if (c < 0x20 || c == 0x2028 || c == 0x2029)
						result.append(String.format("\\u%04x", (int) c));
					else
						result.append(c);
				}
			}
		}
		return result.append('"').toString();
	}
}
