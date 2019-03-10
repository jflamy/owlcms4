/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.displaySetup;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.results.ResultsBoard;
import app.owlcms.ui.home.ContentWrapping;
import app.owlcms.ui.home.MainNavigationContent;
import app.owlcms.ui.preparation.CategoryContent;

/**
 * The Class DisplayNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "displays", layout = DisplayNavigationLayout.class)
public class DisplayNavigationContent extends VerticalLayout
		implements ContentWrapping {

	/**
	 * Instantiates a new display navigation content.
	 */
	public DisplayNavigationContent() {
		Button attempt = new Button("Attempt Board",
				buttonClickEvent -> UI.getCurrent()
					.navigate(AttemptBoard.class));
		Button results = new Button("Results Board",
			buttonClickEvent -> UI.getCurrent()
				.navigate(ResultsBoard.class));
		Button referee = new Button("Referee Decision Display",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button jury = new Button("Jury Display",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button plates = new Button("Plates Display",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		
		FlexibleGridLayout grid = MainNavigationContent.navigationGrid(
			attempt,
			results,
			referee,
			jury,
			plates);
		
		referee.setEnabled(false);
		jury.setEnabled(false);
		plates.setEnabled(false);
		
		fillH(grid, this);

	}

}
