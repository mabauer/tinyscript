package de.mkbauer.tinyscript.interpreter;

import org.eclipse.emf.ecore.EObject;


public class BuiltinFunction extends Function {

	/** The maximum number of arguments of a builtin that takes any number of them. */
	public static final int VARIADIC = Integer.MAX_VALUE;

	private String name;
	private int length;
	private int maxArgs = VARIADIC;
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

	protected void setMaxArgs(int maxArgs) {
		this.maxArgs = maxArgs;
	}

	/**
	 * Checks the number of arguments of a call written in a script: more than the builtin
	 * takes is a TypeError (they would be ignored silently). Callbacks, e.g. of map, are not
	 * checked: like in Javascript, they get (item, index, array) and use what they need.
	 */
	public void checkMaxArgs(int count, EObject call) {
		if (count <= maxArgs)
			return;
		String takes = switch (maxArgs) {
			case 0 -> "takes no arguments";
			case 1 -> "takes at most 1 argument";
			default -> "takes at most " + maxArgs + " arguments";
		};
		throw new TinyscriptTypeError(getName() + "() " + takes + ", not " + count, call);
	}

	@Override
	public int getLength() {
		return length;
	}



	@Override
	public String kind() {
		return "the builtin function " + getName();
	}

}
