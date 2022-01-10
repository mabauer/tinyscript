package de.mkbauer.tinyscript.runtime.fs;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import de.mkbauer.tinyscript.interpreter.BuiltinFunction;
import de.mkbauer.tinyscript.interpreter.TinyscriptEngine;
import de.mkbauer.tinyscript.interpreter.TSObject;
import de.mkbauer.tinyscript.interpreter.TSValue;

public class WriteFile extends BuiltinFunction {
		
		private static final String NAME = "writeFile";
		
		public WriteFile(TinyscriptEngine engine) {
			super(engine);
		}

		@Override
		public TSValue apply(boolean asConstructor, TSObject self, List<TSValue> args) {
			checkArgs(args);
			String fileName = args.get(0).asString();
			String contents = args.get(1).asString();
			
			// Make sure we do not allow file access in sandboxed mode
			enforceSandboxing();
						
			try {
				try (
					PrintStream out = new PrintStream(new FileOutputStream(fileName))) {
				    out.print(contents);
				}
			}
			catch (IOException e) {
				// If we can't write the file, we return undefined
				return TSValue.UNDEFINED;
			}
			return new TSValue(contents.length());
		}
		

		@Override
		public String getName() {
			return NAME;
		}

		@Override
		public int getLength() {
			return 2;
		}
		
		

	}