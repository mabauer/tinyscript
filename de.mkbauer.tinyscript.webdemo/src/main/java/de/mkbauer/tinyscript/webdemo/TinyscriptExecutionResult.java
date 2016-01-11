package de.mkbauer.tinyscript.webdemo;

public class TinyscriptExecutionResult {
	
	private int errorCode;
	private String value;
	private String output;
	private String errorMessage;
	private int errorLine;
	
	public TinyscriptExecutionResult(String value, String output) {
		this.value = value;
		this.output = output;
		this.errorCode = 0;
	}
	
	public TinyscriptExecutionResult(String value, String output, String errorMessage, int errorLine) {
		this.value = value;
		this.output = output;
		this.errorCode = 1;
		this.errorMessage = errorMessage;
		this.errorLine = errorLine;
	}
	
	public String getValue() {
		return value;
	}
	
	public String getOutput() {
		return output;
	}
	
	public int getErrorCode() {
		return errorCode;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public int getErrorLine() {
		return errorLine;
	}

}
