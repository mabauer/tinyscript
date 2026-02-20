package de.mkbauer.tinyscript.jvmscriptengine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.generator.TinyscriptGenerator;
import de.mkbauer.tinyscript.ts.Tinyscript;

public class JvmScriptEngineRunner {
	
	@Inject
	private TinyscriptGenerator generator; 

	@Inject
	public JvmScriptEngineRunner(TinyscriptGenerator generator) {
		this.generator = generator;
	}
	
	private static final Logger logger = Logger.getLogger(JvmScriptEngineRunner.class);
	
	public static ScriptEngine getJavascriptEngine() {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("Graal.js");
		if (engine == null) {
			engine = new ScriptEngineManager().getEngineByName("nashorn");
		}
		return engine;
	}

	public void execute(Tinyscript script) {
		ScriptEngine engine = getJavascriptEngine();
		if (engine == null) {
			logger.error("Could not get JDK's embedded Javascript engine: Neither Nashorn nor Graal.js are present.");
			throw new IllegalStateException("No JavaScript engine available (neither Nashorn nor Graal.js)");
		}

		String jsCode = generator.generate(script, true).toString();
		try {
			engine.eval(jsCode);
		}
		catch (ScriptException e) {
			// TODO: Improve error handling
			if (e.getMessage().contains("Assertation failed"))
				throw new TinyscriptAssertationError(e);
			throw new TinyscriptRuntimeException(e);
		}
		
	}
	
	

}
