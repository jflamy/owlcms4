/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Utility class to manage configuration file requests across all servlets.
 * Uses a shared lock to prevent multiple simultaneous requests.
 */
public class ConfigurationRequestUtil {

    private static final int WAIT_FOR_CONFIG = 5 * 1000; // 5 seconds
    private static final Lock configLock = new ReentrantLock();
    private static final boolean USE_FILES_DIR = false; // true = filesystem temp dir, false = in-memory (default)
    
    private static volatile long lastConfigRequestTime = 0;
    private static volatile boolean configRequested = false;
    private static volatile boolean configurationReceived = false; // Track if we've actually received config files

    private static final Logger logger = (Logger) LoggerFactory.getLogger(ConfigurationRequestUtil.class);

    /**
     * Requests configuration files if the local override directory is missing.
     * Uses a shared lock across all servlets to prevent multiple concurrent requests.
     *
     * @param resp the HTTP response to send the error
     * @return true if configuration is missing and request was sent, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean requestConfigIfMissing(HttpServletResponse resp) throws IOException {
        return requestConfigIfMissing(resp, null);
    }

    /**
     * Requests configuration files if the local override directory is missing.
     * Uses a shared lock across all servlets to prevent multiple concurrent requests.
     *
     * @param resp the HTTP response to send the error
     * @param servletName the name of the servlet making the request (for logging)
     * @return true if configuration is missing and request was sent, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public static boolean requestConfigIfMissing(HttpServletResponse resp, String servletName) throws IOException {
        boolean doReturn = false;
        String caller = servletName != null ? " from " + servletName : "";
        
        // If we've received configuration, allow normal operation (retry will succeed)
        if (configurationReceived) {
            if (StartupUtils.isDebugSetting()) {
                logger.debug("Configuration already received{}, proceeding normally", caller);
            }
            return false;
        }
        
        if (StartupUtils.isDebugSetting()) {
            logger.warn("Configuration check{}: configRequested = {}, configurationReceived = {}", caller, configRequested, configurationReceived);
        }
        
        // Check if timeout has passed since last request
        long now = System.currentTimeMillis();
        if (configRequested && (now - lastConfigRequestTime) > WAIT_FOR_CONFIG) {
            if (StartupUtils.isDebugSetting()) {
                logger.warn("Configuration request timeout, resetting");
            }
            configRequested = false;
        }
        
        if (!configRequested) {
            if (configLock.tryLock()) {
                try {
                    // Double-check after acquiring lock
                    if (!configRequested) {
                        if (StartupUtils.isDebugSetting()) {
                            String message = "Local override directory not present: requesting remote configuration files" + caller + ".";
                            logger.info(message);
                            logger.info("requesting customization");
                            logger.info("Sending 412 to trigger configuration upload, client will retry after config is received{}", caller);
                        }
                        resp.sendError(412, "Missing configuration files.");
                        configRequested = true;
                        lastConfigRequestTime = System.currentTimeMillis();
                        doReturn = true;
                        Thread.sleep(WAIT_FOR_CONFIG);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    configLock.unlock();
                }
            } else {
                if (StartupUtils.isDebugSetting()) {
                    logger.info("configuration has already been requested{}, exiting", caller);
                }
                doReturn = true;
            }
        } else {
            if (StartupUtils.isDebugSetting()) {
                logger.info("configuration already requested{}, waiting for retry after config is received", caller);
            }
            doReturn = true;
        }
        
        return doReturn;
    }

    /**
     * Marks that configuration has been successfully received.
     * Should be called when configuration files are unpacked/loaded.
     */
    public static void markConfigurationReceived() {
        if (StartupUtils.isDebugSetting()) {
            logger.info("Configuration marked as received");
        }
        configurationReceived = true;
        resetConfigurationRequest();
    }

    /**
     * Returns whether to use filesystem-based temp directory for configuration.
     * @return true for filesystem temp dir, false for in-memory
     */
    public static boolean useFilesDir() {
        return USE_FILES_DIR;
    }

    /**
     * Resets the configuration request state. Should be called when configuration is successfully received.
     */
    public static void resetConfigurationRequest() {
        configRequested = false;
        lastConfigRequestTime = 0;
        logger.info("Configuration request state reset");
    }

}
