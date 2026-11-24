package de.mkbauer.tinyscript.runtime;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.eclipse.xtext.resource.XtextResourceSet;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;

/**
 * The members of the builtins and the number of arguments their functions take, read once from
 * the builtin objects of a TinyscriptEngine (not a second list), for the checks before the run:
 * Math.sqr, JSON.parse(s, f) or parseInt() are found there.
 */
public final class BuiltinSignatures {

	/**
	 * A builtin function takes min to max arguments (max {@link #VARIADIC}: any number); new can
	 * call it if it is a constructor (Map, Error).
	 */
	public record Signature(int min, int max, boolean constructor) {
		public static final int VARIADIC = BuiltinFunction.VARIADIC;
	}

	/**
	 * The members of a builtin value: the signature of a builtin function, null for another
	 * member (Math.PI, Number.prototype).
	 */
	public record Members(Map<String, Signature> members) {
		public boolean has(String name) {
			return members.containsKey(name);
		}

		public Signature signature(String name) {
			return members.get(name);
		}
	}

	/** The kinds of literals whose methods are known: "number", "string", "array". */
	public static final String NUMBER = "number", STRING = "string", ARRAY = "array";

	private final Map<String, Signature> functions;
	private final Map<String, Members> globals;
	private final Map<String, Members> literals;

	private static volatile BuiltinSignatures instance;

	private BuiltinSignatures(TinyscriptEngine engine) {
		Map<String, Signature> functions = new HashMap<>();
		Map<String, Members> globals = new HashMap<>();
		TSObject global = engine.getGlobalContext().getGlobalObject();
		for (String name : global.getOwnPropertyNames()) {
			TSValue value = global.get(name);
			if (value instanceof BuiltinFunction function)
				functions.put(name, signature(function));
			if (value instanceof TSObject object)
				globals.put(name, members(object));
		}
		Map<String, Members> literals = new HashMap<>();
		// A number has only the methods of Number.prototype (toFixed, toString), see TinyscriptEngine
		Map<String, Signature> numberMethods = new LinkedHashMap<>(members((TSObject) ((TSObject) global.get("Number")).get("prototype"), true).members());
		numberMethods.remove("constructor");
		literals.put(NUMBER, new Members(Collections.unmodifiableMap(numberMethods)));
		// Strings and arrays have their length of their own, not from the prototype
		literals.put(STRING, withLength(members((TSObject) ((TSObject) global.get("String")).get("prototype"))));
		literals.put(ARRAY, withLength(members((TSObject) ((TSObject) global.get("Array")).get("prototype"))));
		this.functions = Collections.unmodifiableMap(functions);
		this.globals = Collections.unmodifiableMap(globals);
		this.literals = Collections.unmodifiableMap(literals);
	}

	/**
	 * The signatures, read once (with a resource set of its own for the engine's builtins, made
	 * only then).
	 */
	public static BuiltinSignatures get(Supplier<XtextResourceSet> resourceSet) {
		BuiltinSignatures result = instance;
		if (result == null) {
			synchronized (BuiltinSignatures.class) {
				if (instance == null)
					instance = new BuiltinSignatures(new TinyscriptEngine(resourceSet.get()));
				result = instance;
			}
		}
		return result;
	}

	/** The signature of a global builtin function (parseInt, range), or null. */
	public Signature function(String name) {
		return functions.get(name);
	}

	/** The members of a global builtin value (Math, JSON, Number), or null. */
	public Members global(String name) {
		return globals.get(name);
	}

	/** The members of a literal of the kind {@link #NUMBER}, {@link #STRING} or {@link #ARRAY}. */
	public Members literal(String kind) {
		return literals.get(kind);
	}

	private static Members withLength(Members members) {
		Map<String, Signature> result = new LinkedHashMap<>(members.members());
		result.put("length", null);
		return new Members(Collections.unmodifiableMap(result));
	}

	private static Signature signature(BuiltinFunction function) {
		return new Signature(function.getLength(), function.getMaxArgs(), function.isConstructor());
	}

	// The members of an object and its prototypes
	private static Members members(TSObject object) {
		return members(object, false);
	}

	private static Members members(TSObject object, boolean ownOnly) {
		Map<String, Signature> members = new LinkedHashMap<>();
		for (TSObject o = object; o != null; o = ownOnly ? null : o.getPrototype()) {
			for (String name : o.getOwnPropertyNames()) {
				if (!members.containsKey(name)) {
					TSValue value = o.get(name);
					members.put(name, value instanceof BuiltinFunction function ? signature(function) : null);
				}
			}
		}
		// Every object has its prototype as __proto__ (not among the property names)
		members.putIfAbsent("__proto__", null);
		return new Members(Collections.unmodifiableMap(members));
	}
}
