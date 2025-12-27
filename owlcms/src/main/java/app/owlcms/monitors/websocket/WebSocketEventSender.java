/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors.websocket;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.TranslationsZipHelper;
import ch.qos.logback.classic.Logger;

/**
 * Manages WebSocket connections for sending event data to remote systems.
 * Supports both ws:// and wss:// protocols.
 */
public class WebSocketEventSender {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(WebSocketEventSender.class);
	private static final int INITIAL_RECONNECT_DELAY_MS = 1000;  // Start with 1 second
	private static final int MAX_RECONNECT_DELAY_MS = 30000;     // Cap at 30 seconds
	private static final int EXPONENTIAL_BACKOFF_ATTEMPTS = 5;   // 1s, 2s, 4s, 8s, 16s, then cap at 30s
	
	/** Protocol version for WebSocket messages. Incremented when message format changes. */
	public static final String PROTOCOL_VERSION = "2.3.0";
	
	private static Map<String, WebSocketEventSender> sendersByUrl = new HashMap<>();
	private static ObjectMapper objectMapper = createObjectMapper();
	
	private static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

	/**
	 * Get or create a WebSocketEventSender for the given URL.
	 * Uses a supplier to dynamically fetch the current URL on reconnects.
	 * Optionally accepts a callback to invoke on connection open (including reconnects).
	 */
	public static synchronized WebSocketEventSender getOrCreate(
			String url, 
			java.util.function.Supplier<String> urlSupplier,
			Runnable onOpenCallback) {
		if (url == null || url.trim().isEmpty()) {
			return null;
		}
		
		WebSocketEventSender sender = sendersByUrl.get(url);
		if (sender == null) {
			sender = new WebSocketEventSender(url, urlSupplier);
			// Set callback BEFORE connecting to avoid race condition
			if (onOpenCallback != null) {
				sender.setOnOpenCallback(onOpenCallback);
			}
			// Now connect - callback is ready to fire
			sender.connect();
			sendersByUrl.put(url, sender);
		}
		return sender;
	}
	
	/**
	 * Get or create a WebSocketEventSender for the given URL (without onOpen callback).
	 */
	public static synchronized WebSocketEventSender getOrCreate(String url, java.util.function.Supplier<String> urlSupplier) {
		return getOrCreate(url, urlSupplier, null);
	}
	
	/**
	 * Get or create a WebSocketEventSender for the given URL (legacy compatibility).
	 */
	public static synchronized WebSocketEventSender getOrCreate(String url) {
		return getOrCreate(url, () -> url, null);
	}
	
	/**
	 * Close sender for old URL and create new sender for updated URL.
	 * Used when URL configuration changes (e.g., port correction).
	 */
	public static synchronized WebSocketEventSender updateUrl(String oldUrl, String newUrl) {
		if (oldUrl != null && !oldUrl.equals(newUrl)) {
			logger.info("WebSocket URL changed from {} to {}, closing old connection and creating new one", oldUrl, newUrl);
			closeSender(oldUrl);
		}
		return getOrCreate(newUrl);
	}

	/**
	 * Close and remove a sender for the given URL
	 */
	public static synchronized void closeSender(String url) {
		WebSocketEventSender sender = sendersByUrl.remove(url);
		if (sender != null) {
			sender.close();
		}
	}

	/**
	 * Close all WebSocket connections
	 */
	public static synchronized void closeAll() {
		for (WebSocketEventSender sender : sendersByUrl.values()) {
			sender.close();
		}
		sendersByUrl.clear();
	}

	/**
	 * Send translations to all connected WebSocket clients.
	 * Used when translations are reloaded to broadcast the updated translations.
	 */
	public static synchronized void sendTranslationsToAll() {
		if (!TranslationsZipHelper.hasTranslationsAvailable()) {
			logger.debug("translations not available, cannot send to all clients");
			return;
		}

		byte[] translationsZipBytes = TranslationsZipHelper.createTranslationsZipBytes();
		if (translationsZipBytes.length == 0) {
			logger.debug("failed to create translations ZIP, cannot send to all clients");
			return;
		}

		int sentCount = 0;
		for (WebSocketEventSender sender : sendersByUrl.values()) {
			boolean sent = sender.sendBinary("translations_zip", translationsZipBytes);
			if (sent) {
				sentCount++;
			}
		}
		logger.debug("sent translations ZIP to {}/{} connected WebSocket clients ({} bytes)",
		        sentCount, sendersByUrl.size(), translationsZipBytes.length);
	}

	private String url;
	private java.util.function.Supplier<String> urlSupplier;
	private WebSocketClient client;
	private int reconnectAttempts = 0;
	private boolean intentionallyClosed = false;
	private boolean connecting = false;
	private Map<String, Runnable> missingDataCallbacks = new HashMap<>();
	private Runnable onOpenCallback = null;

	private WebSocketEventSender(String url, java.util.function.Supplier<String> urlSupplier) {
		this.url = url;
		this.urlSupplier = urlSupplier;
		// DO NOT call connect() here - getOrCreate() will call it after setting callbacks
		// This avoids race condition where connection opens before callbacks are registered
	}
	
	/**
	 * Set callback to be invoked when server requests specific missing data type
	 * @param dataType Type of data ("database", "flags", "styles", "pictures")
	 * @param callback Callback to invoke when this data type is requested
	 */
	public void setMissingDataCallback(String dataType, Runnable callback) {
		this.missingDataCallbacks.put(dataType, callback);
	}

	/**
	 * Set callback to be invoked when WebSocket connection opens.
	 * The callback is invoked only on actual connection open events (including reconnects),
	 * not when this method is called on an already-open connection.
	 * @param callback Callback to invoke when connection opens
	 */
	public void setOnOpenCallback(Runnable callback) {
		synchronized (this) {
			this.onOpenCallback = callback;
		}
	}

	private synchronized void connect() {
		if (intentionallyClosed) {
			logger.debug("Skipping WebSocket connect for {} because it was intentionally closed", url);
			return;
		}
		if (connecting) {
			logger.debug("WebSocket connect already in progress for {}", url);
			return;
		}
		if (client != null && client.isOpen()) {
			logger.debug("WebSocket already connected or connecting to {}", url);
			return;
		}

		connecting = true;

		try {
			URI uri = new URI(url);
			
			this.client = new WebSocketClient(uri) {
				@Override
				public void onOpen(ServerHandshake handshake) {
					logger.info("✓ Connection established: {}", url);
					synchronized (WebSocketEventSender.this) {
						connecting = false;
						reconnectAttempts = 0;
						if (onOpenCallback != null) {
							// Invoke the on-open callback on every successful connection
							// (including reconnects) so initial data can be re-sent.
							onOpenCallback.run();
						}
					}
				}

				@Override
				public void onMessage(String message) {
					logger.debug("WebSocket message received from {}: {}", url, message);
					WebSocketEventSender.this.handleServerMessage(message);
				}

				@Override
				public void onClose(int code, String reason, boolean remote) {
					if (remote) {
						logger.info("✗ Connection closed by remote: {} (code: {}, reason: {})", 
								url, code, reason);
					} else {
						logger.debug("Connection closed by local: {} (code: {})", url, code);
					}
					synchronized (WebSocketEventSender.this) {
						connecting = false;
					}
					
					if (!intentionallyClosed) {
						scheduleReconnect();
					}
				}

				@Override
				public void onError(Exception ex) {
					logger.warn("✗ Connection refused: {} - {}", url, LoggerUtils.exceptionMessage(ex));
					synchronized (WebSocketEventSender.this) {
						connecting = false;
					}
				}
			};
			
			// Set connection timeout
			this.client.setConnectionLostTimeout(30);
			
			// Connect asynchronously
			this.client.connect();
			
		} catch (URISyntaxException e) {
			connecting = false;
			logger.error("Invalid WebSocket URL {}: {}", url, LoggerUtils.exceptionMessage(e));
		} catch (Exception e) {
			connecting = false;
			logger.error("Unexpected error initiating WebSocket connection to {}: {}", url,
					LoggerUtils.exceptionMessage(e));
		}
	}

	private void scheduleReconnect() {
		final int delayMs;
		
		synchronized (this) {
			if (intentionallyClosed) {
				logger.debug("Skipping reconnect for {} because it was intentionally closed", url);
				return;
			}
			if (connecting) {
				logger.debug("Reconnect already scheduled or in progress for {}", url);
				return;
			}
			reconnectAttempts++;
			
			// Exponential backoff: 1s, 2s, 4s, 8s, 16s, then cap at 30s
			if (reconnectAttempts <= EXPONENTIAL_BACKOFF_ATTEMPTS) {
				int delay = INITIAL_RECONNECT_DELAY_MS * (int) Math.pow(2, reconnectAttempts - 1);
				delayMs = Math.min(delay, MAX_RECONNECT_DELAY_MS);
			} else {
				delayMs = MAX_RECONNECT_DELAY_MS;
			}
			
			connecting = true;
			logger.info("Retrying connection to {} in {}s (attempt {})", 
					url, delayMs / 1000, reconnectAttempts);
		}
		
		new Thread(() -> {
			try {
				TimeUnit.MILLISECONDS.sleep(delayMs);
				synchronized (WebSocketEventSender.this) {
					if (intentionallyClosed) {
						connecting = false;
						return;
					}
					String currentUrl = urlSupplier.get();
					if (currentUrl != null && !currentUrl.equals(WebSocketEventSender.this.url)) {
						logger.info("WebSocket URL changed from {} to {} before reconnect, updating",
						        WebSocketEventSender.this.url, currentUrl);
						WebSocketEventSender.this.url = currentUrl;
						reconnectAttempts = 0; // Reset retry count for new URL
					}
					connecting = false;
					connect();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				synchronized (WebSocketEventSender.this) {
					connecting = false;
				}
			}
		}).start();
	}
	
	/**
	 * Handle incoming messages from the server
	 */
	private void handleServerMessage(String message) {
		try {
			// Try to parse as JSON
			Map<?, ?> response = objectMapper.readValue(message, Map.class);
			
			// Check for status field indicating 428 (Precondition Required)
			Object status = response.get("status");
			if (status != null && (status.equals(428) || status.equals("428"))) {
				// Parse the missing array to determine what data is needed
				Object missingObj = response.get("missing");
				java.util.List<?> missingList = null;
				
				if (missingObj instanceof java.util.List) {
					missingList = (java.util.List<?>) missingObj;
				}
				
				if (missingList != null && !missingList.isEmpty()) {
					logger.debug("WebSocket server {} returned 428 - missing data: {}", url, missingList);
					
					// Check what types of data are missing and invoke appropriate callbacks
					for (Object missing : missingList) {
						String missingType = missing != null ? missing.toString() : "";
						Runnable callback = missingDataCallbacks.get(missingType);
						
						if (callback != null) {
							logger.info("Invoking callback for missing data type: {}", missingType);
							callback.run();
						} else {
							logger.error("No callback registered for missing data type '{}' - cannot fulfill server request. " +
							        "Register a callback using setMissingDataCallback(\"{}\", callback)", missingType, missingType);
						}
					}
				} else {
					// No missing array - log error about malformed 428 response
					logger.error("WebSocket server {} returned 428 without 'missing' array - malformed response. " +
					        "Expected format: {{\"status\": 428, \"missing\": [\"database\"]}}", url);
				}
			}
		} catch (Exception e) {
			logger.debug("WebSocket message from {} not a status response: {}", url, message);
		}
	}

	/**
	 * Send data as a JSON payload over the WebSocket connection with a message type wrapper.
	 * 
	 * @param messageType Type of message (update, timer, decision, database)
	 * @param data Map of key-value pairs to send as JSON payload
	 */
	public void send(String messageType, Map<String, ?> data) {
		if (client == null || !client.isOpen()) {
			logger.debug("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return;
		}

		try {
			// Wrap payload with message type and protocol version for robustness
			Map<String, Object> wrapper = new HashMap<>();
			wrapper.put("version", PROTOCOL_VERSION);
			wrapper.put("type", messageType);
			wrapper.put("payload", data);
			
			// Convert to JSON string using Jackson
			String jsonPayload = objectMapper.writeValueAsString(wrapper);
			String preview = jsonPayload.length() > 50 ? jsonPayload.substring(0, 50) + "..." : jsonPayload;
			logger.debug("Sending WebSocket message type '{}' to {}: {} {}", messageType, url, preview, LoggerUtils.whereFrom());
			client.send(jsonPayload);
			logger.debug("Sent WebSocket message type '{}' to {}: {}", messageType, url, jsonPayload);
		} catch (Exception e) {
			logger.error("Failed to send WebSocket message to {}: {}", url, LoggerUtils.exceptionMessage(e));
		}
	}
	
	/**
	 * Send pre-serialized JSON payload by wrapping it with a message type.
	 * Parses the JSON once to create a proper JSON structure (not string concatenation).
	 * 
	 * @param messageType Type of message (typically "database")
	 * @param jsonPayload Pre-serialized JSON string to wrap as a JSON object
	 */
	public void sendPreSerializedJson(String messageType, String jsonPayload) {
		if (client == null || !client.isOpen()) {
			logger.debug("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return;
		}

		try {
			// Parse the JSON payload into a JsonNode (parse once)
			JsonNode payloadNode = objectMapper.readTree(jsonPayload);
			
			// Create wrapper as JSON structure with protocol version
			ObjectNode wrapper = objectMapper.createObjectNode();
			wrapper.put("version", PROTOCOL_VERSION);
			wrapper.put("type", messageType);
			wrapper.set("payload", payloadNode);
			
			// Serialize the wrapper (single serialization)
			String wrappedJson = objectMapper.writeValueAsString(wrapper);
			client.send(wrappedJson);
			logger.debug("Sent WebSocket message type '{}' to {} with pre-serialized JSON payload ({} chars)", 
					messageType, url, jsonPayload.length());
		} catch (Exception e) {
			logger.error("Failed to send WebSocket message with pre-serialized JSON to {}: {}", url, LoggerUtils.exceptionMessage(e));
		}
	}
	
	/**
	 * Send raw JSON payload over the WebSocket connection with a message type wrapper.
	 * The payload is parsed as JSON and included directly (not as a string).
	 * 
	 * @param messageType Type of message (typically "database")
	 * @param jsonPayload Raw JSON string to send as payload
	 */
	public void sendRawJson(String messageType, String jsonPayload) {
		if (client == null || !client.isOpen()) {
			logger.debug("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return;
		}

		try {
			// Parse the raw JSON payload
			Object parsedPayload = objectMapper.readValue(jsonPayload, Object.class);
			
			// Wrap with message type and protocol version
			Map<String, Object> wrapper = new HashMap<>();
			wrapper.put("version", PROTOCOL_VERSION);
			wrapper.put("type", messageType);
			wrapper.put("payload", parsedPayload);
			
			// Convert to JSON string using Jackson
			String wrappedJson = objectMapper.writeValueAsString(wrapper);
			client.send(wrappedJson);
			logger.debug("Sent WebSocket message type '{}' to {} with raw JSON payload ({} chars)", 
					messageType, url, jsonPayload.length());
		} catch (Exception e) {
			logger.error("Failed to send WebSocket message with raw JSON to {}: {}", url, LoggerUtils.exceptionMessage(e));
		}
	}

	/**
	 * Send an object payload over the WebSocket connection with a message type wrapper.
	 * The object is serialized directly (not converted to string first).
	 * 
	 * @param messageType Type of message (typically "database")
	 * @param payload Object to send as payload
	 */
	public boolean sendObject(String messageType, Object payload) {
		if (client == null || !client.isOpen()) {
			logger.debug("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return false;
		}

		try {
			// Wrap with message type and protocol version
			Map<String, Object> wrapper = new HashMap<>();
			wrapper.put("version", PROTOCOL_VERSION);
			wrapper.put("type", messageType);
			wrapper.put("payload", payload);
			
			// Convert to JSON string using Jackson
			String wrappedJson = objectMapper.writeValueAsString(wrapper);
			client.send(wrappedJson);
			logger.debug("Sent WebSocket message type '{}' to {} with object payload", 
				messageType, url);
			return true;
		} catch (Exception e) {
			logger.error("Failed to send WebSocket message with object to {}: {}", url, LoggerUtils.exceptionMessage(e));
			return false;
		}
	}

	/**
	 * Send binary data over the WebSocket connection with protocol version, message type header, and payload.
	 * 
	 * Uses the WebSocket binary frame (opcode 0x2) which is automatically distinguished
	 * from JSON text frames (opcode 0x1) at the protocol level. This means the server
	 * can distinguish binary from JSON without parsing, simply by checking the frame type.
	 * 
	 * Binary Frame Format:
	 * - First 4 bytes: protocol version length (big-endian int)
	 * - Next N bytes: protocol version as UTF-8 string (e.g., "2.0.0")
	 * - Next 4 bytes: message type length (big-endian int)
	 * - Next M bytes: message type as UTF-8 string (e.g., "flags")
	 * - Remaining bytes: binary payload data
	 * 
	 * Example: To send "flags" with 100KB of ZIP data using protocol version "2.1.0":
	 * [0x00, 0x00, 0x00, 0x05] [2, ., 0, ., 0] [0x00, 0x00, 0x00, 0x05] [f, l, a, g, s] [100KB of ZIP bytes...]
	 * 
	 * @param messageType Type identifier for the binary data (e.g., "flags", "pictures")
	 * @param binaryData The binary payload to send
	 * @return true if sent successfully, false if socket not ready
	 */
	public boolean sendBinary(String messageType, byte[] binaryData) {
		if (client == null || !client.isOpen()) {
			logger.debug("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return false;
		}

		try {
			byte[] versionBytes = PROTOCOL_VERSION.getBytes("UTF-8");
			byte[] typeBytes = messageType.getBytes("UTF-8");
			
			// Create buffer: 4 bytes (version length) + version bytes + 4 bytes (type length) + type bytes + binary data
			ByteBuffer frame = ByteBuffer.allocate(4 + versionBytes.length + 4 + typeBytes.length + binaryData.length);
			
			// Write version length as big-endian int
			frame.putInt(versionBytes.length);
			
			// Write version string
			frame.put(versionBytes);
			
			// Write type length as big-endian int
			frame.putInt(typeBytes.length);
			
			// Write type string
			frame.put(typeBytes);
			
			// Write binary data
			frame.put(binaryData);
			
			// Flip to prepare for reading
			frame.flip();
			
			client.send(frame);
			logger.info("Sent binary WebSocket message version='{}' type='{}' to {} ({} bytes total, {} bytes payload)",
					PROTOCOL_VERSION, messageType, url, frame.capacity(), binaryData.length);
			return true;
		} catch (Exception e) {
			logger.error("Failed to send binary WebSocket message to {}: {}", url, LoggerUtils.exceptionMessage(e));
			return false;
		}
	}

	/**
	 * Close the WebSocket connection
	 */
	public void close() {
		synchronized (this) {
			intentionallyClosed = true;
			connecting = false;
		}
		if (client != null) {
			try {
				client.closeBlocking();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.debug("Interrupted while closing WebSocket to {}", url);
			}
		}
	}

	/**
	 * Check if the WebSocket is currently connected
	 */
	public boolean isConnected() {
		return client != null && client.isOpen();
	}
}
