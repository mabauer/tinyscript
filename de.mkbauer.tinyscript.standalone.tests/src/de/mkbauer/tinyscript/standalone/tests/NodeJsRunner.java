package de.mkbauer.tinyscript.standalone.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

/**
 * Runs JavaScript code through the system {@code node} binary.
 *
 * <p>Designed for use as a "differential oracle": given generated JavaScript,
 * verify that its observable output matches that of the Tinyscript interpreter.
 */
public class NodeJsRunner {

    // Generous: the first node process of a CI run can take several seconds to start
    // (AdventOfCode2024Test.testDay01 was killed after 5 s once); a script that really
    // hangs still fails
    private static final int TIMEOUT_MS = 30_000;

    private static Boolean nodeAvailable = null;

    /**
     * Returns {@code true} if {@code node} is on the PATH and can be invoked.
     * The result is cached after the first call.
     */
    public static boolean isAvailable() {
        if (nodeAvailable == null) {
            nodeAvailable = probe();
        }
        return nodeAvailable;
    }

    private static boolean probe() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            CommandLine cmd = CommandLine.parse("node --version");
            DefaultExecutor executor = DefaultExecutor.builder().get();
            executor.setStreamHandler(new PumpStreamHandler(out, out));
            ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                    .setTimeout(java.time.Duration.ofMillis(TIMEOUT_MS))
                    .get();
            executor.setWatchdog(watchdog);
            int exit = executor.execute(cmd);
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Writes {@code jsCode} to a temporary file, runs it with {@code node},
     * and returns the standard output as a string.
     *
     * @param jsCode JavaScript source to execute
     * @param args the arguments of the script (process.argv after the script)
     * @return everything written to stdout
     * @throws AssertionError if node exits with a non-zero status; the error
     *     message includes the node stderr output so the failure is actionable
     */
    public static String run(String jsCode, String... args) {
        return runWithInput(jsCode, "", args);
    }

    /**
     * Like {@link #run(String, String...)}, with {@code input} as the standard input.
     */
    public static String runWithInput(String jsCode, String input, String... args) {
        Result result = execute(jsCode, input, args);
        if (result.exitCode() != 0) {
            throw new AssertionError(
                    "node exited with code " + result.exitCode() + "\n"
                    + "--- node stderr ---\n" + result.stderr());
        }
        return result.stdout();
    }

    /** What a node run gave: its exit code and everything it wrote. */
    public record Result(int exitCode, String stdout, String stderr) {
    }

    /**
     * Runs {@code jsCode} with node and returns its exit code and output, also when it fails.
     */
    public static Result execute(String jsCode, String input, String... args) {
        File tmpFile = null;
        try {
            tmpFile = Files.createTempFile("tinyscript_test_", ".js").toFile();
            Files.writeString(tmpFile.toPath(), jsCode, StandardCharsets.UTF_8);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            CommandLine cmd = new CommandLine("node");
            // The generated programs have source maps (see TinyscriptTranspilerTestBase)
            cmd.addArgument("--enable-source-maps");
            cmd.addArgument(tmpFile.getAbsolutePath());
            for (String arg : args)
                cmd.addArgument(arg, false);

            DefaultExecutor executor = DefaultExecutor.builder().get();
            executor.setStreamHandler(new PumpStreamHandler(stdout, stderr,
                    new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))));
            ExecuteWatchdog watchdog = ExecuteWatchdog.builder()
                    .setTimeout(java.time.Duration.ofMillis(TIMEOUT_MS))
                    .get();
            executor.setWatchdog(watchdog);
            // Accept any exit code — we will check manually
            executor.setExitValues(null);

            int exitCode;
            try {
                exitCode = executor.execute(cmd);
            } catch (ExecuteException e) {
                exitCode = e.getExitValue();
            }

            return new Result(exitCode, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));

        } catch (IOException e) {
            throw new AssertionError("Failed to invoke node: " + e.getMessage(), e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }
}
