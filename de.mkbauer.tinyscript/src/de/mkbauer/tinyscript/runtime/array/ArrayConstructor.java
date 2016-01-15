package de.mkbauer.tinyscript.runtime.array;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.array.ToString;

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
	}
	
	@Override
	public TSValue apply(boolean asConstrcutor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		ev.checkAndIncreaseObjectCreations();
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
