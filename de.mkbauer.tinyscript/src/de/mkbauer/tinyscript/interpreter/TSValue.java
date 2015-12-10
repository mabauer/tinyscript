package de.mkbauer.tinyscript.interpreter;

public class TSValue { // implements Comparable<TSValue> 
	
	public static final TSValue UNDEFINED = new TSValue();

	private Object value;
	
    public TSValue(Object v) {
        value = v;
        // only accept boolean, list, number or string types
        if(!(isBoolean() || isNumber() || isString())) {
            throw new RuntimeException("invalid data type: " + v + " (" + v.getClass() + ")");
        }
    }
	
	private TSValue() {
		value = null;
	}
	
	public String asString() {
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
		if ((isNumber()) && (asDouble() == 0.0)) // TODO: Use delta
			return false;
		return ((Boolean)value).booleanValue();
	}
	
	public boolean isBoolean() {
		return (value instanceof Boolean);
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
	
	public String toString() {
		if (value != null) {
			return asString();
		}
		else {
			return "NULL";
		}
	}
}


