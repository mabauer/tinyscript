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
	

	public final static String NAME = "String";
	
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
	public TSValue apply(TSObject self, List<TSValue> args) {
		if (args.size() > 0) {
			String str = args.get(0).asString();
			if (self instanceof StringObject) {
				((StringObject) self).setValue(str);
			}
			else {
				return new TSValue(str);
			}
		}
		return new TSValue(self);
	}
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public TSObject createObject() {
		return createObject(null);
	}
	
	public StringObject createObject(TSValue value) {
		StringObject newObject = new StringObject(engine, value);
		newObject.setPrototype(getPrototypeProperty().asObject());
		newObject.defineDefaultProperty("constructor", new TSValue(this));
		return newObject;
	}

}
