/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.record.RecordDefinitionReader;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public class RecordDefinitionReaderTest {

    final Logger logger = (Logger) LoggerFactory.getLogger(RecordDefinitionReaderTest.class);

    public RecordDefinitionReaderTest() {
        logger.setLevel(Level.TRACE);
    }
    
    @BeforeClass
    public static void setupTests() {
        System.err.println("setup");
        JPAService.init(true, true);
        TestData.insertInitialData(5, true);
    }

    @AfterClass
    public static void tearDownTests() {
        JPAService.close();
    }

    @Test
    public void test() throws IOException, SAXException, InvalidFormatException {

        String streamURI = "/testData/IWF Records.xlsx";
        System.err.println("testing "+this.getClass().getResourceAsStream(streamURI));

        try (InputStream xmlInputStream = this.getClass().getResourceAsStream(streamURI)) {
            System.err.println("found "+streamURI);
            Workbook wb = null;
            try {
                wb = WorkbookFactory.create(xmlInputStream);
                System.err.println("created "+wb);
                int i = RecordDefinitionReader.createRecords(wb, null, streamURI);
                System.err.println("created "+i+" records");
                assertEquals(180, i);
            } finally {
                if (wb != null) {
                    wb.close();
                }
            }
        }
    }

}
