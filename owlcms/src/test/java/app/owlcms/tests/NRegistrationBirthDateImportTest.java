package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.ProdData;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;

public class NRegistrationBirthDateImportTest {

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		ProdData.insertInitialData(0);
		System.setProperty("enableEmbeddedMqtt", "false");
		app.owlcms.init.OwlcmsFactory.initDefaultFOP();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Before
	public void resetDatabase() {
		JPAService.close();
		JPAService.init(true, true);
		Config.initConfig();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
		ProdData.insertInitialData(0);
		app.owlcms.init.OwlcmsFactory.initDefaultFOP();
	}

	private static byte[] sheet(String... birthDates) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Athletes");
			Row header = sheet.createRow(0);
			String[] keys = { "LastName", "FirstName", "Registration.birth", "M/F" };
			for (int column = 0; column < keys.length; column++) {
				header.createCell(column)
				        .setCellValue(Translator.translateNoOverrideOrElseNull(keys[column], Locale.ENGLISH));
			}
			for (int i = 0; i < birthDates.length; i++) {
				Row row = sheet.createRow(i + 1);
				row.createCell(0).setCellValue("Last" + i);
				row.createCell(1).setCellValue("First" + i);
				row.createCell(2).setCellValue(birthDates[i]);
				row.createCell(3).setCellValue("M");
			}
			workbook.write(output);
			return output.toByteArray();
		}
	}

	private static NRegistrationFileProcessor processor(Locale locale) {
		NRegistrationFileProcessor processor = new NRegistrationFileProcessor(false, locale);
		processor.setSessionOptions(NRegistrationFileProcessor.SessionOptions.IGNORE_SESSIONS);
		processor.setAthleteOptions(NRegistrationFileProcessor.AthleteOptions.UPDATE_ADD_ATHLETES);
		return processor;
	}

	private static Athlete find(String lastName) {
		return AthleteRepository.findAll().stream().filter(a -> lastName.equals(a.getLastName())).findFirst()
		        .orElse(null);
	}

	@Test
	public void conflictingOrderRejectsTheWholeSheet() throws Exception {
		byte[] bytes = sheet("25/03/1990", "04/05/1991", "03/25/1992");
		StringBuilder messages = new StringBuilder();

		boolean usable = processor(Locale.US).checkBirthDates(new ByteArrayInputStream(bytes),
		        msg -> messages.append(msg));
		assertFalse("the check should refuse the sheet: " + messages, usable);

		messages.setLength(0);
		int imported = processor(Locale.US).doProcessAthletes(new ByteArrayInputStream(bytes), false,
		        msg -> messages.append(msg), () -> {
		        });
		assertEquals("no athlete is imported: " + messages, 0, imported);
		assertEquals(0, AthleteRepository.findAll().size());
	}

	@Test
	public void everyOffendingCellIsReported() throws Exception {
		byte[] bytes = sheet("25/03/1990", "04/05/91", "15/15/1992", "garbage");
		StringBuilder messages = new StringBuilder();

		assertFalse(processor(Locale.US).checkBirthDates(new ByteArrayInputStream(bytes), msg -> messages.append(msg)));
		String text = messages.toString();
		assertTrue(text, text.contains("C3 "));
		assertTrue(text, text.contains("C4 "));
		assertTrue(text, text.contains("C5 "));
		assertFalse(text, text.contains("C2 "));
		assertEquals(0, AthleteRepository.findAll().size());
	}

	@Test
	public void aDayAboveTwelveDecidesTheOrderOfTheAmbiguousDates() throws Exception {
		byte[] bytes = sheet("25.03.1990", "04-05-1991", "1992-06-07");
		StringBuilder messages = new StringBuilder();

		assertTrue(processor(Locale.US).checkBirthDates(new ByteArrayInputStream(bytes), msg -> messages.append(msg)));
		int imported = processor(Locale.US).doProcessAthletes(new ByteArrayInputStream(bytes), false,
		        msg -> messages.append(msg), () -> {
		        });
		assertEquals(messages.toString(), 3, imported);

		Athlete first = find("Last0");
		Athlete second = find("Last1");
		Athlete third = find("Last2");
		assertNotNull(first);
		assertNotNull(second);
		assertNotNull(third);
		assertEquals(LocalDate.of(1990, 3, 25), first.getFullBirthDate());
		assertEquals("the locale is month-first, the file proves day-first", LocalDate.of(1991, 5, 4),
		        second.getFullBirthDate());
		assertEquals(LocalDate.of(1992, 6, 7), third.getFullBirthDate());
	}

	@Test
	public void aRefusedSheetDeletesNobodyEvenWhenTheOptionIsDeleteAthletes() throws Exception {
		StringBuilder messages = new StringBuilder();
		NRegistrationFileProcessor first = processor(Locale.US);
		int existing = first.doProcessAthletes(new ByteArrayInputStream(sheet("25/03/1990", "13/04/1991")), false,
		        msg -> messages.append(msg), () -> {
		        });
		assertEquals(messages.toString(), 2, existing);
		assertEquals(2, AthleteRepository.findAll().size());

		NRegistrationFileProcessor replace = processor(Locale.US);
		replace.setAthleteOptions(NRegistrationFileProcessor.AthleteOptions.DELETE_ATHLETES);
		StringBuilder refusal = new StringBuilder();
		boolean proceed = replace.prepareAthleteImport(
		        new ByteArrayInputStream(sheet("25/03/1990", "04/05/91", "garbage")), true,
		        msg -> refusal.append(msg));
		assertFalse("a bad sheet must be refused", proceed);
		assertTrue(refusal.toString(), refusal.toString().contains("C3 "));
		assertEquals("the existing athletes must still be there", 2, AthleteRepository.findAll().size());
		assertNotNull(find("Last0"));
		assertNotNull(find("Last1"));
	}

	@Test
	public void anAcceptedSheetDeletesTheExistingAthletesWhenTheOptionIsDeleteAthletes() throws Exception {
		StringBuilder messages = new StringBuilder();
		processor(Locale.US).doProcessAthletes(new ByteArrayInputStream(sheet("25/03/1990", "13/04/1991")), false,
		        msg -> messages.append(msg), () -> {
		        });
		assertEquals(2, AthleteRepository.findAll().size());

		NRegistrationFileProcessor replace = processor(Locale.US);
		replace.setAthleteOptions(NRegistrationFileProcessor.AthleteOptions.DELETE_ATHLETES);
		assertTrue(replace.prepareAthleteImport(new ByteArrayInputStream(sheet("25/03/1992")), true,
		        msg -> messages.append(msg)));
		assertEquals("the athletes are deleted once the sheet is accepted", 0, AthleteRepository.findAll().size());
	}

	@Test
	public void aSessionsOnlyFileIsNotCheckedForBirthDates() throws Exception {
		NRegistrationFileProcessor sessions = processor(Locale.US);
		assertTrue(sessions.prepareAthleteImport(new ByteArrayInputStream(sheet("garbage")), false, msg -> {
		}));
	}

	@Test
	public void withoutEvidenceTheLocaleOrderIsUsed() throws Exception {
		byte[] bytes = sheet("04/05/1991");
		StringBuilder messages = new StringBuilder();

		int imported = processor(Locale.US).doProcessAthletes(new ByteArrayInputStream(bytes), false,
		        msg -> messages.append(msg), () -> {
		        });
		assertEquals(messages.toString(), 1, imported);
		assertEquals(LocalDate.of(1991, 4, 5), find("Last0").getFullBirthDate());
	}
}
