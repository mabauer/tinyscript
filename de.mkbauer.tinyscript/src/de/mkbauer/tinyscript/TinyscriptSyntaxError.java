package de.mkbauer.tinyscript;

public class TinyscriptSyntaxError extends TinyscriptRuntimeException {
	
	private String filename = null;
	
	private int line = 0;
	
	public TinyscriptSyntaxError(String message) {
		super(message);
	}
	
	public TinyscriptSyntaxError(String message, String filename, int line) {
		super(message);
		this.filename = filename;
		this.line = line;
	}
	
	@Override
	public String getAffectedFilename() {
		return filename;
	}
	
	@Override
	public int getAffectedLine() {
		return line;
	}
	
	@Override
	public String getLocation() {
		String result = " at (";
		if (filename != null) {
			result = result + filename;
		}
		else {
			result = result + "?";
		}
		result = result + ":" + line + ")";
		return result;
	}
	

}
