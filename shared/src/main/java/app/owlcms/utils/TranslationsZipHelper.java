/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * Utility class to generate and serialize all translations for all 26 locales as a zipped JSON archive.
 * Creates an in-memory ZIP file containing a single translations.json file with the complete translation structure.
 * 
 * This is used by owlcms-tracker to pre-load all translations for all locales, enabling complete translation
 * maps with regional variants (e.g., fr-CA gets all fr keys + 10 overrides) without runtime fallback needed.
 */
public class TranslationsZipHelper {

	final static Logger logger = (Logger) LoggerFactory.getLogger(TranslationsZipHelper.class);
	private static final ObjectMapper MAPPER = new ObjectMapper();

	/**
	 * Create a ZIP archive containing translations.json with nested JSON structure
	 * for all 26 locales and their complete translation maps (with regional variant merging).
	 * 
	 * Structure:
	 * {
	 *   "locales": {
	 *     "en": { "key1": "value1", "key2": "value2", ... },
	 *     "en_US": { "key1": "value1", ... },
	 *     "fr": { "key1": "valeur1", ... },
	 *     "fr_CA": { "key1": "valeur1", ... (with 10 overrides) },
	 *     ... (all 26 locales)
	 *   }
	 * }
	 * 
	 * @return byte array containing the zipped translations.json, or empty array if generation fails
	 */
	public static byte[] createTranslationsZipBytes() {
		try {
			// Build nested locales structure with all translations
			Map<String, Object> translationsStructure = buildTranslationsStructure();
			
			// Convert to JSON string
			String jsonContent = MAPPER.writerWithDefaultPrettyPrinter()
					.writeValueAsString(translationsStructure);
			
			@SuppressWarnings("unchecked")
			Map<String, Map<String, String>> localesMap = 
				(Map<String, Map<String, String>>) translationsStructure.getOrDefault("locales", new HashMap<>());
			logger.info("Translation ZIP structure: {} bytes, {} locales in structure",
					jsonContent.length(), localesMap.size());
			
			// Create ZIP archive with translations.json
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
				// Add translations.json entry
				ZipEntry entry = new ZipEntry("translations.json");
				zipOut.putNextEntry(entry);
				
				// Write JSON content
				byte[] jsonBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
				zipOut.write(jsonBytes);
				zipOut.closeEntry();
				
				zipOut.finish();
				zipOut.flush();
			}
			
			byte[] result = baos.toByteArray();
			logger.info("Created translations ZIP archive: {} bytes containing {} locales with ~1310 keys per regional variant",
					result.length, getAllLocales().size());
			return result;
		} catch (Exception e) {
			logger.error("Failed to create translations ZIP: {}", LoggerUtils.exceptionMessage(e));
			return new byte[0];
		}
	}

	/**
	 * Build the nested translations structure with all locales and their translation maps.
	 * Handles regional variant merging: for example, fr-CA gets all fr translations plus 10 overrides.
	 * Uses the public Translator.getMapForLocale() which safely accesses the internal cached bundles.
	 * Includes a checksum computed over all locale data for integrity verification.
	 * 
	 * @return Map structure with "locales" key containing all locale translation maps and a checksum
	 */
	private static Map<String, Object> buildTranslationsStructure() {
		Map<String, Object> root = new HashMap<>();
		Map<String, Map<String, String>> localesMap = new HashMap<>();
		
		List<Locale> availableLocales = getAllLocales();
		logger.info("Building translations for {} available locales: {}", 
				availableLocales.size(), availableLocales);
		
		// Calculate checksum over all translations
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (Exception e) {
			logger.error("Failed to create MessageDigest: {}", LoggerUtils.exceptionMessage(e));
			md = null;
		}
		
		// Process each locale
		for (Locale locale : availableLocales) {
			Map<String, String> localeTranslations = new HashMap<>();
			
			// First, get base language translations if this is a regional variant
			if (locale.getCountry() != null && !locale.getCountry().isEmpty()) {
				Locale baseLanguage = new Locale(locale.getLanguage());
				// Get all base language translations
				Map<String, String> baseTranslations = Translator.getMapForLocale(baseLanguage);
				logger.debug("Base language {} for {} has {} keys", 
						baseLanguage, locale, baseTranslations.size());
				localeTranslations.putAll(baseTranslations);
			}
			
			// Then overlay region-specific translations (overrides base)
			Map<String, String> regionTranslations = Translator.getMapForLocale(locale);
			logger.debug("Region-specific {} has {} keys", locale, regionTranslations.size());
			localeTranslations.putAll(regionTranslations);
			
			if (!localeTranslations.isEmpty()) {
				localesMap.put(locale.toString(), localeTranslations);
				logger.info("Added {} translations for locale: {} (final size: {} keys)",
						regionTranslations.size(), locale, localeTranslations.size());
				
				// Update checksum with locale data
				if (md != null) {
					md.update(locale.toString().getBytes(StandardCharsets.UTF_8));
					for (Map.Entry<String, String> entry : localeTranslations.entrySet()) {
						md.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
						md.update(entry.getValue().getBytes(StandardCharsets.UTF_8));
					}
				}
			} else {
				logger.debug("No translations found for locale: {}", locale);
			}
		}
		
		logger.info("Translation structure contains {} locales", localesMap.size());
		root.put("locales", localesMap);
		
		// Add checksum
		if (md != null) {
			byte[] digestBytes = md.digest();
			String checksum = java.util.HexFormat.of().formatHex(digestBytes);
			root.put("translationsChecksum", checksum);
			logger.info("Translations checksum: {}", checksum);
		}
		
		return root;
	}

	/**
	 * Get all available locales from Translator.
	 * 
	 * @return List of all 26 locales
	 */
	private static List<Locale> getAllLocales() {
		return Translator.getAllAvailableLocales();
	}

	/**
	 * Check if translations are available.
	 * 
	 * @return true if locales are available, false otherwise
	 */
	public static boolean hasTranslationsAvailable() {
		List<Locale> locales = getAllLocales();
		return locales != null && !locales.isEmpty();
	}
}
