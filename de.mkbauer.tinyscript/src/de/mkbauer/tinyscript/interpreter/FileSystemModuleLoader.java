package de.mkbauer.tinyscript.interpreter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.URI;

/**
 * Loads modules from the file system.
 * <ul>
 * <li>Relative paths ({@code "./x"}, {@code "../lib/y"}) resolve against the importing file
 * (or the working directory, if the importer is not a file, e.g. a REPL line).</li>
 * <li>Bare paths ({@code "x"}, {@code "math/vector"}) are searched in the module path.</li>
 * <li>The extension {@code .tys} may be omitted.</li>
 * <li>{@code "std:…"} imports the standard library.</li>
 * <li>A module is identified by its canonical path, so a symlink is the same module.</li>
 * </ul>
 */
public class FileSystemModuleLoader implements ModuleLoader {

	private static final String EXTENSION = ".tys";

	private final List<Path> modulePath;

	private final StandardLibraryModuleLoader standardLibrary = new StandardLibraryModuleLoader();

	public FileSystemModuleLoader(List<Path> modulePath) {
		this.modulePath = List.copyOf(modulePath);
	}

	@Override
	public URI resolve(String path, URI importer) {
		if (path.startsWith(StandardLibraryModuleLoader.PREFIX))
			return standardLibrary.resolve(path, importer);
		String fileName = withExtension(path);
		List<Path> candidates;
		if (path.startsWith("./") || path.startsWith("../")) {
			// EMF treats relative URIs (e.g. "__repl1.tys" of a REPL line) as files, too
			boolean importerIsFile = importer.isFile() && importer.hasAbsolutePath();
			Path base = importerIsFile ? Path.of(importer.toFileString()).getParent() : Path.of("").toAbsolutePath();
			candidates = List.of(base.resolve(fileName).normalize());
		}
		else {
			candidates = modulePath.stream().map(dir -> dir.resolve(fileName).normalize()).toList();
		}
		for (Path candidate : candidates) {
			if (Files.isRegularFile(candidate)) {
				try {
					return URI.createFileURI(candidate.toRealPath().toString());
				}
				catch (IOException e) {
					throw new TinyscriptReferenceError("Cannot read module \"" + path + "\": " + e.getMessage());
				}
			}
		}
		throw new TinyscriptReferenceError("Module \"" + path + "\" not found (searched: "
				+ candidates.stream().map(Path::toString).collect(Collectors.joining(", ")) + ")");
	}

	private static String withExtension(String path) {
		String lastSegment = path.substring(path.lastIndexOf('/') + 1);
		if (!lastSegment.contains("."))
			return path + EXTENSION;
		if (!lastSegment.endsWith(EXTENSION))
			throw new TinyscriptReferenceError("\"" + path + "\" is not a tinyscript module (expected " + EXTENSION + ")");
		return path;
	}
}
