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
		defineDefaultProperty(this, "isArray", new IsArray(engine));
	}
	
	private void defineArrayPrototype() {
		// TODO Add other Array methods
		defineDefaultProperty(getPrototypeProperty().asObject(), "toString", new ToString(engine));
		defineDefaultProperty(getPrototypeProperty().asObject(), "push", new Push(engine));
		defineDefaultProperty(getPrototypeProperty().asObject(), "pop", new Pop(engine));
		defineDefaultProperty(getPrototypeProperty().asObject(), "unshift", new Unshift(engine));
		defineDefaultProperty(getPrototypeProperty().asObject(), "shift", new Shift(engine));
		defineDefaultProperty(getPrototypeProperty().asObject(), "map", new Map(engine));
		defineDefaultProperty(getPrototypeProperty().asObject(), "filter", new Filter(engine));
		defineDefaultProperty(getPrototypeProperty().asObject(), "join", new Join(engine));
	}
	
	@Override
	public TSValue apply(boolean asConstrcutor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		return new TSValue(new ArrayObject(engine));
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
