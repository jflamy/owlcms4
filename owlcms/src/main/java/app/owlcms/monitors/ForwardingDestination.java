/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
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
		Map<String, ForwardingDestination> destinationsByUrl = new LinkedHashMap<>();
		Set<String> conflictedUrls = new HashSet<>();
		if (config == null) {
			return new ArrayList<>();
		}
		String publicResultsUrl = config.getParamPublicResultsURL();
		String publicResultsKey = config.getParamUpdateKey();
		String videoDataUrl = config.getParamVideoDataURL();
		String videoDataKey = config.getParamVideoDataKey();
		logger.debug(
		        "forwarder configuration: publicResults URL={} (from {}), updateKey {} (from {}); videoData URL={} (from {}), videoDataKey {} (from {})",
		        publicResultsUrl, source("remote", publicResultsUrl),
		        keyPresence(publicResultsKey), source("updateKey", publicResultsKey),
		        videoDataUrl, source("videodata", videoDataUrl),
		        keyPresence(videoDataKey), source("videoDataKey", videoDataKey));
		addDestination(destinationsByUrl, conflictedUrls, publicResultsUrl, publicResultsKey);
		addDestination(destinationsByUrl, conflictedUrls, videoDataUrl, videoDataKey);
		return new ArrayList<>(destinationsByUrl.values());
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
		String publicResultsUrl = config.getParamPublicResultsURL();
		String publicResultsKey = config.getParamUpdateKey();
		String videoDataUrl = config.getParamVideoDataURL();
		String videoDataKey = config.getParamVideoDataKey();
		logger.info(
		        "forwarder configuration: publicResults URL={} (from {}), updateKey {} (from {}); videoData URL={} (from {}), videoDataKey {} (from {})",
		        publicResultsUrl, source("remote", publicResultsUrl),
		        keyPresence(publicResultsKey), source("updateKey", publicResultsKey),
		        videoDataUrl, source("videodata", videoDataUrl),
		        keyPresence(videoDataKey), source("videoDataKey", videoDataKey));
	}

	private static void addDestination(Map<String, ForwardingDestination> destinationsByUrl, Set<String> conflictedUrls,
			String baseUrl, String updateKey) {
		String normalizedUrl = normalizeBaseUrl(baseUrl);
		if (normalizedUrl == null || conflictedUrls.contains(normalizedUrl)) {
			return;
		}
		ForwardingDestination destination = new ForwardingDestination(normalizedUrl, updateKey);
		if (!destination.isHttp() && !destination.isWebSocket()) {
			logger.error("forwarding destination URL must start with http://, https://, ws://, or wss://: {}", normalizedUrl);
			return;
		}
		ForwardingDestination existing = destinationsByUrl.get(normalizedUrl);
		if (existing == null) {
			destinationsByUrl.put(normalizedUrl, destination);
			return;
		}
		if (Objects.equals(existing.getUpdateKey(), destination.getUpdateKey())) {
			return;
		}
		logger.error("forwarding destination {} is configured with conflicting update keys; destination disabled", normalizedUrl);
		destinationsByUrl.remove(normalizedUrl);
		conflictedUrls.add(normalizedUrl);
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
	 * Indicate where a resolved value comes from: a runtime override (environment variable or
	 * system property) or the saved database configuration. Used for INFO-level diagnostics.
	 */
	private static String source(String envParam, String resolvedValue) {
		if (resolvedValue == null || resolvedValue.isBlank()) {
			return "unset";
		}
		return StartupUtils.getStringParam(envParam) != null ? "environment variable" : "database";
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
