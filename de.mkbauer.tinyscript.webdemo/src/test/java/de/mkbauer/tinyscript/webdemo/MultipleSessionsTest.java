package de.mkbauer.tinyscript.webdemo;

import java.io.InputStream;
import java.util.Map;

import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import de.mkbauer.tinyscript.interpreter.ResourceConsumption;

public class MultipleSessionsTest {
	
	private static final int THREADS = 10;
	private static final String PUBLIC_FOLDER = "public";
	private static final String URI = "http://localhost:8080/execute";
	
	private static class Session implements Runnable {
		
		private String script;
		
		public Session(String script) {
			this.script = script;
		}

		@Override
		public void run() {
			runScriptOnServer(script);	
		}
		
	}
	    
	private static String convertStreamToString(java.io.InputStream in) {
	    java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
	    String result = s.hasNext() ? s.next() : "";
	    return result;
	}	
	
	public static String readScriptFromFile(String filename) {
		InputStream in = MultipleSessionsTest.class.getClassLoader()
                .getResourceAsStream(PUBLIC_FOLDER + java.io.File.separator + filename);
		String script = convertStreamToString(in);
		return script;
	}
	
	public static void runScriptOnServer(String script) {
		System.out.println("Running script...");
	    RestTemplate restTemplate = new RestTemplate();
	    TinyscriptExecutionResult result = restTemplate.postForObject(URI, script, TinyscriptExecutionResult.class); 
	    ResourceConsumption statistics = result.getStatistics();
	    System.out.println("Results: time=" + statistics.getExecutionTime() +"ms"
	    		+ ", cpu=" + statistics.getMxCpuTime() / 1000000 + "ms"
	    		+ ", malloc=" + statistics.getMxMAlloc()/1024 + "K"
	    		+ ", objs=" + statistics.getObjects() + "/" + statistics.getObjectsMax()
	    		+ ", mem=" + statistics.getMemory()/1024 + "K/" + statistics.getMemoryMax()/1014 + "K"
	    		+ ", creates=" + statistics.getObjectCreations());
	}
	
	public static void main(String[] args) {
		for (int i = 1; i <= THREADS; i++) {
			Thread test = new Thread(new Session(readScriptFromFile("people.ts")));
	        test.start();
		}
	}

}
