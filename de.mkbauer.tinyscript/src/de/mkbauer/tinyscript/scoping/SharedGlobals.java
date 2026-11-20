package de.mkbauer.tinyscript.scoping;

import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Marks resources whose top-level names are visible in the other resources of the same
 * resource set: the builtins and the lines of an interactive REPL session. All other
 * resources (files) are isolated; they share names only via import / export.
 */
public final class SharedGlobals {

	private static final class Marker extends AdapterImpl {
		@Override
		public boolean isAdapterForType(Object type) {
			return type == SharedGlobals.class;
		}
	}

	private SharedGlobals() {
	}

	public static void share(Resource resource) {
		if (!isShared(resource))
			resource.eAdapters().add(new Marker());
	}

	public static boolean isShared(Resource resource) {
		return resource.eAdapters().stream().anyMatch(adapter -> adapter instanceof Marker);
	}
}
