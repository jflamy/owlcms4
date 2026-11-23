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
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import app.owlcms.access.SameMachineProof;
import app.owlcms.endpoints.ControlPanelServlet;
import ch.qos.logback.classic.Logger;

/**
 * Wrapper that launches app.owlcms.Main as a separate process and automatically restarts it 
 * if it exits with non-zero status. Passes through all VM arguments and environment variables.
 *
 * @author Jean-François Lamy
 */
public class MainWrapper {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(MainWrapper.class);
    /** Restarts in a row where OWLCMS did not complete its startup; a restart that worked starts the count again. */
    private static final int MAX_RESTARTS = 3;
    /** Time given to owlcms to stop its services and finalize the audit log before it is force-killed. */
    private static final int SHUTDOWN_WAIT_SECONDS = 10;
    /** Bound on the stop request; it is part of {@link #SHUTDOWN_WAIT_SECONDS}. */
    private static final Duration STOP_REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final String DAEMON_MODE_ENV = "CONTROLPANEL_RUN_AS_DAEMON";
    private static final String WRAPPER_PID_ENV = "OWLCMS_WRAPPER_PID";
    private static final String LOAD_JSON_ENV = "OWLCMS_LOADJSON";
    private static final String LOAD_JSON_PROPERTY = "loadJson";
    public static final String DEFAULT_CONTROL_PANEL_VERSION = "3.10.0-alpha02";
    private static volatile Process currentProcess;
    private static volatile boolean shuttingDown = false;

    private static boolean isDaemonMode(Map<String, String> environment) {
        String value = environment.get(DAEMON_MODE_ENV);
        if (value == null) {
            return false;
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
        case "1", "true", "yes", "on" -> true;
        default -> false;
        };
    }

    /**
     * A launch that loads an export is never restarted: a restart would load the file again and replace whatever
     * happened since, whether the first load failed or OWLCMS stopped later.
     */
    private static boolean isLoadJsonLaunch(Map<String, String> environment) {
        String value = environment.get(LOAD_JSON_ENV);
        if (value == null || value.isBlank()) {
            value = System.getProperty(LOAD_JSON_PROPERTY);
        }
        return value != null && !value.isBlank();
    }

    /** @return the VM arguments without the system-property form of OWLCMS_LOADJSON */
    static List<String> withoutStartupImport(List<String> vmArgs) {
        String property = "-D" + LOAD_JSON_PROPERTY;
        List<String> kept = new ArrayList<>();
        for (String arg : vmArgs) {
            if (!arg.equals(property) && !arg.startsWith(property + "=")) {
                kept.add(arg);
            }
        }
        return kept;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /**
     * @param started true if the process that just exited had completed its startup
     * @return the number of restarts in a row without a completed startup, counting the one about to be made
     */
    static int restartCountAfterExit(int restartCount, boolean started) {
        return (started ? 0 : restartCount) + 1;
    }

    /** @return the file that app.owlcms.Main creates once its startup has completed; null if it cannot be set up */
    private static Path createStartedMarker() {
        try {
            Path marker = Files.createTempFile("owlcms-started-", ".marker");
            marker.toFile().deleteOnExit();
            return marker;
        } catch (IOException | RuntimeException e) {
            logger./**/warn("Cannot create the startup marker, restarts will not reset the restart count: {}",
                    e.toString());
            return null;
        }
    }

    /** @return true if the marker was present, i.e. the process had completed its startup; the marker is removed */
    private static boolean consumeStartedMarker(Path marker) {
        try {
            return marker != null && Files.deleteIfExists(marker);
        } catch (IOException | RuntimeException e) {
            logger./**/warn("Cannot read the startup marker {}: {}", marker, e.toString());
            return false;
        }
    }

    /**
     * Asks owlcms to stop itself, so that it closes its inputs, finalizes the audit log and exits 0. The stop request
     * of the control panel is used when owlcms was given a control panel token, which it requires; it works on every
     * platform. Otherwise, or if the request is not accepted, the process is asked to terminate normally (SIGTERM),
     * which runs the same shutdown, where the platform supports it. On Windows that would kill the process at once,
     * so owlcms is then left to stop on its own until the deadline.
     */
    private static void requestGracefulStop(Process p) throws InterruptedException {
        String token = SameMachineProof.configuredToken();
        if (token != null) {
            int port = Main.configuredServerPort();
            try {
                int status = sendStopRequest(port, token);
                if (status >= 200 && status < 300) {
                    logger.info("Subprocess accepted the stop request on port {}", port);
                    return;
                }
                logger./**/warn("Subprocess refused the stop request on port {} (HTTP {})", port, status);
            } catch (ConnectException e) {
                // expected when the signal that stops the wrapper also reached owlcms, which closes its web server first
                logger.info("Subprocess is not accepting requests on port {}: already stopping, or not started", port);
            } catch (IOException e) {
                logger./**/warn("Stop request to the subprocess on port {} failed: {}", port, e.toString());
            }
        }
        if (p.supportsNormalTermination()) {
            logger.info("Asking subprocess PID {} to terminate", p.pid());
            p.destroy();
        } else {
            logger.info("No graceful stop available for subprocess PID {}: owlcms has no control panel token",
                    p.pid());
        }
    }

    /** @return the HTTP status of the control panel stop request sent to the owlcms on this machine */
    private static int sendStopRequest(int port, String token) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder().connectTimeout(STOP_REQUEST_TIMEOUT).build();
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + "/controlpanel/stop?by=" + ControlPanelServlet.BY_WRAPPER))
                .timeout(STOP_REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /** Emergency stop: something is stuck. The process and its descendants are killed; hooks do not run. */
    private static void forceKill(Process p) {
        p.toHandle().descendants().forEach(child -> {
            logger.info("Killing descendant PID {}", child.pid());
            child.destroyForcibly();
        });
        p.destroyForcibly();
        try {
            p.waitFor(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String... args) throws Exception {
        // Stop the subprocess when the wrapper is terminated: ask owlcms to stop, then force it after the deadline
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shuttingDown = true;
            Process p = currentProcess;
            logger.info("Shutdown hook triggered");
            if (p == null || !p.isAlive()) {
                logger.info("No subprocess to terminate (p={}, alive={})", p, (p != null ? p.isAlive() : "n/a"));
                return;
            }
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(SHUTDOWN_WAIT_SECONDS);
            try {
                requestGracefulStop(p);
                // Let the child finish its own shutdown: inputs close, then the audit log is sealed and finalized; a
                // kill before that would leave the last audit records unsealed
                logger.info("Waiting up to {}s for subprocess to shut down gracefully", SHUTDOWN_WAIT_SECONDS);
                if (p.waitFor(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS)) {
                    logger.info("Subprocess exited gracefully");
                } else {
                    logger./**/warn("Subprocess still running after {}s, force-killing", SHUTDOWN_WAIT_SECONDS);
                    forceKill(p);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Interrupted while waiting for the subprocess, force-killing");
                forceKill(p);
            } catch (RuntimeException e) {
                logger.error("Error during shutdown: {}", e.getMessage());
                forceKill(p);
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
        // set once Main asks for a restart after its startup load: later launches keep the current database
        boolean startupImportDropped = false;
        Path startedMarker = createStartedMarker();
        while (restartCount <= MAX_RESTARTS) {
            try {
                logger.info("Starting app.owlcms.Main as separate process (restart count: {})", restartCount);
                
                // Build the command
                List<String> command = new ArrayList<>();
                command.add(javaBin);
                command.addAll(startupImportDropped ? withoutStartupImport(vmArgs) : vmArgs);
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
                subprocessEnv.putIfAbsent("OWLCMS_CONTROLPANEL", DEFAULT_CONTROL_PANEL_VERSION); // Fallback enables restart after JSON import
                if (startupImportDropped) {
                    subprocessEnv.remove(LOAD_JSON_ENV);
                }
                if (!isWindows() && !isDaemonMode(env)) {
                    subprocessEnv.put(WRAPPER_PID_ENV, Long.toString(ProcessHandle.current().pid()));
                }
                consumeStartedMarker(startedMarker);
                if (startedMarker != null) {
                    subprocessEnv.put(Main.WRAPPER_STARTED_FILE_ENV, startedMarker.toString());
                }
                
                currentProcess = pb.start();
                
                // Wait for process to complete
                int exitCode = currentProcess.waitFor();
                currentProcess = null;
                boolean started = consumeStartedMarker(startedMarker);
                
                logger.info("Subprocess exited with code {}", exitCode);
                
                if (shuttingDown) {
                    logger.info("Wrapper is shutting down, not restarting (subprocess exit code {})", exitCode);
                    wrapperExitCode = exitCode;
                    break;
                }
                
                if (exitCode == Main.EXIT_RESTART_WITHOUT_STARTUP_IMPORT) {
                    if (!startupImportDropped) {
                        logger.info("app.owlcms.Main asked for a restart after its startup load; restarting without {}",
                                LOAD_JSON_ENV);
                    }
                    startupImportDropped = true;
                } else if (!startupImportDropped && isLoadJsonLaunch(env)) {
                    logger.info("app.owlcms.Main exited with code {} and {} is set, not restarting", exitCode,
                            LOAD_JSON_ENV);
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
                restartCount = restartCountAfterExit(restartCount, started);
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
