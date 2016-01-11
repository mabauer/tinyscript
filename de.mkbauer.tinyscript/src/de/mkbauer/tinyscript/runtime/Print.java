package de.mkbauer.tinyscript.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.GlobalExecutionContext;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Print extends BuiltinFunction {

	public Print(GlobalExecutionContext globalContext) {
		super(globalContext);
	}

	@Override
	public TSValue apply(TSObject self, List<TSValue> args) {
		checkArgs(args);
		OutputStream os = globalContext.getStdOut();
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
	public int getLength() {
		return 0;
	}

}
