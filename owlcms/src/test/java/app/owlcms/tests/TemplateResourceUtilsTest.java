/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import app.owlcms.utils.Resource;
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

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	/** Creates a Resource with a fake (non-existent) path — sufficient for filtering tests. */
	private static Resource res(String fileName) {
		return new Resource(fileName, Path.of("fake", fileName));
	}

	// ---------------------------------------------------------------------------
	// filterTemplatesByPaperSize — display name assignment
	// ---------------------------------------------------------------------------

	/**
	 * Single LETTER variant, LETTER locale: the paper-size suffix and extension are
	 * stripped and set as the display name.
	 */
	@Test
	public void singleLetterVariant_displayNameStripped() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("NestedStartList-LETTER.xlsx")), null, Locale.US);
		assertEquals(1, result.size());
		assertEquals("NestedStartList", result.get(0).getDisplayName());
		assertEquals("NestedStartList", result.get(0).toString());
	}

	/**
	 * LETTER + A4 variants, LETTER preferred, nothing currently selected:
	 * only the LETTER file survives; its display name is stripped.
	 */
	@Test
	public void twoVariants_onlyPreferredKept_displayNameStripped() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("NestedStartList-LETTER.xlsx"), res("NestedStartList-A4.xlsx")),
		        null, Locale.US);
		assertEquals(1, result.size());
		assertEquals("NestedStartList-LETTER.xlsx", result.get(0).getFileName());
		assertEquals("NestedStartList", result.get(0).getDisplayName());
	}

	/**
	 * LETTER + A4 variants, LETTER preferred, A4 is the currently-selected template:
	 * both survive (A4 kept as the saved template), both strip to the same key →
	 * collision → both display the full file name.
	 */
	@Test
	public void twoVariants_selectedNonPreferred_collision_fullNameShown() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("NestedStartList-LETTER.xlsx"), res("NestedStartList-A4.xlsx")),
		        "NestedStartList-A4.xlsx", Locale.US);
		assertEquals(2, result.size());
		for (Resource r : result) {
			assertNull("collision: displayName must be null for " + r.getFileName(), r.getDisplayName());
			assertTrue("collision: toString() must return full fileName",
			        r.toString().equals(r.getFileName()));
		}
	}

	/**
	 * foo-LETTER.xlsx + foo.xlsx (no paper-size suffix), LETTER preferred, nothing selected:
	 * both group under key "foo"; only foo-LETTER.xlsx matches the preferred size →
	 * it survives alone → strip count == 1 → displayName = "foo".
	 */
	@Test
	public void paperSizeAndNoSuffixVariant_onlyPaperSizeKept_displayNameStripped() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("foo-LETTER.xlsx"), res("foo.xlsx")), null, Locale.US);
		assertEquals(1, result.size());
		assertEquals("foo-LETTER.xlsx", result.get(0).getFileName());
		assertEquals("foo", result.get(0).getDisplayName());
	}

	/**
	 * foo-LETTER.xlsx + foo.xlsx, LETTER preferred, foo.xlsx is the selected template:
	 * both survive; both strip to "foo" → collision → full file names shown.
	 */
	@Test
	public void paperSizeAndNoSuffixVariant_selectedNoSuffix_collision_fullNameShown() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("foo-LETTER.xlsx"), res("foo.xlsx")), "foo.xlsx", Locale.US);
		assertEquals(2, result.size());
		for (Resource r : result) {
			assertNull("collision: displayName must be null for " + r.getFileName(), r.getDisplayName());
		}
	}

	/**
	 * Two completely different templates, LETTER preferred:
	 * each has its own key → no collision → both get stripped display names.
	 */
	@Test
	public void twoDistinctTemplates_bothGetStrippedDisplayNames() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("StartList-LETTER.xlsx"), res("Protocol-LETTER.xlsx")), null, Locale.US);
		assertEquals(2, result.size());
		assertEquals("StartList", result.stream().filter(r -> r.getFileName().startsWith("StartList"))
		        .findFirst().get().getDisplayName());
		assertEquals("Protocol", result.stream().filter(r -> r.getFileName().startsWith("Protocol"))
		        .findFirst().get().getDisplayName());
	}

	/**
	 * The locale-based API only returns "LETTER" (US/CA) or "A4" (everything else).
	 * "None" is only reachable via the timezone-based path. So a non-US/CA locale
	 * (e.g. es-MX) resolves to A4 and filters normally: only the A4 variant survives
	 * and gets a stripped display name.
	 */
	@Test
	public void nonUsLocale_resolvesToA4_filtersNormally() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("foo-LETTER.xlsx"), res("foo-A4.xlsx")), null,
		        Locale.forLanguageTag("es-MX"));
		assertEquals(1, result.size());
		assertEquals("foo-A4.xlsx", result.get(0).getFileName());
		assertEquals("foo", result.get(0).getDisplayName());
	}

	/**
	 * LOCALE_NO_PAPER_SIZE sentinel: filter returns all resources unchanged, no display names set.
	 * This is the testable equivalent of Config.defaultPaperSize == "None".
	 */
	@Test
	public void noPaperSizeSentinel_allResourcesReturnedUnfiltered() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("foo-LETTER.xlsx"), res("foo-A4.xlsx")), null,
		        TemplateResourceUtils.LOCALE_NO_PAPER_SIZE);
		assertEquals(2, result.size());
		for (Resource r : result) {
			assertNull("None path: displayName must not be set", r.getDisplayName());
		}
	}

	/**
	 * LETTER variant with locale suffix, LETTER preferred:
	 * the locale suffix is preserved in the display name, paper-size token stripped.
	 */
	@Test
	public void localeSuffixPreservedInDisplayName() {
		List<Resource> result = TemplateResourceUtils.filterTemplatesByPaperSize(
		        List.of(res("Template_LETTER-es-SV.xlsx")), null, Locale.US);
		assertEquals(1, result.size());
		assertEquals("Template-es-SV", result.get(0).getDisplayName());
	}
}
