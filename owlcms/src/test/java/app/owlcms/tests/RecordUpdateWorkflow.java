/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordDefinitionReader;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.spreadsheet.JXLSExportRecords;

/**
 * Manual integration workflow for rebuilding record files from completed competition results.
 */
public class RecordUpdateWorkflow {

	private static final String EXPORT_TEMPLATE = "/templates/records/dataExchange_groups.xlsx";
	private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter
	        .ofPattern("yyyyMMdd'T'HHmmssxx");
	private static final String DATABASE_PROPERTY = "owlcms.recordUpdate.database";
	private static final String RECORDS_PROPERTY = "owlcms.recordUpdate.records";
	private static final String OUTPUT_PROPERTY = "owlcms.recordUpdate.output";

	private boolean jpaInitialized;
	private boolean jdbcPropertyChanged;
	private String originalJdbcUrl;
	private Path fixtureDirectory;

	@After
	public void tearDownTest() throws Exception {
		Competition.setCurrent(null);
		if (jpaInitialized) {
			JPAService.close();
		}
		if (jdbcPropertyChanged) {
			if (originalJdbcUrl == null) {
				System.clearProperty("JDBC_DATABASE_URL");
			} else {
				System.setProperty("JDBC_DATABASE_URL", originalJdbcUrl);
			}
		}
		deleteFixtureDirectory();
	}

	@Test
	public void rebuildRecordFilesFromJsonDatabase() throws Exception {
		Path database = getConfiguredDatabase();
		assumeTrue("JSON variant requires a .json database", database.toString().toLowerCase().endsWith(".json"));
		initializeEmptyDatabase();

		try (InputStream input = Files.newInputStream(database)) {
			new CompetitionData().restore(input);
		}
		rebuildRecordFiles();
	}

	@Test
	public void rebuildRecordFilesFromH2Database() throws Exception {
		Path database = getConfiguredDatabase();
		assumeTrue("H2 variant requires a .db database", database.toString().toLowerCase().endsWith(".db"));
		initializeFromH2Database(database);
		rebuildRecordFiles();
	}

	private void rebuildRecordFiles() throws Exception {
		Path recordsDirectory = Path.of(System.getProperty(RECORDS_PROPERTY));
		Path outputDirectory = Path.of(System.getProperty(OUTPUT_PROPERTY));
		assertTrue("Records directory does not exist: " + recordsDirectory, Files.isDirectory(recordsDirectory));

		RecordRepository.clearLoadedRecords();
		RecordRepository.clearNewRecords();
		new RecordDefinitionReader().readFolder(recordsDirectory);
		assertFalse("No records were loaded from " + recordsDirectory, RecordRepository.findAllLoadedRecords().isEmpty());
		RecordRepository.acceptProvisionalRecordsWithFilters(null, null, null, null, null, "PROVISIONAL", "HISTORY");

		RecordRepository.recomputeNewRecords();
		Files.createDirectories(outputDirectory);
		String timestamp = FILE_TIMESTAMP.format(ZonedDateTime.now());
		exportByFederation(outputDirectory,
		        RecordRepository.findWithFilters(null, null, null, null, null, "ALL", "CURRENT", null), timestamp);
	}

	private Path getConfiguredDatabase() {
		String databaseValue = System.getProperty(DATABASE_PROPERTY);
		assumeTrue("Set -D" + DATABASE_PROPERTY + ", -D" + RECORDS_PROPERTY + " and -D" + OUTPUT_PROPERTY
		        + " to run the record update workflow",
		        databaseValue != null
		                && System.getProperty(RECORDS_PROPERTY) != null
		                && System.getProperty(OUTPUT_PROPERTY) != null);
		Path database = Path.of(databaseValue);
		assertTrue("Database does not exist: " + database, Files.isRegularFile(database));
		return database;
	}

	private void initializeEmptyDatabase() {
		initializeApplication("jdbc:h2:mem:recordUpdateJson-" + UUID.randomUUID()
		        + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0", true);
	}

	private void initializeFromH2Database(Path database) throws Exception {
		Main.injectSuppliers();
		JPAService.close();
		Competition.setCurrent(null);
		String memoryJdbcUrl = "jdbc:h2:mem:recordUpdateH2-" + UUID.randomUUID()
		        + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0";
		setJdbcUrl(memoryJdbcUrl);

		fixtureDirectory = Files.createTempDirectory("record-update-db-");
		Path copiedDatabase = fixtureDirectory.resolve("source.mv.db");
		Path scriptFile = fixtureDirectory.resolve("source.sql");
		Files.copy(database, copiedDatabase, StandardCopyOption.REPLACE_EXISTING);

		String sourceBase = copiedDatabase.toAbsolutePath().toString().replaceAll("\\.mv\\.db$", "");
		String sourceUrl = "jdbc:h2:file:" + sourceBase + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0";
		String escapedScriptFile = escapePath(scriptFile);
		try (Connection source = DriverManager.getConnection(sourceUrl, "sa", "");
		        Statement sourceStatement = source.createStatement()) {
			sourceStatement.execute("SCRIPT TO '" + escapedScriptFile + "'");
		}
		try (Connection target = DriverManager.getConnection(memoryJdbcUrl, "sa", "");
		        Statement targetStatement = target.createStatement()) {
			targetStatement.execute("RUNSCRIPT FROM '" + escapedScriptFile + "'");
		}

		JPAService.init(true, false);
		jpaInitialized = true;
		Config.setCurrent(ConfigRepository.findAll().get(0));
		Config.initConfig();
	}

	private void initializeApplication(String jdbcUrl, boolean reset) {
		Main.injectSuppliers();
		JPAService.close();
		Competition.setCurrent(null);
		setJdbcUrl(jdbcUrl);
		JPAService.init(true, reset);
		jpaInitialized = true;
		Config.initConfig();
	}

	private void setJdbcUrl(String jdbcUrl) {
		originalJdbcUrl = System.getProperty("JDBC_DATABASE_URL");
		System.setProperty("JDBC_DATABASE_URL", jdbcUrl);
		jdbcPropertyChanged = true;
	}

	private static String escapePath(Path path) {
		return path.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "''");
	}

	private void deleteFixtureDirectory() throws Exception {
		if (fixtureDirectory == null || !Files.exists(fixtureDirectory)) {
			return;
		}
		try (var paths = Files.walk(fixtureDirectory)) {
			for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	private static void exportByFederation(Path outputDirectory, List<RecordEvent> records, String timestamp) throws Exception {
		Map<String, List<RecordEvent>> recordsByFederation = records.stream()
		        .collect(Collectors.groupingBy(RecordEvent::getRecordFederation, TreeMap::new, Collectors.toList()));

		for (Map.Entry<String, List<RecordEvent>> entry : recordsByFederation.entrySet()) {
			String federation = entry.getKey().replaceAll("[^A-Za-z0-9._-]", "_");
			Path outputFile = outputDirectory.resolve(federation + "_Records_" + timestamp + ".xlsx");
			JXLSExportRecords export = new JXLSExportRecords(null, entry.getValue());
			try (InputStream template = RecordUpdateWorkflow.class.getResourceAsStream(EXPORT_TEMPLATE)) {
				assertTrue("Record export template not found: " + EXPORT_TEMPLATE, template != null);
				export.setInputStream(template);
				try (InputStream input = export.createInputStream()) {
					Files.copy(input, outputFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
			assertTrue("Record export is empty: " + outputFile, Files.size(outputFile) > 0);
		}
	}
}