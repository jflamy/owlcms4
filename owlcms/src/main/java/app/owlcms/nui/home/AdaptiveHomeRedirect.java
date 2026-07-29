/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.WindowSize;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import app.owlcms.nui.home.navigation.RefereeNavigationContent;

/**
 * Chooses the initial home screen from the browser viewport.
 */
@SuppressWarnings("serial")
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

		WindowSize windowSize = event.getUI().getPage().windowSizeSignal().peek();
		if (windowSize.width() < REFEREE_JURY_HOME_MAXIMUM_WIDTH) {
			event.forwardTo(RefereeNavigationContent.class);
		} else {
			event.forwardTo(HomeNavigationContent.class);
		}
	}
}