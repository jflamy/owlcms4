/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import app.owlcms.data.config.Config;

public final class TemplateResourceUtils {
	private static final Set<String> LETTER_COUNTRIES = Set.of("CA", "US");

	/**
	 * Sentinel locale for "no enforced paper size" (equivalent to the Config value "None").
	 * Pass this to {@link #filterTemplatesByPaperSize} to return all resources unfiltered.
	 * Uses Interlingua ("ia"), a real IANA language tag that is never used as an actual locale.
	 */
	public static final Locale LOCALE_NO_PAPER_SIZE = Locale.forLanguageTag("ia");

	/**
	 * Maps IANA browser timezone IDs to a representative locale. Timezones for US
	 * and Canada resolve to LETTER paper size via {@link #LETTER_COUNTRIES}. All
	 * other entries in this map resolve to "None" (no enforced default). Timezone
	 * IDs absent from this map default to A4.
	 */
	@SuppressWarnings("deprecation") // Locale constructors are deprecated in Java 19+.
	private static final Map<String, Locale> TIMEZONE_TO_LOCALE = Map.ofEntries(
	        // United States
	        entry("America/New_York", new Locale("en", "US")),
	        entry("America/Chicago", new Locale("en", "US")),
	        entry("America/Denver", new Locale("en", "US")),
	        entry("America/Los_Angeles", new Locale("en", "US")),
	        entry("America/Anchorage", new Locale("en", "US")),
	        entry("America/Adak", new Locale("en", "US")),
	        entry("America/Phoenix", new Locale("en", "US")),
	        entry("America/Boise", new Locale("en", "US")),
	        entry("America/Detroit", new Locale("en", "US")),
	        entry("America/Menominee", new Locale("en", "US")),
	        entry("America/Sitka", new Locale("en", "US")),
	        entry("America/Nome", new Locale("en", "US")),
	        entry("America/Juneau", new Locale("en", "US")),
	        entry("America/Yakutat", new Locale("en", "US")),
	        entry("America/Metlakatla", new Locale("en", "US")),
	        entry("America/Indiana/Indianapolis", new Locale("en", "US")),
	        entry("America/Indiana/Knox", new Locale("en", "US")),
	        entry("America/Indiana/Marengo", new Locale("en", "US")),
	        entry("America/Indiana/Petersburg", new Locale("en", "US")),
	        entry("America/Indiana/Tell_City", new Locale("en", "US")),
	        entry("America/Indiana/Vevay", new Locale("en", "US")),
	        entry("America/Indiana/Vincennes", new Locale("en", "US")),
	        entry("America/Indiana/Winamac", new Locale("en", "US")),
	        entry("America/Kentucky/Louisville", new Locale("en", "US")),
	        entry("America/Kentucky/Monticello", new Locale("en", "US")),
	        entry("America/North_Dakota/Beulah", new Locale("en", "US")),
	        entry("America/North_Dakota/Center", new Locale("en", "US")),
	        entry("America/North_Dakota/New_Salem", new Locale("en", "US")),
	        entry("Pacific/Honolulu", new Locale("en", "US")),
	        // Canada
	        entry("America/Toronto", new Locale("en", "CA")),
	        entry("America/Vancouver", new Locale("en", "CA")),
	        entry("America/Winnipeg", new Locale("en", "CA")),
	        entry("America/Halifax", new Locale("en", "CA")),
	        entry("America/St_Johns", new Locale("en", "CA")),
	        entry("America/Edmonton", new Locale("en", "CA")),
	        entry("America/Regina", new Locale("en", "CA")),
	        entry("America/Moncton", new Locale("en", "CA")),
	        entry("America/Whitehorse", new Locale("en", "CA")),
	        entry("America/Yellowknife", new Locale("en", "CA")),
	        entry("America/Dawson", new Locale("en", "CA")),
	        entry("America/Iqaluit", new Locale("en", "CA")),
	        entry("America/Rankin_Inlet", new Locale("en", "CA")),
	        entry("America/Resolute", new Locale("en", "CA")),
	        entry("America/Nipigon", new Locale("en", "CA")),
	        entry("America/Thunder_Bay", new Locale("en", "CA")),
	        entry("America/Rainy_River", new Locale("en", "CA")),
	        entry("America/Creston", new Locale("en", "CA")),
	        entry("America/Swift_Current", new Locale("en", "CA")),
	        entry("America/Glace_Bay", new Locale("en", "CA")),
	        entry("America/Goose_Bay", new Locale("en", "CA")),
	        entry("America/Pangnirtung", new Locale("en", "CA")),
	        // Mexico — no enforced default
	        entry("America/Mexico_City", new Locale("es", "MX")),
	        entry("America/Cancun", new Locale("es", "MX")),
	        entry("America/Merida", new Locale("es", "MX")),
	        entry("America/Monterrey", new Locale("es", "MX")),
	        entry("America/Matamoros", new Locale("es", "MX")),
	        entry("America/Chihuahua", new Locale("es", "MX")),
	        entry("America/Ciudad_Juarez", new Locale("es", "MX")),
	        entry("America/Ojinaga", new Locale("es", "MX")),
	        entry("America/Hermosillo", new Locale("es", "MX")),
	        entry("America/Mazatlan", new Locale("es", "MX")),
	        entry("America/Bahia_Banderas", new Locale("es", "MX")),
	        entry("America/Tijuana", new Locale("es", "MX")),
	        entry("America/Ensenada", new Locale("es", "MX")),
	        // Colombia — no enforced default
	        entry("America/Bogota", new Locale("es", "CO")),
	        // Venezuela — no enforced default
	        entry("America/Caracas", new Locale("es", "VE")),
	        // Chile — no enforced default
	        entry("America/Santiago", new Locale("es", "CL")),
	        entry("Pacific/Easter", new Locale("es", "CL")),
	        // Philippines — no enforced default
	        entry("Asia/Manila", new Locale("en", "PH")),
	        // Costa Rica — no enforced default
	        entry("America/Costa_Rica", new Locale("es", "CR")),
	        // Panama — no enforced default
	        entry("America/Panama", new Locale("es", "PA")),
	        // Cuba — no enforced default
	        entry("America/Havana", new Locale("es", "CU")),
	        // Dominican Republic — no enforced default
	        entry("America/Santo_Domingo", new Locale("es", "DO")),
	        // Belize — no enforced default
	        entry("America/Belize", new Locale("en", "BZ")),
	        // Jamaica — no enforced default
	        entry("America/Jamaica", new Locale("en", "JM")),
	        // Trinidad & Tobago — no enforced default
	        entry("America/Port_of_Spain", new Locale("en", "TT")));


	private TemplateResourceUtils() {
	}

	public static String resolvedDefaultPaperSize(Locale locale) {
		String configured = null;
		try {
			Config config = Config.getCurrent();
			configured = config != null ? config.getDefaultPaperSize() : null;
		} catch (RuntimeException ignored) {
			configured = null;
		}
		if (configured != null && !configured.isBlank()) {
			return configured;
		}
		// Sentinel: caller explicitly requests no paper-size enforcement
		if (LOCALE_NO_PAPER_SIZE.equals(locale)) {
			return "None";
		}
		String country = locale != null ? locale.getCountry() : null;
		if (country != null && LETTER_COUNTRIES.contains(country.toUpperCase(Locale.ROOT))) {
			return "LETTER";
		}
		return "A4";
	}

	/**
	 * Resolves the default paper size from a browser-supplied IANA timezone
	 * identifier (e.g. {@code "America/New_York"}).
	 *
	 * <ul>
	 * <li>US and Canada timezones → {@code "LETTER"}</li>
	 * <li>Timezones listed in {@link #TIMEZONE_TO_LOCALE} for other countries →
	 * {@code "None"} (no enforced default)</li>
	 * <li>Any timezone absent from the map → {@code "A4"}</li>
	 * </ul>
	 *
	 * An explicit {@code Config.defaultPaperSize} always takes precedence.
	 */
	public static String resolvedDefaultPaperSize(String browserTimeZoneId) {
		String configured = null;
		try {
			Config config = Config.getCurrent();
			configured = config != null ? config.getDefaultPaperSize() : null;
		} catch (RuntimeException ignored) {
			configured = null;
		}
		if (configured != null && !configured.isBlank()) {
			return configured;
		}
		return resolvedBrowserDefaultPaperSize(browserTimeZoneId);
	}

	public static String resolvedBrowserDefaultPaperSize(String browserTimeZoneId) {
		if (browserTimeZoneId == null || browserTimeZoneId.isBlank()) {
			return "A4";
		}
		Locale locale = TIMEZONE_TO_LOCALE.get(browserTimeZoneId);
		if (locale == null) {
			// timezone not in map → A4 (Europe, Asia, Africa, etc.)
			return "A4";
		}
		if (LETTER_COUNTRIES.contains(locale.getCountry())) {
			return "LETTER";
		}
		// timezone in map but not US/CA → no enforced default
		return "None";
	}

	public static List<Resource> filterTemplatesByPaperSize(List<Resource> resources, String selectedTemplateName,
	        Locale locale) {
		if (resources == null || resources.isEmpty()) {
			return resources;
		}

		String defaultPaperSize = resolvedDefaultPaperSize(locale);
		if ("None".equalsIgnoreCase(defaultPaperSize)) {
			return resources;
		}

		String preferredPaperSize = defaultPaperSize.toUpperCase(Locale.ROOT);
		Map<String, List<Resource>> grouped = new LinkedHashMap<>();
		for (Resource resource : resources) {
			String fileName = resource.getFileName();
			String key = stripPaperSizeSuffix(fileName);
			grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(resource);
		}

		List<Resource> filtered = new ArrayList<>();
		for (List<Resource> candidates : grouped.values()) {
			List<Resource> matchingPaperSize = new ArrayList<>();
			List<Resource> selected = new ArrayList<>();
			for (Resource candidate : candidates) {
				String fileName = candidate.getFileName();
				if (hasPaperSizeSuffix(fileName, preferredPaperSize)) {
					matchingPaperSize.add(candidate);
				}
				if (selectedTemplateName != null && selectedTemplateName.equals(fileName)) {
					selected.add(candidate);
				}
			}

			if (!matchingPaperSize.isEmpty()) {
				filtered.addAll(matchingPaperSize);
				for (Resource candidate : selected) {
					if (!matchingPaperSize.contains(candidate)) {
						filtered.add(candidate);
					}
				}
			} else {
				filtered.addAll(candidates);
			}
		}

		// Assign display names: strip paper-size suffix and extension.
		// If two resources in the filtered list would produce the same stripped name
		// (collision), leave both displayNames unset so the full file name is shown.
		Map<String, Integer> strippedCount = new LinkedHashMap<>();
		for (Resource r : filtered) {
			String stripped = stripPaperSizeSuffix(r.getFileName());
			strippedCount.merge(stripped, 1, Integer::sum);
		}
		for (Resource r : filtered) {
			String stripped = stripPaperSizeSuffix(r.getFileName());
			if (strippedCount.get(stripped) == 1) {
				r.setDisplayName(stripped);
			}
			// collision → displayName stays null → toString() returns full fileName
		}

		return filtered;
	}

	/**
	 * Returns a collision key / display name for a template file name by stripping:
	 * <ol>
	 *   <li>the file extension (so {@code foo.xlsx} and {@code foo.xls} produce the same key and collide), and</li>
	 *   <li>the paper-size token ({@code -A4}, {@code -LETTER}, {@code -LEGAL}, case-insensitive) when it
	 *       immediately precedes the end of the base name or an optional locale suffix
	 *       (e.g. {@code -en}, {@code -en_US}).</li>
	 * </ol>
	 * Any trailing locale suffix is preserved in the returned value.
	 */
	public static String stripPaperSizeSuffix(String templateName) {
		if (templateName == null) {
			return null;
		}
		// Step 1: strip extension — foo.xlsx and foo.xls both become "foo"
		int extensionIndex = templateName.lastIndexOf('.');
		String withoutExtension = extensionIndex > 0 ? templateName.substring(0, extensionIndex) : templateName;
		// Step 2: strip paper-size token; locale suffix (e.g. -en_US) is preserved via lookahead
		return withoutExtension.replaceFirst("(?i)[-_](A4|LETTER|LEGAL)(?=(?:[-_][A-Za-z]{2}(?:[-_][A-Za-z]{2})?)?$)", "");
	}

	public static boolean hasPaperSizeSuffix(String templateName, String paperSize) {
		if (templateName == null || paperSize == null || paperSize.isBlank()) {
			return false;
		}
		String normalizedPaperSize = paperSize.toUpperCase(Locale.ROOT);
		return templateName.matches("(?i).*[-_]" + normalizedPaperSize
		        + "(?:[-_][A-Za-z]{2}(?:[-_][A-Za-z]{2})?)?(?:\\.[^.]+)?$");
	}
}
