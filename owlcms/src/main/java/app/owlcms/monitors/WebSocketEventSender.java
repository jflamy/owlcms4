/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.net.URI;
import java.net.URISyntaxException;
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
	
	private static Map<String, WebSocketEventSender> sendersByUrl = new HashMap<>();
	private static ObjectMapper objectMapper = createObjectMapper();
	
	private static ObjectMapper createObjectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return mapper;
	}

	/**
	 * Get or create a WebSocketEventSender for the given URL
	 */
	public static synchronized WebSocketEventSender getOrCreate(String url) {
		if (url == null || url.trim().isEmpty()) {
			return null;
		}
		
		WebSocketEventSender sender = sendersByUrl.get(url);
		if (sender == null) {
			sender = new WebSocketEventSender(url);
			sendersByUrl.put(url, sender);
		}
		return sender;
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

	private String url;
	private WebSocketClient client;
	private int reconnectAttempts = 0;
	private boolean intentionallyClosed = false;
	private Runnable onDatabaseRequested;

	private WebSocketEventSender(String url) {
		this.url = url;
		connect();
	}
	
	/**
	 * Set callback to be invoked when server requests full database (428 status)
	 */
	public void setOnDatabaseRequested(Runnable callback) {
		this.onDatabaseRequested = callback;
	}

	private void connect() {
		try {
			URI uri = new URI(url);
			
			this.client = new WebSocketClient(uri) {
				@Override
				public void onOpen(ServerHandshake handshake) {
					logger.info("WebSocket connected to: {}", url);
					reconnectAttempts = 0;
				}

				@Override
				public void onMessage(String message) {
					logger.debug("WebSocket message received from {}: {}", url, message);
					WebSocketEventSender.this.handleServerMessage(message);
				}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				logger.info("WebSocket closed: {} - code: {}, reason: {}, remote: {}", 
						url, code, reason, remote);
				
				// Always attempt to reconnect if not intentionally closed
				if (!intentionallyClosed) {
					scheduleReconnect();
				}
			}				@Override
				public void onError(Exception ex) {
					logger.error("WebSocket error on {}: {}", url, LoggerUtils.exceptionMessage(ex));
				}
			};
			
			// Set connection timeout
			this.client.setConnectionLostTimeout(30);
			
			// Connect asynchronously
			this.client.connect();
			
		} catch (URISyntaxException e) {
			logger.error("Invalid WebSocket URL {}: {}", url, LoggerUtils.exceptionMessage(e));
		}
	}

	private void scheduleReconnect() {
		reconnectAttempts++;
		
		// Exponential backoff: 1s, 2s, 4s, 8s, 16s, then cap at 30s
		final int delayMs;
		if (reconnectAttempts <= EXPONENTIAL_BACKOFF_ATTEMPTS) {
			int delay = INITIAL_RECONNECT_DELAY_MS * (int) Math.pow(2, reconnectAttempts - 1);
			delayMs = Math.min(delay, MAX_RECONNECT_DELAY_MS);
		} else {
			delayMs = MAX_RECONNECT_DELAY_MS;
		}
		
		logger.warn("Scheduling WebSocket reconnect attempt {} for {} in {}ms", 
				reconnectAttempts, url, delayMs);
		
		new Thread(() -> {
			try {
				TimeUnit.MILLISECONDS.sleep(delayMs);
				if (!intentionallyClosed) {
					connect();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
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
				logger.warn("WebSocket server {} returned 428 - requesting full database", url);
				if (onDatabaseRequested != null) {
					onDatabaseRequested.run();
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
			logger.warn("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return;
		}

		try {
			// Wrap payload with message type for robustness
			Map<String, Object> wrapper = new HashMap<>();
			wrapper.put("type", messageType);
			wrapper.put("payload", data);
			
			// Convert to JSON string using Jackson
			String jsonPayload = objectMapper.writeValueAsString(wrapper);
			String preview = jsonPayload.length() > 50 ? jsonPayload.substring(0, 50) + "..." : jsonPayload;
			logger.warn("Sending WebSocket message type '{}' to {}: {} {}", messageType, url, preview, LoggerUtils.whereFrom());
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
			logger.warn("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return;
		}

		try {
			// Parse the JSON payload into a JsonNode (parse once)
			JsonNode payloadNode = objectMapper.readTree(jsonPayload);
			
			// Create wrapper as JSON structure
			ObjectNode wrapper = objectMapper.createObjectNode();
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
			logger.warn("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return;
		}

		try {
			// Parse the raw JSON payload
			Object parsedPayload = objectMapper.readValue(jsonPayload, Object.class);
			
			// Wrap with message type
			Map<String, Object> wrapper = new HashMap<>();
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
			logger.warn("WebSocket not connected to {}, attempting to reconnect", url);
			scheduleReconnect();
			return false;
		}

		try {
			// Wrap with message type
			Map<String, Object> wrapper = new HashMap<>();
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
	 * Close the WebSocket connection
	 */
	public void close() {
		intentionallyClosed = true;
		if (client != null) {
			try {
				client.closeBlocking();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger.warn("Interrupted while closing WebSocket to {}", url);
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
