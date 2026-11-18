/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.ExtendedClientDetails;
import com.vaadin.flow.component.page.WindowSize;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.WebBrowser;

import app.owlcms.access.AuthenticatedPage;
import app.owlcms.nui.home.navigation.RefereeNavigationContent;

/**
 * Chooses the initial home screen: phones and tablets with a narrow viewport get the referee/jury home, everything
 * else (Windows, macOS, Linux, Raspberry Pi) gets the regular home.
 */
@SuppressWarnings("serial")
@AuthenticatedPage
@Route("")
public class AdaptiveHomeRedirect extends Div implements BeforeEnterObserver {

	private static final int REFEREE_JURY_HOME_MAXIMUM_WIDTH = 1200;
	private static boolean refereeJuryHomeEnabled = true;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (!refereeJuryHomeEnabled) {
			event.forwardTo(HomeNavigationContent.class);
			return;
		}

		UI ui = event.getUI();
		WindowSize windowSize = ui.getPage().windowSizeSignal().peek();
		if (windowSize.width() < REFEREE_JURY_HOME_MAXIMUM_WIDTH && isMobilePlatform(ui)) {
			event.forwardTo(RefereeNavigationContent.class);
		} else {
			event.forwardTo(HomeNavigationContent.class);
		}
	}

	private static boolean isMobilePlatform(UI ui) {
		WebBrowser browser = ui.getSession().getBrowser();
		ExtendedClientDetails details = ui.getPage().getExtendedClientDetails();
		return isMobilePlatform(
		        browser != null ? browser.getUserAgent() : null,
		        details != null ? details.getNavigatorPlatform() : null,
		        details != null && details.isTouchDevice());
	}

	/**
	 * Android is recognized from the user agent because its navigator.platform ("Linux armv8l", ...) is the same as a
	 * Raspberry Pi's. iPadOS reports "MacIntel" like a desktop Mac; only the touch support tells them apart.
	 */
	static boolean isMobilePlatform(String userAgent, String navigatorPlatform, boolean touchDevice) {
		if (userAgent != null && (userAgent.contains("Android") || userAgent.contains("iPhone")
		        || userAgent.contains("iPad") || userAgent.contains("iPod"))) {
			return true;
		}
		if (navigatorPlatform == null) {
			return false;
		}
		return navigatorPlatform.startsWith("iPhone") || navigatorPlatform.startsWith("iPad")
		        || navigatorPlatform.startsWith("iPod")
		        || (navigatorPlatform.equals("MacIntel") && touchDevice);
	}
}
