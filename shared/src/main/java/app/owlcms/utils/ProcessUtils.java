package app.owlcms.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class ProcessUtils {
    static Logger logger = (Logger) LoggerFactory.getLogger(ProcessUtils.class);

    private static boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public static void main(String[] args) throws Exception {
        // Where we want to execute
        File location = new File("/Users/beknazarsuranchiyev/Desktop");

        runCommand(location, "ls"); // for Mac(Linux based OS) users list files

        // runCommand(location, "dir"); // For Windows users list files
    }

    public static void runCommand(File whereToRun, String command) throws Exception {
        System.out.println("Running in: " + whereToRun);
        System.out.println("Command: " + command);

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(whereToRun);

        if (isWindows) {
            builder.command("cmd.exe", "/c", command);
        } else {
            builder.command("sh", "-c", command);
        }

        Process process = builder.start();
        OutputStream outputStream = process.getOutputStream();
        InputStream inputStream = process.getInputStream();
        InputStream errorStream = process.getErrorStream();

        CompletableFuture<Process> onProcessExit = process.onExit();
        onProcessExit.thenAccept(ph -> {
            logger.info("PID: {} has stopped", ph.pid());
            try {
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
            }
        });

        printStream(inputStream, Level.DEBUG);
        printStream(errorStream, Level.WARN);
    }

    private static void printStream(InputStream inputStream, Level level) throws IOException {
        new Thread(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (level == Level.DEBUG) {
                        logger.debug(line);
                    } else if (level == Level.INFO) {
                        logger.info(line);
                    } else if (level == Level.WARN) {
                        logger.warn(line);
                    }
                 }
            } catch (IOException e) {
            }
        }).start();
    }
}
