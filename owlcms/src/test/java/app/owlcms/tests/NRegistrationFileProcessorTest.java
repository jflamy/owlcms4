/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.data.platform.Platform;
import app.owlcms.spreadsheet.JXLSRegistrationEmptyExport;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;
import app.owlcms.spreadsheet.RCompetition;
import app.owlcms.spreadsheet.RGroup;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class NRegistrationFileProcessorTest {

    final static Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileProcessorTest.class);

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);  // true, true = in-memory mode, drop and create schema
        Config.initConfig();
        ProdData.insertInitialData(0);
        // Disable MQTT to avoid startup issues in tests
        System.setProperty("enableEmbeddedMqtt", "false");
        // Initialize the default Field of Play which is needed for platform assignment
        app.owlcms.init.OwlcmsFactory.initDefaultFOP();
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    @Before
    public void resetDatabase() {
        // Clear all data before each test
        JPAService.close();
        JPAService.init(true, true);  // Drop and recreate schema
        Config.initConfig();
        ProdData.insertInitialData(0);
        // Initialize the default Field of Play which is needed for platform assignment
        app.owlcms.init.OwlcmsFactory.initDefaultFOP();
    }

    public NRegistrationFileProcessorTest() {
        logger.setLevel(Level.INFO);
    }

    @Test
    public void testGroupCopyFromCopiesStoredPropertiesAndPreservesCompetitionState() {
        Group source = new Group("source");
        source.setAnnouncer("announcer");
        source.setCleanJerkBreakDuration(7);
        source.setCompetitionDirector("director");
        source.setCompetitionSecretary("secretary");
        source.setCompetitionSecretary2("secretary2");
        source.setCompetitionTime(LocalDateTime.of(2026, 9, 11, 10, 0));
        source.setDescription("description");
        source.setDoctor("doctor");
        source.setDoctor2("doctor2");
        source.setDoctor3("doctor3");
        source.setJury1("jury1");
        source.setJury2("jury2");
        source.setJury3("jury3");
        source.setJury4("jury4");
        source.setJury5("jury5");
        source.setMarshall("marshall");
        source.setMarshal2("marshal2");
        source.setMasters(true);
        source.setPlatform(new Platform("sourcePlatform"));
        source.setReferee1("referee1");
        source.setReferee2("referee2");
        source.setReferee3("referee3");
        source.setReserve("reserve");
        source.setReserveJury("reserveJury");
        source.setTechnicalController("technicalController");
        source.setTechnicalController2("technicalController2");
        source.setTechnicalController3("technicalController3");
        source.setTimeKeeper("timeKeeper");
        source.setTis1("tis1");
        source.setTis2("tis2");
        source.setWeighIn1("weighIn1");
        source.setWeighIn2("weighIn2");
        source.setWeighInTime(LocalDateTime.of(2026, 9, 11, 8, 0));

        Group destination = new Group("destination");
        Long destinationId = destination.getId();
        LocalDateTime firstSnatchTime = LocalDateTime.of(2026, 9, 11, 10, 5);
        LocalDateTime firstCJTime = LocalDateTime.of(2026, 9, 11, 11, 5);
        LocalDateTime lastSnatchTime = LocalDateTime.of(2026, 9, 11, 10, 55);
        LocalDateTime lastCJTime = LocalDateTime.of(2026, 9, 11, 11, 55);
        destination.setDone(true);
        destination.setFirstSnatchTime(firstSnatchTime, null);
        destination.setFirstCJTime(firstCJTime, null);
        destination.setLastSnatchDecisionTime(lastSnatchTime, destination, null);
        destination.setLastCJDecisionTime(lastCJTime, destination, null);

        destination.copyFrom(source);

        assertEquals(source.getName(), destination.getName());
        assertEquals(source.getDescription(), destination.getDescription());
        assertEquals(source.getPlatform(), destination.getPlatform());
        assertEquals(source.getCleanJerkBreakDuration(), destination.getCleanJerkBreakDuration());
        assertEquals(source.getCompetitionDirector(), destination.getCompetitionDirector());
        assertEquals(source.getTechnicalController3(), destination.getTechnicalController3());
        assertEquals(source.getTis1(), destination.getTis1());
        assertEquals(source.getTis2(), destination.getTis2());
        assertEquals(destinationId, destination.getId());
        assertEquals(true, destination.isDone());
        assertEquals(firstSnatchTime, destination.getFirstSnatchTime());
        assertEquals(firstCJTime, destination.getFirstCJTime());
        assertEquals(lastSnatchTime, destination.getLastSnatchDecisionTime());
        assertEquals(lastCJTime, destination.getLastCJDecisionTime());
    }

    @Test
    public void testSessionImportSupportsAdditionalSessionFields() throws Exception {
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sessions");
            Row header = sheet.createRow(1);
            String[] headers = { "sessionName", "technicalController3", "competitionDirector", "tis1", "tis2",
                    "cleanJerkBreakDuration" };
            for (int column = 0; column < headers.length; column++) {
                header.createCell(column).setCellValue(headers[column]);
            }
            Row values = sheet.createRow(2);
            values.createCell(0).setCellValue("new session");
            values.createCell(1).setCellValue("controller 3");
            values.createCell(2).setCellValue("director");
            values.createCell(3).setCellValue("technology 1");
            values.createCell(4).setCellValue("technology 2");
            values.createCell(5).setCellValue(7);
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, Locale.ENGLISH);
        processor.setSessionOptions(NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS);
        int processed = processor.doProcessGroups(new ByteArrayInputStream(workbookBytes), false,
                message -> logger.info("Import: {}", message), () -> logger.info("Import complete"));

        assertEquals(1, processed);
        Group imported = GroupRepository.findByName("new session");
        assertNotNull(imported);
        assertEquals("controller 3", imported.getTechnicalController3());
        assertEquals("director", imported.getCompetitionDirector());
        assertEquals("technology 1", imported.getTis1());
        assertEquals("technology 2", imported.getTis2());
        assertEquals(Integer.valueOf(7), imported.getCleanJerkBreakDuration());
    }

    @Test
    public void testOfficialsOnlyUpdateCopiesAdditionalAssignmentsButPreservesBreakDuration() {
        Group existing = new Group("existing session");
        existing.setCleanJerkBreakDuration(12);
        GroupRepository.add(existing);

        RGroup incoming = new RGroup();
        incoming.setGroupName("existing session");
        incoming.setTechController3("controller 3");
        incoming.setCompetitionDirector("director");
        incoming.setTis1("technology 1");
        incoming.setTis2("technology 2");
        incoming.setCleanJerkBreakDuration("7");

        NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, Locale.ENGLISH);
        processor.applySafeSessionUpdates(List.of(incoming));

        Group updated = GroupRepository.findByName("existing session");
        assertEquals("controller 3", updated.getTechnicalController3());
        assertEquals("director", updated.getCompetitionDirector());
        assertEquals("technology 1", updated.getTis1());
        assertEquals("technology 2", updated.getTis2());
        assertEquals(Integer.valueOf(12), updated.getCleanJerkBreakDuration());
    }

    @Test
    public void testRegistrationExportIncludesAdditionalSessionFields() throws Exception {
        Group group = GroupRepository.findAll().get(0);
        group.setTechnicalController3("controller 3");
        group.setCompetitionDirector("director");
        group.setTis1("technology 1");
        group.setTis2("technology 2");
        group.setCleanJerkBreakDuration(7);
        GroupRepository.save(group);

        byte[] bytes;
        JXLSRegistrationEmptyExport export = new JXLSRegistrationEmptyExport();
        try (InputStream input = export.createInputStream()) {
            bytes = input.readAllBytes();
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sessions = workbook.getSheet("Sessions");
            assertNotNull(sessions);
            DataFormatter formatter = new DataFormatter(Locale.ENGLISH);
            Row header = sessions.getRow(1);
            Map<String, Integer> columns = new java.util.LinkedHashMap<>();
            for (int column = 0; column < header.getLastCellNum(); column++) {
                columns.put(formatter.formatCellValue(header.getCell(column)), column);
            }
            Row exported = null;
            for (int row = 2; row <= sessions.getLastRowNum(); row++) {
                Row candidate = sessions.getRow(row);
                if (candidate != null && group.getName().equals(formatter.formatCellValue(candidate.getCell(0)))) {
                    exported = candidate;
                    break;
                }
            }
            assertNotNull(exported);
            assertEquals("controller 3", exportedValue(exported, columns, "Technical Controller 3", formatter));
            assertEquals("director", exportedValue(exported, columns, "Competition Director", formatter));
            assertEquals("technology 1", exportedValue(exported, columns, "Technology Support 1", formatter));
            assertEquals("technology 2", exportedValue(exported, columns, "Technology Support 2", formatter));
            assertEquals("7", exportedValue(exported, columns, "Time before Clean & Jerk", formatter));
        }
    }

    private String exportedValue(Row row, Map<String, Integer> columns, String header, DataFormatter formatter) {
        Integer column = columns.get(header);
        assertNotNull("Sessions header should contain " + header + "; found " + columns.keySet(), column);
        return formatter.formatCellValue(row.getCell(column));
    }

    @Test
    public void testUpdateExistingAthletePreservesFederationCodesAndTeamMembershipData() throws Exception {
        Athlete existingAthlete = new Athlete();
        existingAthlete.setId(42L);
        existingAthlete.setFederationCodes("OLD");

        Athlete incomingAthlete = new Athlete();
        incomingAthlete.setFederationCodes("NEW");

        Athlete.conditionalCopy(existingAthlete, incomingAthlete, false, false, false);

        assertEquals("Federation codes should be updated during metadata-only refresh", "NEW",
                existingAthlete.getFederationCodes());

        LinkedHashSet<Category> eligibles = new LinkedHashSet<>();
        LinkedHashSet<Category> teams = new LinkedHashSet<>();
        LinkedHashSet<Category> mixedTeams = new LinkedHashSet<>();

        NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, Locale.ENGLISH);
        Method updateMethod = NRegistrationFileProcessor.class.getDeclaredMethod(
                "updateExistingAthlete", Athlete.class, Athlete.class,
                LinkedHashSet.class, LinkedHashSet.class, LinkedHashSet.class);
        updateMethod.setAccessible(true);
        updateMethod.invoke(processor, existingAthlete, incomingAthlete, eligibles, teams, mixedTeams);

        assertSame("The updated athlete should keep the captured eligible-category set", eligibles,
                RCompetition.getEligibles(existingAthlete.getId()));
        assertSame("The updated athlete should keep the captured team-membership set", teams,
                RCompetition.getTeams(existingAthlete.getId()));
        assertSame("The updated athlete should keep the captured mixed-team set", mixedTeams,
                RCompetition.getMixedTeams(existingAthlete.getId()));
    }

    @Test
    public void testSessionSpecsImport_Delete() throws Exception {
        // Delete existing sessions and import 16 new ones
        String streamURI = "/testData/iwf/session_specs.xlsx";

        try (InputStream xlsInputStream = this.getClass().getResourceAsStream(streamURI)) {
            assertNotNull("Test file not found: " + streamURI, xlsInputStream);

            byte[] fileBytes = xlsInputStream.readAllBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);

            NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, Locale.ENGLISH);
            processor.setSessionOptions(NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS);
            
            // Reset (delete) all existing sessions first
            processor.resetSessions();
            
            int groupsProcessed = processor.doProcessGroups(bais, false, 
                msg -> logger.info("Import: {}", msg), 
                () -> logger.info("Import complete"));

            logger.info("testSessionSpecsImport_Delete: Processed {} groups from session specs", groupsProcessed);

            List<Group> groups = GroupRepository.findAll();
            logger.info("testSessionSpecsImport_Delete: Total groups in database: {}", groups.size());

            // After reset + import 16: should have 16 total
            assertEquals("Should have processed 16 groups from Excel", 16, groupsProcessed);
            assertEquals("Should have 16 total groups (old deleted, 16 new imported)", 16, groups.size());

            Group firstGroup = groups.stream()
                .filter(g -> "1".equals(g.getName()))
                .findFirst()
                .orElse(null);
            assertNotNull("Group '1' should exist", firstGroup);
            assertEquals("Platform should be 'A'", "A", firstGroup.getPlatform().getName());
        }
    }

    @Test
    public void testSessionSpecsImport_UpdateAfterRename() throws Exception {
        // Rename the initial 8 groups to "1"-"8" to match Excel
        // With UPDATE_ADD_SESSIONS, should update 8 + create 8 = 16 total
        List<Group> initialGroups = GroupRepository.findAll();
        for (int i = 0; i < Math.min(8, initialGroups.size()); i++) {
            Group g = initialGroups.get(i);
            g.setName(String.valueOf(i + 1));  // Rename to "1", "2", ..., "8"
            GroupRepository.save(g);
        }
        
        String streamURI = "/testData/iwf/session_specs.xlsx";

        try (InputStream xlsInputStream = this.getClass().getResourceAsStream(streamURI)) {
            assertNotNull("Test file not found: " + streamURI, xlsInputStream);

            byte[] fileBytes = xlsInputStream.readAllBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);

            NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, Locale.ENGLISH);
            processor.setSessionOptions(NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS);
            
            int groupsProcessed = processor.doProcessGroups(bais, false, 
                msg -> logger.info("Import: {}", msg), 
                () -> logger.info("Import complete"));

            logger.info("testSessionSpecsImport_UpdateAfterRename: Processed {} groups from session specs", groupsProcessed);

            List<Group> groups = GroupRepository.findAll();
            logger.info("testSessionSpecsImport_UpdateAfterRename: Total groups in database: {}", groups.size());

            // UPDATE_ADD_SESSIONS with renamed groups: 8 updates + 8 new = 16 total
            assertEquals("Should have processed 16 groups from Excel", 16, groupsProcessed);
            assertEquals("Should have 16 total groups (8 updated + 8 new)", 16, groups.size());

            Group firstGroup = groups.stream()
                .filter(g -> "1".equals(g.getName()))
                .findFirst()
                .orElse(null);
            assertNotNull("Group '1' should exist after update", firstGroup);
            assertEquals("Platform should be 'A'", "A", firstGroup.getPlatform().getName());
        }
    }

    @Test
    public void testSessionSpecsImport_KeepOriginalNames() throws Exception {
        // With UPDATE_ADD_SESSIONS and original names, 8 old + 16 new = 24 total
        String streamURI = "/testData/iwf/session_specs.xlsx";

        try (InputStream xlsInputStream = this.getClass().getResourceAsStream(streamURI)) {
            assertNotNull("Test file not found: " + streamURI, xlsInputStream);

            byte[] fileBytes = xlsInputStream.readAllBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);

            NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, Locale.ENGLISH);
            processor.setSessionOptions(NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS);
            
            int groupsProcessed = processor.doProcessGroups(bais, false, 
                msg -> logger.info("Import: {}", msg), 
                () -> logger.info("Import complete"));

            logger.info("testSessionSpecsImport_KeepOriginalNames: Processed {} groups from session specs", groupsProcessed);

            List<Group> groups = GroupRepository.findAll();
            logger.info("testSessionSpecsImport_KeepOriginalNames: Total groups in database: {}", groups.size());

            // UPDATE_ADD_SESSIONS with original different names: 8 original + 16 new = 24 total
            assertEquals("Should have processed 16 groups from Excel", 16, groupsProcessed);
            assertEquals("Should have 24 total groups (8 original + 16 new)", 24, groups.size());

            Group firstGroup = groups.stream()
                .filter(g -> "1".equals(g.getName()))
                .findFirst()
                .orElse(null);
            assertNotNull("Group '1' should exist (newly imported)", firstGroup);
            assertEquals("Platform should be 'A'", "A", firstGroup.getPlatform().getName());
        }
    }

}
