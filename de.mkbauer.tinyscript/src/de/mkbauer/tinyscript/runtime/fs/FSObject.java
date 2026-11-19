package de.mkbauer.tinyscript.runtime.fs;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * File access. Not available when the engine runs sandboxed.
 */
public class FSObject extends TSObject {

	public FSObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		defineBuiltinMethod("readFile", this::readFile, 1);
		defineBuiltinMethod("writeFile", this::writeFile, 2);
	}

	// Returns the contents of the file, or undefined if it cannot be read
	private TSValue readFile(TSObject self, TSValue[] args) {
		engine.enforceSandboxing("readFile");
		String result;
		try {
			result = Files.readString(Path.of(args[0].asString()), Charset.defaultCharset());
		}
		catch (IOException | InvalidPathException e) {
			return TSValue.UNDEFINED;
		}
		engine.recordStringCreation(result);
		return TSValue.of(result);
	}

	// Returns the number of characters written, or undefined if the file cannot be written
	private TSValue writeFile(TSObject self, TSValue[] args) {
		engine.enforceSandboxing("writeFile");
		String contents = args[1].asString();
		try {
			Files.writeString(Path.of(args[0].asString()), contents, Charset.defaultCharset());
		}
		catch (IOException | InvalidPathException e) {
			return TSValue.UNDEFINED;
		}
		return TSValue.of(contents.length());
	}

}
