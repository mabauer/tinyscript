package de.mkbauer.tinyscript.runtime.fs;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import de.mkbauer.tinyscript.interpreter.ModuleExports;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptIOError;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.runtime.array.ArrayConstructor;
import de.mkbauer.tinyscript.runtime.array.ArrayObject;

/**
 * The native module std:fs: file access, UTF-8, relative paths against the working
 * directory. Strict: errors end the script (tinyscript has no try / catch), so scripts
 * check with exists() first. Not available when the engine runs sandboxed.
 */
public final class FileSystemModule {

	/** The exported names (the Javascript generator emits the same functions). */
	public static final List<String> EXPORTS = List.of(
			"exists", "readFile", "readLines", "writeFile", "writeLines", "appendFile", "listDir");

	private final TinyscriptEngine engine;

	private FileSystemModule(TinyscriptEngine engine) {
		this.engine = engine;
	}

	public static ModuleExports create(TinyscriptEngine engine) {
		FileSystemModule module = new FileSystemModule(engine);
		Map<String, TSValue> exports = new LinkedHashMap<>();
		exports.put("exists", engine.defineBuiltinFunction("exists", module::exists, 1));
		exports.put("readFile", engine.defineBuiltinFunction("readFile", module::readFile, 1));
		exports.put("readLines", engine.defineBuiltinFunction("readLines", module::readLines, 1));
		exports.put("writeFile", engine.defineBuiltinFunction("writeFile", module::writeFile, 2));
		exports.put("writeLines", engine.defineBuiltinFunction("writeLines", module::writeLines, 2));
		exports.put("appendFile", engine.defineBuiltinFunction("appendFile", module::appendFile, 2));
		exports.put("listDir", engine.defineBuiltinFunction("listDir", module::listDir, 1));
		return new ModuleExports(engine, "\"std:fs\"", exports);
	}

	// exists(path): whether the file or directory exists; never fails
	private TSValue exists(TSObject self, TSValue[] args) {
		String path = string(args[0], "exists", "path");
		try {
			return TSValue.of(Files.exists(Path.of(path)));
		}
		catch (InvalidPathException e) {
			return TSValue.of(false);
		}
	}

	// readFile(path): the contents as a string
	private TSValue readFile(TSObject self, TSValue[] args) {
		String path = string(args[0], "readFile", "path");
		String contents = read(path);
		engine.recordStringCreation(contents);
		return TSValue.of(contents);
	}

	// readLines(path): the lines; a final line break adds no empty line
	private TSValue readLines(TSObject self, TSValue[] args) {
		String contents = read(string(args[0], "readLines", "path"));
		ArrayObject lines = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		if (contents.isEmpty())
			return lines;
		String[] parts = contents.split("\r?\n", -1);
		int count = parts[parts.length - 1].isEmpty() ? parts.length - 1 : parts.length;
		for (int i = 0; i < count; i++) {
			engine.recordStringCreation(parts[i]);
			lines.push(TSValue.of(parts[i]));
		}
		return lines;
	}

	// writeFile(path, text): replaces the file
	private TSValue writeFile(TSObject self, TSValue[] args) {
		write(string(args[0], "writeFile", "path"), string(args[1], "writeFile", "text"));
		return TSValue.UNDEFINED;
	}

	// writeLines(path, lines): replaces the file; each line ends with \n
	private TSValue writeLines(TSObject self, TSValue[] args) {
		String path = string(args[0], "writeLines", "path");
		if (!(args[1] instanceof ArrayObject lines))
			throw new TinyscriptTypeError("fs.writeLines: lines must be an array of strings, not " + args[1].typeOf());
		StringBuilder text = new StringBuilder();
		for (TSValue line : lines.getItems())
			text.append(string(line, "writeLines", "each line")).append('\n');
		write(path, text.toString());
		return TSValue.UNDEFINED;
	}

	// appendFile(path, text): appends, creating the file if needed
	private TSValue appendFile(TSObject self, TSValue[] args) {
		String path = string(args[0], "appendFile", "path");
		String text = string(args[1], "appendFile", "text");
		try {
			Files.writeString(Path.of(path), text, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}
		catch (IOException | InvalidPathException e) {
			throw new TinyscriptIOError("Cannot write file \"" + path + "\": " + reason(e));
		}
		return TSValue.UNDEFINED;
	}

	// listDir(path): the sorted names in the directory
	private TSValue listDir(TSObject self, TSValue[] args) {
		String path = string(args[0], "listDir", "path");
		ArrayObject names = (ArrayObject) engine.getConstructor(ArrayConstructor.NAME).createObject();
		try (Stream<Path> entries = Files.list(Path.of(path))) {
			entries.map(entry -> entry.getFileName().toString()).sorted().forEach(name -> names.push(TSValue.of(name)));
		}
		catch (IOException | InvalidPathException e) {
			throw new TinyscriptIOError("Cannot list directory \"" + path + "\": " + reason(e));
		}
		return names;
	}

	private static String read(String path) {
		try {
			return Files.readString(Path.of(path), StandardCharsets.UTF_8);
		}
		catch (IOException | InvalidPathException e) {
			throw new TinyscriptIOError("Cannot read file \"" + path + "\": " + reason(e));
		}
	}

	private static void write(String path, String text) {
		try {
			Files.writeString(Path.of(path), text, StandardCharsets.UTF_8);
		}
		catch (IOException | InvalidPathException e) {
			throw new TinyscriptIOError("Cannot write file \"" + path + "\": " + reason(e));
		}
	}

	private static String string(TSValue value, String function, String what) {
		if (!value.isString())
			throw new TinyscriptTypeError("fs." + function + ": " + what + " must be a string, not " + value.typeOf());
		return value.asString();
	}

	private static String reason(Exception e) {
		return switch (e) {
			case NoSuchFileException n -> "not found";
			case AccessDeniedException a -> "permission denied";
			case NotDirectoryException n -> "not a directory";
			case CharacterCodingException c -> "not UTF-8";
			default -> e.getMessage();
		};
	}
}
