package app.owlcms.tests;

import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.jpa.JPAService;

public class JSONExportImportTest {
	
    @BeforeClass
    public static void setupTests() {
        Main.injectSuppliers();
        JPAService.init(true, true);
        Config.initConfig();
        TestData.insertInitialData(5, true);
    }

	@Test
	public void test() {
		/*
		 * Most bugs are due to an Exception when serializing causing a truncated output,
		 * or a bug in deserializing (e.g. duplicate objects)
		 */
        CompetitionData competitionData = new CompetitionData();
        try {
        	String s = competitionData.exportDataAsString();
			competitionData.importDataFromString(s);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

}
