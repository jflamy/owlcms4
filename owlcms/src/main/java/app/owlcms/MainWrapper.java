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
    private static final int MAX_RESTARTS = 3;
    private static volatile Process currentProcess;
    private static volatile boolean shuttingDown = false;

    public static void main(String... args) throws Exception {
        // Register shutdown hook to kill subprocess when wrapper is terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shuttingDown = true;
            Process p = currentProcess;
            logger.info("Shutdown hook triggered");
            if (p != null && p.isAlive()) {
                try {
                    // Let the child finish its own shutdown hooks gracefully (up to 5s)
                    logger.info("Waiting up to 5s for subprocess to shut down gracefully");
                    if (p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        logger.info("Subprocess exited gracefully");
                    } else {
                        // Child didn't exit in time — force kill it and descendants
                        logger./**/warn("Subprocess still running after 5s, force-killing");
                        ProcessHandle ph = p.toHandle();
                        ph.descendants().forEach(child -> {
                            logger.info("Killing descendant PID {}", child.pid());
                            child.destroyForcibly();
                        });
                        p.destroyForcibly();
                        p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
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
        int wrapperExitCode = 0;
        while (restartCount <= MAX_RESTARTS) {
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
                
                if (shuttingDown) {
                    logger.info("Wrapper is shutting down, not restarting (subprocess exit code {})", exitCode);
                    wrapperExitCode = exitCode;
                    break;
                }
                
                if (exitCode == 0 || exitCode >= 128) {
                    logger.info("app.owlcms.Main exited with terminal code {}, wrapper exiting", exitCode);
                    wrapperExitCode = exitCode;
                    break;
                }
                
                // Restartable exit code (1-127)
                wrapperExitCode = exitCode;
                restartCount++;
                if (restartCount <= MAX_RESTARTS) {
                    logger.info("app.owlcms.Main exited with code {} - restart {}/{}", 
                                 exitCode, restartCount, MAX_RESTARTS);
                    Thread.sleep(5000);
                    logger.info("Restarting app.owlcms.Main...");
                }
                
            } catch (InterruptedException e) {
                logger.info("MainWrapper interrupted, exiting", e);
                Thread.currentThread().interrupt();
                wrapperExitCode = 130;
                break;
            } catch (IOException e) {
                wrapperExitCode = 1;
                restartCount++;
                if (restartCount <= MAX_RESTARTS) {
                    logger.error("Failed to start app.owlcms.Main (restart {}/{}): {}", 
                                 restartCount, MAX_RESTARTS, e.getMessage(), e);
                    Thread.sleep(5000);
                    logger.info("Retrying app.owlcms.Main...");
                }
            }
        }
        if (restartCount > MAX_RESTARTS) {
            logger.error("Restart limit ({}) reached, wrapper exiting with code {}", MAX_RESTARTS, wrapperExitCode);
        }
        if (wrapperExitCode != 0) {
            System.exit(wrapperExitCode);
        }
    }
}
