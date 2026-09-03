package app.owlcms.fieldofplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import app.owlcms.data.athlete.Gender;

public class PlatformEquipmentCalculatorTest {

	@Test
	public void fifteenKgUsesFiveKgBarToLoadBumpers() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, true, false, true, false, 5);

		assertSelection(5, false, true, select(15, 15, 15, false, false, 0, 20, inventory));
	}

	@Test
	public void sixteenKgAllowsChangePlatesToCompleteBumperLoad() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, true, false, true, false, 5);

		assertSelection(5, false, true, select(16, 15, 15, false, false, 0, 20, inventory));
	}

	@Test
	public void nonStandardBarOmitsCollarsToPreserveBumpers() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, false, false, false, true, 5);

		assertSelection(7, false, true, select(15, 15, 15, false, true, 7, 15, inventory));
	}

	@Test
	public void standardBarOmitsAvailableCollarsToPreserveBumpers() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, true, false, false, true, 5);

		assertSelection(15, false, false, select(20, 15, 15, false, false, 0, 20, inventory));
	}

	@Test
	public void lightBarOmitsAvailableCollarsToPreserveBumpers() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, false, false, true, true, 5);

		assertSelection(5, false, true, select(14, 15, 15, false, false, 0, 20, inventory));
	}

	@Test
	public void noCollars5kgBarIgnoresThreshold() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, false, false, true, true, 5);

		assertSelection(5, false, true, select(15, 15, 15, false, true, false, 0, 30, inventory));
		assertSelection(5, false, true, select(19, 15, 15, false, true, false, 0, 30, inventory));
	}

	@Test
	public void fiveKgBarUsesCollarsAtThresholdWhenToggleDisabled() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, false, false, true, true, 5);

		assertSelection(5, true, true, select(15, 15, 15, false, false, false, 0, 30, inventory));
	}

	@Test
	public void lightU13ExcludesTwentyKgBarForMaleU13Category() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, true, false, false, 5);
		int maximumBarWeight = PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 13, true, false);

		assertSelection(15, false, true, select(25, 20, maximumBarWeight, false, false, 0, 30, inventory));
	}

	@Test
	public void lightU15ExcludesTwentyKgBarForMaleU15Category() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, true, false, true, 5);
		int maximumBarWeight = PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 15, false, true);

		assertSelection(15, false, true, select(25, 20, maximumBarWeight, false, false, 0, 35, inventory));
	}

	@Test
	public void lightBarTogglesOffKeepTwentyKgBarForMaleU13Category() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, true, false, false, 5);
		int maximumBarWeight = PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 13, false, false);

		assertEquals(20, maximumBarWeight);
		assertSelection(20, false, false, select(25, 20, maximumBarWeight, false, false, 0, 30, inventory));
	}

	@Test
	public void lightU13DoesNotApplyToOlderMaleCategories() {
		assertEquals(20, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 14, true, false));
		assertEquals(20, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 15, true, false));
		assertEquals(20, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 17, true, true));
	}

	@Test
	public void lightU15AlsoCoversU13Categories() {
		assertEquals(15, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 13, false, true));
		assertEquals(15, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, 15, true, true));
	}

	@Test
	public void lightBarPolicyIgnoresMissingAgeGroup() {
		assertEquals(20, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.M, null, true, true));
	}

	@Test
	public void femaleAthletesAlwaysUseFifteenKgMaximum() {
		assertEquals(15, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.F, 13, false, false));
		assertEquals(15, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.F, 40, false, false));
		assertEquals(15, PlatformEquipmentCalculator.maximumAllowedBarWeight(Gender.F, null, true, true));
	}

	@Test
	public void usawCollarsOverrideTheCollarThreshold() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, false, false, false, 5);

		assertSelection(20, true, false, select(25, 20, 20, true, false, 0, 30, inventory));
	}

	@Test
	public void noBumpersFallsBackToStandardBarSelection() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, true, false, true, true, 0);

		assertSelection(15, false, false, select(15, 15, 15, false, false, 0, 25, inventory));
	}

	@Test
	public void collarsAreRetainedWhenBumpersStillFit() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(false, true, false, false, true, 5);

		assertSelection(15, true, false, select(25, 15, 15, false, false, 0, 20, inventory));
	}

	@Test
	public void menUseStandardBarWithCollarsAboveThirtyKg() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, false, false, true, 20);

		assertSelection(20, true, false, select(45, 20, 20, false, false, 0, 25, inventory));
	}

	@Test
	public void menUseStandardBarWithoutCollarsAboveThirtyKg() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, false, false, false, 20);

		assertSelection(20, false, false, select(40, 20, 20, false, false, 0, 25, inventory));
	}

	@Test
	public void womenUseStandardBarWithCollarsAboveThirtyKg() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, false, false, true, 20);

		assertSelection(15, true, false, select(40, 15, 15, false, false, 0, 25, inventory));
	}

	@Test
	public void womenUseStandardBarWithoutCollarsAboveThirtyKg() {
		PlatformEquipmentCalculator.Inventory inventory = inventory(true, true, false, false, false, 20);

		assertSelection(15, false, false, select(35, 15, 15, false, false, 0, 25, inventory));
	}

	private PlatformEquipmentCalculator.Selection select(int targetWeight, int standardBarWeight,
			int maximumAllowedBarWeight, boolean useUsawCollars, boolean useNonStandardBar,
			int nonStandardBarWeight, int collarThreshold, PlatformEquipmentCalculator.Inventory inventory) {
		return select(targetWeight, standardBarWeight, maximumAllowedBarWeight, useUsawCollars, false,
				useNonStandardBar, nonStandardBarWeight, collarThreshold, inventory);
	}

	private PlatformEquipmentCalculator.Selection select(int targetWeight, int standardBarWeight,
			int maximumAllowedBarWeight, boolean useUsawCollars, boolean noCollars5kgBar,
			boolean useNonStandardBar, int nonStandardBarWeight, int collarThreshold,
			PlatformEquipmentCalculator.Inventory inventory) {
		PlatformEquipmentCalculator.Request request = new PlatformEquipmentCalculator.Request(
				targetWeight, standardBarWeight, maximumAllowedBarWeight, useUsawCollars, noCollars5kgBar,
				useNonStandardBar, nonStandardBarWeight, collarThreshold, inventory);
		return PlatformEquipmentCalculator.select(request);
	}

	private PlatformEquipmentCalculator.Inventory inventory(boolean bar20, boolean bar15, boolean bar10,
			boolean bar5, boolean collars, int smallestBumperPairWeight) {
		return new PlatformEquipmentCalculator.Inventory(
				bar20, bar15, bar10, bar5, collars, smallestBumperPairWeight);
	}

	private void assertSelection(int barWeight, boolean useCollars, boolean lightBarInUse,
			PlatformEquipmentCalculator.Selection selection) {
		assertEquals("bar weight", barWeight, selection.barWeight());
		assertEquals("collars", useCollars, selection.useCollars());
		assertEquals("light bar", lightBarInUse, selection.lightBarInUse());
	}
}