/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.LoggerFactory;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.i18n.I18NProvider;

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.MemTempUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * This class creates a resource bundle from a CSV file containing the various translations, and provides translations for Components according to the Vaadin
 * translation spec.
 *
 * Static variations of the translation routines are also provided for translations that do not take place inside Vaadin components (e.g. spreadsheets).
 *
 */

public class Translator implements I18NProvider {

	private static final long serialVersionUID = 687252956819191905L;
	private static final Logger logger = (Logger) LoggerFactory.getLogger(Translator.class);
	private static Translator helper = new Translator();
	private static final String BUNDLE_BASE = "translation4";
	private static final String BUNDLE_PACKAGE_SLASH = "/i18n/";
	private static final String GOOGLE_SHEETS_API_KEY = "GOOGLE_SHEETS_API_KEY";
	private static final String GOOGLE_SHEETS_TAB = "GOOGLE_SHEETS_TAB";
	private static final String GOOGLE_SPREADSHEET_ID = "1ZRfYHCARnPCnUEVZYo3Y_7qJGS9z7NRVg-Se7z3lHtE";
	private static final String GOOGLE_SHEETS_API = "https://sheets.googleapis.com/v4/spreadsheets/";
	private static final Path DEVELOPMENT_TRANSLATION_CSV = Paths.get("..", "shared", "src", "main", "resources", "i18n",
	        "translation4.csv");
	private static final Path DEFAULT_LOCAL_TRANSLATION_CSV = Paths.get("local", "i18n", "translation4.csv");
	private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
	private static List<Locale> locales = null;
	private static Locale forcedLocale = null;
	private static ClassLoader i18nloader = null;
	private static int line;
	private static long resetTimeStamp = System.currentTimeMillis();
	private static Supplier<Locale> localeSupplier;
	
	// Static storage for Properties data to use with ResourceBundle.Control
	private static Properties[] storedLanguageProperties = null;
	private static List<Locale> storedLocales = null;
	private static String storedBaseName = null;
	
	// Cache for created ResourceBundles to avoid recreating them
	private static final Map<String, ResourceBundle> bundleCache = new HashMap<>();

	@SuppressWarnings("deprecation") // Locale constructors are deprecated in Java 19+.
	public static Locale createLocale(String localeString) {
		if (localeString == null) {
			throwInvalidLocale(localeString);
			return null; // unreacheable
		} else {
			String[] parts = localeString.split("_");
			if (parts.length == 1) {
				return new Locale(parts[0]);
			} else if (parts.length == 2) {
				return new Locale(parts[0], parts[1]);
			} else if (parts.length >= 3) {
				return new Locale(parts[0], parts[1], parts[2]);
			} else {
				throwInvalidLocale(localeString);
				return null; // unreacheable
			}
		}
	}

	public static List<Locale> getAllAvailableLocales() {
		if (locales == null) {
			Translator.getBundleFromCSV(Locale.ENGLISH);
		}
		return locales;
	}

	public static List<Locale> getUsefulLocales() {
		if (locales == null) {
			Translator.getBundleFromCSV(Locale.ENGLISH);
		}
		return locales.stream().filter(l -> !(l.getCountry() == "" && l.getLanguage() == "es")).collect(Collectors.toList());
	}

	public static List<Locale> getAvailableLocales() {
		return helper.getProvidedLocales();
	}

	public static Locale getForcedLocale() {
		return forcedLocale;
	}

	public static Enumeration<String> getKeys() {
		return Translator.getBundleFromCSV(Locale.ENGLISH).getKeys();
	}

	/**
	 * @return the localeSupplier
	 */
	public static Supplier<Locale> getLocaleSupplier() {
		return localeSupplier;
	}

	public static Map<String, String> getMap() {
		final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(getLocaleSupplier().get());
		Map<String, String> translations = new HashMap<>();
		Enumeration<String> keys = bundle.getKeys();
		String key;
		while (keys.hasMoreElements()) {
			key = keys.nextElement();
			translations.put(key, bundle.getString(key));
		}
		return translations;
	}

	/**
	 * Get all translations for a specific locale as a map.
	 * This safely accesses the internal Properties objects without exposing them.
	 * Uses getBundleFromCSV internally (cached by Java's ResourceBundle).
	 * 
	 * @param locale the locale to get translations for
	 * @return Map of all translations for the given locale, empty map if locale not found
	 */
	public static Map<String, String> getMapForLocale(Locale locale) {
		try {
			final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);
			Map<String, String> translations = new HashMap<>();
			Enumeration<String> keys = bundle.getKeys();
			String key;
			while (keys.hasMoreElements()) {
				key = keys.nextElement();
				translations.put(key, bundle.getString(key));
			}
			return translations;
		} catch (Exception e) {
			// Return empty map if locale not found or error occurs
			return new HashMap<>();
		}
	}

	public static long getResetTimeStamp() {
		return resetTimeStamp;
	}

	public static List<String> readLine(ICsvListReader listReader) throws IOException {
		line++;
		return listReader.read();
	}

	/**
	 * Force a reload of the translation files
	 */
	public static void reset() {
		logger.info("TRANSLATOR: reset() called - clearing caches");
		resetTimeStamp = System.currentTimeMillis();
		locales = null;
		i18nloader = null;
		helper = new Translator();
		bundleCache.clear(); // Clear our custom ResourceBundle cache
		storedLanguageProperties = null; // Clear stored Properties
		storedLocales = null;
		storedBaseName = null;
		logger.debug("TRANSLATOR: cleared translation class loader and custom cache");
		logTranslationCsvSource("cache reset");
	}

	public static void resetFromLocal() {
		reset();
	}

	public static void resetFromGoogleSheets() {
		try {
			Path translationTarget = getDevelopmentTranslationTarget();
			if (translationTarget == null) {
				throw new IllegalStateException("No development translation4.csv found");
			}
			logger.info("fetching translation CSV from Google Sheets");
			byte[] translationCsv = downloadTranslationCsvFromGoogleSheets();
			Files.write(translationTarget, translationCsv);
			logger.info("updated translation CSV from Google Sheets: {}", translationTarget);
			resetFromLocal();
		} catch (IOException e) {
			throw new RuntimeException("Could not update development translation CSV from Google Sheets", e);
		}
	}

	private static Path getDevelopmentTranslationTarget() {
		Path localDir = ResourceWalker.getLocalDirPath();
		Path localTranslation = localDir == null ? null : localDir.resolve("i18n").resolve("translation4.csv");
		List<Path> candidates = new ArrayList<>();
		candidates.add(DEVELOPMENT_TRANSLATION_CSV);
		for (Path directory = Paths.get("").toAbsolutePath().normalize(); directory != null; directory = directory.getParent()) {
			candidates.add(directory.resolve("shared/src/main/resources/i18n/translation4.csv"));
		}
		candidates.add(localTranslation);
		candidates.add(DEFAULT_LOCAL_TRANSLATION_CSV);
		for (Path candidate : candidates) {
			if (candidate != null) {
				Path normalizedCandidate = candidate.toAbsolutePath().normalize();
				boolean regularFile = Files.isRegularFile(normalizedCandidate);
				logger.info("translation CSV candidate={} regularFile={}", normalizedCandidate, regularFile);
				if (regularFile) {
					return normalizedCandidate;
				}
			}
		}
		return null;
	}

	public static void setForcedLocale(Locale locale) {
		// logger.debug("setForcedLocale {} {}",locale, LoggerUtils.stackTrace());
		if (locale != null) {
			locales = getAllAvailableLocales();

			for (Locale l : getAllAvailableLocales()) {
				if (l.getLanguage() == locale.getLanguage()) {
					// thing will work no matter what the country and variant
					Translator.forcedLocale = locale;
					break;
				}
			}
		} else {
			Translator.forcedLocale = null; // use browser-provided locale
		}
	}

	/**
	 * @param localeSupplier the localeSupplier to set
	 */
	public static void setLocaleSupplier(Supplier<Locale> localeSupplier) {
		Translator.localeSupplier = localeSupplier;
	}

	public static String translate(String string) {
		return helper.getTranslation(string, getLocaleSupplier().get());
	}

	public static String translate(String string, Locale locale) {
		return helper.getTranslation(string, locale);
	}

	public static String translateExplicitLocale(String string, Locale locale) {
		return helper.getTranslationExplicitLocale(string, locale);
	}

	public static String translate(String string, Locale locale, Object... params) {
		return helper.getTranslation(string, locale, params);
	}

	public static String translate(String string, Object... params) {
		return helper.getTranslation(string, getLocaleSupplier().get(), params);
	}

	public static String translateNoOverrideOrElseNull(String string, Locale locale) {
		return helper.getTranslationNoOverrideOrElseNull(string, locale);
	}

	public static String translateNoInheritanceOrElseNull(String string, Locale locale) {
		return helper.getTranslationNoInheritanceOrElseNull(string, locale);
	}

	public static String translateOrElseEn(String string, Locale locale) {
		return helper.getTranslationOrElseEn(string, locale);
	}

	public static String translateOrElseNull(String string, Locale locale) {
		return helper.getTranslationOrElseNull(string, locale);
	}

	public static String translateOrElseEmpty(String string, Locale locale) {
		String translationOrElseNull = helper.getTranslationOrElseNull(string, locale);
		return translationOrElseNull != null ? translationOrElseNull : "";
	}

	public static String translateOrElseEmpty(String string) {
		String translationOrElseNull = helper.getTranslationOrElseNull(string, getLocaleSupplier().get());
		return translationOrElseNull != null ? translationOrElseNull : "";
	}
	
	public static String translateOrElseNull(String string) {
		String translationOrElseNull = helper.getTranslationOrElseNull(string, getLocaleSupplier().get());
		return translationOrElseNull;
	}

	public static boolean isRTL(Locale locale) {
		return "ar".contentEquals(locale.getLanguage()) || "he".contentEquals(locale.getLanguage())
		        || "fa".contentEquals(locale.getLanguage());
	}
	
	/**
	 * Return a resource bundle created by reading a CSV files. This creates properties files, and uses the standard caching implementation and bundle hierarchy
	 * as defined by Java.
	 *
	 * Resource bundles are cached by the Java implementation, so this method can be called repeatedly.
	 *
	 * Adapted from https://hub.jmonkeyengine.org/t/i18n-from-csv-calc/31492
	 *
	 * @param locale
	 *
	 * @return
	 */
	private static synchronized ResourceBundle getBundleFromCSV(Locale locale) {
		String baseName = BUNDLE_BASE;
		String csvName = BUNDLE_PACKAGE_SLASH + baseName + ".csv";
		Path bundleDir = null;
		try {
			line = 0;

			if (i18nloader == null) {
				bundleDir = MemTempUtils.createTempDirectory("bundles");

				logger.debug("reloading translation bundles");
				logTranslationCsvSource("bundle reload");
				InputStream csvStream = getTranslationCsvStream(csvName);
				logger.debug("csvStream {} {}", csvName, csvStream);
				if (csvStream == null) {
					throw new RuntimeException(csvName + " not found");
				}
				String csvText = new String(csvStream.readAllBytes(), StandardCharsets.UTF_8);
				if (!csvText.isEmpty() && !(csvText.endsWith("\n") || csvText.endsWith("\r"))) {
					csvText = csvText + "\n";
				}
				ICsvListReader listReader = null;
				try {
					CsvPreference[] preferences = new CsvPreference[] { CsvPreference.STANDARD_PREFERENCE,
					        CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE, CsvPreference.TAB_PREFERENCE };

					List<String> stringList = new ArrayList<>();
					for (CsvPreference preference : preferences) {
						listReader = new CsvListReader(new StringReader(csvText),
						        preference);

						if ((stringList = readLine(listReader)) == null) {
							throw new RuntimeException(csvName + " file is empty");
						} else {
							logger.trace(stringList.toString());
							break;
						}
					}

					final Path[] outFiles = new Path[stringList.size()];
					final Properties[] languageProperties = new Properties[outFiles.length];
					locales = new ArrayList<>();

					int nbLanguages = 0;
					for (int i = 1; i < outFiles.length; i++) {
						String language = stringList.get(i);
						if (language == null || language.isBlank() || "xx".contentEquals(language)) {
							nbLanguages = i - 1;
							break;
						}
						logger.trace("language={} {}", language, i);
						locales.add(createLocale(language));
						if (language != null && !language.isEmpty()) {
							language = "_" + language;
						}
						final Path outfile = bundleDir.resolve(baseName + language + ".properties");
						Files.createFile(outfile);
						outFiles[i] = outfile;
						languageProperties[i] = new Properties();
						nbLanguages = i;
					}

					// reading to properties
					String lastProcessedKey = null;
					while ((stringList = readLine(listReader)) != null) {
						String key = stringList.get(0);
						if (key == null) {
							lastProcessedKey = null;
//							String message = MessageFormat.format("{0} line {1}: key is null", csvName, line);
//							logger.error(message);
//							throw new RuntimeException(message);
						} else {
							key = key.trim();
							lastProcessedKey = key;
							logger.debug(stringList.toString());
							for (int i = 1; i < nbLanguages + 1; i++) {
								// treat the CSV strings using same rules as Properties files.
								// u0000 escapes are translated to Java characters
								String input = i < stringList.size() ? stringList.get(i) : "";
								if (input != null) {
									input = input.trim();
									// "\ " is not valid, \u0020 is needed.
									try {
										String unescapeJava = StringEscapeUtils.unescapeJava(input.trim());
										if (!unescapeJava.isEmpty()) {
											Properties properties = languageProperties[i];
											if (properties == null) {
												String message = MessageFormat
												        .format("{0} line {1}: languageProperties[{2}] is null", csvName, line,
												                i);
												logger.error(message);
												throw new RuntimeException(message);
											}
											properties.setProperty(key, unescapeJava);
										}
									} catch (RuntimeException e) {
										// Convert column index to Excel-style letter (i-1 because i starts at 1)
										String cellCol = columnIndexToExcelLetter(i - 1);
										String cellRef = cellCol + line;
										String message = MessageFormat
										        .format("{0} cell {1}: Invalid unicode escape in key ''{2}''. Content: {3}. Error: {4}",
										                csvName, cellRef, key, input, e.getMessage());
										logger.error(message);
										throw new RuntimeException(message, e);
									}
								}
							}
						}
					}
					logger.debug("{} Translation CSV read loop exited at line {}. Last processed key='{}'",
					        LoggerUtils.whereFrom(), line, lastProcessedKey);
					if (languageProperties.length > 1 && languageProperties[1] != null
					        && !languageProperties[1].containsKey("ImportR.DoIt")) {
						logger.debug("{} Missing translation key after CSV parse: ImportR.DoIt (line {}, last key='{}')",
						        LoggerUtils.whereFrom(), line, lastProcessedKey);
					}

					// writing
					for (int i = 1; i < nbLanguages + 1; i++) {
						// logger.debug("writing to " + outFiles[i].toAbsolutePath());
						languageProperties[i].store(Files.newOutputStream(outFiles[i]), "generated from " + csvName);
					}
					final URL[] urls = { bundleDir.toUri().toURL() };
					i18nloader = new URLClassLoader(urls);

					// reload the files
					ResourceBundle.clearCache();
					bundleCache.clear(); // Clear our custom cache too
					
					// Store Properties for use with custom ResourceBundle.Control
					storedLanguageProperties = languageProperties;
					storedLocales = locales;
					storedBaseName = baseName;

				} catch (IOException e) {
					LoggerUtils.logError(logger, e);
				} finally {
					csvText = null;
					if (listReader != null) {
						try {
							listReader.close();
						} catch (IOException e) {
						}
					}
				}
				
				//LocaleNameSorter.sortLocales(locales);

			}

			// Use a custom ResourceBundle.Control to handle our in-memory Properties
			ResourceBundle.Control customControl = new ResourceBundle.Control() {
				@Override
				public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
						throws IllegalAccessException, InstantiationException, IOException {
					
					if ("java.properties".equals(format) && storedLanguageProperties != null) {
						String bundleName = toBundleName(baseName, locale);
						String resourceName = toResourceName(bundleName, "properties");
						
						// Check cache first (unless reload is forced)
						String cacheKey = resourceName;
						if (!reload && bundleCache.containsKey(cacheKey)) {
							return bundleCache.get(cacheKey);
						}
						
						// Check if we have this resource in our Properties
						for (int i = 1; i < storedLanguageProperties.length; i++) {
							if (storedLanguageProperties[i] != null && i <= storedLocales.size()) {
								Locale propLocale = storedLocales.get(i - 1);
								String expectedFileName = storedBaseName + "_" + propLocale.toString() + ".properties";
								if (resourceName.equals(expectedFileName)) {
									try {
										java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
										storedLanguageProperties[i].store(baos, "Generated from CSV");
										java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
										ResourceBundle bundle = new PropertyResourceBundle(bais);
										// Cache the bundle
										bundleCache.put(cacheKey, bundle);
										return bundle;
									} catch (Exception e) {
										logger.error("Error creating PropertyResourceBundle: {}", e.getMessage());
									}
								}
							}
						}
						
						// Check for root bundle
						if (resourceName.equals(storedBaseName + ".properties") && storedLanguageProperties.length > 1 && storedLanguageProperties[1] != null) {
							try {
								java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
								storedLanguageProperties[1].store(baos, "Generated from CSV - root bundle");
								java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(baos.toByteArray());
								ResourceBundle bundle = new PropertyResourceBundle(bais);
								// Cache the bundle
								bundleCache.put(cacheKey, bundle);
								return bundle;
							} catch (Exception e) {
								logger.error("Error creating root PropertyResourceBundle: {}", e.getMessage());
							}
						}
					}
					
					return null; // Let default handling take over
				}
				
				@Override
				public long getTimeToLive(String baseName, Locale locale) {
					// Cache bundles indefinitely until explicitly cleared
					return TTL_DONT_CACHE; // Use our own caching mechanism
				}
				
				@Override
				public boolean needsReload(String baseName, Locale locale, String format,
						ClassLoader loader, ResourceBundle bundle, long loadTime) {
					// Only reload if the stored properties timestamp is newer
					return resetTimeStamp > loadTime;
				}
			};
			
			ResourceBundle bundle = ResourceBundle.getBundle(baseName, locale, customControl);
			return bundle;
		} catch (IOException e) {
			logger.error("cannot create bundles directory {}", e.getMessage());
			throw new RuntimeException(e);
		} finally {
			// cannot clean up, resource bundle keeps the files open
			// try {
			// FileUtils.deleteDirectory(bundleDir.toFile());
			// } catch (IOException e) {
			// logger.error("cannot delete bundles directory {}", e);
			// throw new RuntimeException(e);
			// }
		}
	}

	private static InputStream getTranslationCsvStream(String csvName) throws IOException {
		return ResourceWalker.getResourceAsStream(csvName);
	}

	/**
	 * Downloads the configured Google Sheets translation CSV through the Sheets API.
	 *
	 * @return the CSV contents encoded as UTF-8
	 * @throws IOException if Google Sheets cannot provide a valid translation CSV
	 */
	public static byte[] downloadTranslationCsvFromGoogleSheets() throws IOException {
		String apiKey = System.getenv(GOOGLE_SHEETS_API_KEY);
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(GOOGLE_SHEETS_API_KEY + " is not configured");
		}
		return downloadTranslationCsv(apiKey);
	}

	private static byte[] downloadTranslationCsv(String apiKey) throws IOException {
		String encodedKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
		String metadataUrl = GOOGLE_SHEETS_API + GOOGLE_SPREADSHEET_ID
		        + "?fields=sheets.properties.title&key=" + encodedKey;
		JsonNode metadata = getGoogleSheetsJson(metadataUrl, "metadata");
		String tab = chooseGoogleSheetsTab(metadata.path("sheets"));
		if (tab == null) {
			throw new IOException("Google Sheets API returned no worksheet titles");
		}

		String quotedTab = "'" + tab.replace("'", "''") + "'";
		String valuesUrl = GOOGLE_SHEETS_API + GOOGLE_SPREADSHEET_ID + "/values/"
		        + URLEncoder.encode(quotedTab, StandardCharsets.UTF_8)
		        + "?majorDimension=ROWS&key=" + encodedKey;
		JsonNode values = getGoogleSheetsJson(valuesUrl, "translation values").path("values");
		if (!values.isArray() || values.isEmpty()) {
			throw new IOException("Google Sheets API returned no translation values");
		}

		StringWriter csv = new StringWriter();
		try (CsvListWriter writer = new CsvListWriter(csv, CsvPreference.EXCEL_PREFERENCE)) {
			for (JsonNode rowNode : values) {
				List<String> row = new ArrayList<>();
				if (rowNode.isArray()) {
					for (JsonNode value : rowNode) {
						row.add(value.asString());
					}
				}
				writer.write(row);
			}
		}
		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static JsonNode getGoogleSheetsJson(String url, String requestName) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		connection.setRequestMethod("GET");
		connection.setConnectTimeout(30_000);
		connection.setReadTimeout(120_000);
		connection.setUseCaches(false);
		connection.setRequestProperty("Cache-Control", "no-cache");
		connection.setRequestProperty("Pragma", "no-cache");
		try {
			int responseCode = connection.getResponseCode();
			if (responseCode < 200 || responseCode >= 300) {
				throw new IOException("Google Sheets API " + requestName + " request failed with HTTP " + responseCode);
			}
			try (InputStream response = connection.getInputStream()) {
				return JSON_MAPPER.readTree(response);
			}
		} finally {
			connection.disconnect();
		}
	}

	private static String chooseGoogleSheetsTab(JsonNode sheets) {
		List<String> titles = new ArrayList<>();
		for (JsonNode sheet : sheets) {
			String title = sheet.path("properties").path("title").asString(null);
			if (title != null && !title.isBlank()) {
				titles.add(title);
			}
		}
		String requested = System.getenv(GOOGLE_SHEETS_TAB);
		List<String> candidates = requested == null || requested.isBlank()
		        ? List.of(BUNDLE_BASE)
		        : List.of(requested, BUNDLE_BASE);
		for (String candidate : candidates) {
			for (String title : titles) {
				if (title.equals(candidate) || title.equalsIgnoreCase(candidate)) {
					return title;
				}
			}
		}
		return titles.isEmpty() ? null : titles.get(0);
	}

	private static void logTranslationCsvSource(String trigger) {
		String csvName = BUNDLE_PACKAGE_SLASH + BUNDLE_BASE + ".csv";
		Path localOverride = ResourceWalker.getLocalDirPath();
		Path localTranslation = localOverride != null ? localOverride.resolve(csvName.substring(1)) : null;
		URL classpathTranslation = Translator.class.getResource(csvName);
		ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
		URL contextTranslation = contextLoader != null ? contextLoader.getResource(csvName.substring(1)) : null;
		String source = localTranslation != null && Files.exists(localTranslation)
		        ? "local override"
		        : classpathTranslation != null
		                ? "classpath"
		                : contextTranslation != null ? "context classpath" : "not found";
		logger.debug("translation CSV source trigger={} source={}", trigger, source);
	}

	private static void throwInvalidLocale(String localeString) {
		String message = MessageFormat.format("invalid locale: {0}", localeString);
		logger.error(message);
		throw new RuntimeException(message);
	}

	@Override
	public List<Locale> getProvidedLocales() {
		if (getForcedLocale() != null) {
			return Arrays.asList(getForcedLocale());
		} else {
			return getAllAvailableLocales();
		}
	}

	/**
	 * @see com.vaadin.flow.i18n.I18NProvider#getTranslation(java.lang.String, java.util.Locale, java.lang.Object[])
	 */
	@Override
	public String getTranslation(String key, Locale locale, Object... params) {
		locale = overrideLocale(locale);
		if (UI.getCurrent() != null) {
			if (isRTL(locale)) {
				UI.getCurrent().getPage().executeJs("document.body.setAttribute('dir', 'rtl')");
			} else {
				UI.getCurrent().getPage().executeJs("document.body.setAttribute('dir', 'ltr')");
			}
		}
		return doTranslation(key, locale, params);
	}

	/**
	 * @see com.vaadin.flow.i18n.I18NProvider#getTranslation(java.lang.String, java.util.Locale, java.lang.Object[])
	 */
	public String getTranslationExplicitLocale(String key, Locale locale, Object... params) {
		return doTranslation(key, locale, params);
	}

	private String doTranslation(String key, Locale locale, Object... params) {
		if (key == null) {
			nullTranslationKey();
			return "";
		}

		final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);

		String value;
		try {
			value = bundle.getString(key);
		} catch (final MissingResourceException e) {
			return "!" + locale.getLanguage() + ": " + key;
		}
		if (params.length > 0) {
			value = format(value, params);
		}
		return value;
	}

	public String getTranslationNoOverrideOrElseNull(String key, Locale locale, Object... params) {
		if (key == null) {
			nullTranslationKey();
			return "";
		}
		final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);

		String value;

		// value = (String) bundle.handleGetObject(key);
		try {
			value = bundle.getString(key);
		} catch (final MissingResourceException e) {
			return null;
		}
		if (params.length > 0) {
			value = format(value, params);
		}
		return value;
	}

	public String getTranslationNoInheritanceOrElseNull(String key, Locale locale, Object... params) {
		if (key == null) {
			nullTranslationKey();
			return "";
		}
		final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);

		String value;

		value = (String) bundle.handleGetObject(key);
		if (params.length > 0) {
			value = format(value, params);
		}
		return value;
	}

	public String getTranslationOrElseEn(String key, Locale locale, Object... params) {
		locale = overrideLocale(locale);

		if (key == null) {
			nullTranslationKey();
			return "";
		}
		final PropertyResourceBundle bundle = (PropertyResourceBundle) getBundleFromCSV(locale);

		String value;
		try {
			value = bundle.getString(key);
		} catch (final MissingResourceException e) {
			PropertyResourceBundle enBundle = (PropertyResourceBundle) getBundleFromCSV(Locale.ENGLISH);
			value = (String) enBundle.handleGetObject(key);
		}
		if (params.length > 0 && value != null) {
			value = format(value, params);
		}
		return value;
	}

	public String getTranslationOrElseNull(String key, Locale locale, Object... params) {
		locale = overrideLocale(locale);
		return getTranslationNoOverrideOrElseNull(key, locale, params);
	}

	public void nullTranslationKey() {
		logger./**/warn("null translation key");
	}

	private String format(String pattern, Object... params) {
		String value = pattern;
		if (params.length > 0) {
			// single quotes must be doubled. If already doubled in the input, fix back.
			pattern = pattern.replaceAll("'", "''");
			pattern = pattern.replaceAll("''''", "''");
			value = MessageFormat.format(pattern, params);
			// logger.trace("format {} input={} params={} \\n result={}", params.getClass(), pattern, params, value);
		}
		return value;
	}

	private Locale overrideLocale(Locale locale) {
		Locale l = getLocaleSupplier().get();
		if (l != null) {
			locale = l;
		} else if (getForcedLocale() != null) {
			locale = getForcedLocale();
		}
		return locale;
	}

	/**
	 * Convert a column index (0-based) to Excel-style letter(s).
	 * Examples: 0 -> A, 1 -> B, 25 -> Z, 26 -> AA, 27 -> AB
	 */
	private static String columnIndexToExcelLetter(int colIndex) {
		StringBuilder result = new StringBuilder();
		int col = colIndex + 1; // Convert to 1-based
		while (col > 0) {
			col--; // Adjust for 0-based Excel column letters
			result.insert(0, (char) ('A' + (col % 26)));
			col /= 26;
		}
		return result.toString();
	}

}
