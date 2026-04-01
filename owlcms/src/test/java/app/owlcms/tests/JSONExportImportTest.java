/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.jpa.JPAService;

public class JSONExportImportTest {
	
    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        TestData.insertInitialData(1, true);
    }

	@Test
	public void test() {
		/*
		 * Most bugs are due to an Exception when serializing causing a truncated output,
		 * or a bug in deserializing (e.g. duplicate objects)
		 */
        List<Championship> championships = ChampionshipRepository.findAll();
        assertFalse("championships should be loaded from age group data", championships.isEmpty());
        for (Championship championship : championships) {
            assertNotNull("championship name should be populated", championship.getName());
        }

        CompetitionData competitionData = new CompetitionData();
        try {
        	String s = competitionData.exportDataAsString();
			competitionData.importDataFromString(s);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
