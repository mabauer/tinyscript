package de.mkbauer.tinyscript.interpreter;

/**
 * Thrown by an optional access a?.b on null or undefined and caught at the end of the chain
 * (a?.b.c(d)), which is then undefined: like Javascript, the rest of the chain is skipped.
 */
public class TSOptionalChainEnd extends RuntimeException {

	static final TSOptionalChainEnd INSTANCE = new TSOptionalChainEnd();

	private TSOptionalChainEnd() {
		super(null, null, false, false);
	}

}
