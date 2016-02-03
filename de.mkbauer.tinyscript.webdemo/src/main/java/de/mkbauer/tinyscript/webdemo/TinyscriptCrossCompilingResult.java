package de.mkbauer.tinyscript.webdemo;

public class TinyscriptCrossCompilingResult {

	private String script;
	
	private int errorCode;
	
	private String errorMessage = "";
	private int errorLine = 0;
	
	public TinyscriptCrossCompilingResult() {
		errorCode = 0;
		script = "";
		errorMessage = "";
		errorLine = 0;
	}
	
	public TinyscriptCrossCompilingResult(String script) {
		errorCode = 0;
		this.script = script;
		errorMessage = "";
		errorLine = 0;
	}
	
	public TinyscriptCrossCompilingResult(String errorMessage, int errorLine) {
		errorCode = 1;
		script = "";
		this.errorMessage = errorMessage;
		this.errorLine = errorLine;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
	}

	public int getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(int errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public int getErrorLine() {
		return errorLine;
	}

	public void setErrorLine(int errorLine) {
		this.errorLine = errorLine;
	}
	
}
