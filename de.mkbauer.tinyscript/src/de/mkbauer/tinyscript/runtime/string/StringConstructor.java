package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.string.prototype.CharAt;
import de.mkbauer.tinyscript.runtime.string.prototype.IndexOf;
import de.mkbauer.tinyscript.runtime.string.prototype.Substring;
import de.mkbauer.tinyscript.runtime.string.prototype.ToString;

public class StringConstructor extends BuiltinConstructor {
	

	public StringConstructor(ExecutionVisitor ev) {
		super(ev);
		defineStringPrototype();
	}
	
	private void defineStringPrototype() {
		// TODO Add other String methods
		defineDefaultProperty(getPrototypeProperty().asObject(), "toString", new ToString(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "charAt", new CharAt(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "substring", new Substring(ev));
		defineDefaultProperty(getPrototypeProperty().asObject(), "indexOf", new IndexOf(ev));
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		if (args.size() > 0)
			return new TSValue(new StringObject(ev, args.get(0)));
		else
			return new TSValue(new StringObject(ev));
	}
	
	@Override
	public String getName() {
		return "String";
	}

	@Override
	public int getLength() {
		return 0;
	}	

}
