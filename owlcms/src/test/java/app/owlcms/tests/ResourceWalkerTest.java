/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.Resource;
import app.owlcms.utils.ResourceWalker;

public class ResourceWalkerTest {

    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
    }

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

        assertTrue(walker.matchesLocale("Protocol_en.xls", new Locale("en", "ZA")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("en", "ZA")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("en", "CA")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en", "ZA")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en", "CA")));

        assertTrue(walker.matchesLocale("Protocol_en.xls", new Locale("en", "ZA", "JHB")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA.xls", new Locale("en", "ZA", "JHB")));
        assertTrue(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en", "ZA", "JHB")));
        assertFalse(walker.matchesLocale("Protocol_en_ZA_JHB.xls", new Locale("en", "ZA", "CT")));
    }
    
    @Ignore
    public void testOverrideList() {
		List<Resource> resourceList = new ResourceWalker().getResourceList("",
		        ResourceWalker::relativeName, null, Locale.ENGLISH,
		        false);
		for (Resource resource: resourceList) {
			System.out.println(resource.getFileName());
		}
    }
    
    @Test
    public void testPRList() {
    	new ResourceWalker().getPRResourceMap(Locale.ENGLISH);
    }

}
