package de.mkbauer.tinyscript.runtime.error;

import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.interpreter.Display;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;

/**
 * An Error object: a message (and a name, from Error.prototype).
 */
public class ErrorObject extends TSObject {

	ErrorObject(TinyscriptEngine engine, TSObject prototype) {
		super(engine, prototype);
	}

	// The error this object was caught as (catch (e)), so that throw e; throws it again
	private TinyscriptRuntimeException origin;

	public TinyscriptRuntimeException getOrigin() {
		return origin;
	}

	public void setOrigin(TinyscriptRuntimeException origin) {
		this.origin = origin;
	}

	public String getMessage() {
		return get("message").asString();
	}

	// Like Javascript's Error.prototype.toString: "Error: message", or "Error" without a message
	@Override
	protected String display(Display display) {
		String name = get("name").asString();
		return getMessage().isEmpty() ? name : name + ": " + getMessage();
	}

}
