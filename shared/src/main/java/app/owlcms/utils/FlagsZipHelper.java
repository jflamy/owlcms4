/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipOutputStream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Utility class to efficiently zip and serialize the flags directory for WebSocket transmission.
 * Creates a ZIP archive of local/flags and provides methods for streaming and base64 encoding.
 */
public class FlagsZipHelper {

	final static Logger logger = (Logger) LoggerFactory.getLogger(FlagsZipHelper.class);

	/**
	 * Get the flags directory path, checking both local overrides and classpath resources.
	 * 
	 * @return Path to the flags directory, or null if not found
	 */
	public static Path getFlagsDirectory() {
		try {
			return ResourceWalker.getFileOrResourcePath("flags");
		} catch (FileNotFoundException e) {
			logger.warn("Flags directory not found");
			return null;
		}
	}

	/**
	 * Create a ZIP archive of the flags directory and return as byte array.
	 * This is the most efficient method for WebSocket transmission.
	 * 
	 * @return byte array containing the zipped flags, or empty array if flags not found
	 */
	public static byte[] createFlagsZipBytes() {
		Path flagsPath = getFlagsDirectory();
		if (flagsPath == null || !Files.exists(flagsPath)) {
			logger.warn("Flags directory not found at {}", flagsPath);
			return new byte[0];
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
			zipDirectory(flagsPath, "", zipOut);
			zipOut.finish();
			zipOut.flush();
		} catch (IOException e) {
			logger.error("Failed to create flags ZIP: {}", LoggerUtils.exceptionMessage(e));
			return new byte[0];
		}

		byte[] result = baos.toByteArray();
		logger.info("Created flags ZIP archive: {} bytes", result.length);
		return result;
	}

	/**
	 * Recursively zip a directory and all its contents.
	 * 
	 * @param dirPath the directory to zip
	 * @param parentPath the parent path prefix within the zip (e.g., "flags/")
	 * @param zipOut the ZipOutputStream to write to
	 * @throws IOException if an I/O error occurs
	 */
	private static void zipDirectory(Path dirPath, String parentPath, ZipOutputStream zipOut) throws IOException {
		File dir = dirPath.toFile();
		File[] files = dir.listFiles();
		
		if (files == null) {
			logger.warn("Cannot list files in directory: {}", dirPath);
			return;
		}

		for (File file : files) {
			// Skip hidden files
			if (file.isHidden()) {
				continue;
			}

			String entryPath = parentPath + file.getName();
			
			if (file.isDirectory()) {
				// For directories, add an entry with trailing slash
				entryPath = entryPath + "/";
				ZipUtils.zipFile(file, entryPath.substring(0, entryPath.length() - 1), zipOut);
				// Recursively zip subdirectory contents
				zipDirectory(file.toPath(), entryPath, zipOut);
			} else {
				// For files, use ZipUtils method
				ZipUtils.zipFile(file, entryPath, zipOut);
			}
		}
	}

	/**
	 * Check if the flags directory exists and contains files.
	 * 
	 * @return true if flags are available, false otherwise
	 */
	public static boolean hasFlagsAvailable() {
		Path flagsPath = getFlagsDirectory();
		if (flagsPath == null || !Files.exists(flagsPath)) {
			return false;
		}
		
		try {
			// Check if directory has at least one file (not counting hidden files)
			File[] files = flagsPath.toFile().listFiles(File::isFile);
			return files != null && files.length > 0;
		} catch (Exception e) {
			logger.debug("Error checking flags availability: {}", LoggerUtils.exceptionMessage(e));
			return false;
		}
	}
}
