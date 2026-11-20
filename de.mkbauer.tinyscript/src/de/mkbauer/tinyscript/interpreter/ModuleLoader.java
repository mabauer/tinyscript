package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.common.util.URI;

/**
 * Resolves the path of an import to a module. Without a module loader (e.g. in the
 * sandboxed webdemo), imports are not available.
 */
public interface ModuleLoader {

	/**
	 * Returns the URI of the module {@code path} refers to, as written in an import of
	 * {@code importer}. The URI identifies the module: two paths that resolve to the same
	 * URI are the same module.
	 *
	 * @throws TinyscriptReferenceError if there is no such module
	 */
	URI resolve(String path, URI importer);
}
