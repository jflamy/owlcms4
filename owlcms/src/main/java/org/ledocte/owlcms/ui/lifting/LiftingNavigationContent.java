/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.displays.results.ResultsBoard;
import org.ledocte.owlcms.ui.home.ContentWrapping;
import org.ledocte.owlcms.ui.home.MainNavigationContent;
import org.ledocte.owlcms.ui.preparation.AthletesContent;
import org.ledocte.owlcms.ui.preparation.CategoryContent;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * The Class LiftingNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "group", layout = LiftingNavigationLayout.class)
public class LiftingNavigationContent extends VerticalLayout
		implements ContentWrapping {

	/**
	 * Instantiates a new lifting navigation content.
	 */
	public LiftingNavigationContent() {
		Button weighIn = new Button("Weigh-In and Start Numbers",
				buttonClickEvent -> UI.getCurrent()
					.navigate(AthletesContent.class));
		Button announcer = new Button("Announcer",
				buttonClickEvent -> UI.getCurrent()
					.navigate(AnnouncerContent.class));
		Button marshall = new Button("Marshall",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button timekeeper = new Button("Timekeeper",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button results = new Button("Current Results",
				buttonClickEvent -> UI.getCurrent()
					.navigate(ResultsBoard.class));
		Button print = new Button("Print Results",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		
		FlexibleGridLayout grid = MainNavigationContent.navigationGrid(
			weighIn,
			announcer,
			marshall,
			timekeeper,
			results,
			print);
		
		marshall.setEnabled(false);
		timekeeper.setEnabled(false);
		print.setEnabled(false);
		
		fillH(grid, this);
	}

}
