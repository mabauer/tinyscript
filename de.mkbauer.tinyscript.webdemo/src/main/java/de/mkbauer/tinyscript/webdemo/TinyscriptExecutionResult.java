package de.mkbauer.tinyscript.webdemo;

import de.mkbauer.tinyscript.interpreter.ResourceConsumption;

public class TinyscriptExecutionResult {

	private int errorCode;
	private String value;
	private String output;
	private ResourceConsumption statistics;
	private String errorMessage;
	private int errorLine;
	
	public TinyscriptExecutionResult() {
		errorCode = 0;
		value = "";
		output = "";
		errorMessage = "";
		errorLine = 0;
	}
	
	public TinyscriptExecutionResult(String value, String output, ResourceConsumption statistics) {
		this.value = value;
		this.output = output;
		this.statistics = statistics;
		this.errorCode = 0;
	}
	
	public TinyscriptExecutionResult(String value, String output, ResourceConsumption statistics, String errorMessage, int errorLine) {
		this.value = value;
		this.output = output;
		this.statistics = statistics;
		this.errorCode = 1;
		this.errorMessage = errorMessage;
		this.errorLine = errorLine;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getOutput() {
		return output;
	}
	
	public void setOutput(String output) {
		this.output = output;
	}
	
	public ResourceConsumption getStatistics() {
		return statistics;
	}

	public void setStatistics(ResourceConsumption statistics) {
		this.statistics = statistics;
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
