package de.mkbauer.tinyscript.interpreter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * The lines of a stream in UTF-8, as node reads them in the generated Javascript: split at
 * {@code \n}, a {@code \r} before it removed; a last line without a line break counts. Reads
 * no further than the line break, so that engines one after the other (files of the REPL)
 * can share a stream. The prompt goes to the engine's stdout.
 */
final class StreamStandardInput implements StandardInput {

	private final InputStream in;
	private final Supplier<OutputStream> stdOut;

	StreamStandardInput(InputStream in, Supplier<OutputStream> stdOut) {
		this.in = in;
		this.stdOut = stdOut;
	}

	@Override
	public String readLine(String prompt) throws IOException {
		OutputStream out = stdOut.get();
		if (!prompt.isEmpty() && out != null) {
			out.write(prompt.getBytes(StandardCharsets.UTF_8));
			out.flush();
		}
		ByteArrayOutputStream line = new ByteArrayOutputStream();
		int b;
		while ((b = in.read()) != -1 && b != '\n')
			line.write(b);
		if (b == -1 && line.size() == 0)
			return null;
		String text = line.toString(StandardCharsets.UTF_8);
		return text.endsWith("\r") ? text.substring(0, text.length() - 1) : text;
	}
}
