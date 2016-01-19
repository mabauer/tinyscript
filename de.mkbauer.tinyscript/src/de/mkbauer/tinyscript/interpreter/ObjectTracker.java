package de.mkbauer.tinyscript.interpreter;

import de.mkbauer.util.WeakHashMapWithCallBack;

import org.apache.log4j.Logger;

public class ObjectTracker implements WeakHashMapWithCallBack.OnExpungeListener<Integer> {
	
	private static final int TSVALUE_SIZE = 8;
	private static final int CHAR_SIZE = 2;
	
	private WeakHashMapWithCallBack<Object, Integer> objects = new WeakHashMapWithCallBack<Object, Integer>();
	
	private long totalMemory = 0;
	
	private final static Logger logger = Logger.getLogger(ObjectTracker.class);
	
	public ObjectTracker() {
		objects.setOnExpungeListener(this);
	}

	public void trackObject(TSObject object) {
		Integer mem = objects.get(object);
		if (mem != null) {
			totalMemory = totalMemory - mem;
		}
		mem = object.getObjectSize()*TSVALUE_SIZE;
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
	
	public int size() {
		return objects.size();
	}
	
	public long memory() {
		// Forces expunging garbage collected entries...
		objects.size();
		return totalMemory;
	}
	
}
