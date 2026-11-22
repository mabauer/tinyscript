package de.mkbauer.tinyscript.runtime.io;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.mkbauer.tinyscript.interpreter.ModuleExports;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptIOError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;

/**
 * The native module std:io: lines of the standard input ({@link TinyscriptEngine#getStdIn()}).
 * The end of the input is null; a failed read is a TinyscriptIOError (catch (e) gets an
 * Error named IOError), wrong arguments are a TypeError. Not available when the engine runs
 * sandboxed.
 */
public final class IoModule {

	/** The exported names (the Javascript generator emits the same functions). */
	public static final List<String> EXPORTS = List.of("readLine", "lines");

	private final TinyscriptEngine engine;

	private IoModule(TinyscriptEngine engine) {
		this.engine = engine;
	}

	public static ModuleExports create(TinyscriptEngine engine) {
		IoModule module = new IoModule(engine);
		Map<String, TSValue> exports = new LinkedHashMap<>();
		exports.put("readLine", engine.defineBuiltinFunction("readLine", module::readLine, 0, 1));
		exports.put("lines", engine.defineBuiltinFunction("lines", module::lines, 0));
		return new ModuleExports(engine, "\"std:io\"", exports);
	}

	// readLine(prompt?): the next line without its line break, null at the end of the input
	private TSValue readLine(TSObject self, TSValue[] args) {
		String prompt = "";
		if (args.length > 0 && args[0] != TSValue.UNDEFINED) {
			if (!args[0].isString())
				throw new TinyscriptTypeError("io.readLine: prompt must be a string, not " + args[0].typeOf());
			prompt = args[0].asString();
		}
		String line = read(prompt);
		return line == null ? TSValue.NULL : TSValue.of(line);
	}

	// lines(): the remaining lines, read one by one by the for loop
	private TSValue lines(TSObject self, TSValue[] args) {
		return new LinesObject(engine, this);
	}

	String read(String prompt) {
		engine.recordOutput(prompt.length());
		String line;
		try {
			line = engine.getStdIn().readLine(prompt);
		}
		catch (IOException e) {
			throw new TinyscriptIOError("Cannot read the standard input: " + e.getMessage());
		}
		if (line != null)
			engine.recordStringCreation(line);
		return line;
	}
}
