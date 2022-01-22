package de.mkbauer.tinyscript.runtime.math;

import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class Random extends BuiltinFunction {
		
		private static final String NAME = "random";
		
		public Random(TinyscriptEngine engine) {
			super(engine);
		}

		@Override
		public TSValue apply(TSObject self, List<TSValue> args) {
			checkArgs(args);
			return new TSValue(java.lang.Math.random());
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