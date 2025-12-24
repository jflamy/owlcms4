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
 * Utility class to efficiently zip and serialize the logos directory for WebSocket transmission.
 * Creates a ZIP archive of local/logos and provides methods for streaming and base64 encoding.
 */
public class LogosZipHelper {

	final static Logger logger = (Logger) LoggerFactory.getLogger(LogosZipHelper.class);

	/**
	 * Get the logos directory path, checking both local overrides and classpath resources.
	 * 
	 * @return Path to the logos directory, or null if not found
	 */
	public static Path getLogosDirectory() {
		try {
			return ResourceWalker.getFileOrResourcePath("logos");
		} catch (FileNotFoundException e) {
			logger.debug("Logos directory not found");
			return null;
		}
	}

	/**
	 * Create a ZIP archive of the logos directory and return as byte array.
	 * This is the most efficient method for WebSocket transmission.
	 * 
	 * @return byte array containing the zipped logos, or empty array if logos not found
	 */
	public static byte[] createLogosZipBytes() {
		Path logosPath = getLogosDirectory();
		if (logosPath == null || !Files.exists(logosPath)) {
			logger.debug("Logos directory not found at {}", logosPath);
			return new byte[0];
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
			// Count files while zipping so we can log how many were included
			int[] fileCount = new int[1];
			fileCount[0] = 0;
			zipDirectory(logosPath, "", zipOut, fileCount);
			zipOut.finish();
			zipOut.flush();
		} catch (IOException e) {
			logger.error("Failed to create logos ZIP: {}", LoggerUtils.exceptionMessage(e));
			return new byte[0];
		}

		byte[] result = baos.toByteArray();
		try {
			// Count files in the created ZIP by scanning entries (robust and avoids re-walking filesystem)
			java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(result);
			java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(bais);
			int counted = 0;
			java.util.zip.ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null) {
				String name = ze.getName();
				if (name != null && !name.endsWith("/")) {
					counted++;
				}
				zis.closeEntry();
			}
			zis.close();
			logger.info("Created logos ZIP archive: {} bytes ({} files)", result.length, counted);
		} catch (Throwable t) {
			logger.info("Created logos ZIP archive: {} bytes", result.length);
		}
		return result;
	}

	/**
	 * Recursively zip a directory and all its contents.
	 * 
	 * @param dirPath the directory to zip
	 * @param parentPath the parent path prefix within the zip (e.g., "logos/")
	 * @param zipOut the ZipOutputStream to write to
	 * @throws IOException if an I/O error occurs
	 */
	private static void zipDirectory(Path dirPath, String parentPath, ZipOutputStream zipOut, int[] fileCount) throws IOException {
		File dir = dirPath.toFile();
		File[] files = dir.listFiles();
		
		if (files == null) {
			logger.debug("Cannot list files in directory: {}", dirPath);
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
				zipDirectory(file.toPath(), entryPath, zipOut, fileCount);
			} else {
				// For files, use ZipUtils method
				ZipUtils.zipFile(file, entryPath, zipOut);
				if (fileCount != null) {
					fileCount[0]++;
				}
			}
		}
	}

	/**
	 * Check if the logos directory exists and contains files.
	 * 
	 * @return true if logos are available, false otherwise
	 */
	public static boolean hasLogosAvailable() {
		Path logosPath = getLogosDirectory();
		if (logosPath == null || !Files.exists(logosPath)) {
			return false;
		}
		
		try {
			// Check if directory has at least one file (not counting hidden files)
			File[] files = logosPath.toFile().listFiles(File::isFile);
			return files != null && files.length > 0;
		} catch (Exception e) {
			logger.debug("Error checking logos availability: {}", LoggerUtils.exceptionMessage(e));
			return false;
		}
	}
}
