package de.mkbauer.tinyscript.interpreter;

import java.util.WeakHashMap;

import org.apache.log4j.Logger;

public class ObjectTracker {
	
	private static final int RECALCULATE_LIMIT = 64;
	private static final double MAX_SIZE_ERROR = 0.5;
	
	private static final int TSVALUE_SIZE = 8;
	private static final int CHAR_SIZE = 2;
	
	private WeakHashMap<Object, Integer> objects = new WeakHashMap<Object, Integer>();
	
	private int size = 0;
	private long totalMemory = 0;
	
	private final static Logger logger = Logger.getLogger(ObjectTracker.class);

	public void trackObject(TSObject object) {
		Integer mem = objects.get(object);
		if (mem != null) {
			totalMemory = totalMemory - mem;
			size--;
		}
		mem = object.getObjectSize()*TSVALUE_SIZE;
		objects.put(object, mem);
		size++;
		totalMemory = totalMemory + mem;
		
	}
	
	public void trackString(String string) {
		Integer mem = objects.get(string);
		if (mem != null) {
			totalMemory = totalMemory - mem;
			size--;
		}
		mem = string.length()*CHAR_SIZE;
		objects.put(string, mem);
		size++;
		totalMemory = totalMemory + mem;
		
	}
	
	public void recalculateSizes() {
		logger.debug("recalculate objects - before: " + size);
		int mem;
		totalMemory = 0;
		size = 0;
		for (Object object: objects.keySet()) {
			if (object instanceof TSObject)
				mem = ((TSObject)object).getObjectSize()*TSVALUE_SIZE;
			else 
				mem = ((String)object).length()*CHAR_SIZE;
			objects.put(object, mem);
			size++;
			totalMemory = totalMemory + mem;
		}
		logger.debug("recalculate objects - after: " + size);
	}
	
	public int size() {
		if (objects.size() <= RECALCULATE_LIMIT || ((double) Math.abs(size - objects.size())) / size > MAX_SIZE_ERROR)
			recalculateSizes();
		return size;
	}
	
	public long memory() {
		if (objects.size() <= RECALCULATE_LIMIT || ((double) Math.abs(size - objects.size())) / size > MAX_SIZE_ERROR)
			recalculateSizes();
		return totalMemory;
	}
	
}
