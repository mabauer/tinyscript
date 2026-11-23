package de.mkbauer.tinyscript.interpreter;

import java.util.List;

/**
 * An interface, interface Shape { area(); scale(factor); }: the names of the methods that a class
 * with implements Shape must have. A value of its own, so that modules can export and import it;
 * it prints as [interface Shape].
 */
public class InterfaceObject extends TSObject {

	private final String name;
	private final List<String> methods;

	public InterfaceObject(TinyscriptEngine engine, String name, List<String> methods) {
		super(engine, engine.getDefaultPrototype());
		this.name = name;
		this.methods = List.copyOf(methods);
	}

	public String getName() {
		return name;
	}

	public List<String> getMethods() {
		return methods;
	}

	@Override
	protected String display(Display display) {
		return "[interface " + name + "]";
	}
}
