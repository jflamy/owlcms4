/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
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
			logger.debug("Flags directory not found");
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
			logger.debug("Flags directory not found at {}", flagsPath);
			return new byte[0];
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
			// Count files while zipping so we can log how many were included
			int[] fileCount = new int[1];
			fileCount[0] = 0;
			zipDirectory(flagsPath, "", zipOut, fileCount);
			zipOut.finish();
			zipOut.flush();
		} catch (IOException e) {
			logger.error("Failed to create flags ZIP: {}", LoggerUtils.exceptionMessage(e));
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
			logger.info("Created flags ZIP archive from {}: {} bytes ({} files)", flagsPath, result.length, counted);
		} catch (Throwable t) {
			logger.info("Created flags ZIP archive from {}: {} bytes", flagsPath, result.length);
		}
		return result;
	}

	/**
	 * Recursively zip a directory and all its contents.
	 * Uses NIO Files API to support both regular filesystems and in-memory filesystems (JimFS).
	 * 
	 * @param dirPath the directory to zip
	 * @param parentPath the parent path prefix within the zip (e.g., "flags/")
	 * @param zipOut the ZipOutputStream to write to
	 * @throws IOException if an I/O error occurs
	 */
	private static void zipDirectory(Path dirPath, String parentPath, ZipOutputStream zipOut, int[] fileCount) throws IOException {
		// Use Files.list() to support JimFS and other non-default filesystems
		try (var stream = Files.list(dirPath)) {
			for (Path path : stream.toList()) {
				// Skip hidden files
				try {
					if (Files.isHidden(path)) {
						continue;
					}
				} catch (IOException e) {
					// Some filesystems don't support isHidden, continue anyway
				}

				String fileName = path.getFileName().toString();
				String entryPath = parentPath + fileName;
				
				if (Files.isDirectory(path)) {
					// For directories, add an entry with trailing slash
					entryPath = entryPath + "/";
					ZipEntry zipEntry = new ZipEntry(entryPath);
					zipOut.putNextEntry(zipEntry);
					zipOut.closeEntry();
					// Recursively zip subdirectory contents
					zipDirectory(path, entryPath, zipOut, fileCount);
				} else {
					// For files, read bytes using NIO and write to zip
					ZipEntry zipEntry = new ZipEntry(entryPath);
					zipOut.putNextEntry(zipEntry);
					Files.copy(path, zipOut);
					zipOut.closeEntry();
					if (fileCount != null) {
						fileCount[0]++;
					}
				}
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
		if (flagsPath == null) {
			logger.warn("hasFlagsAvailable: getFlagsDirectory() returned null");
			return false;
		}
		if (!Files.exists(flagsPath)) {
			logger.warn("hasFlagsAvailable: flags path does not exist: {}", flagsPath);
			return false;
		}
		
		try {
			// Use Files.list() with try-with-resources to properly close the stream
			// This is important for ZIP filesystems which require explicit close
			long fileCount;
			try (var stream = Files.list(flagsPath)) {
				fileCount = stream.filter(Files::isRegularFile).count();
			}
			boolean available = fileCount > 0;
			if (!available) {
				logger.warn("hasFlagsAvailable: flags directory exists but is empty: {} (filesystem: {})", 
						flagsPath, flagsPath.getFileSystem().getClass().getSimpleName());
			} else {
				logger.debug("hasFlagsAvailable: found {} flag files in {}", fileCount, flagsPath);
			}
			return available;
		} catch (Exception e) {
			logger.warn("Error checking flags availability in {}: {} - {}", 
					flagsPath, e.getClass().getSimpleName(), LoggerUtils.exceptionMessage(e));
			return false;
		}
	}
}
