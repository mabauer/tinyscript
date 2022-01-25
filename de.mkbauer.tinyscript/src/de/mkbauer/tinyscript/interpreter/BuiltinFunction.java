package de.mkbauer.tinyscript.interpreter;


public class BuiltinFunction extends Function {
	
	private String name;
	private int length;
	private BuiltinFunctionImplementation implementation;
	
	protected BuiltinFunction(TinyscriptEngine engine) {
		super(engine);
		implementation = null;
	}
	
	public void checkArgs(TSValue[] args) {
		if (args.length < getLength()) {
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
	public TSValue apply(TSObject self, TSValue[] args) {
		checkArgs(args);
		if (implementation == null)
			return TSValue.UNDEFINED;
		return implementation.apply(self, args);
	}
	
	void setLength(int length) {
		this.length = length;
	}

	@Override
	public int getLength() {
		return length;
	}



}
