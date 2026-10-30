package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import app.owlcms.access.IwfComplianceCheck.Item;
import app.owlcms.access.IwfComplianceCheck.Requirement;
import app.owlcms.access.IwfComplianceCheck.Status;

public class IwfComplianceCheckTest {

	private static Status status(List<Item> items, Requirement requirement) {
		return items.stream().filter(i -> i.requirement() == requirement).findFirst().orElseThrow().status();
	}

	@Test
	public void everyRequirementMet() {
		List<Item> items = IwfComplianceCheck.evaluate(true, true, false, true);
		assertEquals(3, items.size());
		assertTrue(items.stream().allMatch(Item::isMet));
	}

	@Test
	public void eachRequirementIsReportedOnItsOwn() {
		List<Item> items = IwfComplianceCheck.evaluate(false, false, false, false);
		assertEquals(Status.NOT_MET, status(items, Requirement.USER_ACCOUNTS));
		assertEquals("the built-in key does not count", Status.NOT_MET, status(items, Requirement.AUDIT_KEY));
		assertEquals(Status.NOT_MET, status(items, Requirement.CONTROL_PANEL_TOKEN));
	}

	@Test
	public void aPendingKeyCountsAfterTheNextRestart() {
		assertEquals(Status.AFTER_RESTART,
		        status(IwfComplianceCheck.evaluate(true, false, true, true), Requirement.AUDIT_KEY));
		assertEquals("a real key in use wins", Status.MET,
		        status(IwfComplianceCheck.evaluate(true, true, true, true), Requirement.AUDIT_KEY));
	}
}
