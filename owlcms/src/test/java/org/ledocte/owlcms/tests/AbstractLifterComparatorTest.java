package org.ledocte.owlcms.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.ledocte.owlcms.data.athleteSort.AbstractLifterComparator;

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
