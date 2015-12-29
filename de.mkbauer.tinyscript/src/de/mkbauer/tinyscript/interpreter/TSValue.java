package de.mkbauer.tinyscript.interpreter;

/**
 * Represents values used in expressions in Tinyscript. 
 * @author markus.bauer
 *
 */
public class TSValue { // implements Comparable<TSValue> 
	
	public static final TSValue UNDEFINED = new TSValue();

	private Object value;
	
    public TSValue(Object v) {
        value = v;
        // only accept boolean, list, number or string types
        if(!(isBoolean() || isNumber() || isString() || isObject())) {
            throw new RuntimeException("invalid data type: " + v + " (" + v.getClass() + ")");
        }
    }
	
	private TSValue() {
		value = null;
	}
	
	public void assign(TSValue other) {
		value = other.value;
	}
	
	public String asString() {
		if (value == null) 
			return "UNDEFINED";
		if (isString()) 
			return (String)value;
		else if (isInt())
			return String.format("%d",  value);
		else if (isMathematicalInteger())
			return String.format("%.0f", value) ;
		else
			return value.toString();
	}
	
	public boolean isString() {
		return (value instanceof String);
	}
	
	public int asInt() {
		return ((Number)value).intValue();
	}
	
	public boolean isInt() {
		return (value instanceof Integer);
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
		return (value instanceof Double);
	}
	
	public boolean isNumber() {
		return isDouble() || isInt();
	}
	
	public boolean asBoolean() {
		if (value==null)
			return false;
		if (isObject())
			return true;
		if (isNumber()) 
			return (asDouble() == 0.0);  // TODO: Use delta
		return ((Boolean)value).booleanValue();
	}
	
	public boolean isBoolean() {
		return (value instanceof Boolean);
	}
	
	public boolean isObject() {
		return (value instanceof TSObject);
	}
	
	public TSObject asObject() {
		return (TSObject) value;
	}
	
	public boolean equals(TSValue other) {
		// TODO: Check with ECMAScript Specs
		if (value == null) {
			if (other == null) {
				return true;
			}
			else {
				return false;
			}
		}
		return value.equals(other.value);
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
        return value.hashCode();
    }
	
	@Override
	public String toString() {
		if (value != null) {
			return value.toString(); // asString();
		}
		else {
			return "UNDEFINED";
		}
	}
}


