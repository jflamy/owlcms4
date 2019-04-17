/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import app.owlcms.data.athleteSort.AbstractLifterComparator;

public class AbstractLifterComparatorTest {

    @Test
    public void testStandard() {
        String prefix;
        prefix = "m";
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"51"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+">105"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"105+"));
    }

    @Test
    public void testMasters() {
        String prefix;
        prefix = "m50-55 ";
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"51"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+">105"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"105+"));
    }

    @Test
    public void testMasters2() {
        String prefix;
        prefix = "m4_";
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"51"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+">105"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"105+"));
    }

    @Test
    public void testStandardKg() {
        String prefix;
        prefix = "m";
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"51kg"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+">105kg"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"105kg+"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"105+kg"));
    }

    @Test
    public void testMastersKg() {
        String prefix;
        prefix = "m50-65 ";
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"51kg"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+">105kg"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"105kg+"));
        assertEquals(prefix,AbstractLifterComparator.getCategoryPrefix(prefix+"105+kg"));
    }
}
