/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import app.owlcms.data.technicalofficial.TechnicalOfficialNameParser;

public class TechnicalOfficialNameParserTest {

    @Test
    public void parseCommaSeparatedName() {
        TechnicalOfficialNameParser.NameParts parts = TechnicalOfficialNameParser.parse("Smith, John");

        assertEquals("Smith", parts.getLastName());
        assertEquals("John", parts.getFirstName());
    }

    @Test
    public void parseSpaceSeparatedName() {
        TechnicalOfficialNameParser.NameParts parts = TechnicalOfficialNameParser.parse("John Smith");

        assertEquals("Smith", parts.getLastName());
        assertEquals("John", parts.getFirstName());
    }

    @Test
    public void parseMultiWordFirstName() {
        TechnicalOfficialNameParser.NameParts parts = TechnicalOfficialNameParser.parse("John Robert Smith");

        assertEquals("Smith", parts.getLastName());
        assertEquals("John Robert", parts.getFirstName());
    }

    @Test
    public void parseBlankOrNull() {
        TechnicalOfficialNameParser.NameParts blankParts = TechnicalOfficialNameParser.parse("   ");
        assertEquals("", blankParts.getLastName());
        assertEquals("", blankParts.getFirstName());

        TechnicalOfficialNameParser.NameParts nullParts = TechnicalOfficialNameParser.parse(null);
        assertEquals("", nullParts.getLastName());
        assertEquals("", nullParts.getFirstName());
    }

}
