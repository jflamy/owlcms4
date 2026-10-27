/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.PlatformRepository;

public class TrackerCheckpointRestoreTest {

	@BeforeClass
	public static void setupDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Test
	public void trackerDatabaseZipRestoresIntoMemory() throws Exception {
		try (InputStream checkpoint = getClass().getResourceAsStream("/startup/tracker-checkpoint.zip")) {
			assertNotNull("tracker checkpoint fixture", checkpoint);
			FormatDetector.importData(checkpoint);
		}

		assertEquals("Spring Equinox Open", Competition.getCurrent().getCompetitionName());
		assertEquals(5, AthleteRepository.findAll().size());
		assertEquals(List.of("F1", "M1", "M2", "Y1"),
		        GroupRepository.findAll().stream().map(group -> group.getName()).sorted().toList());
		assertEquals(List.of("A"),
		        PlatformRepository.findAll().stream().map(platform -> platform.getName()).sorted().toList());
	}
}
