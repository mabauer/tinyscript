package de.mkbauer.tinyscript.standalone.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * The examples of the web demo (de.mkbauer.tinyscript.webdemo/frontend/public): each one gives
 * the same output in the interpreter and, transpiled to Javascript, in node. A new example is
 * tested without a change here. (The webdemo's ExamplesTests run them as the demo does,
 * sandboxed, and check the example menu.)
 */
public class DemoExamplesTest extends TinyscriptTranspilerTestBase {

    private static final Path DIRECTORY = Path.of("../de.mkbauer.tinyscript.webdemo/frontend/public").toAbsolutePath().normalize();

    /** Examples whose output differs on each run (Math.random): they only have to run. */
    private static final Set<String> RANDOM_OUTPUT = Set.of("people.tys");

    // TODO(TODO.md: Generated Javascript prints values like the interpreter): these examples
    // print whole arrays or objects, which node prints differently; they only have to run
    private static final Set<String> PRINT_WHOLE_OBJECTS = Set.of("arrays.tys", "expressions.tys", "modules.tys", "oo.tys");

    static List<String> examples() throws IOException {
        try (Stream<Path> files = Files.list(DIRECTORY)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".tys"))
                    .sorted()
                    .toList();
        }
    }

    @ParameterizedTest
    @MethodSource("examples")
    public void testExample(String example) {
        Path file = DIRECTORY.resolve(example);
        if (RANDOM_OUTPUT.contains(example) || PRINT_WHOLE_OBJECTS.contains(example))
            assertFalse(runModule(file).isEmpty(), example + " printed nothing");
        else
            assertSameOutputModule(file);
    }
}
