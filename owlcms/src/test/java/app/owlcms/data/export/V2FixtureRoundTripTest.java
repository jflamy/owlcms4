/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.access.AccessStartup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.v2.CompetitionDataV2;
import app.owlcms.data.jpa.JPAService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Loads a V2 export and checks that the loaded competition is the one that was exported. */
public class V2FixtureRoundTripTest {

	private static final String FIXTURE = "/startup/competition-director-acceptance.json";
	private static final String CATEGORY_CODE = "Open_M67";
	private static final ObjectMapper MAPPER = JsonMapper.builder().build();

	@BeforeClass
	public static void loadFixture() throws Exception {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		// as at startup: category names include the translated gender code
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		FormatDetector.importData(openFixture());
		// the fixture is in accounts mode: let the background account initialization finish before closing
		AccessStartup.roleAccountInitialization().join();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Test
	public void importedCategoryCanBeFoundByCode() {
		Category category = CategoryRepository.findByCode(CATEGORY_CODE);
		assertNotNull("category " + CATEGORY_CODE + " should be found by its code after import", category);
		assertEquals(CATEGORY_CODE, category.getComputedCode());
	}

	@Test
	public void athletesKeepTheirRegistrationCategory() {
		List<Athlete> athletes = AthleteRepository.findAll();
		assertEquals(4, athletes.size());
		for (Athlete athlete : athletes) {
			assertNotNull(athlete.getLastName() + " should keep its category", athlete.getCategory());
			assertEquals(athlete.getLastName(), CATEGORY_CODE, athlete.getCategory().getComputedCode());
		}
	}

	@Test
	public void athletesKeepTheirParticipations() {
		for (Athlete athlete : AthleteRepository.findAll()) {
			List<Participation> participations = athlete.getParticipations();
			assertFalse(athlete.getLastName() + " should keep its participations", participations.isEmpty());
			assertEquals(athlete.getLastName(), List.of(CATEGORY_CODE),
			        participations.stream().map(p -> p.getCategory().getComputedCode()).toList());
		}
	}

	/**
	 * The fixture was written by an earlier export. A fresh export may add fields (readers ignore unknown ones), but
	 * must keep every field of the fixture with the same name, type and value.
	 */
	@Test
	public void exportKeepsEveryFieldOfTheFixture() throws Exception {
		JsonNode fixture = withoutExportMetadata(MAPPER.readTree(openFixture()));
		JsonNode exported = withoutExportMetadata(exportDatabase());
		List<String> differences = new ArrayList<>();
		collectMissing("", fixture, exported, differences);
		assertTrue("fresh export differs from the fixture:\n" + String.join("\n", differences),
		        differences.isEmpty());
	}

	/** Loading an export and exporting again gives the same file. */
	@Test
	public void exportIsStableAcrossReload() throws Exception {
		JsonNode first = exportDatabase();
		FormatDetector.importData(new ByteArrayInputStream(MAPPER.writeValueAsBytes(first)));
		AccessStartup.roleAccountInitialization().join();
		JsonNode second = exportDatabase();
		// compared as text so that a failure shows where the two differ
		assertEquals(pretty(withoutExportMetadata(first)), pretty(withoutExportMetadata(second)));
	}

	private static JsonNode exportDatabase() throws Exception {
		try (InputStream exported = new CompetitionDataV2().fromDatabase().exportData()) {
			return MAPPER.readTree(exported);
		}
	}

	/** Records every place where {@code expected} is not contained in {@code actual}. */
	private static void collectMissing(String path, JsonNode expected, JsonNode actual, List<String> differences) {
		if (differences.size() >= 50) {
			return;
		}
		if (actual == null || actual.isMissingNode()) {
			differences.add(path + ": missing");
		} else if (expected.isObject() && actual.isObject()) {
			for (String name : expected.propertyNames()) {
				collectMissing(path + "." + name, expected.get(name), actual.get(name), differences);
			}
		} else if (expected.isArray() && actual.isArray()) {
			if (expected.size() != actual.size()) {
				differences.add(path + ": " + expected.size() + " elements, now " + actual.size());
			} else {
				for (int i = 0; i < expected.size(); i++) {
					collectMissing(path + "[" + i + "]", expected.get(i), actual.get(i), differences);
				}
			}
		} else if (!expected.equals(actual)) {
			differences.add(path + ": " + expected + " now " + actual);
		}
	}

	private static InputStream openFixture() {
		InputStream in = V2FixtureRoundTripTest.class.getResourceAsStream(FIXTURE);
		assertNotNull("fixture " + FIXTURE, in);
		return in;
	}

	/**
	 * Values that change on every export or import, whatever the competition: when and by which version the file was
	 * written, and the competition's generated database id (a new row is inserted on each import).
	 */
	private static JsonNode withoutExportMetadata(JsonNode export) {
		((ObjectNode) export).remove("exportDate");
		((ObjectNode) export.get("config")).remove("appVersion");
		((ObjectNode) export.get("competition")).remove("id");
		return export;
	}

	private static String pretty(JsonNode node) {
		return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
	}
}
