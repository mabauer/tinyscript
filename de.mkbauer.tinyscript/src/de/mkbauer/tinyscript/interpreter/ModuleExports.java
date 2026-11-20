package de.mkbauer.tinyscript.interpreter;

import java.util.Map;

/**
 * The exports of a module: an object with one property per exported name, read-only
 * once the module has run. Every importer gets the same instance.
 */
public class ModuleExports extends TSObject {

	private final String moduleName;

	private final boolean frozen;

	ModuleExports(TinyscriptEngine engine, String moduleName, Map<String, TSValue> exports) {
		super(engine, engine.getDefaultPrototype());
		this.moduleName = moduleName;
		exports.forEach(super::put);
		frozen = true;
	}

	public String getModuleName() {
		return moduleName;
	}

	@Override
	public void put(String key, TSValue value) {
		if (frozen)
			throw new TinyscriptTypeError("Cannot change the exports of module " + moduleName);
		super.put(key, value);
	}

	@Override
	public void setOwnPropertyDescriptor(String key, TSPropertyDescriptor desc) {
		if (frozen)
			throw new TinyscriptTypeError("Cannot change the exports of module " + moduleName);
		super.setOwnPropertyDescriptor(key, desc);
	}
}
