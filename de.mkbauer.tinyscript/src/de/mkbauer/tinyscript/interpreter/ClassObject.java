package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.tinyscript.ts.ClassDeclaration;

/**
 * A class: like Javascript, a constructor function (the class's constructor method, if it
 * has one) whose prototype holds the methods. Unlike a constructor function, it can only be
 * called with new.
 */
public class ClassObject extends InterpretedFunction {

	private final ClassDeclaration declaration;

	public ClassObject(TinyscriptEngine engine, ClassDeclaration declaration) {
		super(engine);
		this.declaration = declaration;
	}

	public ClassDeclaration getDeclaration() {
		return declaration;
	}

	/**
	 * A plain call, Num(3) or items.map(Num): like Javascript, a TypeError.
	 */
	@Override
	public TSValue apply(TSObject self, TSValue[] args) {
		throw new TinyscriptTypeError(getName() + " is a class: use new " + getName() + "(…)");
	}

	/**
	 * new Num(3): runs the constructor (if there is one) on the new object.
	 */
	public void initialize(TSObject self, TSValue[] args) {
		super.apply(self, args);
	}

	@Override
	public String getName() {
		return declaration.getId().getName();
	}

	// print(Num) shows [class Num], as node does
	@Override
	protected String label() {
		return "[class " + getName() + "]";
	}

}
