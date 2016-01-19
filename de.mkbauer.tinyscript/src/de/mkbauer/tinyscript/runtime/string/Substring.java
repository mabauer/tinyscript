package de.mkbauer.tinyscript.runtime.string;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.ResourceMonitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Substring extends BuiltinFunction {
	
	private static final String NAME = "substring";

	public Substring(ExecutionVisitor ev) {
		super(ev);
	}

	@Override
	public TSValue apply(boolean asConstructor, TSObject self,
			List<TSValue> args) {
		checkArgs(args);
		String result = "";
		if (self instanceof StringObject) {
			String str = TSObject.toString(ev, new TSValue(self));
			int start = TSObject.toInteger(ev, args.get(0)); 
			int end = str.length();
			if (args.size() > 1) {
				end = TSObject.toInteger(ev, args.get(1)); 
			}
			if (start < 0)
				start = 0;
			if (start >= str.length())
				start = str.length();
			if (end < 0)
				end = 0;
			if (end >= str.length()) 
				end = str.length();
			if (start <= end) 
				result = str.substring(start, end);
			else
				result = str.substring(end, start);
		}
		ResourceMonitor monitor = ev.getResourceMonitor();
		if (monitor != null)
			monitor.monitorStringCreation(result);
		return new TSValue(result);
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getLength() {
		return 1;
	}

}
