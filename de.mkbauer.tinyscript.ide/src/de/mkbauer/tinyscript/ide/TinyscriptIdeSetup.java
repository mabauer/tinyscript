/*
 * generated by Xtext 2.14.0
 */
package de.mkbauer.tinyscript.ide;

import com.google.inject.Guice;
import com.google.inject.Injector;
import de.mkbauer.tinyscript.TinyscriptRuntimeModule;
import de.mkbauer.tinyscript.TinyscriptStandaloneSetup;
import org.eclipse.xtext.util.Modules2;

/**
 * Initialization support for running Xtext languages as language servers.
 */
public class TinyscriptIdeSetup extends TinyscriptStandaloneSetup {

	@Override
	public Injector createInjector() {
		return Guice.createInjector(Modules2.mixin(new TinyscriptRuntimeModule(), new TinyscriptIdeModule()));
	}
	
}
