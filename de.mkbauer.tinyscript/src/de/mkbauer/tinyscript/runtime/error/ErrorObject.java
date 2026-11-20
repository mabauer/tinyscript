package de.mkbauer.tinyscript.runtime.error;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;

/**
 * An Error object: a message (and a name, from Error.prototype).
 */
public class ErrorObject extends TSObject {

	ErrorObject(TinyscriptEngine engine, TSObject prototype) {
		super(engine, prototype);
	}

	public String getMessage() {
		return get("message").asString();
	}

	// Like Javascript's Error.prototype.toString: "Error: message", or "Error" without a message
	@Override
	public String toString() {
		String name = get("name").asString();
		return getMessage().isEmpty() ? name : name + ": " + getMessage();
	}

}
