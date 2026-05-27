package com.fitnessai.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class TestResultExtension implements TestWatcher, AfterAllCallback, BeforeEachCallback {

    private static final List<String> logs = new ArrayList<>();
    private static final List<String> failedTests = new ArrayList<>();
    private static final List<String> failedLogs = new ArrayList<>();
    private static final List<String> succeededTests = new ArrayList<>();

    // ThreadLocal buffer to capture standard console output and errors printed during each test execution
    private static final ThreadLocal<StringBuilder> threadStdout = ThreadLocal.withInitial(StringBuilder::new);
    private static PrintStream originalOut;
    private static PrintStream originalErr;

    static {
        // Safe redirect System.out to capture console logs for the current test thread
        originalOut = System.out;
        System.setOut(new PrintStream(originalOut) {
            @Override
            public void write(int b) {
                originalOut.write(b);
                threadStdout.get().append((char) b);
            }
            @Override
            public void write(byte[] b, int off, int len) {
                originalOut.write(b, off, len);
                threadStdout.get().append(new String(b, off, len));
            }
        });

        // Safe redirect System.err to capture error logs for the current test thread
        originalErr = System.err;
        System.setErr(new PrintStream(originalErr) {
            @Override
            public void write(int b) {
                originalErr.write(b);
                threadStdout.get().append((char) b);
            }
            @Override
            public void write(byte[] b, int off, int len) {
                originalErr.write(b, off, len);
                threadStdout.get().append(new String(b, off, len));
            }
        });
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        // Clear ThreadLocal buffer before each test execution to ensure per-test logs isolation
        threadStdout.get().setLength(0);
    }

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testName = context.getDisplayName();
        succeededTests.add(testName);
        logs.add("SUCCESS: " + testName);

        // Attach console output to Allure report
        String log = threadStdout.get().toString();
        if (!log.isEmpty()) {
            io.qameta.allure.Allure.addAttachment("Console Log (Stdout/Stderr)", log);
        }
        threadStdout.get().setLength(0); // Clear buffer
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String testName = context.getDisplayName();
        failedTests.add(testName);
        logs.add("FAILED: " + testName + " - " + cause.getMessage());

        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        String failureDetails = "FAILED TEST: " + testName + "\n\nError Message: " + cause.getMessage() + "\n\nStack Trace:\n" + sw.toString() + "\n====================\n";
        failedLogs.add(failureDetails);

        // Attach failure stack trace to Allure report
        io.qameta.allure.Allure.addAttachment("Failure Stack Trace", failureDetails);

        // Attach console output to Allure report
        String log = threadStdout.get().toString();
        if (!log.isEmpty()) {
            io.qameta.allure.Allure.addAttachment("Console Log (Stdout/Stderr)", log);
        }
        threadStdout.get().setLength(0); // Clear buffer
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        logs.add("ABORTED: " + context.getDisplayName() + " - " + cause.getMessage());
        threadStdout.get().setLength(0); // Clear buffer
    }

    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        logs.add("DISABLED: " + context.getDisplayName() + " - " + reason.orElse("No reason"));
        threadStdout.get().setLength(0); // Clear buffer
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        // Allow Allure a brief moment to finish writing raw JSONs to disk
        Thread.sleep(1000);

        String timestamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
        File baseDir = new File("E:\\FitnessAI-Java\\FitnessAI-Java");
        File testResultDir = new File(baseDir, "TestResult/" + timestamp);
        testResultDir.mkdirs();

        // 1. Generate Allure Report using local allure.bat into TestResult/yymmddhhmmss/
        File allureBat = new File(baseDir, ".allure/allure-2.24.0/bin/allure.bat");
        if (allureBat.exists()) {
            System.out.println("🚀 Compiling Allure report programmatically to " + testResultDir.getAbsolutePath());
            ProcessBuilder pb = new ProcessBuilder(
                    allureBat.getAbsolutePath(),
                    "generate",
                    "allure-results",
                    "--clean",
                    "-o",
                    testResultDir.getAbsolutePath()
            );
            pb.directory(baseDir);
            Process process = pb.start();
            
            // Read output stream to ensure no blocking
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Allure CLI] " + line);
            }
            process.waitFor();
            System.out.println("✅ Allure report generated successfully inside " + testResultDir.getAbsolutePath());
        } else {
            System.err.println("⚠️ Allure CLI batch tool not found at: " + allureBat.getAbsolutePath());
        }

        // 2. Generate the yymmddhhmmss.txt log report
        StringBuilder logContent = new StringBuilder();
        logContent.append("==================================================\n");
        logContent.append("FITNESSAI TEST EXECUTION REPORT\n");
        logContent.append("==================================================\n");
        logContent.append("Test Run Timestamp: ").append(timestamp).append("\n");
        logContent.append("Execution Date:     ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
        logContent.append("--------------------------------------------------\n");
        logContent.append("Total Executed:     ").append(logs.size()).append("\n");
        logContent.append("Passed:             ").append(succeededTests.size()).append("\n");
        logContent.append("Failed:             ").append(failedTests.size()).append("\n");
        logContent.append("Success Rate:       ").append(String.format("%.2f%%", (double) succeededTests.size() / logs.size() * 100)).append("\n");
        logContent.append("--------------------------------------------------\n\n");
        logContent.append("--- Detailed Execution Log ---\n");
        for (String log : logs) {
            logContent.append(log).append("\n");
        }

        Files.write(Paths.get(testResultDir.getAbsolutePath(), timestamp + ".txt"), logContent.toString().getBytes());
        System.out.println("📝 Test run log generated at: " + testResultDir.getAbsolutePath() + "/" + timestamp + ".txt");

        // 3. Generate failtest directory if failures exist
        if (!failedTests.isEmpty()) {
            File failDir = new File(testResultDir, "failtest");
            failDir.mkdirs();
            for (int i = 0; i < failedTests.size(); i++) {
                String testName = failedTests.get(i);
                String details = failedLogs.get(i);
                String safeName = testName.replaceAll("[^a-zA-Z0-9_-]", "_");
                Files.write(Paths.get(failDir.getAbsolutePath(), safeName + ".txt"), details.getBytes());
            }
            System.out.println("❌ Failures recorded inside " + failDir.getAbsolutePath());
        }

        // 4. Clean up raw allure-results JSON directory to keep workspace clean
        File rawResults = new File(baseDir, "allure-results");
        if (rawResults.exists()) {
            System.out.println("🧹 Cleaning up raw allure-results JSON files...");
            deleteDirectory(rawResults);
            System.out.println("🧹 Done! All raw allure-results JSON files removed.");
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        deleteDirectory(f);
                    } else {
                        f.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}
