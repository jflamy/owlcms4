/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.displayselection;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.displays.attemptboard.AthleteFacingAttemptBoard;
import app.owlcms.displays.attemptboard.AthleteFacingDecisionBoard;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.scoreboard.Scoreboard;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class DisplayNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "displays", layout = OwlcmsRouterLayout.class)
public class DisplayNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final static Logger logger = (Logger)LoggerFactory.getLogger(DisplayNavigationContent.class);
	static { logger.setLevel(Level.INFO); }

	/**
	 * Instantiates a new display navigation content.
	 */
	public DisplayNavigationContent() {
		VerticalLayout intro = new VerticalLayout();
		addP(intro, getTranslation("DisplayNavigationContent.0")); //$NON-NLS-1$
		addP(intro, getTranslation("DisplayNavigationContent.1")); //$NON-NLS-1$
		intro.getElement().getStyle().set(getTranslation("DisplayNavigationContent.2"), "0"); //$NON-NLS-1$ //$NON-NLS-2$

		Button attempt = new Button(
				getTranslation("DisplayNavigationContent.4"), //$NON-NLS-1$
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(AttemptBoard.class)));
		Button scoreboard = new Button(
				getTranslation("DisplayNavigationContent.5"), //$NON-NLS-1$
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(Scoreboard.class)));
		Button referee = new Button(
			getTranslation("DisplayNavigationContent.6"), //$NON-NLS-1$
			buttonClickEvent -> UI.getCurrent().getPage()
				.executeJavaScript(getWindowOpener(AthleteFacingDecisionBoard.class)));
		Button athleteFacingAttempt = new Button(
				getTranslation("DisplayNavigationContent.7"), //$NON-NLS-1$
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(AthleteFacingAttemptBoard.class)));

		Button plates = new Button(
				getTranslation("DisplayNavigationContent.8"), //$NON-NLS-1$
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(Scoreboard.class)));

		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(
			attempt,
			scoreboard,
			plates
			);
		FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(
			referee,
			athleteFacingAttempt	
			);
		plates.setEnabled(false);

		fillH(intro, this);
		doGroup(getTranslation("DisplayNavigationContent.9"), grid1, this); //$NON-NLS-1$
		doGroup(getTranslation("DisplayNavigationContent.10"), grid2, this); //$NON-NLS-1$
	}

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#createTopBarGroupField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}

	/**
	 * @see app.owlcms.ui.shared.BaseNavigationContent#getTitle()
	 */
	@Override
	protected String getTitle() {
		return getTranslation("DisplayNavigationContent.11"); //$NON-NLS-1$
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("DisplayNavigationContent.12"); //$NON-NLS-1$
	}
	
}
