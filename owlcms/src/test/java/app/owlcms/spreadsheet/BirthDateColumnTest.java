package app.owlcms.spreadsheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;

import org.junit.Test;

import app.owlcms.spreadsheet.BirthDateColumn.Kind;
import app.owlcms.spreadsheet.BirthDateColumn.Order;
import app.owlcms.spreadsheet.BirthDateColumn.Problem;

public class BirthDateColumnTest {

	private static final Locale ECUADOR = Locale.forLanguageTag("es-EC");

	private static BirthDateColumn column(Locale locale, String... texts) {
		BirthDateColumn column = new BirthDateColumn(locale);
		for (int i = 0; i < texts.length; i++) {
			column.add("D" + (i + 2), texts[i]);
		}
		return column;
	}

	@Test
	public void aDayAboveTwelveFixesTheOrderForAmbiguousDates() {
		BirthDateColumn c = column(Locale.US, "25/03/1990", "04/05/1991");
		assertFalse(c.hasProblems());
		assertEquals(Order.DMY, c.order());
		assertFalse("the order was proven, not assumed", c.isOrderAssumed());
		assertEquals("1990-03-25", c.normalize("25/03/1990"));
		assertEquals("1991-05-04", c.normalize("04/05/1991"));
	}

	@Test
	public void unanimousMonthFirstVotesWinOverTheLocale() {
		BirthDateColumn c = column(ECUADOR, "03/25/1990", "04/05/1991");
		assertFalse(c.hasProblems());
		assertEquals(Order.MDY, c.order());
		assertEquals("1991-04-05", c.normalize("04/05/1991"));
	}

	@Test
	public void conflictingVotesRejectTheSheet() {
		BirthDateColumn c = column(Locale.US, "25/03/1990", "04/05/1991", "03/25/1992");
		assertTrue(c.hasProblems());
		assertNull(c.order());
		List<Problem> problems = c.problems();
		assertEquals(1, problems.size());
		Problem p = problems.get(0);
		assertEquals(Kind.CONFLICTING_ORDER, p.kind());
		assertEquals("D2", p.address());
		assertEquals("D4", p.otherAddress());
	}

	@Test
	public void withoutEvidenceTheLocaleDecidesAndItIsReported() {
		BirthDateColumn us = column(Locale.US, "04/05/1991");
		assertFalse(us.hasProblems());
		assertEquals(Order.MDY, us.order());
		assertTrue(us.isOrderAssumed());
		assertEquals("1991-04-05", us.normalize("04/05/1991"));

		BirthDateColumn ecuador = column(ECUADOR, "04/05/1991");
		assertFalse(ecuador.hasProblems());
		assertEquals(Order.DMY, ecuador.order());
		assertTrue(ecuador.isOrderAssumed());
		assertEquals("1991-05-04", ecuador.normalize("04/05/1991"));
	}

	@Test
	public void noWarningWhenTheOrderDoesNotMatter() {
		BirthDateColumn c = column(Locale.US, "05/05/1991", "1990-03-15", "1985", "34000");
		assertFalse(c.hasProblems());
		assertFalse(c.isOrderAssumed());
	}

	@Test
	public void aTwoDigitYearRejectsTheSheet() {
		BirthDateColumn c = column(Locale.US, "25/03/1990", "04/05/91", "1.2.99");
		assertTrue(c.hasProblems());
		List<Problem> problems = c.problems();
		assertEquals(2, problems.size());
		assertEquals(Kind.TWO_DIGIT_YEAR, problems.get(0).kind());
		assertEquals("D3", problems.get(0).address());
		assertEquals(Kind.TWO_DIGIT_YEAR, problems.get(1).kind());
		assertEquals("D4", problems.get(1).address());
	}

	@Test
	public void twoDigitYearsDoNotVote() {
		BirthDateColumn c = column(Locale.US, "25/03/90", "04/05/1991");
		assertEquals(Order.MDY, c.order());
		assertEquals(1, c.problems().size());
	}

	@Test
	public void delimitersAreInterchangeableWhenTheOrderIsConsistent() {
		BirthDateColumn c = column(Locale.US, "25.03.1990", "04-05-1991", "7 8 1992", "13/10/1993");
		assertFalse(c.hasProblems());
		assertEquals(Order.DMY, c.order());
		assertEquals("1990-03-25", c.normalize("25.03.1990"));
		assertEquals("1991-05-04", c.normalize("04-05-1991"));
		assertEquals("1992-08-07", c.normalize("7 8 1992"));
	}

	@Test
	public void isoDatesAreAcceptedAndDoNotVote() {
		BirthDateColumn c = column(Locale.US, "1990-03-15", "1991/04/05", "2024. 03. 15.", "25/03/1992", "04/05/1993");
		assertFalse(c.hasProblems());
		assertEquals(Order.DMY, c.order());
		assertEquals("1990-03-15", c.normalize("1990-03-15"));
		assertEquals("1991-04-05", c.normalize("1991/04/05"));
		assertEquals("2024-03-15", c.normalize("2024. 03. 15."));
		assertEquals("1993-05-04", c.normalize("04/05/1993"));
	}

	@Test
	public void impossibleDatesAndGarbageAreAllReportedTogether() {
		BirthDateColumn c = column(Locale.US, "25/03/1990", "15/15/1990", "30/02/1991", "not a date", "31/04/1992",
		        "1990-02-30");
		assertTrue(c.hasProblems());
		List<Problem> problems = c.problems();
		assertEquals(5, problems.size());
		for (Problem p : problems) {
			assertEquals(Kind.INVALID, p.kind());
		}
		assertEquals(List.of("D3", "D4", "D5", "D6", "D7"), problems.stream().map(Problem::address).toList());
	}

	@Test
	public void whatWasAcceptedBeforeKeepsWorking() {
		BirthDateColumn c = column(Locale.US, "1990", "34000", "1985-06-30", "", "   ");
		assertFalse(c.hasProblems());
		assertEquals("1990", c.normalize("1990"));
		assertEquals("34000", c.normalize("34000"));
		assertEquals("1985-06-30", c.normalize("1985-06-30"));
	}

	@Test
	public void anEmptyColumnHasNoProblem() {
		BirthDateColumn c = column(Locale.US);
		assertFalse(c.hasProblems());
		assertFalse(c.isOrderAssumed());
	}
}
