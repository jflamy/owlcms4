package app.owlcms.nui.admin;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Test;

public class BirthRepairServiceTest {

	@Test
	public void dateRepairAddsExactlyOneDay() {
		assertEquals(LocalDate.of(2004, 1, 1),
		        BirthDateRepairService.repairedBirthDate(LocalDate.of(2003, 12, 31)));
		assertEquals(LocalDate.of(2024, 3, 1),
		        BirthDateRepairService.repairedBirthDate(LocalDate.of(2024, 2, 29)));
	}

	@Test
	public void yearRepairUsesJanuaryFirstOfFollowingYear() {
		assertEquals(LocalDate.of(2004, 1, 1),
		        BirthYearRepairService.repairedBirthDate(LocalDate.of(2003, 12, 2)));
		assertEquals(LocalDate.of(2004, 1, 1),
		        BirthYearRepairService.repairedBirthDate(LocalDate.of(2003, 1, 1)));
	}
}