/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Restricts H2 JDBC URLs to process-local databases: an embedded file database or a named in-memory database.
 *
 * <p>
 * Server modes (TCP, SSL, AUTO_SERVER), lock bypass (FILE_LOCK), startup scripts (INIT) and any other option not
 * explicitly supported are rejected, so that no other process can open or alter the database while owlcms runs.
 */
public final class H2UrlPolicy {

	public static final String FILE_PREFIX = "jdbc:h2:file:";
	public static final String MEM_PREFIX = "jdbc:h2:mem:";

	private static final String H2_PREFIX = "jdbc:h2:";
	private static final Set<String> SUPPORTED_OPTIONS = Set.of("DB_CLOSE_DELAY", "TRACE_LEVEL_FILE",
	        "TRACE_LEVEL_SYSTEM_OUT");
	private static final Pattern INTEGER_VALUE = Pattern.compile("-?\\d{1,9}");

	private H2UrlPolicy() {
	}

	public static boolean isH2(String url) {
		return url != null && url.regionMatches(true, 0, H2_PREFIX, 0, H2_PREFIX.length());
	}

	/**
	 * @param url an H2 JDBC URL
	 * @throws IllegalArgumentException if the URL is not an embedded file or named in-memory database, or uses an
	 *                                  unsupported, malformed or duplicate option
	 */
	public static void validate(String url) {
		if (!isH2(url)) {
			throw new IllegalArgumentException("Not an H2 database URL: " + url);
		}
		int separator = url.indexOf(';');
		String database = separator < 0 ? url : url.substring(0, separator);
		if (database.startsWith(FILE_PREFIX)) {
			if (database.substring(FILE_PREFIX.length()).isBlank()) {
				throw new IllegalArgumentException("H2 file database URL has no path: " + url);
			}
		} else if (database.startsWith(MEM_PREFIX)) {
			if (database.substring(MEM_PREFIX.length()).isBlank()) {
				throw new IllegalArgumentException(
				        "H2 in-memory database URL must be named (" + MEM_PREFIX + "name): " + url);
			}
		} else {
			throw new IllegalArgumentException("Only embedded H2 databases are supported (" + FILE_PREFIX + "path or "
			        + MEM_PREFIX + "name); server and other H2 modes are rejected: " + url);
		}
		if (separator >= 0) {
			parseOptions(url, url.substring(separator + 1));
		}
	}

	/**
	 * Appends each default option whose key is not already present in the URL.
	 *
	 * @param url      an H2 JDBC URL
	 * @param defaults options of the form {@code KEY=VALUE}
	 * @return the URL with missing defaults appended
	 */
	public static String withDefaultOptions(String url, String... defaults) {
		int separator = url.indexOf(';');
		Map<String, String> present = separator < 0 ? Map.of() : parseOptions(url, url.substring(separator + 1));
		StringBuilder sb = new StringBuilder(url);
		for (String option : defaults) {
			String key = option.substring(0, option.indexOf('=')).toUpperCase(Locale.ROOT);
			if (!present.containsKey(key)) {
				sb.append(';').append(option);
			}
		}
		return sb.toString();
	}

	private static Map<String, String> parseOptions(String url, String settings) {
		if (settings.indexOf('\\') >= 0) {
			throw new IllegalArgumentException("Escaped characters are not allowed in H2 URL options: " + url);
		}
		Map<String, String> options = new LinkedHashMap<>();
		for (String setting : settings.split(";", -1)) {
			if (setting.isEmpty()) {
				continue;
			}
			int equal = setting.indexOf('=');
			if (equal <= 0) {
				throw new IllegalArgumentException("Malformed H2 URL option '" + setting + "': " + url);
			}
			String key = setting.substring(0, equal).trim().toUpperCase(Locale.ROOT);
			String value = setting.substring(equal + 1);
			if (!SUPPORTED_OPTIONS.contains(key)) {
				throw new IllegalArgumentException("Unsupported H2 URL option " + key + " (supported: "
				        + String.join(", ", SUPPORTED_OPTIONS.stream().sorted().toList()) + "): " + url);
			}
			if (!INTEGER_VALUE.matcher(value).matches()) {
				throw new IllegalArgumentException("Invalid value for H2 URL option " + key + ": " + url);
			}
			if (options.putIfAbsent(key, value) != null) {
				throw new IllegalArgumentException("Duplicate H2 URL option " + key + ": " + url);
			}
		}
		return options;
	}
}
