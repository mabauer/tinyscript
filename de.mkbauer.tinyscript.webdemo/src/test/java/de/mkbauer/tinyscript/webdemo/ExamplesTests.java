package de.mkbauer.tinyscript.webdemo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every example of the web demo runs without an error, as the demo runs it (sandboxed, with
 * its limits), and the example menu of the toolbar lists exactly the example files. A new
 * example in frontend/public is tested without a change here. (The standalone tests also
 * compare the output of the examples with node: DemoExamplesTest.)
 */
public class ExamplesTests {

	/** The examples as written; the Vite build copies them to the served folder "public". */
	private static final Path EXAMPLES = Path.of("frontend/public");

	private static final Path TOOLBAR = Path.of("frontend/src/components/Toolbar.vue");

	private static final Pattern TOOLBAR_FILE = Pattern.compile("file:\\s*'([^']+)'");

	private TinyscriptExecutionService executionService;

	@BeforeEach
	public void setUp() {
		executionService = new TinyscriptExecutionService();
	}

	static List<String> examples() throws IOException {
		try (Stream<Path> files = Files.list(EXAMPLES)) {
			return files.map(file -> file.getFileName().toString())
					.filter(name -> name.endsWith(".tys"))
					.sorted()
					.toList();
		}
	}

	@ParameterizedTest
	@MethodSource("examples")
	public void testExampleRunsWithoutError(String example) throws IOException {
		// As served: from the classpath, where the build put the copy
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("public/" + example)) {
			assertNotNull(in, example + " is not in the served folder public/ (run npm run build)");
			String script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			TinyscriptExecutionResult result = executionService.executeScriptFromString(script);
			assertEquals(0, result.getErrorCode(), example + ": " + result.getErrorMessage());
		}
	}

	@Test
	public void testToolbarListsEveryExample() throws IOException {
		Matcher matcher = TOOLBAR_FILE.matcher(Files.readString(TOOLBAR));
		List<String> listed = matcher.results().map(match -> match.group(1)).sorted().toList();
		assertEquals(examples(), listed, "The examples in Toolbar.vue and the files in " + EXAMPLES);
	}

}
