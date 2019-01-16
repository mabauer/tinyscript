package de.mkbauer.tinyscript;

import org.eclipse.emf.ecore.EObject;

public class TinyscriptRuntimeException extends RuntimeException {
	
	private static final long serialVersionUID = 2517723085390256381L;

	private EObject node;
	private TSStacktraceElement[] tsStacktrace;
	
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
	
	public void setTinyscriptStacktrace(TSStacktraceElement[] stacktrace) {
		this.tsStacktrace = stacktrace; 
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
	
	public String getTinyscriptStacktraceAsString() {
		String result = "";
		if (tsStacktrace != null) {
			for (TSStacktraceElement elem: tsStacktrace) {
				result = result + "    at " + elem.toString() + System.lineSeparator();
			}
		}
		return result;
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
