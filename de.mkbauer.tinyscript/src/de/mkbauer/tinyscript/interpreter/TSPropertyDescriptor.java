package de.mkbauer.tinyscript.interpreter;

public class TSPropertyDescriptor {
	
	private static final boolean DEFAULT_CONFIGURABLE = true;
	private static final boolean DEFAULT_WRITEABLE = true;
	private static final boolean DEFAULT_ENUMERABLE = true;
	
	private boolean configurable;
	
	private boolean writeable;
	
	private boolean enumerable;
	
	private TSValue value;
	
	public TSPropertyDescriptor() {
		initialize();
		this.value = TSValue.UNDEFINED;
	}
	
	public TSPropertyDescriptor(TSValue value) {
		initialize();
		this.value = value;
	}
	
	protected void initialize() {
		this.configurable = DEFAULT_CONFIGURABLE;
		this.writeable = DEFAULT_WRITEABLE;
		this.enumerable = DEFAULT_ENUMERABLE;
	}
	
	public boolean isConfigurable() {
		return configurable;
	}

	public void setConfigurable(boolean configurable) {
		this.configurable = configurable;
	}

	public boolean isWriteable() {
		return writeable;
	}

	public void setWriteable(boolean writeable) {
		this.writeable = writeable;
	}

	public boolean isEnumerable() {
		return enumerable;
	}

	public void setEnumerable(boolean enumerable) {
		this.enumerable = enumerable;
	}

	public TSValue getValue() {
		return value;
	}

	public void setValue(TSValue value) {
		this.value = value;
	}

	private TSObject toObject() {
		// TODO: Complete implementation
		return null;
	}
	
	public String toString() {
		return "{ value: " + value.asString() + " }";
	}

}
