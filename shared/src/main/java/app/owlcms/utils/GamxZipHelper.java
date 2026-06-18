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
 * Utility class to zip and serialize the GAMX parameter directory for WebSocket transmission.
 * Creates a ZIP archive of the {@code gamx} resource directory (the {@code params-*.json} tables)
 * and provides methods for streaming. The bundle is delivered on demand, exactly like flags and
 * logos: OWLCMS holds the canonical files, and a tracker plugin that needs them requests the zip
 * once via the 428 handshake.
 */
public class GamxZipHelper {

	final static Logger logger = (Logger) LoggerFactory.getLogger(GamxZipHelper.class);

	/**
	 * Get the gamx directory path, checking both local overrides and classpath resources.
	 *
	 * @return Path to the gamx directory, or null if not found
	 */
	public static Path getGamxDirectory() {
		try {
			return ResourceWalker.getFileOrResourcePath("gamx");
		} catch (FileNotFoundException e) {
			logger.debug("Gamx directory not found");
			return null;
		}
	}

	/**
	 * Create a ZIP archive of the gamx directory and return as byte array.
	 * This is the most efficient method for WebSocket transmission.
	 *
	 * @return byte array containing the zipped gamx parameters, or empty array if not found
	 */
	public static byte[] createGamxZipBytes() {
		Path gamxPath = getGamxDirectory();
		if (gamxPath == null || !Files.exists(gamxPath)) {
			logger.debug("Gamx directory not found at {}", gamxPath);
			return new byte[0];
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
			int[] fileCount = new int[1];
			fileCount[0] = 0;
			zipDirectory(gamxPath, "", zipOut, fileCount);
			zipOut.finish();
			zipOut.flush();
		} catch (IOException e) {
			logger.error("Failed to create gamx ZIP: {}", LoggerUtils.exceptionMessage(e));
			return new byte[0];
		}

		byte[] result = baos.toByteArray();
		try {
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
			logger.info("Created gamx ZIP archive from {}: {} bytes ({} files)", gamxPath, result.length, counted);
		} catch (Throwable t) {
			logger.info("Created gamx ZIP archive from {}: {} bytes", gamxPath, result.length);
		}
		return result;
	}

	/**
	 * Recursively zip a directory and all its contents.
	 * Uses NIO Files API to support both regular filesystems and in-memory filesystems (JimFS),
	 * as well as jar/zip filesystems for classpath resources.
	 *
	 * @param dirPath the directory to zip
	 * @param parentPath the parent path prefix within the zip
	 * @param zipOut the ZipOutputStream to write to
	 * @param fileCount single-element array used to count files added
	 * @throws IOException if an I/O error occurs
	 */
	private static void zipDirectory(Path dirPath, String parentPath, ZipOutputStream zipOut, int[] fileCount) throws IOException {
		try (var stream = Files.list(dirPath)) {
			for (Path path : stream.toList()) {
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
					entryPath = entryPath + "/";
					ZipEntry zipEntry = new ZipEntry(entryPath);
					zipOut.putNextEntry(zipEntry);
					zipOut.closeEntry();
					zipDirectory(path, entryPath, zipOut, fileCount);
				} else {
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
	 * Check if the gamx directory exists and contains files.
	 * Uses NIO Files API to support both regular filesystems and in-memory filesystems (JimFS).
	 *
	 * @return true if gamx parameters are available, false otherwise
	 */
	public static boolean hasGamxAvailable() {
		Path gamxPath = getGamxDirectory();
		if (gamxPath == null || !Files.exists(gamxPath)) {
			return false;
		}

		try {
			long fileCount;
			try (var stream = Files.list(gamxPath)) {
				fileCount = stream.filter(Files::isRegularFile).count();
			}
			return fileCount > 0;
		} catch (Exception e) {
			logger.debug("Error checking gamx availability: {}", LoggerUtils.exceptionMessage(e));
			return false;
		}
	}
}
