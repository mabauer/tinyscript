package de.mkbauer.tinyscript.interpreter;

import java.net.URL;

import org.eclipse.emf.common.util.URI;

/**
 * Loads the standard library modules: tinyscript files in the core bundle
 * ({@code "std:array"} is {@code /std/array.tys}) and native modules implemented in Java
 * ({@code "std:fs"}, see {@link Modules#isNative}). On its own it allows no other
 * imports, e.g. in the sandboxed webdemo.
 */
public class StandardLibraryModuleLoader implements ModuleLoader {

	public static final String PREFIX = "std:";

	@Override
	public URI resolve(String path, URI importer) {
		if (!path.startsWith(PREFIX))
			throw new TinyscriptReferenceError("Only standard library modules (std:…) are available here, not \"" + path + "\"");
		String name = path.substring(PREFIX.length());
		if (Modules.NATIVE_MODULES.contains(name))
			return URI.createURI(path);
		URL module = name.matches("[a-z]+") ? StandardLibraryModuleLoader.class.getResource("/std/" + name + ".tys") : null;
		if (module == null)
			throw new TinyscriptReferenceError("Standard library module \"" + path + "\" not found");
		return URI.createURI(module.toString());
	}
}
