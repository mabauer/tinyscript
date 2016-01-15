package de.mkbauer.tinyscript.nashorn;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.eclipse.emf.common.util.WrappedException;

import com.google.inject.Inject;

import de.mkbauer.tinyscript.TinyscriptAssertationError;
import de.mkbauer.tinyscript.TinyscriptRuntimeException;
import de.mkbauer.tinyscript.generator.TinyscriptGenerator;
import de.mkbauer.tinyscript.ts.Tinyscript;

public class NashornRunner {
	
	@Inject
	private TinyscriptGenerator generator; 

	@Inject
	public NashornRunner(TinyscriptGenerator generator) {
		this.generator = generator;
	}
	
	public void execute(Tinyscript script) {
		ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
		
		String jsCode = generator.generate(script, true).toString();
		try {
			nashorn.eval(jsCode);
		}
		catch (ScriptException e) {
			// TODO: Improve error handling
			if (e.getMessage().contains("Assertation failed"))
				throw new TinyscriptAssertationError(e);
			throw new TinyscriptRuntimeException(e);
		}
		
	}

}
