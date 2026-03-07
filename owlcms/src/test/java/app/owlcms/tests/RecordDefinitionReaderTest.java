/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static app.owlcms.tests.AllTests.assertEqualsToReferenceFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
                List<String> s = new RecordDefinitionReader().createRecords(wb, streamURI, null);
                assertEquals("180 records inserted.", s.get(s.size()-1));
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
        new RecordDefinitionReader().readZip(zipStream);
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
        new RecordDefinitionReader().readZip(zipStream);
        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 16, 66.0, null, null);
        assertEquals("wrong number of results", 18, results.size());
    }

    @Test
    public void _05_testNoMatch() throws IOException {
        String zipURI = "/testData/records/IWFRecords.zip";
        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
        new RecordDefinitionReader().readZip(zipStream);
        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 12, 66.0D, null, null);
        assertEquals("wrong number of results", 0, results.size());
    }

    @Test
    public void _06_testYthMatch() throws IOException {
        String zipURI = "/testData/records/IWFRecords.zip";
        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
        new RecordDefinitionReader().readZip(zipStream);
        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 13, 66.0D, null, null);
        assertEquals("wrong number of results", 3, results.size());
    }

//    @Test
//    @Ignore
//    public void _08_testJson() throws IOException {
//        String zipURI = "/testData/records/IWF_EWF.zip";
//        InputStream zipStream = this.getClass().getResourceAsStream(zipURI);
//        RecordDefinitionReader.readZip(zipStream);
//        List<RecordEvent> results = RecordRepository.findFiltered(Gender.M, 16, 110.0D, null, null);
//        assertEquals("wrong number of results", 18, results.size());
//        JsonValue json = RecordFilter.buildRecordJson(results, null, null, null);
//        System.out.println(json.toJson());
//    }

    @Test
    public void _09_testOrder() throws IOException {
        String streamURI = "/testData/records/ruRecords.xlsx";
        final String resName = "/records/orderCheck.txt";
        
        try (InputStream xmlInputStream = this.getClass().getResourceAsStream(streamURI)) {
            Workbook wb = null;
            try {
                wb = WorkbookFactory.create(xmlInputStream);
                new RecordDefinitionReader().createRecords(wb, streamURI, null);
                
                List<RecordEvent> records = RecordRepository.findFiltered(null, null, null, null, null);
                records.sort(new JXLSExportRecords(null,false, false).sortRecords());
                
                String results = records.stream().map(RecordEvent::toString).collect(
                        Collectors.joining(System.lineSeparator(),"",System.lineSeparator()));
                assertEqualsToReferenceFile(resName, results);
            } finally {
                if (wb != null) {
                    wb.close();
                }
            }
        }
    }
    
    @Test
    public void _10_testMessages() throws IOException {
        String streamURI = "/testData/records/test.xlsx";
        final String resName = "/records/errorCheck.txt";
        final String recName = "/records/recordsCheck.txt";
        
        try (InputStream xmlInputStream = this.getClass().getResourceAsStream(streamURI)) {
            Workbook wb = null;
            try {
                wb = WorkbookFactory.create(xmlInputStream);
                List<String> errors = new RecordDefinitionReader().createRecords(wb, streamURI, null);
                
                List<RecordEvent> records = RecordRepository.findFiltered(null, null, null, null, null);
                records.sort(new JXLSExportRecords(null,true, false).sortRecords());
                
                String results = records.stream().map(RecordEvent::toString).collect(
                        Collectors.joining(System.lineSeparator(),"",System.lineSeparator()));
                assertEqualsToReferenceFile(recName, results);
                
                String errorString = errors.stream().collect(
                        Collectors.joining(System.lineSeparator(),"",System.lineSeparator()));
                assertEqualsToReferenceFile(resName, errorString);
            } finally {
                if (wb != null) {
                    wb.close();
                }
            }
        }
    }

    @Test
    public void _11_testOfficialImportReplacesByLogicalKeyAcrossFileNames() throws IOException {
        try (Workbook firstWorkbook = createWorkbook("QC", "Provincial", "F", "SR", 15, 999, 71, 76, "SNATCH", 101);
                Workbook secondWorkbook = createWorkbook("QC", "Provincial", "F", "SR", 15, 999, 71, 76, "SNATCH", 99)) {
            new RecordDefinitionReader().createRecords(firstWorkbook, "first.xlsx", "first_upload");
            assertEquals(1, RecordRepository.findAll().size());

            new RecordDefinitionReader().createRecords(secondWorkbook, "second.xlsx", "second_upload");

            List<RecordEvent> allRecords = RecordRepository.findAll();
            assertEquals(1, allRecords.size());
            RecordEvent correctedRecord = allRecords.get(0);
            assertEquals(99.0D, correctedRecord.getRecordValue(), 0.001D);
            assertEquals("second_upload", correctedRecord.getFileName());
        }
    }

    @Test
    public void _12_testOfficialImportAbsorbsMatchingLocalProvisional() throws IOException {
        RecordEvent provisional = new RecordEvent();
        provisional.setRecordFederation("QC");
        provisional.setRecordName("Provincial");
        provisional.setGender(Gender.F);
        provisional.setAgeGrp("SR");
        provisional.setAgeGrpLower(15);
        provisional.setAgeGrpUpper(999);
        provisional.setBwCatLower(71);
        provisional.setBwCatUpper(76);
        provisional.setRecordLift(app.owlcms.data.athleteSort.Ranking.SNATCH);
        provisional.setRecordValue(101.0D);
        provisional.setAthleteName("Athlete One");
        provisional.setRecordDate(LocalDate.of(2026, 3, 7));
        provisional.setEvent("Reference Meet");
        provisional.setEventLocation("Montreal");
        provisional.setGroupNameString("A");
        provisional.setFileName("local_export");
        RecordRepository.save(provisional);

        try (Workbook workbook = createWorkbook(
                "QC", "Provincial", "F", "SR", 15, 999, 71, 76, "SNATCH", 101,
                "Athlete One", "2026-03-07", "Reference Meet", "Montreal", null)) {
            new RecordDefinitionReader().createRecords(workbook, "reference.xlsx", "reference_upload");
        }

        List<RecordEvent> allRecords = RecordRepository.findAll();
        assertEquals(1, allRecords.size());
        assertTrue(allRecords.get(0).getGroupNameString() == null || allRecords.get(0).getGroupNameString().isBlank());
        assertEquals("reference_upload", allRecords.get(0).getFileName());
    }

    private Workbook createWorkbook(
            String federation,
            String recordName,
            String gender,
            String ageGroup,
            int ageLower,
            int ageUpper,
            int bwLower,
            int bwUpper,
            String lift,
            double recordValue) {
        return createWorkbook(federation, recordName, gender, ageGroup, ageLower, ageUpper, bwLower, bwUpper, lift, recordValue, null, null, null, null, null);
    }

    private Workbook createWorkbook(
            String federation,
            String recordName,
            String gender,
            String ageGroup,
            int ageLower,
            int ageUpper,
            int bwLower,
            int bwUpper,
            String lift,
            double recordValue,
            String athleteName,
            String recordDate,
            String event,
            String place,
            String group) {
        Workbook workbook = new XSSFWorkbook();
        var sheet = workbook.createSheet("records");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("federation");
        header.createCell(1).setCellValue("recordname");
        header.createCell(2).setCellValue("agegroup");
        header.createCell(3).setCellValue("gender");
        header.createCell(4).setCellValue("agelow");
        header.createCell(5).setCellValue("ageupper");
        header.createCell(6).setCellValue("bwlow");
        header.createCell(7).setCellValue("bwupper");
        header.createCell(8).setCellValue("recordlift");
        header.createCell(9).setCellValue("recordvalue");
		header.createCell(10).setCellValue("athletename");
		header.createCell(11).setCellValue("date");
		header.createCell(12).setCellValue("event");
		header.createCell(13).setCellValue("place");
		header.createCell(14).setCellValue("group");

        Row row = sheet.createRow(1);
        row.createCell(0).setCellValue(federation);
        row.createCell(1).setCellValue(recordName);
        row.createCell(2).setCellValue(ageGroup);
        row.createCell(3).setCellValue(gender);
        row.createCell(4).setCellValue(ageLower);
        row.createCell(5).setCellValue(ageUpper);
        row.createCell(6).setCellValue(bwLower);
        row.createCell(7).setCellValue(bwUpper);
        row.createCell(8).setCellValue(lift);
        row.createCell(9).setCellValue(recordValue);
        if (athleteName != null) {
            row.createCell(10).setCellValue(athleteName);
        }
        if (recordDate != null) {
            row.createCell(11).setCellValue(recordDate);
        }
        if (event != null) {
            row.createCell(12).setCellValue(event);
        }
        if (place != null) {
            row.createCell(13).setCellValue(place);
        }
        if (group != null) {
            row.createCell(14).setCellValue(group);
        }
        return workbook;
    }
}
