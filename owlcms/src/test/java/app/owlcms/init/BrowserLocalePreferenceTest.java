/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.init;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.DateTimeUtils;

public class BrowserLocalePreferenceTest {

	private static final List<Locale> TRANSLATED = locales("en", "en-US", "en-CA", "es", "es-419", "es-EC", "fr",
	        "fr-CA");
	private static final Locale ES_CL = Locale.forLanguageTag("es-CL");
	private static final Locale ES_AR = Locale.forLanguageTag("es-AR");

	@BeforeClass
	public static void setup() {
		// translations are read through the application configuration
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void teardown() {
		JPAService.close();
	}

	@Test
	public void browserOrderWinsOverAnExactMatchFurtherDown() {
		assertEquals(ES_AR, pick("es-AR", "en"));
		assertEquals(Locale.forLanguageTag("es-US"), pick("es-US", "en-US"));
	}

	@Test
	public void exactTranslationIsPreferred() {
		assertEquals(Locale.forLanguageTag("es-EC"), pick("es-EC", "es"));
		assertEquals(Locale.forLanguageTag("fr-CA"), pick("fr-CA", "en-US"));
	}

	@Test
	public void browserCountryIsKeptOnlyWhenJavaFormatsIt() {
		assertEquals(Locale.forLanguageTag("en-GB"), pick("en-GB", "en"));
		assertEquals(Locale.forLanguageTag("fr"), pick("fr-SE", "fr"));
		// Java has no Spanish formats for Russia: generic Spanish, not another Spanish country
		assertEquals(Locale.forLanguageTag("es"), pick("es-RU", "ru-RU"));
	}

	@Test
	public void untranslatedLanguagesAreSkipped() {
		assertEquals(Locale.forLanguageTag("fr-CA"), pick("ko-KR", "fr-CA"));
		assertEquals(Locale.ENGLISH, pick("ko-KR"));
	}

	/** The release notes example: same Spanish text in Chile and Argentina, different dates. */
	@Test
	public void chileAndArgentinaShareTheTextButNotTheDates() {
		assertEquals(ES_CL, pick("es-CL"));
		assertEquals(ES_AR, pick("es-AR"));

		assertEquals(Translator.translate("Snatch", Locale.forLanguageTag("es")), Translator.translate("Snatch", ES_CL));
		assertEquals(Translator.translate("Snatch", ES_CL), Translator.translate("Snatch", ES_AR));

		LocalDate date = LocalDate.of(2026, 11, 14);
		String chile = date.format(DateTimeFormatter.ofPattern(DateTimeUtils.localizedShortDatePattern(ES_CL), ES_CL));
		String argentina = date.format(DateTimeFormatter.ofPattern(DateTimeUtils.localizedShortDatePattern(ES_AR), ES_AR));
		assertEquals("14-11-2026", chile);
		assertEquals("14/11/2026", argentina);
		assertNotEquals(chile, argentina);
	}

	@Test
	public void browserVariantFollowsTheGenericLanguageInTheMenu() {
		List<Locale> menu = OwlcmsSession.languageMenuChoices(TRANSLATED, locales("es-CL", "en"));
		assertEquals(locales("en", "en-US", "en-CA", "es", "es-CL", "es-419", "es-EC", "fr", "fr-CA"), menu);

		// an existing variant moves next to the generic language and is not listed twice
		menu = OwlcmsSession.languageMenuChoices(TRANSLATED, locales("es-EC"));
		assertEquals(locales("en", "en-US", "en-CA", "es", "es-EC", "es-419", "fr", "fr-CA"), menu);

		// unsupported combination: nothing added
		assertEquals(TRANSLATED, OwlcmsSession.languageMenuChoices(TRANSLATED, locales("es-RU")));
	}

	private static Locale pick(String... browser) {
		return OwlcmsSession.browserPreference(locales(browser), TRANSLATED);
	}

	private static List<Locale> locales(String... tags) {
		return Arrays.stream(tags).map(Locale::forLanguageTag).toList();
	}
}
