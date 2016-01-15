package de.mkbauer.tinyscript.webdemo;

import de.mkbauer.tinyscript.interpreter.ResourceConsumption;

public class TinyscriptExecutionResult {
	
	private int errorCode;
	private String value;
	private String output;
	private ResourceConsumption statistics;
	private String errorMessage;
	private int errorLine;
	
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
	
	public String getOutput() {
		return output;
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
	
	public String getErrorMessage() {
		return errorMessage;
	}
	
	public int getErrorLine() {
		return errorLine;
	}

}
