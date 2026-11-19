package de.mkbauer.tinyscript.runtime.string;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class StringConstructor extends BuiltinConstructor {

	public final static String NAME = "String";

	public StringConstructor(TinyscriptEngine engine) {
		super(engine);
		TSObject stringPrototype = getPrototypeProperty().asObject();
		stringPrototype.defineBuiltinMethod("toString", this::stringToString, 0);
		stringPrototype.defineBuiltinMethod("charAt", this::charAt, 1);
		stringPrototype.defineBuiltinMethod("substring", this::substring, 1);
		stringPrototype.defineBuiltinMethod("indexOf", this::indexOf, 1);
	}

	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		if (args.length > 0) {
			String str = args[0].asString();
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

	// String.prototype methods

	private TSValue stringToString(TSObject self, TSValue[] args) {
		if (!(self instanceof StringObject))
			throw new TinyscriptTypeError("Function String.prototype.toString only works for String objects.");
		return new TSValue(((StringObject) self).getValue());
	}

	private TSValue charAt(TSObject self, TSValue[] args) {
		String result = "";
		if (self instanceof StringObject) {
			String str = TSObject.toString(engine, new TSValue(self));
			int pos = TSObject.toInteger(engine, args[0]);
			if (pos >= 0 && pos < str.length()) {
				result = str.substring(pos, pos + 1);
			}
		}
		return new TSValue(result);
	}

	private TSValue substring(TSObject self, TSValue[] args) {
		String result = "";
		if (self instanceof StringObject) {
			String str = TSObject.toString(engine, new TSValue(self));
			int start = TSObject.toInteger(engine, args[0]);
			int end = (args.length > 1) ? TSObject.toInteger(engine, args[1]) : str.length();
			// Clamp both indices to the string and swap them if necessary (like JavaScript)
			start = Math.clamp(start, 0, str.length());
			end = Math.clamp(end, 0, str.length());
			result = str.substring(Math.min(start, end), Math.max(start, end));
		}
		engine.recordStringCreation(result);
		return new TSValue(result);
	}

	private TSValue indexOf(TSObject self, TSValue[] args) {
		if (self instanceof StringObject) {
			return new TSValue(((StringObject) self).getValue().indexOf(args[0].asString()));
		}
		return new TSValue(-1);
	}

}
