package de.mkbauer.tinyscript.interpreter;

import java.util.Objects;

import de.mkbauer.tinyscript.runtime.array.ArrayObject;
import de.mkbauer.tinyscript.runtime.string.StringObject;

/**
 * Represents values used in expressions in Tinyscript. 
 * @author markus.bauer
 *
 */
public class TSValue { // implements Comparable<TSValue> 
	
	public static final TSValue UNDEFINED = new TSValue();
	public static final TSValue NULL = new TSValue();
	public static final TSValue NAN = new TSValue(Double.NaN);

	private Object value;
	
    public TSValue(Object v) {
        value = v;
        if (value instanceof Number)
        	value = ((Number) value).doubleValue();
        // only accept boolean, list, number or string types
        /* 
        if(!(isBoolean() || isDouble() || isString() || isObject())) {
            throw new RuntimeException("invalid data type: " + v + " (" + v.getClass() + ")");
        }
    	*/
    }
	
	private TSValue() {
		value = null;
	}
	
	public void assign(TSValue other) {
		value = other.value;
	}
	
	public String asString() {
		if (this == TSValue.UNDEFINED) 
			return "Undefined";
		if (this == TSValue.NULL) 
			return "Null";
		if (isPrimitiveString()) 
			return (String)value;
		else if (isMathematicalInteger())
			return String.format("%.0f", value) ;
		else
			return value.toString();
	}
	
	public boolean isString() {
		return (value != null && (value instanceof String || value instanceof StringObject));
	}
	
	public boolean isPrimitiveString() {
		return (value != null && value instanceof String);
	}
	
	public int asInt() {
		return ((Number)value).intValue();
	}
	
	public boolean isMathematicalInteger() {
		if (!isDouble())
			return false;
		return (Math.rint(asDouble()) == asDouble()); 
	}
	
	public double asDouble() {
		return ((Number)value).doubleValue();
	}
	
	public boolean isDouble() {
		return (value != null && value instanceof Double);
	}
	
	public boolean isNumber() {
		return isDouble() && !(Double.isNaN(asDouble()));
	}
	
	public boolean isInfinity() {
		return isNumber() && Double.isInfinite(asDouble());
	}
	
	public boolean asBoolean() {
		if (value == null)
			return false;
		if (value instanceof Boolean b)
			return b;
		if (value instanceof Double d)
			return d != 0.0 && !d.isNaN();
		if (value instanceof String s)
			return !s.isEmpty();
		return true;
	}
	
	public boolean isBoolean() {
		return (value != null && value instanceof Boolean);
	}
	
	public boolean isObject() {
		return (value != null && value instanceof TSObject);
	}
	
	public boolean isArray() {
		return (value != null && value instanceof ArrayObject);
	}
	
	public TSObject asObject() {
		return (TSObject) value;
	}
	
	public ArrayObject asArray() {
		return (ArrayObject) value;
	}
	
	public boolean isCallable() {
		if (value != null && value instanceof Function) 
			return true;
		return false;
	}
	
	public boolean isObjectCoercible() {
		return (value!=null);
	}
	
	public String typeOf() {
		if (this == TSValue.UNDEFINED)
			return "undefined";
		if (this == TSValue.NULL)
			return "object";
		if (isBoolean())
			return "boolean";
		if (isDouble())
			return "number";
		if (isPrimitiveString())
			return "string";
		if (isCallable())
			return "function";
		return "object";
	}
	
	/**
	 * Tinyscript's {@code ==}: no type coercion, but {@code null == undefined},
	 * and numbers compare like in Javascript ({@code NaN != NaN}, {@code 0 == -0}).
	 */
	public boolean tinyscriptEquals(TSValue other) {
		if (value == null)
			return other.value == null;
		if (isDouble() && other.isDouble())
			return asDouble() == other.asDouble();
		if (isString() && other.isString())
			return toString().equals(other.toString());
		return value.equals(other.value);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		if (!(other instanceof TSValue otherValue) || value == null)
			return false;
		return value.equals(otherValue.value);
	}

/*	
    @Override
	public int compareTo(TSValue o) {
		// TODO Auto-generated method stub
		return 0;
	}
*/
	
	@Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
	
	@Override
	public String toString() {
		if (this == TSValue.UNDEFINED) 
			return "Undefined";
		if (this == TSValue.NULL) 
			return "Null";
		if (isString()) 
			return "'" + value.toString() + "'";
		else if (isMathematicalInteger())
			return String.format("%.0f", value) ;
		else
			return value.toString();
	}
}


