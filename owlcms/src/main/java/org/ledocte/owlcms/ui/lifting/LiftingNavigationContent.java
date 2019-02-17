/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.ui.home.MainNavigationContent;
import org.ledocte.owlcms.ui.preparation.CategoryContent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * The Class LiftingNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "groupLifting", layout = LiftingNavigationLayout.class)
public class LiftingNavigationContent extends VerticalLayout {

	/**
	 * Instantiates a new lifting navigation content.
	 */
	public LiftingNavigationContent() {
		add(MainNavigationContent.navigationGrid(
			new Button("Weigh-In and Start Numbers",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Announcer",
					buttonClickEvent -> UI.getCurrent()
						.navigate(AnnouncerContent.class)),
			new Button("Marshall",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Timekeeper",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Current Results",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Print Results",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class))));
	}

}
