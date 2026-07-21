/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 ******************************************************************************/
package app.owlcms.i18n;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

/**
 * Downloads the Google Sheets translations and compares them with a local CSV.
 */
public final class TranslationComparison {

	private static final int DIFFERENCES_FOUND = 1;
	private static final int INVALID_ARGUMENTS = 2;
	private static final int DOWNLOAD_OR_PARSE_ERROR = 4;

	private TranslationComparison() {
	}

	/**
	 * Usage: TranslationComparison &lt;local_csv&gt; &lt;downloaded_remote_csv&gt;.
	 *
	 * @param args the local CSV and the destination for the Google Sheets snapshot
	 */
	public static void main(String... args) {
		int exitCode = run(args);
		if (exitCode != 0) {
			System.exit(exitCode);
		}
	}

	static int run(String... args) {
		if (args.length != 2) {
			System.err.println("Usage: TranslationComparison <local_csv> <downloaded_remote_csv>");
			return INVALID_ARGUMENTS;
		}

		Path localCsv = Path.of(args[0]);
		Path remoteCsv = Path.of(args[1]);
		try {
			Files.write(remoteCsv, Translator.downloadTranslationCsvFromGoogleSheets());
			List<List<String>> localRows = readRows(localCsv);
			List<List<String>> remoteRows = readRows(remoteCsv);
			return printComparison(localRows, remoteRows);
		} catch (IOException | RuntimeException e) {
			System.err.println("ERROR: Could not download or compare Google Sheets translations: " + e.getMessage());
			return DOWNLOAD_OR_PARSE_ERROR;
		}
	}

	private static List<List<String>> readRows(Path csv) throws IOException {
		List<List<String>> rows = new ArrayList<>();
		try (BufferedReader source = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
				ICsvListReader reader = new CsvListReader(source, CsvPreference.EXCEL_PREFERENCE)) {
			List<String> row;
			while ((row = reader.read()) != null) {
				rows.add(new ArrayList<>(row));
			}
		}
		if (!rows.isEmpty() && !rows.get(0).isEmpty()) {
			rows.get(0).set(0, removeBom(rows.get(0).get(0)));
		}
		return rows;
	}

	private static int printComparison(List<List<String>> localRows, List<List<String>> remoteRows) {
		List<String> localHeader = headerOf(localRows);
		List<String> remoteHeader = headerOf(remoteRows);
		TranslationCsv local = TranslationCsv.from(localRows);
		TranslationCsv remote = TranslationCsv.from(remoteRows);
		List<String> differences = new ArrayList<>();

		if (!localHeader.equals(remoteHeader)) {
			differences.add("  Header\n  Language column layout differs between files\n    Local: "
					+ String.join(",", localHeader) + "\n    Remote: " + String.join(",", remoteHeader));
		}
		appendBlankKeyDifference(differences, "HEAD-refreshed local copy", local.keyLineNumbers);
		appendBlankKeyDifference(differences, "Google Sheets", remote.keyLineNumbers);

		List<String> keys = new ArrayList<>(local.keyOrder);
		for (String key : remote.keyOrder) {
			if (!local.rowsByKey.containsKey(key)) {
				keys.add(key);
			}
		}

		for (String key : keys) {
			List<String> localRow = local.rowsByKey.get(key);
			List<String> remoteRow = remote.rowsByKey.get(key);
			if (localRow == null) {
				differences.add("  " + key + "\n  Row only in Google Sheets");
			} else if (remoteRow == null) {
				differences.add("  " + key + "\n  Row only in HEAD-refreshed local copy");
			} else {
				appendChangedLanguageDifference(differences, key, localHeader, localRow, remoteRow);
			}
		}

		if (differences.isEmpty()) {
			System.out.println("No differences found - files are in sync");
			return 0;
		}

		System.out.println("Found " + differences.size() + " keys with differences:\n");
		for (String difference : differences) {
			System.out.println(difference);
			System.out.println();
		}
		System.out.println("\nSummary: " + differences.size() + " keys with differences");
		return DIFFERENCES_FOUND;
	}

	private static List<String> headerOf(List<List<String>> rows) {
		if (rows.isEmpty()) {
			return List.of();
		}
		List<String> header = new ArrayList<>(rows.get(0));
		while (!header.isEmpty() && valueAt(header, header.size() - 1).isEmpty()) {
			header.remove(header.size() - 1);
		}
		return header;
	}

	private static void appendBlankKeyDifference(List<String> differences, String source,
			Map<String, List<Integer>> keyLineNumbers) {
		List<Integer> blankKeyLines = keyLineNumbers.get("");
		if (blankKeyLines != null) {
			differences.add("  Blank column 1\n  " + source + " has empty column-1 cells on line(s): "
					+ joinLineNumbers(blankKeyLines));
		}
	}

	private static void appendChangedLanguageDifference(List<String> differences, String key, List<String> header,
			List<String> localRow, List<String> remoteRow) {
		List<String> changedLanguages = new ArrayList<>();
		for (int column = 1; column < Math.max(localRow.size(), remoteRow.size()); column++) {
			String localValue = valueAt(localRow, column);
			String remoteValue = valueAt(remoteRow, column);
			if (!localValue.equals(remoteValue)) {
				String language = column < header.size() ? valueAt(header, column) : "col" + column;
				changedLanguages.add("  Language: " + language + "\n    Local: " + abbreviate(localValue)
						+ "\n    Remote: " + abbreviate(remoteValue));
			}
		}
		if (!changedLanguages.isEmpty()) {
			differences.add("  " + key + "\n" + String.join("\n", changedLanguages));
		}
	}

	private static String valueAt(List<String> row, int column) {
		if (column >= row.size() || row.get(column) == null) {
			return "";
		}
		return row.get(column);
	}

	private static String removeBom(String value) {
		return value != null && value.startsWith("\uFEFF") ? value.substring(1) : value;
	}

	private static String abbreviate(String value) {
		return value.length() > 50 ? value.substring(0, 50) + "..." : value;
	}

	private static String joinLineNumbers(List<Integer> lines) {
		List<String> values = new ArrayList<>();
		for (Integer line : lines) {
			values.add(line.toString());
		}
		return String.join(", ", values);
	}

	private static final class TranslationCsv {
		private final Map<String, List<String>> rowsByKey = new HashMap<>();
		private final List<String> keyOrder = new ArrayList<>();
		private final Map<String, List<Integer>> keyLineNumbers = new HashMap<>();

		private static TranslationCsv from(List<List<String>> rows) {
			TranslationCsv translationCsv = new TranslationCsv();
			for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
				List<String> row = rows.get(rowIndex);
				if (row.isEmpty()) {
					continue;
				}
				String key = valueAt(row, 0);
				translationCsv.keyLineNumbers.computeIfAbsent(key, ignored -> new ArrayList<>()).add(rowIndex + 1);
				if (!key.isEmpty()) {
					translationCsv.rowsByKey.put(key, row);
					translationCsv.keyOrder.add(key);
				}
			}
			return translationCsv;
		}
	}
}