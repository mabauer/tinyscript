package de.mkbauer.tinyscript.runtime.math;

import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;

import de.mkbauer.tinyscript.interpreter.BuiltinFunctionImplementation;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class MathObject extends TSObject {
	
	public MathObject(TinyscriptEngine engine) {
		super(engine, engine.getDefaultPrototype());
		
		defineDefaultProperty("PI", java.lang.Math.PI);
		defineBuiltinMethod("sqrt", 
				(self, args) -> { 
					return new TSValue(java.lang.Math.sqrt(TSObject.toNumber(engine, args[0])));
				}, 
				1);
		defineDefaultProperty("round", engine.defineBuiltinFunction("round", 
				new BuiltinFunctionImplementation() {

					@Override
					public TSValue apply(TSObject self, TSValue[] args) {
						return new TSValue(java.lang.Math.round(TSObject.toNumber(engine, args[0])));
					}
					
				}, 
				1));
		defineDefaultProperty("random", new Random(engine));
	}
	
}
