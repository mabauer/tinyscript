package de.mkbauer.tinyscript;

import org.eclipse.emf.ecore.EObject;

public class TinyscriptRuntimeException extends RuntimeException {
	
	private static final long serialVersionUID = 2517723085390256381L;

	/** Of a deep call stack, this many innermost and outermost calls are kept. */
	public static final int SHORTENED_STACKTRACE_CALLS = 10;

	private EObject node;
	private TSStacktraceElement[] tsStacktrace;
	/** The number of calls left out of a deep call stack, and where. */
	private int omittedCalls;
	private int omittedAt;
	
	public TinyscriptRuntimeException() {
		super();
		this.node = null;
	}
	
	public TinyscriptRuntimeException(String message) {
		super(message);
		this.node = null;
	}
	
	public TinyscriptRuntimeException(String message, EObject node) {
		super(message);
		this.node = node;
	}
	
	/**
	 * Attaches the AST node where the error happened, unless the error already knows it.
	 */
	public void attachNodeIfAbsent(EObject node) {
		if (this.node == null)
			this.node = node;
	}

	public void setTinyscriptStacktrace(TSStacktraceElement[] stacktrace) {
		setTinyscriptStacktrace(stacktrace, 0, 0);
	}

	/**
	 * Sets a call stack of which {@code omittedCalls} calls, before {@code stacktrace[omittedAt]}, are left out.
	 */
	public void setTinyscriptStacktrace(TSStacktraceElement[] stacktrace, int omittedCalls, int omittedAt) {
		this.tsStacktrace = stacktrace;
		this.omittedCalls = omittedCalls;
		this.omittedAt = omittedAt;
	}
		
	public TinyscriptRuntimeException(Exception e) {
		super(e);
		this.node = null;
	}
	
	public EObject getNode() {
		return node; 
	}

	public TSStacktraceElement[] getTinyscriptStacktrace() {
		return tsStacktrace;
	}
	
	/**
	 * The tinyscript call stack, one call per line; of a deep one, only the innermost and
	 * outermost calls.
	 */
	public String getTinyscriptStacktraceAsString() {
		StringBuilder result = new StringBuilder();
		if (tsStacktrace != null) {
			for (int i = 0; i < tsStacktrace.length; i++) {
				if (omittedCalls > 0 && i == omittedAt)
					result.append("    ... " + omittedCalls + " more calls" + System.lineSeparator());
				result.append("    at " + tsStacktrace[i] + System.lineSeparator());
			}
		}
		return result.toString();
	}
	
	public String getAffectedFilename() {
		if (node != null) {
			return TinyscriptModelUtil.getFilenameOfASTNode(node);
		}
		return null;
	}
	
	public int getAffectedLine() {
		if (node != null) {
			return TinyscriptModelUtil.getLineOfASTNode(node);
		}
		return 0;
	}

	public String getLocation() {
		String result = "";
		if (tsStacktrace != null && tsStacktrace.length > 0) {
			result = " at " + tsStacktrace[0].toString();
		}
		return result;
	}
	
	public String toString() {
		return getClass().getName() + ": " + getMessage() + getLocation();
	}

}
