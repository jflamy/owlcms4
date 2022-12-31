/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import app.owlcms.Main;
import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.spreadsheet.RAthlete;
import app.owlcms.spreadsheet.RCompetition;
import ch.qos.logback.classic.Logger;
import net.sf.jxls.reader.ReaderBuilder;
import net.sf.jxls.reader.ReaderConfig;
import net.sf.jxls.reader.XLSDataReadException;
import net.sf.jxls.reader.XLSReader;

public class RegistrationReaderTest {

    final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationReaderTest.class);
    private static final String REGISTRATION_READER_SPEC = "/templates/registration/RegistrationReader.xml";

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

    @Test
    public void test() throws IOException, SAXException, InvalidFormatException {

        String streamURI = "/testData/registration.xls";

        try (InputStream xmlInputStream = this.getClass().getResourceAsStream(REGISTRATION_READER_SPEC)) {

            ReaderConfig.getInstance().setUseDefaultValuesForPrimitiveTypes(true);
            XLSReader reader = ReaderBuilder.buildFromXML(xmlInputStream);

            try (InputStream xlsInputStream = this.getClass().getResourceAsStream(streamURI)) {
                RCompetition c = new RCompetition();
                List<RAthlete> athletes = new ArrayList<>();

                Map<String, Object> beans = new HashMap<>();
                beans.put("competition", c);
                beans.put("athletes", athletes);

                logger.info("Reading the data...");
                reader.read(xlsInputStream, beans);

                logger.info("Read " + athletes.size() + " athletes into `athletes` list");

                List<Athlete> collect = athletes.stream().map(r -> r.getAthlete()).collect(Collectors.toList());
                AllTests.assertEqualsToReferenceFile("/reg_results.txt", DebugUtils.longDump(collect));
            } catch (XLSDataReadException e) {
                Throwable cause = e.getCause();
                Throwable cause2 = (cause != null ? cause.getCause() : null);

                logger.error("cannot read cell {}: {}", e.getCellName(), cause2 != null ? cause2.getLocalizedMessage()
                        : (cause != null ? cause.getLocalizedMessage() : "Error"));
            }
        }
    }

}
