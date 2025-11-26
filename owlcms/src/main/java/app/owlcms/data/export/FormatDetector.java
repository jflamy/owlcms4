/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import java.io.BufferedInputStream;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Utility class to detect the format version of a JSON export
 * and route to the appropriate importer.
 */
public class FormatDetector {

	final static Logger logger = (Logger) LoggerFactory.getLogger(FormatDetector.class);

	/**
	 * Detect the format version and import the data using the appropriate importer.
	 * 
	 * @param inputStream The input stream containing the JSON data
	 * @throws Exception if import fails
	 */
	public static void importData(InputStream inputStream) throws Exception {
		// Mark the stream so we can reset it after detection
		BufferedInputStream bis = new BufferedInputStream(inputStream);
		bis.mark(8192); // Mark up to 8KB for detection
		
		try {
			String version = detectVersion(bis);
			logger.info("Detected JSON format version: {}", version);
			
			// Reset stream to beginning
			bis.reset();
			
			if ("2.0".equals(version)) {
				// Use V2 importer
				CompetitionDataV2 dataV2 = new CompetitionDataV2();
				dataV2.restore(bis);
			} else {
				// Use default (V1) importer
				CompetitionData dataV1 = new CompetitionData();
				dataV1.restore(bis);
			}
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
			throw e;
		} finally {
			bis.close();
		}
	}

	/**
	 * Detect the format version by examining the JSON structure.
	 * 
	 * @param inputStream The input stream to examine (should be buffered and marked)
	 * @return The version string ("1.0" for legacy, "2.0" for new format)
	 */
	public static String detectVersion(InputStream inputStream) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			
			// Read the JSON as a tree to examine structure
			JsonNode root = mapper.readTree(inputStream);
			
			// Check if formatVersion field exists
			if (root.has("formatVersion")) {
				String version = root.get("formatVersion").asText();
				logger.debug("Found formatVersion field: {}", version);
				return version;
			}
			
			// Check for V2-specific fields
			if (root.has("sessions")) {
				logger.debug("Found 'sessions' field, assuming V2 format");
				return "2.0";
			}
			
			// Check for V1-specific fields
			if (root.has("groups")) {
				logger.debug("Found 'groups' field, assuming V1 format");
				return "1.0";
			}
			
			// Default to V1
			logger.debug("No version indicators found, defaulting to V1 format");
			return "1.0";
			
		} catch (Exception e) {
			logger.warn(LoggerUtils.whereFrom() + " Error detecting format version, defaulting to V1: {}", e.getMessage());
			return "1.0";
		}
	}

	/**
	 * Convenience method to detect version from a string.
	 * 
	 * @param jsonString The JSON string to examine
	 * @return The version string ("1.0" for legacy, "2.0" for new format)
	 */
	public static String detectVersionFromString(String jsonString) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			JsonNode root = mapper.readTree(jsonString);
			
			if (root.has("formatVersion")) {
				return root.get("formatVersion").asText();
			}
			
			if (root.has("sessions")) {
				return "2.0";
			}
			
			if (root.has("groups")) {
				return "1.0";
			}
			
			return "1.0";
		} catch (Exception e) {
			logger.warn(LoggerUtils.whereFrom() + " Error detecting format version from string: {}", e.getMessage());
			return "1.0";
		}
	}
}
