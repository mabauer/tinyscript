package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.util.WeakHashMapWithCallBack;

import org.apache.log4j.Logger;

public class ResourceMonitor implements WeakHashMapWithCallBack.OnExpungeListener<Integer> {
	
	public static final int TSVALUE_SIZE = 8;
	public static final int CHAR_SIZE = 2;
	
	private ResourceLimits resourceLimits;
	private ResourceConsumption resourceConsumption;
	private ResourceConsumption totalResourceConsumption;
	private boolean useObjectTracking;
	
	long startTime;
	
	private WeakHashMapWithCallBack<Object, Integer> objects; 
	
	private long totalMemory = 0;
	
	private final static Logger logger = Logger.getLogger(ResourceMonitor.class);
	
	public ResourceMonitor() {
		resourceLimits = ResourceLimits.UNLIMITED;
		useObjectTracking = false;
		totalResourceConsumption = new ResourceConsumption();
	}
	
	public void start() {
		if (useObjectTracking && objects == null) {
			objects = new WeakHashMapWithCallBack<Object, Integer>();
			objects.setOnExpungeListener(this);
		}
		resourceConsumption = new ResourceConsumption();
		startTime = System.nanoTime();
	}
	
	public void stop() {
		final long duration = (System.nanoTime() - startTime) / 1000000;
		resourceConsumption.executionTime = duration; 
		totalResourceConsumption.add(resourceConsumption);
	}
	
	public void configureLimits(ResourceLimits limits) {
		resourceLimits = limits;
		if (resourceLimits.maxObjects > 0) 
			useObjectTracking = true;
		if (resourceLimits.maxMemory > 0)
			useObjectTracking = true;
	}
	
	public void enableObjectTracking() {
		useObjectTracking = true;
	}
	
	public ResourceConsumption getLastResourceConsumption() {
		return resourceConsumption;
	}
	
	public ResourceConsumption getTotalResourceConsumption() {
		return totalResourceConsumption;
	}
	
	public void monitorStatements() {
		resourceConsumption.statements++;
		if (resourceLimits.maxStatements > 0 && resourceConsumption.statements > resourceLimits.maxStatements) {
			throw new TinyscriptResourceLimitViolation("Statement limit reached");
		}
	}
	
	public void monitorObjectCreation(TSObject object) {
		resourceConsumption.objectCreations++;
		if (resourceLimits.maxObjectCreations > 0 && resourceConsumption.objectCreations > resourceLimits.maxObjectCreations) {
			throw new TinyscriptResourceLimitViolation("Object creation limit reached");
		}
		monitorObjectSizeChange(object);		
	}
	
	public void monitorObjectSizeChange(TSObject object) {
		if (resourceLimits.maxObjectSize > 0 && object.getObjectSize() > resourceLimits.maxObjectSize) 
			throw new TinyscriptResourceLimitViolation("Object size limit reached");
		if (useObjectTracking) {
			trackObject(object);
			checkObjectsAndMemoryConsumption();
		}
	}
	
	public void monitorStringCreation(String string) {
		resourceConsumption.objectCreations++;
		if (resourceLimits.maxStringLength > 0 && string.length() > resourceLimits.maxStringLength) 
			throw new TinyscriptResourceLimitViolation("String length limit reached");
		if (useObjectTracking) {
			trackString(string);
			checkObjectsAndMemoryConsumption();	
		}
	}
	
	public void checkCallDepth(int callDepth) {
		if (callDepth > resourceConsumption.callDepth) {
			resourceConsumption.callDepth = callDepth;
		}
		if (resourceLimits.maxCallDepth >0 && resourceConsumption.callDepth > resourceLimits.maxCallDepth) {
			throw new TinyscriptResourceLimitViolation("Call depth limit reached");
		}
	}
	
	private void checkObjectsAndMemoryConsumption() {
		int currentObjects = countObjects();
		resourceConsumption.objects = currentObjects;
		if (currentObjects > resourceConsumption.objectsMax)
			resourceConsumption.objectsMax = currentObjects;
		if (resourceLimits.maxObjects > 0 && currentObjects > resourceLimits.maxObjects) {
			throw new TinyscriptResourceLimitViolation("Object limit reached");
		}
		long currentMemory = memory();
		resourceConsumption.memory = currentMemory;
		if ( currentMemory > resourceConsumption.memoryMax)
			resourceConsumption.memoryMax = currentMemory;
		if (resourceLimits.maxMemory > 0 && currentMemory > resourceLimits.maxMemory) {
			throw new TinyscriptResourceLimitViolation("Memory limit reached");
		}
	}

	public void trackObject(TSObject object) {
		Integer mem = objects.get(object);
		if (mem != null) {
			totalMemory = totalMemory - mem;
		}
		mem = object.estimateMemory();
		objects.put(object, mem);
		totalMemory = totalMemory + mem;		
	}
	
	public void trackString(String string) {
		Integer mem = objects.get(string);
		if (mem != null) {
			totalMemory = totalMemory - mem;
		}
		mem = string.length()*CHAR_SIZE;
		objects.put(string, mem);
		totalMemory = totalMemory + mem;	
	}
	
	@Override
	public void onExpunge(Integer value) {
		// logger.debug("Freed object of size : " + value);
		totalMemory = totalMemory - value;	
	}
	
	public int countObjects() {
		return objects.size();
	}
	
	public long memory() {
		// Forces expunging garbage collected entries...
		objects.size();
		return totalMemory;
	}
	
}
