package app.owlcms.tests;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Test;

import app.owlcms.utils.ResourceWalker;

public class ResourceWalkerTest {

    @Test
    public void test() {
        ResourceWalker walker = new ResourceWalker();
        assertTrue(walker.matchesLocale("Protocol.xls", null));
        assertFalse(walker.matchesLocale("Protocol_en.xls", null));
        
        assertTrue(walker.matchesLocale("Protocol_en.xls", new Locale("en")));
        assertFalse(walker.matchesLocale("Protocol_en.xls", new Locale("fr")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("en")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("fr")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("fr")));
        
        assertTrue(walker.matchesLocale("Protocol_en.xls", new Locale("en","ZA")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("en","ZA")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("en","CA")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en","ZA")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en","CA")));
        
        assertTrue(walker.matchesLocale("Protocol_en.xls", new Locale("en","ZA","JHB")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("en","ZA","JHB")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en","ZA","JHB")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en","ZA","CT")));
    }

}
