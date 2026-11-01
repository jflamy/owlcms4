package app.owlcms.nui.home;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ControlPanelVersionTest {

	@Test
	public void olderControlPanelsAreTooOld() {
		assertTrue(HomeNavigationContent.isControlPanelTooOld("3.9.0"));
		assertTrue(HomeNavigationContent.isControlPanelTooOld("3.7.0"));
		assertTrue(HomeNavigationContent.isControlPanelTooOld("v2.4.1"));
		assertTrue("before the required prerelease", HomeNavigationContent.isControlPanelTooOld("3.10.0-alpha00"));
		assertTrue("not notarized", HomeNavigationContent.isControlPanelTooOld("3.10.0-alpha01"));
	}

	@Test
	public void theRequiredPrereleaseAndLaterAreAccepted() {
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.0-alpha02"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("v3.10.0-alpha02"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.0-alpha03"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.0-beta01"));
		assertFalse("development builds report a snapshot",
		        HomeNavigationContent.isControlPanelTooOld("3.10.0-SNAPSHOT"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.0"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.1"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("4.0.0"));
	}
}
