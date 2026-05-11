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
		try {
			// Ensure stream supports mark/reset
			if (!inputStream.markSupported()) {
				inputStream = new BufferedInputStream(inputStream);
			}
			
			// Mark position - we'll read a small portion to detect format, then reset
			// 4KB should be more than enough to see formatVersion/sessions/groups fields
			inputStream.mark(4096);
			
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			
			// Create a streaming parser with auto-close disabled so it won't close our stream
			com.fasterxml.jackson.core.JsonParser parser = mapper.getFactory()
				.createParser(inputStream)
				.disable(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE);
			
			String version = "1.0"; // default
			boolean hasFormatVersion = false;
			boolean hasSessions = false;
			boolean hasGroups = false;
			
			// Read just the root level field names
			if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
				while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT) {
					String fieldName = parser.currentName();
					if ("formatVersion".equals(fieldName)) {
						parser.nextToken();
						version = parser.getText();
						hasFormatVersion = true;
						break; // Found explicit version, no need to check further
					} else if ("sessions".equals(fieldName)) {
						hasSessions = true;
					} else if ("groups".equals(fieldName)) {
						hasGroups = true;
					}
					parser.skipChildren(); // Skip the field value
					
					// If we found enough info to determine version, stop
					if (hasSessions || hasGroups) {
						break;
					}
				}
			}
			parser.close();
			
			// Determine version if not explicitly set
			if (!hasFormatVersion) {
				if (hasSessions && !hasGroups) {
					version = "2.0";
				}
			}
			
			logger.info("Detected JSON format version: {}", version);
			
			// Reset stream to beginning for full import
			inputStream.reset();
			
			if ("2.0".equals(version)) {
				new CompetitionDataV2().restore(inputStream);
			} else {
				new CompetitionData().restore(inputStream);
			}
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
			throw e;
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
			logger.debug(LoggerUtils.whereFrom() + " Error detecting format version, defaulting to V1: {}", e.getMessage());
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
			logger.debug(LoggerUtils.whereFrom() + " Error detecting format version from string: {}", e.getMessage());
			return "1.0";
		}
	}
	
}
