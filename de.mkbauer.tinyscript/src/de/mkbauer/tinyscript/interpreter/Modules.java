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

import de.mkbauer.tinyscript.TinyscriptSyntaxError;
import de.mkbauer.tinyscript.ts.ImportStatement;
import de.mkbauer.tinyscript.ts.Tinyscript;

/**
 * Resolving and loading modules; shared by the interpreter and the Javascript generator,
 * so both report the same errors.
 */
public final class Modules {

	private Modules() {
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
		if (!issues.isEmpty()) {
			Issue issue = issues.get(0);
			throw new TinyscriptSyntaxError(issue.getMessage() + " in " + uri.toFileString() + ":" + issue.getLineNumber(),
					uri.toFileString(), issue.getLineNumber());
		}
		return (Tinyscript) resource.getContents().get(0);
	}

	/**
	 * The error for importing {@code module} while it is still being loaded, naming the
	 * chain of imports from the main script ({@code loading}: innermost first).
	 */
	public static TinyscriptReferenceError cyclicImport(Deque<URI> loading, URI module) {
		List<String> chain = new ArrayList<>();
		loading.descendingIterator().forEachRemaining(uri -> chain.add(uri.lastSegment()));
		chain.add(module.lastSegment());
		return new TinyscriptReferenceError("Cyclic import: " + String.join(" → ", chain));
	}

	public static String displayName(URI module) {
		return "\"" + module.lastSegment() + "\"";
	}
}
