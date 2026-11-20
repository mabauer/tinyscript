package de.mkbauer.tinyscript.runtime.error;

import de.mkbauer.tinyscript.interpreter.BuiltinConstructor;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TinyscriptTypeError;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSString;
import de.mkbauer.tinyscript.interpreter.TSValue;

/**
 * Error(message), for throw new Error("..."): an object with a message, as in Javascript
 * (without the stack and the subclasses like TypeError).
 */
public class ErrorConstructor extends BuiltinConstructor {

	public static final String NAME = "Error";

	public ErrorConstructor(TinyscriptEngine engine) {
		super(engine);
		setMaxArgs(1);
		TSObject errorPrototype = getPrototypeProperty().asObject();
		errorPrototype.defineDefaultProperty("constructor", this);
		errorPrototype.defineDefaultProperty("name", NAME);
		errorPrototype.defineDefaultProperty("message", "");
		errorPrototype.defineBuiltinMethod("toString", (self, args) -> TSValue.of(self.toString()), 0);
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
		return new ErrorObject(engine, getPrototypeProperty().asObject());
	}

	// new Error(message); like Javascript, Error(message) without new works the same
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		ErrorObject error = self instanceof ErrorObject e ? e : (ErrorObject) createObject();
		if (args.length > 0 && args[0] != TSValue.UNDEFINED) {
			if (!(args[0] instanceof TSString message))
				throw new TinyscriptTypeError("Error() needs a string as message, not " + args[0].typeOf());
			error.defineDefaultProperty("message", message.value());
		}
		return error;
	}

}
