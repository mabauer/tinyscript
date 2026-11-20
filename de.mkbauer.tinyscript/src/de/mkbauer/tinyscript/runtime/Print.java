package de.mkbauer.tinyscript.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * The global print(...) function: writes its arguments, followed by a line separator,
 * to the engine's stdout.
 */
public final class Print {

	private Print() {
	}

	public static BuiltinFunction create(TinyscriptEngine engine) {
		return engine.defineBuiltinFunction("print", (self, args) -> print(engine, args), 0, BuiltinFunction.VARIADIC);
	}

	private static TSValue print(TinyscriptEngine engine, TSValue[] args) {
		OutputStream os = engine.getStdOut();
		StringBuilder line = new StringBuilder();
		for (TSValue arg: args) {
			line.append(arg.asString());
		}
		line.append(System.lineSeparator());
		// Check the output limit before writing, so a script cannot flood the output
		engine.recordOutput(line.length());
		try {
			if (os != null) {
				OutputStreamWriter writer = new OutputStreamWriter(os);
				writer.write(line.toString());
				writer.flush();
			}
		}
		catch(IOException e) {
			throw new TinyscriptRuntimeException("Cannot write output: " + e.getMessage());
		}
		return TSValue.UNDEFINED;
	}

}
