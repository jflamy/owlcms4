/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.ForwardingConnection;
import app.owlcms.utils.StartupUtils;

/**
 * One forwarding destination: a normalized base URL and its explicitly paired update key.
 */
public final class ForwardingDestination {
	private static final Logger logger = LoggerFactory.getLogger(ForwardingDestination.class);

	private final String baseUrl;
	private final String updateKey;

	public ForwardingDestination(String baseUrl, String updateKey) {
		this.baseUrl = normalizeBaseUrl(baseUrl);
		this.updateKey = normalizeKey(updateKey);
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getUpdateKey() {
		return updateKey;
	}

	public boolean isHttp() {
		return baseUrl != null && (baseUrl.startsWith("http://") || baseUrl.startsWith("https://"));
	}

	public boolean isWebSocket() {
		return baseUrl != null && (baseUrl.startsWith("ws://") || baseUrl.startsWith("wss://"));
	}

	public String updateUrl() {
		return baseUrl + "/update";
	}

	public String timerUrl() {
		return baseUrl + "/timer";
	}

	public String decisionUrl() {
		return baseUrl + "/decision";
	}

	public String configUrl() {
		return baseUrl + "/config";
	}

	public static List<ForwardingDestination> fromConfig(Config config) {
		return resolveDestinations(config, false);
	}

	private static List<ForwardingDestination> resolveDestinations(Config config, boolean logResolution) {
		Map<String, ForwardingDestination> destinationsByUrl = new LinkedHashMap<>();
		if (config == null) {
			return new ArrayList<>();
		}
		if (!config.getParamEventForwardingEnabled()) {
			if (logResolution) {
				logger.info("forwarder configuration disabled by OWLCMS_ENABLEEVENTFORWARDING");
			}
			return new ArrayList<>();
		}
		for (DestinationInput input : collectDestinationInputs(config, logResolution)) {
			addDestination(destinationsByUrl, input, logResolution);
		}
		return new ArrayList<>(destinationsByUrl.values());
	}

	/**
	 * Single source of truth for the ordered destination inputs. Persisted connections are followed
	 * by non-blank environment connections, so environment keys win when URLs deduplicate.
	 */
	private static List<DestinationInput> collectDestinationInputs(Config config, boolean logResolution) {
		List<DestinationInput> inputs = new ArrayList<>();
		if (logResolution) {
			logRawInputs(config);
		}
		int connectionNumber = 1;
		List<String> inactiveManagedUrls = new ArrayList<>();
		for (ForwardingConnection connection : config.getEffectiveForwardingDestinations()) {
			if (connection != null && connection.isControlPanelManaged() && !connection.isActive()) {
				inactiveManagedUrls.add(normalizeBaseUrl(connection.getUrl()));
			}
			if (connection != null && connection.isActive()) {
				inputs.add(new DestinationInput(connection.getUrl(), connection.getUpdateKey(),
				        "database connection " + connectionNumber));
			}
			connectionNumber++;
		}
		addEnvironmentInput(inputs, "OWLCMS_REMOTE", "remote", "updateKey", false);
		addEnvironmentInput(inputs, "OWLCMS_VIDEODATA", "videodata", "videoDataKey", false);
		inputs.removeIf(input -> inactiveManagedUrls.contains(normalizeBaseUrl(input.baseUrl())));
		return inputs;
	}

	private static void logRawInputs(Config config) {
		int connectionNumber = 1;
		for (ForwardingConnection connection : config.getForwardingDestinations()) {
			logger.info("forwarder database input: connection={}, active={}, URL={}, key={}", connectionNumber++,
			        connection.isActive(), displayUrl(connection.getUrl()), keyPresence(connection.getUpdateKey()));
		}
		logRawEnvironmentInput("OWLCMS_REMOTE", "remote", "updateKey");
		logRawEnvironmentInput("OWLCMS_VIDEODATA", "videodata", "videoDataKey");
	}

	private static void logRawEnvironmentInput(String name, String urlParam, String keyParam) {
		String url = StartupUtils.getStringParam(urlParam);
		logger.info("forwarder environment input: name={}, URL={}, key={}", name, displayUrl(url),
		        keyPresence(StartupUtils.getStringParam(keyParam)));
	}

	private static void addEnvironmentInput(List<DestinationInput> inputs, String name, String urlParam,
			String keyParam, boolean logResolution) {
		String url = StartupUtils.getStringParam(urlParam);
		if (url != null && !url.isBlank()) {
			if ("remote".equals(urlParam)) {
				url = url.replaceFirst("/update$", "");
			}
			String key = StartupUtils.getStringParam(keyParam);
			if (logResolution) {
				logger.info("forwarder environment input added: name={}, URL={}, key={}", name, url,
				        keyPresence(key));
			}
			inputs.add(new DestinationInput(url, key, "environment " + name));
		} else if (logResolution) {
			logger.info("forwarder environment input ignored: name={}, reason={}", name,
			        url == null ? "unset" : "blank");
		}
	}

	/** One configured (URL, key) pair before normalization and deduplication into a destination. */
	private record DestinationInput(String baseUrl, String updateKey, String source) {
	}

	/**
	 * Emit a single INFO-level summary of the resolved forwarder configuration. Unlike
	 * {@link #fromConfig(Config)} (which is called once per FOP and per protocol), this is meant to
	 * be called once globally: at startup and once per config-change reconciliation. The URLs are
	 * shown together with where each value came from (environment variable, database, or unset), and
	 * the keys are reduced to their first/last characters so stale values can be spotted without
	 * exposing the secret.
	 */
	public static void logConfiguration(Config config) {
		if (config == null) {
			return;
		}
		List<String> summaries = new ArrayList<>();
		for (ForwardingDestination destination : resolveDestinations(config, true)) {
			summaries.add(destination.getBaseUrl() + " [key " + keyPresence(destination.getUpdateKey()) + "]");
		}
		logger.info("forwarder configuration resolved: enabled={}, destinations={}",
		        config.getParamEventForwardingEnabled(), summaries);
	}

	private static void addDestination(Map<String, ForwardingDestination> destinationsByUrl, DestinationInput input,
			boolean logResolution) {
		String normalizedUrl = normalizeBaseUrl(input.baseUrl());
		if (normalizedUrl == null) {
			if (logResolution) {
				logger.info("forwarder input skipped: source={}, reason=URL unset or blank", input.source());
			}
			return;
		}
		ForwardingDestination destination = new ForwardingDestination(normalizedUrl, input.updateKey());
		if (!destination.isHttp() && !destination.isWebSocket()) {
			logger.error("forwarding destination URL must start with http://, https://, ws://, or wss://: {}", normalizedUrl);
			return;
		}
		ForwardingDestination previous = destinationsByUrl.put(normalizedUrl, destination);
		if (logResolution) {
			if (previous == null) {
				logger.info("forwarder input added: source={}, URL={}, key={}", input.source(), normalizedUrl,
				        keyPresence(destination.getUpdateKey()));
			} else {
				logger.info("forwarder input deduped by priority: source={}, URL={}, retained key={}, replaced key={}",
				        input.source(), normalizedUrl, keyPresence(destination.getUpdateKey()),
				        keyPresence(previous.getUpdateKey()));
			}
		}
	}

	private static String displayUrl(String url) {
		if (url == null) {
			return "unset";
		}
		return url.isBlank() ? "blank" : url;
	}

	private static String normalizeBaseUrl(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			return null;
		}
		return baseUrl.trim().replaceFirst("/+$", "");
	}

	private static String normalizeKey(String updateKey) {
		if (updateKey == null || updateKey.isBlank()) {
			return null;
		}
		return updateKey;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ForwardingDestination)) {
			return false;
		}
		ForwardingDestination other = (ForwardingDestination) obj;
		return Objects.equals(baseUrl, other.baseUrl) && Objects.equals(updateKey, other.updateKey);
	}

	@Override
	public int hashCode() {
		return Objects.hash(baseUrl, updateKey);
	}

	/**
	 * Show the first and last characters of a key (with its length) so that stale values can be
	 * spotted in the log without exposing the full secret.
	 */
	private static String keyPresence(String key) {
		if (key == null || key.isBlank()) {
			return "none";
		}
		if (key.length() == 1) {
			return key.charAt(0) + "\u2026 (len 1)";
		}
		return key.charAt(0) + "\u2026" + key.charAt(key.length() - 1) + " (len " + key.length() + ")";
	}
}
