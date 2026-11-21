package de.mkbauer.tinyscript.interpreter;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import de.mkbauer.tinyscript.runtime.array.ArrayObject;

/**
 * Converts a value into the form print and string conversion show: {@code {x: 1, s: "a"}},
 * {@code [1, 2]}, {@code Map {...}}. Each object type contributes its own form via
 * {@link TSObject#display(Display)} and formats the values it contains with {@link #format(TSValue)}.
 *
 * <ul>
 * <li>A reference back to an object that is still being shown (a cycle) appears as {@code [Circular]};
 *     an object that is only shared, not cyclic, is shown each time.</li>
 * <li>Below {@link #MAX_DEPTH} levels of nesting, objects appear as {@code [Object]} / {@code [Array]},
 *     so a long linked list cannot exhaust the Java stack.</li>
 * <li>With {@code userToString}, an object with a {@code toString} method written in tinyscript
 *     is shown as the string it returns, also inside arrays and other objects.</li>
 * </ul>
 */
public final class Display {

	public static final int MAX_DEPTH = 50;

	private final boolean userToString;
	// The objects currently being shown: the path from the outermost value to the current one
	private final Set<TSObject> path = Collections.newSetFromMap(new IdentityHashMap<>());

	public Display(boolean userToString) {
		this.userToString = userToString;
	}

	/**
	 * The display form of a value inside an object: strings are quoted.
	 */
	public String format(TSValue value) {
		if (!(value instanceof TSObject object))
			return value instanceof TSString ? value.toString() : value.asString();
		if (path.contains(object))
			return "[Circular]";
		if (userToString && object.get("toString") instanceof InterpretedFunction toString) {
			TSValue result = toString.apply(object, new TSValue[0]);
			if (result instanceof TSString s)
				return s.value();
		}
		if (path.size() >= MAX_DEPTH)
			return object instanceof ArrayObject ? "[Array]" : "[Object]";
		path.add(object);
		try {
			return object.display(this);
		}
		finally {
			path.remove(object);
		}
	}
}
