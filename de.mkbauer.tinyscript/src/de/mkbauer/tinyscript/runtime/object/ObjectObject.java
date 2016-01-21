package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.object.prototype.HasOwnProperty;
import de.mkbauer.tinyscript.runtime.object.prototype.ToString;


public class ObjectObject extends BuiltinConstructor {
	
	private final static String NAME = "Object";
	
	public ObjectObject(ExecutionVisitor ev) {
		super(ev);
		
		initializeDefaultPrototype(ev, ev.getDefaultPrototype());
		setPrototypeProperty(ev.getDefaultPrototype());
		defineDefaultProperty(this, "keys", new Keys(ev));
	}
	
	public static void initializeDefaultPrototype(ExecutionVisitor ev, TSObject defaultPrototype) {
		defineDefaultProperty(defaultPrototype, "toString", new ToString(ev));
		defineDefaultProperty(defaultPrototype, "hasOwnProperty", new HasOwnProperty(ev));
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		return new TSValue(new TSObject(ev, ev.getDefaultPrototype()));
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		// TODO Find out why?
		return 1;
	}


}
