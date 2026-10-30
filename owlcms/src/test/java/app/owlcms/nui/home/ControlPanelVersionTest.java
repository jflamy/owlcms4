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
	}

	@Test
	public void theRequiredVersionItsPrereleasesAndLaterAreAccepted() {
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.0"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("v3.10.0"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.0-beta01"));
		assertFalse("development builds report a snapshot",
		        HomeNavigationContent.isControlPanelTooOld("3.10.0-SNAPSHOT"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("3.10.1"));
		assertFalse(HomeNavigationContent.isControlPanelTooOld("4.0.0"));
	}
}
