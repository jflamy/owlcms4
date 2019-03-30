/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 *
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.displays;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.results.ResultsBoard;
import app.owlcms.ui.home.BaseNavigationContent;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.home.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class DisplayNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "displays", layout = OwlcmsRouterLayout.class)
public class DisplayNavigationContent extends BaseNavigationContent implements NavigationPage {

	Logger logger = (Logger)LoggerFactory.getLogger(DisplayNavigationContent.class);
	{ logger.setLevel(Level.DEBUG); }

	/**
	 * Instantiates a new display navigation content.
	 */
	public DisplayNavigationContent() {
		VerticalLayout intro = new VerticalLayout();
		addParagraph(intro, "Use the dropdown to select the platform where the display is located.");
		addParagraph(intro, "Use one of the buttons below to open a display.");
		intro.getElement().getStyle().set("margin-bottom", "0");

		Button attempt = new Button("Attempt Board",
			buttonClickEvent -> UI.getCurrent().getPage()
			.executeJavaScript(getWindowOpener(AttemptBoard.class)));
		Button results = new Button("Results Board",
			buttonClickEvent -> UI.getCurrent().getPage().executeJavaScript(getWindowOpener(ResultsBoard.class)));
		Button referee = new Button("Referee Decision Display",
			buttonClickEvent -> UI.getCurrent().getPage().executeJavaScript(getWindowOpener(ResultsBoard.class)));
		Button jury = new Button("Jury Display",
			buttonClickEvent -> UI.getCurrent().getPage().executeJavaScript(getWindowOpener(ResultsBoard.class)));
		Button plates = new Button("Plates Display",
			buttonClickEvent -> UI.getCurrent().getPage().executeJavaScript(getWindowOpener(ResultsBoard.class)));

		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			attempt,
			results,
			referee,
			jury,
			plates);

		referee.setEnabled(false);
		jury.setEnabled(false);
		plates.setEnabled(false);

		fillH(intro, this);
		fillH(grid, this);
	}

	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}
	
}
