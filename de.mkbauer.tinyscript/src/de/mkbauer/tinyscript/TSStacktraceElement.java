package de.mkbauer.tinyscript;

public class TSStacktraceElement {
	
	private String fileName;
	private String functionName;
	private int line;
	
	public TSStacktraceElement(String function, int line) {
		this.fileName = null;
		this.functionName = function;
		this.line = line;
	}

	public TSStacktraceElement(String fileName, String function, int line) {
		this.fileName= fileName;
		this.functionName = function;
		this.line = line;
	}

	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFunctionName() {
		return functionName;
	}
	
	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}
	
	public int getLine() {
		return line;
	}
	
	public void setLine(int line) {
		this.line = line;
	}
	
	public String toString() {
		String result = functionName + "(";
		if (fileName != null) {
			result = result + fileName;
		}
		else {
			result = result + "?";			
		}
		result = result + ":" + line + ")";
		return result;
	}
}
