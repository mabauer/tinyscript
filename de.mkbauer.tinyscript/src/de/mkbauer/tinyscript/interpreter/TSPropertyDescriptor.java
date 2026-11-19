package de.mkbauer.tinyscript.interpreter;

public class TSPropertyDescriptor {
	
	private static final boolean DEFAULT_CONFIGURABLE = true;
	private static final boolean DEFAULT_WRITABLE = true;
	private static final boolean DEFAULT_ENUMERABLE = true;
	
	private boolean configurable;
	
	private boolean writable;
	
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
		this.writable = DEFAULT_WRITABLE;
		this.enumerable = DEFAULT_ENUMERABLE;
	}
	
	public boolean isConfigurable() {
		return configurable;
	}

	public void setConfigurable(boolean configurable) {
		this.configurable = configurable;
	}

	public boolean isWritable() {
		return writable;
	}

	public void setWritable(boolean writable) {
		this.writable = writable;
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
		propertyDescObject.put("writable", TSValue.of(isWritable()));
		propertyDescObject.put("configurable", TSValue.of(isConfigurable()));
		propertyDescObject.put("enumerable", TSValue.of(isEnumerable()));		
		propertyDescObject.put("value", getValue());
		return propertyDescObject;
	}
	
	public static TSPropertyDescriptor fromObject(TSObject propertyDescObject) {
		TSPropertyDescriptor propertyDescriptor = new TSPropertyDescriptor();
		if (propertyDescObject.contains("writable") && !propertyDescObject.get("writable").asBoolean()) 
			propertyDescriptor.setWritable(false);
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
