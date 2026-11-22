package de.mkbauer.tinyscript.interpreter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.validation.CheckMode;
import org.eclipse.xtext.validation.Issue;

import de.mkbauer.tinyscript.TinyscriptDiagnostics;

import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.runtime.fs.FileSystemModule;
import de.mkbauer.tinyscript.runtime.io.IoModule;
import de.mkbauer.tinyscript.ts.ImportStatement;
import de.mkbauer.tinyscript.ts.Tinyscript;

/**
 * Resolving and loading modules; shared by the interpreter and the Javascript generator,
 * so both report the same errors.
 */
public final class Modules {

	/** Standard library modules implemented in Java; their URI is "std:name". */
	public static final List<String> NATIVE_MODULES = List.of("fs", "io");

	private Modules() {
	}

	/**
	 * Whether a module is one of the standard library written in tinyscript (std:array,
	 * std:string): loaded from /std in the core bundle.
	 */
	public static boolean isStandardLibrary(URI module) {
		if (module == null || module.lastSegment() == null || !module.lastSegment().endsWith(".tys"))
			return false;
		java.net.URL resource = Modules.class.getResource("/std/" + module.lastSegment());
		return resource != null && resource.toString().equals(module.toString());
	}

	public static boolean isNative(URI module) {
		return "std".equals(module.scheme());
	}

	/** The names a native module exports. */
	public static List<String> nativeExports(URI module) {
		return switch (module.opaquePart()) {
			case "fs" -> FileSystemModule.EXPORTS;
			case "io" -> IoModule.EXPORTS;
			default -> throw new IllegalArgumentException("Not a native module: " + module);
		};
	}

	/**
	 * Creates the exports of a native module.
	 *
	 * @throws TinyscriptUnsupportedOperationError if the module is not available sandboxed
	 */
	public static ModuleExports createNative(URI module, TinyscriptEngine engine, boolean sandboxed) {
		if (sandboxed)
			throw new TinyscriptUnsupportedOperationError(module + " is not available when running sandboxed");
		return switch (module.opaquePart()) {
			case "fs" -> FileSystemModule.create(engine);
			case "io" -> IoModule.create(engine);
			default -> throw new IllegalArgumentException("Not a native module: " + module);
		};
	}

	/** The file name of a module, or the name of a native module ("std:fs"). */
	private static String name(URI module) {
		return module.lastSegment() != null ? module.lastSegment() : module.toString();
	}

	/**
	 * The URI of the module an import refers to.
	 *
	 * @throws TinyscriptUnsupportedOperationError if there is no module loader
	 */
	public static URI resolve(ModuleLoader loader, ImportStatement importStatement) {
		if (loader == null)
			throw new TinyscriptUnsupportedOperationError("Modules are not available here", importStatement);
		return loader.resolve(importStatement.getPath(), importStatement.eResource().getURI());
	}

	/**
	 * Loads and validates the module at {@code uri}.
	 *
	 * @throws TinyscriptSyntaxError naming the module file and line
	 */
	public static Tinyscript load(ResourceSet resourceSet, URI uri) {
		Resource resource = resourceSet.getResource(uri, true);
		List<Issue> issues = ((XtextResource) resource).getResourceServiceProvider().getResourceValidator()
				.validate(resource, CheckMode.ALL, CancelIndicator.NullImpl);
		List<Issue> errors = TinyscriptDiagnostics.relevantErrors(issues);
		if (!errors.isEmpty()) {
			Issue error = errors.get(0);
			int column = TinyscriptDiagnostics.column(error);
			throw new TinyscriptSyntaxError(error.getMessage() + " in " + uri.toFileString() + ":" + error.getLineNumber() + ":" + column,
					uri.toFileString(), error.getLineNumber(), column);
		}
		return (Tinyscript) resource.getContents().get(0);
	}

	/**
	 * The error for importing {@code module} while it is still being loaded, naming the
	 * chain of imports from the main script ({@code loading}: innermost first).
	 */
	public static TinyscriptReferenceError cyclicImport(Deque<URI> loading, URI module) {
		List<String> chain = new ArrayList<>();
		loading.descendingIterator().forEachRemaining(uri -> chain.add(name(uri)));
		chain.add(name(module));
		return new TinyscriptReferenceError("Cyclic import: " + String.join(" → ", chain));
	}

	public static String displayName(URI module) {
		return "\"" + name(module) + "\"";
	}
}
