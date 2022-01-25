package de.mkbauer.tinyscript.runtime.fs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class ReadFile extends BuiltinFunction {
		
		private static final String NAME = "readFile";
		
		public ReadFile(TinyscriptEngine engine) {
			super(engine);
		}

		@Override
		public TSValue apply(TSObject self, TSValue[] args) {
			checkArgs(args);
			String result = null;
			String fileName = args[0].asString();
			
			// Make sure we do not allow file access in sandboxed mode
			enforceSandboxing();
			
			try {
				Charset cs = Charset.defaultCharset();
				result = readFile(fileName, cs);
			}
			catch (IOException e) {
				// If we can't read the file, we return undefined
				return TSValue.UNDEFINED;
			}
			engine.recordStringCreation(result);
			return new TSValue(result);
		}
		
		private String readFile(String file, Charset cs)
	            throws IOException {
		    // No real need to close the BufferedReader/InputStreamReader
		    // as they're only wrapping the stream
		    FileInputStream stream = new FileInputStream(file);
		    try {
		        Reader reader = new BufferedReader(new InputStreamReader(stream, cs));
		        StringBuilder builder = new StringBuilder();
		        char[] buffer = new char[8192];
		        int read;
		        while ((read = reader.read(buffer, 0, buffer.length)) > 0) {
		            builder.append(buffer, 0, read);
		        }
		        return builder.toString();
		    } 
		    finally {
		        // Potential issue here: if this throws an IOException,
		        // it will mask any others. Normally I'd use a utility
		        // method which would log exceptions and swallow them
		        stream.close();
		    }        
		}

		@Override
		public String getName() {
			return NAME;
		}

		@Override
		public int getLength() {
			return 1;
		}
		
		

	}