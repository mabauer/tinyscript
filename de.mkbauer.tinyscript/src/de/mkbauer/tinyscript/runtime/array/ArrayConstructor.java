package de.mkbauer.tinyscript.runtime.array;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.prototype.Filter;
import de.mkbauer.tinyscript.runtime.array.prototype.Join;
import de.mkbauer.tinyscript.runtime.array.prototype.Map;
import de.mkbauer.tinyscript.runtime.array.prototype.Pop;
import de.mkbauer.tinyscript.runtime.array.prototype.Push;
import de.mkbauer.tinyscript.runtime.array.prototype.Shift;
import de.mkbauer.tinyscript.runtime.array.prototype.ToString;
import de.mkbauer.tinyscript.runtime.array.prototype.Unshift;

public class ArrayConstructor extends BuiltinConstructor {
	
	private static final String NAME = "Array";
	
	public ArrayConstructor(TinyscriptEngine engine) {
		super(engine);
		defineArrayPrototype();
		defineDefaultProperty("isArray", new IsArray(engine));
	}
	
	private void defineArrayPrototype() {
		// TODO Add other Array methods
		TSObject arrayPrototype = getPrototypeProperty().asObject();
		arrayPrototype.defineDefaultProperty("toString", new ToString(engine));
		arrayPrototype.defineDefaultProperty("push", new Push(engine));
		arrayPrototype.defineDefaultProperty("pop", new Pop(engine));
		arrayPrototype.defineDefaultProperty("unshift", new Unshift(engine));
		arrayPrototype.defineDefaultProperty("shift", new Shift(engine));
		arrayPrototype.defineDefaultProperty("map", new Map(engine));
		arrayPrototype.defineDefaultProperty("filter", new Filter(engine));
		arrayPrototype.defineDefaultProperty("join", new Join(engine));
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

}
