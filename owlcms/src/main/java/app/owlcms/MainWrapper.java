/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Wrapper that launches app.owlcms.Main as a separate process and automatically restarts it 
 * if it exits with non-zero status. Passes through all VM arguments and environment variables.
 *
 * @author Jean-François Lamy
 */
public class MainWrapper {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(MainWrapper.class);
    private static volatile Process currentProcess;

    public static void main(String... args) throws Exception {
        // Register shutdown hook to kill subprocess when wrapper is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Process p = currentProcess;
            logger.info("Shutdown hook triggered");
            if (p != null && p.isAlive()) {
                logger.info("Terminating subprocess and descendants");
                try {
                    // Kill all descendants first (child processes spawned by Main)
                    ProcessHandle ph = p.toHandle();
                    ph.descendants().forEach(child -> {
                        logger.info("Killing descendant PID {}", child.pid());
                        child.destroyForcibly();
                    });
                    // Now kill the main subprocess - use destroyForcibly on Windows
                    logger.info("Killing subprocess PID {}", ph.pid());
                    p.destroyForcibly();
                    // Wait briefly for termination
                    if (p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.info("Subprocess terminated");
                    } else {
                        logger./**/warn("Subprocess still running after 3s wait");
                    }
                } catch (Exception e) {
                    logger.error("Error during shutdown: {}", e.getMessage());
                    p.destroyForcibly();
                }
            } else {
                logger.info("No subprocess to terminate (p={}, alive={})", p, (p != null ? p.isAlive() : "n/a"));
            }
        }, "MainWrapper-Shutdown"));
        // Get the current JVM's arguments and filter out debug-related ones
        List<String> allVmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        logger.debug("MainWrapper started with VM arguments: {}", allVmArgs);
        
        // Filter out debug/agent arguments that can't be reused
        List<String> vmArgs = new ArrayList<>();
        for (String arg : allVmArgs) {
            if (arg.startsWith("-Xrunjdwp") || 
                arg.startsWith("-agentlib:jdwp") ||
                arg.startsWith("-Xdebug") ||
                arg.contains("suspend=")) {
                logger.debug("Filtering out debug argument: {}", arg);
                continue;
            }
            vmArgs.add(arg);
        }
        logger.debug("Filtered VM arguments for subprocess: {}", vmArgs);
        
        // Get current environment variables
        Map<String, String> env = System.getenv();
        logger.debug("Environment variables count: {}", env.size());

        // Get the java command to use
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        
        // Get the classpath
        String classpath = System.getProperty("java.class.path");

        int restartCount = 0;
        while (true) {
            try {
                logger.info("Starting app.owlcms.Main as separate process (restart count: {})", restartCount);
                
                // Build the command
                List<String> command = new ArrayList<>();
                command.add(javaBin);
                command.addAll(vmArgs);
                command.add("-cp");
                command.add(classpath);
                command.add("app.owlcms.Main");
                for (String arg : args) {
                    command.add(arg);
                }
                
                logger.debug("Command: {}", String.join(" ", command));
                
                // Start the process
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.inheritIO(); // Redirect stdout/stderr to parent process
                Map<String, String> subprocessEnv = pb.environment();
                subprocessEnv.putAll(env); // Pass all environment variables
                subprocessEnv.put("OWLCMS_CONTROLPANEL", "3.1.0"); // Triggers restart after JSON import
                
                currentProcess = pb.start();
                
                // Wait for process to complete
                int exitCode = currentProcess.waitFor();
                currentProcess = null;
                
                logger.info("Subprocess exited with code {}", exitCode);
                
                if (exitCode == 0) {
                    logger.info("app.owlcms.Main exited normally with code 0, wrapper exiting");
                    break; // Exit successfully
                } else {
                    restartCount++;
                    logger.info("app.owlcms.Main exited with non-zero code {} - initiating restart (count: {})", 
                                 exitCode, restartCount);
                    Thread.sleep(5000); // Wait 5 seconds before restart
                    logger.info("Restarting app.owlcms.Main...");
                }
                
            } catch (InterruptedException e) {
                logger.info("MainWrapper interrupted, exiting", e);
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                restartCount++;
                logger.error("Failed to start app.owlcms.Main (restart count: {}): {}", 
                             restartCount, e.getMessage(), e);
                Thread.sleep(5000); // Wait 5 seconds before retry
                logger.info("Retrying app.owlcms.Main...");
            }
        }
    }
}
