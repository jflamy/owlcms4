/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors.websocket;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.monitors.websocket.ForwarderPayloadBuilder.CompetitionDataExport;
import app.owlcms.utils.FlagsZipHelper;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.PicturesZipHelper;
import app.owlcms.utils.TranslationsZipHelper;
import ch.qos.logback.classic.Logger;

/**
 * Handles WebSocket and HTTP communication for forwarding events.
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
	 * Send data via WebSocket or HTTP POST based on URL scheme.
	 */
	public void sendPost(String url, String updateKey, Map<String, ?> parameters, String messageType) {
		if (url == null) {
			return;
		}

		// Check if URL is WebSocket (ws:// or wss://)
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			sendWebSocket(url, messageType, parameters);
			return;
		}

		Integer previousDebounceHash = this.debouncingHash.get(url);
		Long previousDebounceMillis = this.debouncingMillis.get(url);
		long deltaMillis = System.currentTimeMillis() - (previousDebounceMillis != null ? previousDebounceMillis : 0);
		Integer hashCode = ForwarderPayloadBuilder.computeParametersHash(parameters);

		// debounce, sometimes several identical updates in a rapid succession
		// identical updates are ok after 1 sec.
		if (!hashCode.equals(previousDebounceHash) || (deltaMillis > 1000)) {
			new Thread(() -> doPost(url, updateKey, parameters)).start();

			this.debouncingHash.put(url, hashCode);
			this.debouncingMillis.put(url, System.currentTimeMillis());
		}
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
	public void sendFullCompetitionData(String url, String updateKey) {
		logger.debug("{}sendFullCompetitionData called for url: {}", FieldOfPlay.getLoggingName(fop), url);

		if (url == null) {
			logger.error("cannot send full competition data, url or updateKey is null - url:{}, updateKey:{}", url,
			        updateKey);
			return;
		}
		if (updateKey == null) {
			logger.debug("no updateKey configured for {}, proceeding without one", url);
		}

		CompetitionDataExport export = ForwarderPayloadBuilder.exportCompetitionData(fop);
		if (export == null) {
			logger.debug("{}unable to build competition data payload for {}", FieldOfPlay.getLoggingName(fop), url);
			return;
		}

		// Check if URL is WebSocket
		if (url.startsWith("ws://") || url.startsWith("wss://")) {
			// Send via WebSocket with checksum and parsed JSON structure
			WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
			if (sender != null) {
				Map<String, Object> payload = new LinkedHashMap<>();
				payload.put("databaseChecksum", export.checksum());
				payload.put("database", export.structure());
				boolean sent = sender.sendObject("database", payload);
				if (sent) {
					logger.debug("{}sent full competition data via WebSocket to {}",
					        FieldOfPlay.getLoggingName(fop), url);
				} else {
					logger.debug("{}could not send full competition data via WebSocket to {} (socket not ready)",
					        FieldOfPlay.getLoggingName(fop), url);
				}
			}
			return;
		}

		// HTTP POST path - wrap with checksum for database endpoint
		try {
			// ALWAYS construct the database endpoint URL - extract base URL and add /database
			String baseUrl = url;
			// Remove any path after the port/host
			if (baseUrl.contains("://")) {
				String[] parts = baseUrl.split("://");
				if (parts.length == 2) {
					String protocol = parts[0];
					String hostPart = parts[1];
					// Find the first slash after the host:port
					int slashIndex = hostPart.indexOf('/');
					if (slashIndex != -1) {
						hostPart = hostPart.substring(0, slashIndex);
					}
					baseUrl = protocol + "://" + hostPart;
				}
			}
			String databaseUrl = baseUrl + "/database";
			logger.debug("{}ALWAYS sending to database endpoint: {} (from original: {})",
			        FieldOfPlay.getLoggingName(fop), databaseUrl, url);
			HttpPost post = new HttpPost(databaseUrl);

			// Wrap database with checksum in JSON structure
			Map<String, Object> wrapper = new LinkedHashMap<>();
			wrapper.put("databaseChecksum", export.checksum());
			// Parse the JSON string to include as nested structure
			try {
				Object databaseStructure = ForwarderPayloadBuilder.getObjectMapper().readValue(export.json(),
				        Object.class);
				wrapper.put("database", databaseStructure);
			} catch (Exception parseEx) {
				logger.error("{}failed to parse competition data JSON: {}",
				        FieldOfPlay.getLoggingName(fop), LoggerUtils.exceptionMessage(parseEx));
				return;
			}
			String wrappedJson = ForwarderPayloadBuilder.getObjectMapper().writeValueAsString(wrapper);

			// Send the wrapped JSON data
			post.setHeader("Content-Type", "application/json; charset=UTF-8");
			post.setEntity(new StringEntity(wrappedJson, "UTF-8"));

			logger.debug("{}posting database with checksum to endpoint {}",
			        FieldOfPlay.getLoggingName(fop), databaseUrl);

			try (CloseableHttpClient httpClient = HttpClients.createDefault();
			        CloseableHttpResponse response = httpClient.execute(post)) {
				StatusLine statusLine = response.getStatusLine();
				Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
				if (statusCode != null && statusCode != 200) {
					if (statusCode == 404) {
						logger.debug(
						        "{}database endpoint not available at {} - 404 Not Found (endpoint not implemented)",
						        FieldOfPlay.getLoggingName(fop), databaseUrl);
					} else if (statusCode >= 500) {
						logger.error("{}server error sending to database endpoint {} - response: {}",
						        FieldOfPlay.getLoggingName(fop), databaseUrl, statusLine);
					} else if (statusCode >= 400) {
						logger.error("{}client error sending to database endpoint {} - response: {}",
						        FieldOfPlay.getLoggingName(fop), databaseUrl, statusLine);
					} else {
						logger.debug("{}unexpected response from database endpoint {} - response: {}",
						        FieldOfPlay.getLoggingName(fop), databaseUrl, statusLine);
					}
				} else {
					logger.debug("{}successfully sent full competition data to database endpoint {} - response: 200 OK",
					        FieldOfPlay.getLoggingName(fop), databaseUrl);
				}
				EntityUtils.toString(response.getEntity());
			} catch (Exception e1) {
				logger.debug("{}database endpoint not available at {} - {} (this is not fatal)",
				        FieldOfPlay.getLoggingName(fop), databaseUrl, LoggerUtils.exceptionMessage(e1));
			}
		} catch (Exception e2) {
			logger.debug("{}could not send full competition data to {} - {} (this is not fatal)",
			        FieldOfPlay.getLoggingName(fop), url, LoggerUtils.exceptionMessage(e2));
		}
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

		logger.debug("{}HTTP endpoint for translations not implemented ({})", FieldOfPlay.getLoggingName(fop), url);
	}

	/**
	 * Send configuration to HTTP endpoint.
	 */
	public void sendConfig(String destination, Map<String, String> config) {
		if (destination == null) {
			return;
		}

		if (destination.startsWith("ws://") || destination.startsWith("wss://")) {
			// WebSocket doesn't need config send
			return;
		}

		if (destination.endsWith("/update") || destination.endsWith("/timer") || destination.endsWith("/decision")) {
			try {
				HttpPost post = new HttpPost(destination.replace("/update", "/config").replace("/timer", "/config")
				        .replace("/decision", "/config"));

				MultipartEntityBuilder builder = MultipartEntityBuilder.create();
				for (Entry<String, String> entry : config.entrySet()) {
					builder.addPart(entry.getKey(),
					        new StringBody(entry.getValue(), ContentType.create("text/plain", "UTF-8")));
				}
				HttpEntity entity = builder.build();
				post.setEntity(entity);

				try (CloseableHttpClient httpClient = HttpClients.createDefault();
				        CloseableHttpResponse response = httpClient.execute(post)) {
					StatusLine statusLine = response.getStatusLine();
					Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
					if (statusCode != null && statusCode != 200) {
						logger.error("{}could not send config to {} {} {}", FieldOfPlay.getLoggingName(fop),
						        destination,
						        statusLine,
						        LoggerUtils.whereFrom(1));
					}
					EntityUtils.toString(response.getEntity());
				} catch (Exception e1) {
					logger.error("{}could not send config to {} {}", FieldOfPlay.getLoggingName(fop), destination,
					        LoggerUtils.exceptionMessage(e1));
				}
			} catch (Exception e2) {
				logger.error("{}could not send config to {} {}", FieldOfPlay.getLoggingName(fop), destination, e2);
			}
		}
	}

	// Private helper for HTTP POST
	private void doPost(String url, String updateKey, Map<String, ?> parameters) {
		HttpPost post = new HttpPost(url);
		try {
			List<NameValuePair> params = new ArrayList<>();
			for (Entry<String, ?> entry : parameters.entrySet()) {
				String value = ForwarderPayloadBuilder.convertParameterValue(entry.getValue(), fop);
				if (value != null) {
					params.add(new BasicNameValuePair(entry.getKey(), value));
				}
			}
			post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error("{}encoding error: {}", FieldOfPlay.getLoggingName(fop), LoggerUtils.exceptionMessage(e));
			return;
		}

		try (CloseableHttpClient httpClient = HttpClients.createDefault();
		        CloseableHttpResponse response = httpClient.execute(post)) {
			StatusLine statusLine = response.getStatusLine();
			Integer statusCode = statusLine != null ? statusLine.getStatusCode() : null;
			if (statusCode != null && statusCode != 200) {
				logger.error("{}POST to {} returned status {}", FieldOfPlay.getLoggingName(fop), url, statusLine);
			}
			EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.debug("{}POST to {} failed: {}", FieldOfPlay.getLoggingName(fop), url,
			        LoggerUtils.exceptionMessage(e));
		}
	}

	/**
	 * Static method to register startup data callbacks for WebSocket connections.
	 * When a connection opens, sends database, translations_zip, and flags_zip in sequence.
	 * 
	 * @param videoUrl  the video data WebSocket URL (if configured)
	 * @param updateUrl the public results WebSocket URL (if configured)
	 */
	public static void registerStartupDataCallbacks(String videoUrl, String updateUrl) {
		logger.info("Registering startup data callbacks for WebSocket trackers");

		// Export competition data once (for all connections)
		CompetitionDataExport export = ForwarderPayloadBuilder.exportCompetitionDataStatic();
		if (export == null) {
			logger.debug("Unable to build competition data payload for startup");
			return;
		}

		// Create translations ZIP bytes once
		if (!TranslationsZipHelper.hasTranslationsAvailable()) {
			logger.debug("Translations not available for startup send");
			return;
		}
		byte[] translationsZipBytes = TranslationsZipHelper.createTranslationsZipBytes();

		// Create flags ZIP bytes once
		if (!FlagsZipHelper.hasFlagsAvailable()) {
			logger.debug("Flags not available for startup send");
			return;
		}
		byte[] flagsZipBytes = FlagsZipHelper.createFlagsZipBytes();

		// Create pictures ZIP bytes once (optional - may not exist)
		final byte[] picturesZipBytes = PicturesZipHelper.hasPicturesAvailable()
		        ? PicturesZipHelper.createPicturesZipBytes()
		        : new byte[0];

		// Register for video data URL
		if (videoUrl != null && !videoUrl.trim().isEmpty()
		        && (videoUrl.startsWith("ws://") || videoUrl.startsWith("wss://"))) {
			registerStartupCallbacksForUrl(videoUrl, export, translationsZipBytes, flagsZipBytes, picturesZipBytes);
		}

		// Register for public results URL
		if (updateUrl != null && !updateUrl.trim().isEmpty()
		        && (updateUrl.startsWith("ws://") || updateUrl.startsWith("wss://"))) {
			registerStartupCallbacksForUrl(updateUrl, export, translationsZipBytes, flagsZipBytes, picturesZipBytes);
		}
	}

	private static void registerStartupCallbacksForUrl(String url, CompetitionDataExport export,
	        byte[] translationsZipBytes, byte[] flagsZipBytes, byte[] picturesZipBytes) {
		WebSocketEventSender sender = WebSocketEventSender.getOrCreate(url);
		if (sender != null) {
			// Single onOpenCallback that sends all three data types
			sender.setOnOpenCallback(() -> {
				logger.info("WebSocket connected to {}, sending startup data", url);

				// Send database
				Map<String, Object> dbPayload = new LinkedHashMap<>();
				dbPayload.put("databaseChecksum", export.checksum());
				dbPayload.put("database", export.structure());
				boolean sent = sender.sendObject("database", dbPayload);
				if (sent) {
					logger.debug("Sent startup database via WebSocket to {}", url);
				} else {
					logger.debug("Could not send startup database via WebSocket to {} (socket not ready)", url);
				}

				// Send translations_zip
				sent = sender.sendBinary("translations_zip", translationsZipBytes);
				if (sent) {
					logger.debug("Sent startup translations_zip via WebSocket to {}", url);
				} else {
					logger.debug("Could not send startup translations_zip via WebSocket to {} (socket not ready)", url);
				}

				// Send flags_zip
				sent = sender.sendBinary("flags_zip", flagsZipBytes);
				if (sent) {
					logger.debug("Sent startup flags_zip via WebSocket to {}", url);
				} else {
					logger.debug("Could not send startup flags_zip via WebSocket to {} (socket not ready)", url);
				}
			});

			// Register missing data callbacks for on-demand requests
			sender.setMissingDataCallback("database", () -> {
				Map<String, Object> payload = new LinkedHashMap<>();
				payload.put("databaseChecksum", export.checksum());
				payload.put("database", export.structure());
				sender.sendObject("database", payload);
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
