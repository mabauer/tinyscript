package de.mkbauer.tinyscript.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Print extends BuiltinFunction {
	
	private final static String NAME = "print";

	public Print(TinyscriptEngine engine) {
		super(engine);
	}

	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		OutputStream os = engine.getStdOut();
		try {
			if (os != null) {
				OutputStreamWriter writer = new OutputStreamWriter(os);
				for (TSValue arg: args) {
					writer.write(arg.asString());
				}
				writer.write(System.lineSeparator());
				writer.flush();
			}
		}
		catch(IOException e) {
			// TODO: Do something appropriate, e.g. log it
		}
		return TSValue.UNDEFINED;
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
