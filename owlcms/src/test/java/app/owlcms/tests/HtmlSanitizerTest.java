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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.HtmlSanitizer;

public class HtmlSanitizerTest {

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
	public void plainImportedNamesStayPlainAndMultilingual() {
		assertEquals("O'Brien, Jr.", HtmlSanitizer.toPlainText("O'Brien, Jr."));
		assertEquals("U<15", HtmlSanitizer.toPlainText("U<15"));
		assertEquals("张伟 — Петров — Մկրտչյան",
		        HtmlSanitizer.toPlainText("张伟 — Петров — Մկրտչյան"));
		assertEquals("Smith John", HtmlSanitizer.toPlainText("Smith <b>John</b>"));
		assertEquals("Bob", HtmlSanitizer.toPlainText("<script>alert(1)</script>Bob"));
	}

	@Test
	public void plainTextCleanupDoesNotActivateEncodedMarkup() {
		assertEquals("&lt;img src=x onerror=alert(1)&gt;",
		        HtmlSanitizer.toPlainText("<b>&lt;img src=x onerror=alert(1)&gt;</b>"));
	}

	@Test
	public void formattedHtmlKeepsApprovedImagesLinksAndIcons() {
		String input = "<strong>Warning</strong><br>"
		        + "<a href=\"/age\" target=\"_blank\">Age</a>"
		        + "<img src=\"icons/warning.svg\" alt=\"warning\">"
		        + "<vaadin-icon icon=\"vaadin:warning\"></vaadin-icon>";
		assertEquals(input, HtmlSanitizer.sanitizeFormattedHtml(input));
	}

	@Test
	public void formattedHtmlRejectsScriptCapableMarkupAndAttributes() {
		String result = HtmlSanitizer.sanitizeFormattedHtml(
		        "<svg><animate xlink:href=\"#x\" attributeName=\"href\""
		                + " values=\"https://safe.invalid;javascript:alert(1)\"></animate>"
		                + "<a id=\"x\"><text>Click</text></a></svg>"
		                + "<span style=\"position:fixed\" onclick=\"alert(2)\">Text</span>"
		                + "<script>alert(3)</script>"
		                + "<a href=\"javascript:alert(4)\">Bad link</a>");

		assertEquals("<a>Click</a><span>Text</span><a>Bad link</a>", result);
		assertFalse(result.contains("javascript:"));
		assertFalse(result.contains("onclick"));
		assertFalse(result.contains("style="));
		assertFalse(result.contains("<svg"));
		assertFalse(result.contains("<script"));
	}

	@Test
	public void translateKeepsTrustedMarkupAndStripsTagsFromInsertedValues() {
		String result = Translator.translate("Championship.DeleteWithAgeGroupsWarning", Locale.ENGLISH,
		        "<img src=x onerror=alert(1)>Open");
		assertTrue("translation markup is trusted and kept", result.contains("<strong>"));
		assertTrue("the inserted value keeps its text", result.contains("Delete the Open championship?"));
		assertFalse("tags in inserted values are removed", result.contains("<img"));

		assertEquals("ordinary text is unchanged", "Delete the O'Brien & Sons, U<15 championship?",
		        Translator.translate("Championship.DeleteWithAgeGroupsWarning", Locale.ENGLISH,
		                "O'Brien & Sons, U<15").split("<br>")[0]);
		assertEquals("numbers are formatted as before", "1,234 kg needed on the bar.",
		        Translator.translate("Notification.WeightToBeLoaded", Locale.ENGLISH, 1234));
		assertFalse("encoded markup is never turned into a tag",
		        Translator.translate("Championship.DeleteWithAgeGroupsWarning", Locale.ENGLISH,
		                "<b>&lt;img src=x onerror=alert(1)&gt;</b>").contains("<img"));
	}
}
