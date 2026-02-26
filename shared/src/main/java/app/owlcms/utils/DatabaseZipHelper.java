/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;

/**
 * Utility class to compress competition database JSON for WebSocket transmission.
 * Creates a ZIP archive containing competition.json and provides as byte array.
 * Option C compression: reduces 70-80% of database payload size.
 */
public class DatabaseZipHelper {

	final static Logger logger = (Logger) LoggerFactory.getLogger(DatabaseZipHelper.class);
	static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Create a ZIP archive containing the competition database JSON.
	 * Wraps the provided payload in a ZIP file for efficient transmission.
	 *
	 * @param databasePayload the competition database object to compress
	 * @return byte array containing the zipped database, or empty array on error
	 */
	public static byte[] createDatabaseZipBytes(Object databasePayload) {
		if (databasePayload == null) {
			logger./**/warn("[DatabaseZipHelper] Payload is null, returning empty ZIP");
			return new byte[0];
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
			// Convert payload to JSON string
			String jsonString = objectMapper.writeValueAsString(databasePayload);
			byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

			// Create ZIP entry for competition.json
			ZipEntry entry = new ZipEntry("competition.json");
			zipOut.putNextEntry(entry);
			zipOut.write(jsonBytes);
			zipOut.closeEntry();

			zipOut.finish();
			zipOut.flush();

			byte[] result = baos.toByteArray();
			double ratio = 100.0 * (1.0 - (double) result.length / jsonBytes.length);
			logger.info("[DatabaseZipHelper] Created database ZIP: {} bytes (from {} bytes, {:.1f}% reduction)",
					result.length, jsonBytes.length, ratio);
			return result;

		} catch (IOException e) {
			logger.error("[DatabaseZipHelper] Failed to create database ZIP: {}", LoggerUtils.exceptionMessage(e));
			return new byte[0];
		}
	}

	/**
	 * Create a ZIP archive with metadata only (empty database message).
	 * Used in Option C to signal that binary database will follow.
	 *
	 * @param metadata object containing minimal database metadata (competition name, FOPs, etc.)
	 * @return byte array containing the zipped metadata, or empty array on error
	 */
	public static byte[] createMetadataZipBytes(Object metadata) {
		if (metadata == null) {
			logger./**/warn("[DatabaseZipHelper] Metadata is null, returning empty ZIP");
			return new byte[0];
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
			// Convert metadata to JSON string
			String jsonString = objectMapper.writeValueAsString(metadata);
			byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

			// Create ZIP entry for metadata.json
			ZipEntry entry = new ZipEntry("metadata.json");
			zipOut.putNextEntry(entry);
			zipOut.write(jsonBytes);
			zipOut.closeEntry();

			zipOut.finish();
			zipOut.flush();

			byte[] result = baos.toByteArray();
			logger.info("[DatabaseZipHelper] Created metadata ZIP: {} bytes", result.length);
			return result;

		} catch (IOException e) {
			logger.error("[DatabaseZipHelper] Failed to create metadata ZIP: {}", LoggerUtils.exceptionMessage(e));
			return new byte[0];
		}
	}
}
