/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import app.owlcms.Main;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordDefinitionReader;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.spreadsheet.JXLSExportRecords;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.JsonValue;

// subsequent tests depend on features tested in earlier tests
// tests themselves do not depend on work done in earlier tests.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RecordDefinitionReaderTest {

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        TestData.insertInitialData(5, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    final Logger logger = (Logger) LoggerFactory.getLogger(RecordDefinitionReaderTest.class);

    public RecordDefinitionReaderTest() {
        logger.setLevel(Level.TRACE);
    }

    @Before
    public void _00_beforeEachTest() {
        try {
            RecordRepository.clearLoadedRecords();
            RecordRepository.clearNewRecords();
        } catch (IOException e) {
        }
    }

    @Test
    public void _00_testClear() throws IOException {
        RecordRepository.clearLoadedRecords();
        RecordRepository.clearNewRecords();
        JPAService.runInTransaction(em -> {
            try {
                List<RecordEvent> all = RecordRepository.findAll();
                assertEquals(all.size(), 0);
            } catch (Exception e) {
                LoggerUtils.logError(logger, e);
            }
            return null;
        });
    }

    @Test
    public void _01_testIndividualFile() throws IOException, SAXException, InvalidFormatException {

        String streamURI = "/testData/records/EWFRecords.xlsx";

        try (InputStream xmlInputStream = this.getClass().getResourceAsStream(streamURI)) {
            Workbook wb = null;
            try {
                wb = WorkbookFactory.create(xmlInputStream);
                int i = RecordDefinitionReader.createRecords(wb, streamURI, null);
                assertEquals(180, i);
            } finally {
                if (wb != null) {
                    wb.close();
                }
            }
        }
    }

    @Test
    public void _02_testZippedFile() throws IOException, SAXException, InvalidFormatException {
        String zipURI = "/testData/records/IWF_EWF.zip";
        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
        RecordDefinitionReader.readZip(zipStream);
        assertEquals("expected size wrong", 360, RecordRepository.findAll().size());
    }

    @Test
    public void _03_testReload() throws IOException, SAXException, InvalidFormatException {
        String zipURI = "/testData/records/EWFRecords.zip";
        RecordRepository.reloadDefinitions(zipURI);
        assertEquals("expected size wrong", 180, RecordRepository.findAll().size());
    }

    @Test
    public void _04_testRetrieval() throws IOException {
        String zipURI = "/testData/records/IWF_EWF.zip";
        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
        RecordDefinitionReader.readZip(zipStream);
        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 16, 66.0, null, null);
        assertEquals("wrong number of results", 18, results.size());
    }

    @Test
    public void _05_testNoMatch() throws IOException {
        String zipURI = "/testData/records/IWFRecords.zip";
        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
        RecordDefinitionReader.readZip(zipStream);
        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 12, 66.0D, null, null);
        assertEquals("wrong number of results", 0, results.size());
    }

    @Test
    public void _06_testYthMatch() throws IOException {
        String zipURI = "/testData/records/IWFRecords.zip";
        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
        RecordDefinitionReader.readZip(zipStream);
        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 13, 66.0D, null, null);
        assertEquals("wrong number of results", 3, results.size());
    }
    
    @Test
    public void _08_testJson() throws IOException {
        String zipURI = "/testData/records/IWF_EWF.zip";
        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
        RecordDefinitionReader.readZip(zipStream);
        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 16, 110.0D, null, null);
        assertEquals("wrong number of results", 18, results.size());
        JsonValue json = RecordRepository.buildRecordJson(results, null, null, null);
        System.out.println(json.toJson());
    }
    
    @Test
    public void _09_testOrder() throws IOException {
        String streamURI = "/testData/records/ruRecords.xlsx";
        final String resName = "/records/orderCheck.txt";
        
        try (InputStream xmlInputStream = this.getClass().getResourceAsStream(streamURI)) {
            Workbook wb = null;
            try {
                wb = WorkbookFactory.create(xmlInputStream);
                RecordDefinitionReader.createRecords(wb, streamURI, null);
                
                List<RecordEvent> records = RecordRepository.findFiltered(null, null, null, null, null);
                records.sort(new JXLSExportRecords(null).sortRecords());
                
                String results = records.stream().map(RecordEvent::toString).collect(Collectors.joining(System.lineSeparator()));
                assertEqualsToReferenceFile(resName, results+System.lineSeparator());
            } finally {
                if (wb != null) {
                    wb.close();
                }
            }
        }
        
    }
}
