package de.mkbauer.tinyscript.runtime.array;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.prototype.Filter;
import de.mkbauer.tinyscript.runtime.array.prototype.Map;
import de.mkbauer.tinyscript.runtime.array.prototype.Pop;
import de.mkbauer.tinyscript.runtime.array.prototype.Push;
import de.mkbauer.tinyscript.runtime.array.prototype.Shift;
import de.mkbauer.tinyscript.runtime.array.prototype.ToString;
import de.mkbauer.tinyscript.runtime.array.prototype.Unshift;

public class ArrayConstructor extends BuiltinConstructor {
	
	private static final String NAME = "Array";
	
	public ArrayConstructor(ExecutionVisitor ev) {
		super(ev);
		defineArrayPrototype();
		defineDefaultProperty(this, "isArray", new IsArray(ev));
	}
	
	private void defineArrayPrototype() {
		// TODO Add other Array methods
		defineDefaultProperty(getPrototypeProperty().asObject(), "toString", new ToString(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "push", new Push(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "pop", new Pop(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "unshift", new Unshift(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "shift", new Shift(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "map", new Map(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "filter", new Filter(ev));
	}
	
	@Override
	public TSValue apply(boolean asConstrcutor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		return new TSValue(new ArrayObject(ev));
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
