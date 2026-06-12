/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.junit.Test;

import app.owlcms.utils.TemplateResourceUtils;

public class TemplateResourceUtilsTest {

	@Test
	public void stripsOnlyPaperSizeSuffixWhileKeepingLocaleSuffix() {
		assertTrue(TemplateResourceUtils.stripPaperSizeSuffix("Template_LETTER-es-SV.xlsx")
		        .endsWith("-es-SV"));
		assertTrue(TemplateResourceUtils.stripPaperSizeSuffix("Template_LETTER_es_SV.xlsx")
		        .endsWith("_es_SV"));
		assertTrue(TemplateResourceUtils.stripPaperSizeSuffix("Template_LETTER-es_SV.xlsx")
		        .endsWith("-es_SV"));
		assertTrue(TemplateResourceUtils.stripPaperSizeSuffix("Template_LETTER_es-SV.xlsx")
		        .endsWith("_es-SV"));
		assertFalse(TemplateResourceUtils.stripPaperSizeSuffix("Template_LETTER-es-SV.xlsx")
		        .contains("LETTER"));
	}

	@Test
	public void resolvesDefaultPaperSizeByCountry() {
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize(Locale.US));
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize(Locale.CANADA));
		assertEquals("A4", TemplateResourceUtils.resolvedDefaultPaperSize(Locale.FRANCE));
	}

	/**
	 * Browser timezone → paper size path.
	 *
	 * Rules (when no Config override is set):
	 * <ul>
	 * <li>US or Canada IANA timezone → LETTER</li>
	 * <li>Timezone in the explicit map for other countries → None (no enforced
	 * default)</li>
	 * <li>Timezone absent from the map → A4</li>
	 * </ul>
	 */
	@Test
	public void resolvesDefaultPaperSizeByBrowserTimezone() {
		// US timezones → LETTER
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("America/New_York"));
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("America/Chicago"));
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("America/Denver"));
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("America/Los_Angeles"));
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("Pacific/Honolulu"));
		// Canada timezones → LETTER
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("America/Toronto"));
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("America/Vancouver"));
		assertEquals("LETTER", TemplateResourceUtils.resolvedDefaultPaperSize("America/St_Johns"));
		// "No default" timezones (in the explicit map but not US/CA) → None
		assertEquals("None", TemplateResourceUtils.resolvedDefaultPaperSize("America/Bogota"));
		assertEquals("None", TemplateResourceUtils.resolvedDefaultPaperSize("America/Mexico_City"));
		assertEquals("None", TemplateResourceUtils.resolvedDefaultPaperSize("America/Caracas"));
		assertEquals("None", TemplateResourceUtils.resolvedDefaultPaperSize("Asia/Manila"));
		assertEquals("None", TemplateResourceUtils.resolvedDefaultPaperSize("America/Panama"));
		// Timezones absent from the map → A4
		assertEquals("A4", TemplateResourceUtils.resolvedDefaultPaperSize("Europe/Paris"));
		assertEquals("A4", TemplateResourceUtils.resolvedDefaultPaperSize("Asia/Tokyo"));
		assertEquals("A4", TemplateResourceUtils.resolvedDefaultPaperSize("Africa/Cairo"));
		// null / blank → A4
		assertEquals("A4", TemplateResourceUtils.resolvedDefaultPaperSize((String) null));
		assertEquals("A4", TemplateResourceUtils.resolvedDefaultPaperSize(""));
	}

	@Test
	public void recognizesPaperSizeSuffixIncludingLocaleVariant() {
		assertTrue(TemplateResourceUtils.hasPaperSizeSuffix("Template_LETTER-es-SV.xlsx", "LETTER"));
		assertTrue(TemplateResourceUtils.hasPaperSizeSuffix("Template_LETTER_es_SV.xlsx", "LETTER"));
		assertTrue(TemplateResourceUtils.hasPaperSizeSuffix("Template_LETTER-es_SV.xlsx", "LETTER"));
		assertTrue(TemplateResourceUtils.hasPaperSizeSuffix("Template_LETTER_es-SV.xlsx", "LETTER"));
		assertTrue(TemplateResourceUtils.hasPaperSizeSuffix("Template_A4.xlsx", "A4"));
		assertFalse(TemplateResourceUtils.hasPaperSizeSuffix("Template-es-SV.xlsx", "LETTER"));
	}
}
