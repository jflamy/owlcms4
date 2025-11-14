/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;
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
    public void testSessionSpecsImport_Delete() throws Exception {
        // Delete existing sessions and import 16 new ones
        String streamURI = "/testData/iwf/session_specs.xlsx";

        try (InputStream xlsInputStream = this.getClass().getResourceAsStream(streamURI)) {
            assertNotNull("Test file not found: " + streamURI, xlsInputStream);

            byte[] fileBytes = xlsInputStream.readAllBytes();
            ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes);

            NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false);
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

            NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false);
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

            NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false);
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
