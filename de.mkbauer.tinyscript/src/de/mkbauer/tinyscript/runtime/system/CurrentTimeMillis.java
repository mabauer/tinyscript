package de.mkbauer.tinyscript.runtime.system;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.ExecutionVisitor;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class CurrentTimeMillis extends BuiltinFunction {
		
		private static final String NAME = "currentTimeMillis";
		
		public CurrentTimeMillis(ExecutionVisitor ev) {
			super(ev);
		}

		@Override
		public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
			checkArgs(args);
			return new TSValue(java.lang.System.currentTimeMillis());
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