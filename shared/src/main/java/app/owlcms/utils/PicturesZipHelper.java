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
 * Utility class to efficiently zip and serialize the pictures directory for WebSocket transmission.
 * Creates a ZIP archive of the pictures directory and provides methods for streaming and encoding.
 */
public class PicturesZipHelper {

	final static Logger logger = (Logger) LoggerFactory.getLogger(PicturesZipHelper.class);

	/**
	 * Get the pictures directory path, checking both local overrides and classpath resources.
	 * 
	 * @return Path to the pictures directory, or null if not found
	 */
	public static Path getPicturesDirectory() {
		try {
			return ResourceWalker.getFileOrResourcePath("pictures");
		} catch (FileNotFoundException e) {
			logger.debug("Pictures directory not found");
			return null;
		}
	}

	/**
	 * Check if pictures are available.
	 * 
	 * @return true if pictures directory exists and is not empty, false otherwise
	 */
	public static boolean hasPicturesAvailable() {
		Path picturesPath = getPicturesDirectory();
		if (picturesPath == null || !Files.exists(picturesPath)) {
			return false;
		}
		try {
			return Files.list(picturesPath).findAny().isPresent();
		} catch (IOException e) {
			logger.debug("Error checking for pictures: {}", LoggerUtils.exceptionMessage(e));
			return false;
		}
	}

	/**
	 * Create a ZIP archive of the pictures directory and return as byte array.
	 * This is the most efficient method for WebSocket transmission.
	 * 
	 * @return byte array containing the zipped pictures, or empty array if pictures not found
	 */
	public static byte[] createPicturesZipBytes() {
		Path picturesPath = getPicturesDirectory();
		if (picturesPath == null || !Files.exists(picturesPath)) {
			logger.debug("Pictures directory not found at {}", picturesPath);
			return new byte[0];
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
			zipDirectory(picturesPath, "", zipOut);
			zipOut.finish();
			zipOut.flush();
		} catch (IOException e) {
			logger.error("Failed to create pictures ZIP: {}", LoggerUtils.exceptionMessage(e));
			return new byte[0];
		}

		byte[] result = baos.toByteArray();
		logger.info("Created pictures ZIP archive: {} bytes", result.length);
		return result;
	}

	/**
	 * Recursively zip a directory and all its contents.
	 * 
	 * @param dirPath the directory to zip
	 * @param parentPath the parent path prefix within the zip (e.g., "pictures/")
	 * @param zipOut the ZipOutputStream to write to
	 * @throws IOException if an I/O error occurs
	 */
	private static void zipDirectory(Path dirPath, String parentPath, ZipOutputStream zipOut) throws IOException {
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
				zipDirectory(file.toPath(), entryPath, zipOut);
			} else {
				// For files, just add the file
				ZipUtils.zipFile(file, entryPath, zipOut);
			}
		}
	}

}
