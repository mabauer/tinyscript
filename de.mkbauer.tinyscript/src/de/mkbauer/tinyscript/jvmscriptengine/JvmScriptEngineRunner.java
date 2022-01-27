package de.mkbauer.tinyscript.jvmscriptengine;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
	
	public void execute(Tinyscript script) {
		// Try to load the new GraalVM-based JS engine first...
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("graal.js");
		if (engine == null) {
			// ...that failed, so let's use Nashorn
			engine = new ScriptEngineManager().getEngineByName("nashorn");
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
