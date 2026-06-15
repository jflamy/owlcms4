/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
		logger.warn("***** OWLCMS key trace: forwarding destinations publicResultsUrl={} publicResultsKey={} videoDataUrl={} videoDataKey={} videoKeySameAsPublicKey={}",
		        publicResultsUrl, debugKey(publicResultsKey), videoDataUrl, debugKey(videoDataKey),
		        Objects.equals(videoDataKey, publicResultsKey));
		addDestination(destinationsByUrl, conflictedUrls, publicResultsUrl, publicResultsKey);
		addDestination(destinationsByUrl, conflictedUrls, videoDataUrl, videoDataKey);
		return new ArrayList<>(destinationsByUrl.values());
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

	private static String debugKey(String key) {
		if (key == null) {
			return "<null>";
		}
		if (key.isBlank()) {
			return "<blank>";
		}
		return "<prefix=" + maskPrefix(key) + " length=" + key.length() + " sha256=" + sha256Prefix(key)
		        + " equalsPublicresults=" + Objects.equals(key, "publicresults") + ">";
	}

	private static String maskPrefix(String key) {
		int shown = Math.min(4, key.length());
		return key.substring(0, shown) + "...";
	}

	private static String sha256Prefix(String key) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < Math.min(6, digest.length); i++) {
				sb.append(String.format("%02x", digest[i]));
			}
			return sb.toString();
		} catch (Exception e) {
			return "unavailable";
		}
	}
}
