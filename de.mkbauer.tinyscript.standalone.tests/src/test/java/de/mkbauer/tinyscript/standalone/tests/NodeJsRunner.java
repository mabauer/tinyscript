package de.mkbauer.tinyscript.standalone.tests;

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

    private static final int TIMEOUT_MS = 5_000;

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
     * @return everything written to stdout
     * @throws AssertionError if node exits with a non-zero status; the error
     *     message includes the node stderr output so the failure is actionable
     */
    public static String run(String jsCode) {
        File tmpFile = null;
        try {
            tmpFile = Files.createTempFile("tinyscript_test_", ".js").toFile();
            Files.writeString(tmpFile.toPath(), jsCode, StandardCharsets.UTF_8);

            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();

            CommandLine cmd = new CommandLine("node");
            cmd.addArgument(tmpFile.getAbsolutePath());

            DefaultExecutor executor = DefaultExecutor.builder().get();
            executor.setStreamHandler(new PumpStreamHandler(stdout, stderr));
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

            String stderrStr = stderr.toString(StandardCharsets.UTF_8);
            if (exitCode != 0) {
                throw new AssertionError(
                        "node exited with code " + exitCode + "\n"
                        + "--- node stderr ---\n" + stderrStr);
            }
            return stdout.toString(StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new AssertionError("Failed to invoke node: " + e.getMessage(), e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }
}
