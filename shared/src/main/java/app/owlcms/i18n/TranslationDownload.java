/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 ("NPOSL-3.0")
 *******************************************************************************/
package app.owlcms.i18n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Downloads the Google Sheets translations to a specified CSV file. */
public final class TranslationDownload {

	private TranslationDownload() {
	}

	/**
	 * Downloads the translations to the supplied destination path.
	 *
	 * @param args the destination CSV path
	 * @throws IOException if the translations cannot be downloaded or written
	 */
	public static void main(String... args) throws IOException {
		if (args.length != 1) {
			System.err.println("Usage: TranslationDownload <destination_csv>");
			System.exit(2);
		}
		Files.write(Path.of(args[0]), Translator.downloadTranslationCsvFromGoogleSheets());
	}
}