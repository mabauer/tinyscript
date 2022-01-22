package de.mkbauer.tinyscript.interpreter;

import java.util.List;


public class BuiltinFunction extends Function {
	
	private String name;
	private int length;
	private BuiltinFunctionImplementation implementation;
	
	protected BuiltinFunction(TinyscriptEngine engine) {
		super(engine);
		implementation = null;
	}
	
	public void checkArgs(List <TSValue> args) {
		if (args.size() < getLength()) {
			throw new TinyscriptArgumentError("Builtin function " + getName() + " requires at least " + getLength() + " arguments.", 
					engine.getCurrentContext().currentExpression);
		}
	}
	
	public void enforceSandboxing() {
		if (engine.isSandboxed()) {
			throw new TinyscriptUnsupportedOperationError("Builtin function " + getName() + " is not available when running sandboxed.",
					engine.getCurrentContext().currentExpression);
		}
	}
	
	void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	public void defineImplementation(BuiltinFunctionImplementation implementation) {
		this.implementation = implementation;
		
	}

	@Override
	public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
		checkArgs(args);
		if (implementation == null)
			return TSValue.UNDEFINED;
		return implementation.apply(asConstructor, self, args);
	}
	
	void setLength(int length) {
		this.length = length;
	}

	@Override
	public int getLength() {
		return length;
	}



}
