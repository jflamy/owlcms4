/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors.websocket;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.monitors.websocket.ForwarderPayloadBuilder.CompetitionDataExport;
import app.owlcms.utils.FlagsZipHelper;
import app.owlcms.utils.LogosZipHelper;
import app.owlcms.utils.DatabaseZipHelper;
import app.owlcms.utils.PicturesZipHelper;
import app.owlcms.utils.TranslationsZipHelper;
import ch.qos.logback.classic.Logger;

/**
 * Handles WebSocket communication for forwarding events to trackers.
 * WebSocket-only - no HTTP support.
 * Manages debouncing, URL tracking, and message delivery.
 */
public class WebSocketSender {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(WebSocketSender.class);

	private final FieldOfPlay fop;
	private final Map<String, Integer> debouncingHash = new HashMap<>();
	private final Map<String, Long> debouncingMillis = new HashMap<>();
	private String currentPublicResultsUrl = null;
	private String currentVideoDataUrl = null;

	public WebSocketSender(FieldOfPlay fop) {
		this.fop = fop;
	}

	/**
	 * Send data via WebSocket connection.
	 * WebSocket-only - URL must start with ws:// or wss://.
	 * 
	 * @param url WebSocket URL (ws:// or wss://)
	 * @param updateKey update key for authentication (optional)
	 * @param parameters data to send
	 * @param messageType type of message (update, timer, decision)
	 */
	public void send(String url, String updateKey, Map<String, ?> parameters, String messageType) {
		if (url == null) {
			return;
		}

		// Only WebSocket URLs are supported
		if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
			logger.debug("{}ignoring non-WebSocket URL: {}", FieldOfPlay.getLoggingName(fop), url);
			return;
		}

		sendWebSocket(url, messageType, parameters);
	}

	/**
	 * Send data via WebSocket connection with message type.
	 */
	public void sendWebSocket(String url, String messageType, Map<String, ?> parameters) {
		// Determine which URL this is (publicResults or videoData)
		Config current = Config.getCurrent();
		String publicResultsUrl = current.getParamUpdateUrl();
		String videoDataUrl = current.getParamVideoDataUpdateUrl();

		boolean isPublicResults = url != null && url.equals(publicResultsUrl);
		boolean isVideoData = url != null && url.equals(videoDataUrl);

		// Check if URL has changed and close old connection if needed
		if (isPublicResults && this.currentPublicResultsUrl != null && !this.currentPublicResultsUrl.equals(url)) {
			logger.info("{}PublicResults URL changed from {} to {}, closing old connection",
			        FieldOfPlay.getLoggingName(fop), this.currentPublicResultsUrl, url);
			WebSocketEventSender.closeSender(this.currentPublicResultsUrl);
			this.currentPublicResultsUrl = url;
		} else if (isPublicResults && this.currentPublicResultsUrl == null) {
			this.currentPublicResultsUrl = url;
		}

		if (isVideoData && this.currentVideoDataUrl != null && !this.currentVideoDataUrl.equals(url)) {
			logger.info("{}VideoData URL changed from {} to {}, closing old connection",
			        FieldOfPlay.getLoggingName(fop), this.currentVideoDataUrl, url);
			WebSocketEventSender.closeSender(this.currentVideoDataUrl);
			this.currentVideoDataUrl = url;
		} else if (isVideoData && this.currentVideoDataUrl == null) {
			this.currentVideoDataUrl = url;
		}

		Integer previousDebounceHash = this.debouncingHash.get(url);
		Long previousDebounceMillis = this.debouncingMillis.get(url);
		long deltaMillis = System.currentTimeMillis() - (previousDebounceMillis != null ? previousDebounceMillis : 0);
		Integer hashCode = ForwarderPayloadBuilder.computeParametersHash(parameters);

		// debounce, sometimes several identical updates in a rapid succession
		// identical updates are ok after 1 sec.
		if (!hashCode.equals(previousDebounceHash) || (deltaMillis > 1000)) {
			// Pass URL supplier so sender can re-check config on reconnect
			WebSocketEventSender sender;
			if (isPublicResults) {
				sender = WebSocketEventSender.getOrCreate(url, () -> Config.getCurrent().getParamUpdateUrl());
			} else if (isVideoData) {
				sender = WebSocketEventSender.getOrCreate(url, () -> Config.getCurrent().getParamVideoDataUpdateUrl());
			} else {
				sender = WebSocketEventSender.getOrCreate(url);
			}

			if (sender != null) {
				// Set up callback for 428 status response (database requested)
				sender.setMissingDataCallback("database", () -> {
					Config currentCallback = Config.getCurrent();
					String updateKey = currentCallback.getParamUpdateKey();
					if (updateKey == null) {
						updateKey = currentCallback.getParamVideoDataKey();
					}
					sendFullCompetitionData(url, updateKey);
				});

				// Set up callback for 428 status response (flags requested)
				sender.setMissingDataCallback("flags", () -> {
					sendFlags(url);
				});

				// Set up callback for 428 status response (translations requested)
				sender.setMissingDataCallback("translations", () -> {
					sendTranslations(url);
				});

				sender.send(messageType, parameters);
			}

			this.debouncingHash.put(url, hashCode);
			this.debouncingMillis.put(url, System.currentTimeMillis());
		}
	}

	/**
	 * Send full competition data to a URL.
	 */
	/**
	 * Send competition database via WebSocket using compressed binary ZIP format.
	 * SINGLE DATABASE SENDING ROUTINE - always uses binary ZIP for efficiency (70-80% reduction).
	 * 
	 * Called when:
	 * 1. Remote system requests database (via 428 Precondition Required status)
	 * 2. Initial connection to tracker
	 * 
	 * Binary ZIP format is used for all WebSocket transmissions:
	 * - Typical database: ~700KB JSON → ~200KB ZIP
	 * - Provides consistent, efficient transmission
	 * 
	 * @param url the WebSocket URL to send the database to
	 * @param updateKey the update key for validation (optional, not used for WebSocket binary format)
	 */
	public void sendFullCompetitionData(String url, String updateKey) {
		logger.debug("{}sendFullCompetitionData called for url: {}", FieldOfPlay.getLoggingName(fop), url);

		if (url == null) {
			logger.error("cannot send database, url is null");
			return;
		}

		CompetitionDataExport export = ForwarderPayloadBuilder.exportCompetitionData(fop);
		if (export == null) {
			logger.debug("{}unable to build competition data payload for {}", FieldOfPlay.getLoggingName(fop), url);
			return;
		}

		// Only support WebSocket for database transmission (binary ZIP format)
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				// Compress database using binary ZIP format
				// Use export.structure() (parsed object) not export.json() (string)
				byte[] databaseZipBytes = DatabaseZipHelper.createDatabaseZipBytes(
					export.structure()
				);

				if (databaseZipBytes.length > 0) {
					boolean sent = sender.sendBinary("database_zip", databaseZipBytes);
					if (sent) {
						// Log compression ratio for reference
						String jsonDatabase = export.json();
						double ratio = 100.0 * (1.0 - (double) databaseZipBytes.length / jsonDatabase.getBytes().length);
						logger.info(
							"{}sent database ZIP via WebSocket to {} ({} bytes, from {}, {:.1f}% reduction)",
							FieldOfPlay.getLoggingName(fop), url, databaseZipBytes.length,
							jsonDatabase.getBytes().length, ratio
						);
					} else {
						logger.debug(
							"{}could not send database ZIP via WebSocket to {} (socket not ready)",
							FieldOfPlay.getLoggingName(fop), url
						);
					}
				} else {
					logger.error("{}failed to create database ZIP for {}",
						FieldOfPlay.getLoggingName(fop), url);
				}
			}
			return;
		}

		logger.debug("{}database transmission requires WebSocket ({})",
			FieldOfPlay.getLoggingName(fop), url);
	}

	/**
	 * Send flags directory as a zipped archive via WebSocket.
	 */
	public void sendFlags(String url) {
		logger.debug("{}sendFlags called for url: {}", FieldOfPlay.getLoggingName(fop), url);

		if (url == null) {
			logger.error("cannot send flags, url is null");
			return;
		}

		if (!FlagsZipHelper.hasFlagsAvailable()) {
			logger.debug("{}flags not available, cannot send", FieldOfPlay.getLoggingName(fop));
			return;
		}

		// Check if URL is WebSocket
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				byte[] flagsZipBytes = FlagsZipHelper.createFlagsZipBytes();
				if (flagsZipBytes.length > 0) {
					boolean sent = sender.sendBinary("flags_zip", flagsZipBytes);
					if (sent) {
						logger.debug("{}sent flags_zip ZIP via WebSocket binary to {} ({} bytes)",
						        FieldOfPlay.getLoggingName(fop), url, flagsZipBytes.length);
					} else {
						logger.debug("{}could not send flags_zip ZIP via WebSocket to {} (socket not ready)",
						        FieldOfPlay.getLoggingName(fop), url);
					}
				} else {
					logger.debug("{}failed to create flags ZIP for {}", FieldOfPlay.getLoggingName(fop), url);
				}
			}
			return;
		}

		logger.debug("{}HTTP endpoint for flags not implemented ({})", FieldOfPlay.getLoggingName(fop), url);
	}

	/**
	 * Send all translations for all locales as a zipped JSON archive via WebSocket.
	 */
	public void sendTranslations(String url) {
		logger.debug("{}sendTranslations called for url: {}", FieldOfPlay.getLoggingName(fop), url);

		if (url == null) {
			logger.error("cannot send translations, url is null");
			return;
		}

		if (!TranslationsZipHelper.hasTranslationsAvailable()) {
			logger.debug("{}translations not available, cannot send", FieldOfPlay.getLoggingName(fop));
			return;
		}

		// Check if URL is WebSocket
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				byte[] translationsZipBytes = TranslationsZipHelper.createTranslationsZipBytes();
				if (translationsZipBytes.length > 0) {
					boolean sent = sender.sendBinary("translations_zip", translationsZipBytes);
					if (sent) {
						logger.debug(
						        "{}sent translations ZIP via WebSocket binary to {} ({} bytes with all 26 locales)",
						        FieldOfPlay.getLoggingName(fop), url, translationsZipBytes.length);
					} else {
						logger.debug("{}could not send translations ZIP via WebSocket to {} (socket not ready)",
						        FieldOfPlay.getLoggingName(fop), url);
					}
				} else {
					logger.debug("{}failed to create translations ZIP for {}", FieldOfPlay.getLoggingName(fop), url);
				}
			}
			return;
		}

		logger.debug("{}non-WebSocket URL not supported ({})", FieldOfPlay.getLoggingName(fop), url);
	}

	/**
	 * Static method to register startup data callbacks for WebSocket connections.
	 * When a connection opens, sends database, translations_zip, and flags_zip in sequence.
	 * 
	 * @param videoUrl  the video data WebSocket URL (if configured)
	 * @param updateUrl the public results WebSocket URL (if configured)
	 */
	public static void registerStartupDataCallbacks(String videoUrl, String updateUrl) {
		logger.info("Registering startup data callbacks for WebSocket trackers (videoUrl={}, updateUrl={})", 
				videoUrl, updateUrl);

		// Export competition data once (for all connections)
		CompetitionDataExport export = ForwarderPayloadBuilder.exportCompetitionDataStatic();
		if (export == null) {
			logger.error("Unable to build competition data payload for startup - aborting WebSocket registration");
			return;
		}

		// Create translations ZIP bytes once
		if (!TranslationsZipHelper.hasTranslationsAvailable()) {
			logger.error("Translations not available for startup send - aborting WebSocket registration");
			return;
		}
		byte[] translationsZipBytes = TranslationsZipHelper.createTranslationsZipBytes();

		// Create flags ZIP bytes once (optional - may not exist)
		final byte[] flagsZipBytes = FlagsZipHelper.hasFlagsAvailable()
		        ? FlagsZipHelper.createFlagsZipBytes()
		        : new byte[0];

		// Create pictures ZIP bytes once (optional - may not exist)
		final byte[] picturesZipBytes = PicturesZipHelper.hasPicturesAvailable()
		        ? PicturesZipHelper.createPicturesZipBytes()
		        : new byte[0];

		// Create logos ZIP bytes once (optional - may not exist)
		final byte[] logosZipBytes = LogosZipHelper.hasLogosAvailable()
		        ? LogosZipHelper.createLogosZipBytes()
		        : new byte[0];

		// Register for video data URL
		if (videoUrl != null && !videoUrl.trim().isEmpty()
		        && (videoUrl.startsWith("ws://") || videoUrl.startsWith("wss://"))) {
			registerStartupCallbacksForUrl(videoUrl, export, translationsZipBytes, flagsZipBytes, picturesZipBytes, logosZipBytes);
		}

		// Register for public results URL
		if (updateUrl != null && !updateUrl.trim().isEmpty()
		        && (updateUrl.startsWith("ws://") || updateUrl.startsWith("wss://"))) {
			registerStartupCallbacksForUrl(updateUrl, export, translationsZipBytes, flagsZipBytes, picturesZipBytes, logosZipBytes);
		}
	}

	private static void registerStartupCallbacksForUrl(String url, CompetitionDataExport export,
	        byte[] translationsZipBytes, byte[] flagsZipBytes, byte[] picturesZipBytes, byte[] logosZipBytes) {
		logger.info("Startup send mode for {}: BINARY(database_zip)", url);

		// Synchronize to ensure callbacks are registered before the WebSocket connection
		// can open and send messages (preventing "No callback registered" errors)
		synchronized (WebSocketEventSender.class) {
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				// Register missing data callbacks FIRST (before onOpenCallback)
				// This ensures callbacks are available if the connection opens immediately
				sender.setMissingDataCallback("database", () -> {
					// Create ZIP on-demand when requested
					byte[] zipBytes = DatabaseZipHelper.createDatabaseZipBytes(export.structure());
					if (zipBytes.length > 0) {
						sender.sendBinary("database_zip", zipBytes);
					} else {
						logger.error("No database ZIP available to send to {}", url);
					}
				});

				sender.setMissingDataCallback("translations_zip", () -> {
					sender.sendBinary("translations_zip", translationsZipBytes);
				});

				sender.setMissingDataCallback("flags_zip", () -> {
					sender.sendBinary("flags_zip", flagsZipBytes);
				});

				// Pictures are sent on-demand only, not at startup
				sender.setMissingDataCallback("pictures_zip", () -> {
					if (picturesZipBytes.length > 0) {
						sender.sendBinary("pictures_zip", picturesZipBytes);
					}
				});

				// Logos are sent on-demand only, not at startup
				sender.setMissingDataCallback("logos_zip", () -> {
					if (logosZipBytes.length > 0) {
						sender.sendBinary("logos_zip", logosZipBytes);
					}
				});

				// Single onOpenCallback that sends all three data types
				// IMPORTANT: Send database first (binary) to authenticate if needed,
				// then send binary frames (translations_zip, flags_zip) which require prior auth
				sender.setOnOpenCallback(() -> {
					logger.info("WebSocket connected to {}, sending startup data (mode=BINARY)", url);

					// Send database FIRST as binary ZIP
					// Create ZIP now that socket is open - no race condition
					byte[] databaseZipBytes = DatabaseZipHelper.createDatabaseZipBytes(export.structure());
					if (databaseZipBytes.length > 0) {
						boolean sent = sender.sendBinary("database_zip", databaseZipBytes);
						if (sent) {
							logger.info("Sent startup database_zip via WebSocket to {} (auth step)", url);
						} else {
							logger.error("Could not send startup database_zip via WebSocket to {} (socket not ready)", url);
						}
					} else {
						logger.error("No database ZIP prepared for startup send to {}", url);
					}

					// Send binary frames AFTER authentication (requires valid updateKey from database message)
					// Send translations_zip
					boolean sentBin = sender.sendBinary("translations_zip", translationsZipBytes);
					if (sentBin) {
						logger.info("Sent startup translations_zip via WebSocket to {}", url);
					} else {
						logger.error("Could not send startup translations_zip via WebSocket to {} (socket not ready)", url);
					}

					// Send flags_zip (optional - may not exist)
					if (flagsZipBytes != null && flagsZipBytes.length > 0) {
						sentBin = sender.sendBinary("flags_zip", flagsZipBytes);
						if (sentBin) {
							logger.info("Sent startup flags_zip via WebSocket to {}", url);
						} else {
							logger.warn("Could not send startup flags_zip via WebSocket to {} (socket not ready)", url);
						}
					}
				});
			}
		}
	}

	// Getters for current URL tracking
	public String getCurrentPublicResultsUrl() {
		return currentPublicResultsUrl;
	}

	public String getCurrentVideoDataUrl() {
		return currentVideoDataUrl;
	}
}
