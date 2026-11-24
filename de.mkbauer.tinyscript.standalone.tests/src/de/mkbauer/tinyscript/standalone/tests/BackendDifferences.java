package de.mkbauer.tinyscript.standalone.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Collects where both backends fail in the same way, but with another class or message (a goal
 * for good messages, not part of equality): target/backend-differences.txt, written anew by each
 * test run.
 */
final class BackendDifferences {

    static final Path REPORT = Path.of("target", "backend-differences.txt");

    // Loaded with the first assertSameFailure of a run: the report of an earlier run goes
    static {
        try {
            Files.deleteIfExists(REPORT);
        }
        catch (IOException e) {
            // Only a report
        }
    }

    private BackendDifferences() {
    }

    static synchronized void note(String tsCode, TinyscriptTranspilerTestBase.Failure interpreter,
            TinyscriptTranspilerTestBase.Failure node) {
        boolean sameName = interpreter.name().equals(node.name());
        boolean sameMessage = interpreter.message().equals(node.message());
        if (sameName && sameMessage)
            return;
        String entry = "--- " + tsCode.replace("\n", "\n    ") + "\n"
                + "    interpreter: " + interpreter.name() + ": " + interpreter.message() + "\n"
                + "    node:        " + node.name() + ": " + node.message() + "\n";
        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e) {
            // Only a report
        }
    }
}
