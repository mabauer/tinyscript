package de.mkbauer.tinyscript;

public class TinyscriptSyntaxError extends TinyscriptRuntimeException {
	
	private String filename = null;
	
	private int line = 0;

	private int column = 0;
	
	public TinyscriptSyntaxError(String message) {
		super(message);
	}
	
	public TinyscriptSyntaxError(String message, String filename, int line) {
		this(message, filename, line, 0);
	}

	public TinyscriptSyntaxError(String message, String filename, int line, int column) {
		super(message);
		this.filename = filename;
		this.line = line;
		this.column = column;
	}

	public int getAffectedColumn() {
		return column;
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
		result = result + ":" + line + (column > 0 ? ":" + column : "") + ")";
		return result;
	}
	

}
