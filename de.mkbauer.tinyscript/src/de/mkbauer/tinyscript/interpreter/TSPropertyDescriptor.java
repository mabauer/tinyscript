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

	public TSObject toObject(TinyscriptEngine engine) {
		TSObject propertyDescObject = new TSObject(engine, engine.getDefaultPrototype());
		propertyDescObject.put("writeable", new TSValue(isWriteable()));
		propertyDescObject.put("configurable", new TSValue(isConfigurable()));
		propertyDescObject.put("enumerable", new TSValue(isEnumerable()));		
		propertyDescObject.put("value", getValue());
		return propertyDescObject;
	}
	
	public static TSPropertyDescriptor fromObject(TSObject propertyDescObject) {
		TSPropertyDescriptor propertyDescriptor = new TSPropertyDescriptor();
		if (propertyDescObject.contains("writeable") && !propertyDescObject.get("writeable").asBoolean()) 
			propertyDescriptor.setWriteable(false);
		if (propertyDescObject.contains("configurable") && !propertyDescObject.get("configurable").asBoolean()) 
			propertyDescriptor.setConfigurable(false);
		if (propertyDescObject.contains("enumerable") && !propertyDescObject.get("enumerable").asBoolean()) 
			propertyDescriptor.setEnumerable(false);
		propertyDescriptor.setValue(propertyDescObject.get("value"));
		return propertyDescriptor;
	}
	
	public String toString() {
		return "{ value: " + value.asString() + " }";
	}

}
