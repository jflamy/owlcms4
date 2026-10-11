/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * Cleans text that comes from outside the program's own translations.
 *
 * Applied at approved places only: values inserted into translations (Translator), validation error labels, and
 * athlete and technical official imports. Translations themselves are trusted and never filtered.
 */
public final class HtmlSanitizer {

	private static final String BASE_URI = "http://owlcms.invalid/";
	private static final Pattern TAG = Pattern.compile("<\\s*[a-zA-Z/!?]");
	private static final Safelist FORMATTING = Safelist.relaxed()
	        .addAttributes(":all", "class", "title", "dir", "lang")
	        .addAttributes("a", "target", "rel")
	        .addTags("vaadin-icon")
	        .addAttributes("vaadin-icon", "icon")
	        .preserveRelativeLinks(true);

	private HtmlSanitizer() {
	}

	/**
	 * Keep approved formatting, images and links while removing script-capable elements and attributes.
	 *
	 * SVG, MathML, forms, embedded documents, event handlers, style attributes and unsafe URL protocols are not
	 * in the allowlist. Relative URLs are kept as written.
	 */
	public static String sanitizeFormattedHtml(String html) {
		if (html == null) {
			return "";
		}
		return Jsoup.clean(html, BASE_URI, FORMATTING, outputSettings());
	}

	/**
	 * For imported fields that may contain formatting (custom1, custom2).
	 */
	public static String cleanFormattedText(String value) {
		if (value == null || !TAG.matcher(value).find()) {
			return value;
		}
		return sanitizeFormattedHtml(value);
	}

	/**
	 * For imported names, which are plain text. Actual tags and script contents are removed. Encoded markup
	 * remains encoded so cleaning cannot turn inert text into active markup.
	 */
	public static String toPlainText(String value) {
		if (value == null || !TAG.matcher(value).find()) {
			return value;
		}
		return Jsoup.clean(value, BASE_URI, Safelist.none(), outputSettings());
	}

	private static Document.OutputSettings outputSettings() {
		return new Document.OutputSettings()
		        .charset(StandardCharsets.UTF_8)
		        .prettyPrint(false);
	}
}
