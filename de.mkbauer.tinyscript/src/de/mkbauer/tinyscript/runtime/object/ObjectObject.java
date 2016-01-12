package de.mkbauer.tinyscript.runtime.object;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinType;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;


public class ObjectObject extends BuiltinType {
	
	public ObjectObject(ExecutionVisitor ev) {
		super(ev);
		
		initializeDefaultPrototype(ev, ev.getDefaultPrototype());
		setPrototypeProperty(ev.getDefaultPrototype());
	}
	
	public static void initializeDefaultPrototype(ExecutionVisitor ev, TSObject defaultPrototype) {
		defineDefaultProperty(defaultPrototype, "toString", new ToString(ev));
		defineDefaultProperty(defaultPrototype, "hasOwnProperty", new HasOwnProperty(ev));
	}
	
	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return "Object";
	}

	@Override
	public int getLength() {
		// TODO Find out why?
		return 1;
	}



}
