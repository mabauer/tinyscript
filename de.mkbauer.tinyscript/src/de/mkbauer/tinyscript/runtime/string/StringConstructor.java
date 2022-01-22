package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.runtime.string.prototype.CharAt;
import de.mkbauer.tinyscript.runtime.string.prototype.IndexOf;
import de.mkbauer.tinyscript.runtime.string.prototype.Substring;
import de.mkbauer.tinyscript.runtime.string.prototype.ToString;

public class StringConstructor extends BuiltinConstructor {
	

	public StringConstructor(TinyscriptEngine engine) {
		super(engine);
		defineStringPrototype();
	}
	
	private void defineStringPrototype() {
		// TODO Add other String methods
		TSObject stringPrototypeObject = getPrototypeProperty().asObject();
		stringPrototypeObject.defineDefaultProperty("toString", new ToString(engine));
		stringPrototypeObject.defineDefaultProperty("charAt", new CharAt(engine));
		stringPrototypeObject.defineDefaultProperty("substring", new Substring(engine));
		stringPrototypeObject.defineDefaultProperty("indexOf", new IndexOf(engine));
	}
	
	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		// TODO Handle arguments
		if (args.size() > 0)
			return new TSValue(new StringObject(engine, args.get(0)));
		else
			return new TSValue(new StringObject(engine));
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
