/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import app.owlcms.utils.StartupUtils;

/**
 * Resolves {@code OWLCMS_LOADJSON}, the export (JSON or ZIP) that replaces the competition at startup.
 *
 * The setting names either a file or a folder. For a folder, the most recently modified {@code *.json} or
 * {@code *.zip} directly inside it is used. Other names, such as the temporary files written while a checkpoint is
 * being saved, are never candidates.
 */
public final class StartupImportSource {

	/** A file that can be imported, with what an operator needs to choose between several. */
	public record Candidate(Path path, Instant lastModified, long size) {
	}

	private StartupImportSource() {
	}

	/**
	 * @return the file to import, or empty when {@code OWLCMS_LOADJSON} is unset or blank
	 * @throws IllegalStateException if the setting names something that cannot be imported
	 */
	public static Optional<Path> resolve() {
		return resolve(StartupUtils.getStringParam("loadJson"));
	}

	static Optional<Path> resolve(String setting) {
		if (setting == null || setting.isBlank()) {
			return Optional.empty();
		}
		Path path = Path.of(unquote(setting.trim()));
		if (Files.isDirectory(path)) {
			return Optional.of(latest(path)
			        .orElseThrow(() -> new IllegalStateException("No .json or .zip file found in folder " + path)));
		}
		if (!Files.isRegularFile(path)) {
			throw new IllegalStateException("OWLCMS_LOADJSON does not name an existing file or folder: " + path);
		}
		return Optional.of(path);
	}

	/** @return the candidates directly inside the folder, newest first */
	public static List<Candidate> candidates(Path folder) {
		try (Stream<Path> entries = Files.list(folder)) {
			return entries
			        .filter(StartupImportSource::isCandidate)
			        .map(StartupImportSource::describe)
			        .sorted(Comparator.comparing(Candidate::lastModified).reversed()
			                .thenComparing(candidate -> candidate.path().getFileName().toString(),
			                        Comparator.reverseOrder()))
			        .toList();
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot list folder " + folder, e);
		}
	}

	/** @return the most recently modified candidate in the folder */
	public static Optional<Path> latest(Path folder) {
		return candidates(folder).stream().map(Candidate::path).findFirst();
	}

	static boolean isCandidate(Path path) {
		if (!Files.isRegularFile(path)) {
			return false;
		}
		String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".json") || name.endsWith(".zip");
	}

	private static Candidate describe(Path path) {
		try {
			return new Candidate(path, Files.getLastModifiedTime(path).toInstant(), Files.size(path));
		} catch (IOException e) {
			throw new UncheckedIOException("Cannot read " + path, e);
		}
	}

	/** Paths are often pasted with the quotes added by a file manager. */
	private static String unquote(String value) {
		if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
