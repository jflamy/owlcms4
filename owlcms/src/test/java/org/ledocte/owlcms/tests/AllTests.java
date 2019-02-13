/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

//import org.concordiainternational.competition.spreadsheet.ExtenXLSReader;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.ledocte.owlcms.data.athlete.Athlete;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

@RunWith(Suite.class)
@Suite.SuiteClasses( { LifterSorterTest.class, LifterTest.class, TwoMinutesRuleTest.class })
public class AllTests {

    final static String lineSeparator = System.getProperty("line.separator"); //$NON-NLS-1$
    final private static Logger logger = (Logger)LoggerFactory.getLogger(AllTests.class);

    /**
     * @param lifterList
     * @return ordered printout of lifters, one per line.
     */
    public static String shortDump(List<Athlete> lifterList) {
        StringBuffer sb = new StringBuffer();
        for (Athlete lifter : lifterList) {
            sb.append(lifter.getLastName() + " " + lifter.getFirstName() //$NON-NLS-1$
                + " " + lifter.getNextAttemptRequestedWeight() //$NON-NLS-1$
                + " " + (lifter.getAttemptsDone() + 1) //$NON-NLS-1$
                + " " + lifter.getLotNumber()); //$NON-NLS-1$
            sb.append(AllTests.lineSeparator);
        }
        return sb.toString();
    }

    /**
     * @param lifterList
     * @return ordered printout of lifters, one per line.
     */
    public static String longDump(List<Athlete> lifterList) {
        StringBuffer sb = new StringBuffer();
        for (Athlete lifter : lifterList) {
            sb.append(lifter.longDump());
            sb.append(AllTests.lineSeparator);
        }
        return sb.toString();
    }

    /**
     * @param lifterList
     * @return ordered printout of lifters, one per line.
     */
    static String longDump(List<Athlete> lifterList, boolean includeTimeStamp) {
        StringBuffer sb = new StringBuffer();
        for (Athlete lifter : lifterList) {
        	sb.append(lifter.longDump());
            sb.append(AllTests.lineSeparator);
        }
        return sb.toString();
    }

    /**
     * Compare actual with expected that is read from a file (a resource found
     * on the class path)
     *
     * @param referenceFilePath
     *            a path of the form /filename where filename is located in a
     *            directory that is found on the class path.
     * @param actual
     */
    static public void assertEqualsToReferenceFile(final String referenceFilePath, String actual) {
        String name = "/testData" + referenceFilePath;
		InputStream is = AllTests.class.getResourceAsStream(name); //$NON-NLS-1$
		logger.info("comparing results to reference file {}", name);
        if (is != null) {
            String expected = getContents(is);
            assertEquals(referenceFilePath, expected, actual);
        } else {
            System.err.println("------ if ok, copy following to " + referenceFilePath); //$NON-NLS-1$
            System.err.println(actual);
            System.err.println("------"); //$NON-NLS-1$
            fail(referenceFilePath + " not found"); //$NON-NLS-1$
        }
    }

    static public String getContents(InputStream is) {
        StringBuilder contents = new StringBuilder();

        try {
            // use buffering, reading one line at a time
            // FileReader always assumes default encoding is OK!
            BufferedReader input = new BufferedReader(new InputStreamReader(is));
            try {
                String line = null; // not declared within while loop
                /*
                 * readLine is a bit quirky : it returns the content of a line
                 * MINUS the newline. it returns null only for the END of the
                 * stream. it returns an empty String if two newlines appear in
                 * a row.
                 */
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator")); //$NON-NLS-1$
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return contents.toString();
    }

}
