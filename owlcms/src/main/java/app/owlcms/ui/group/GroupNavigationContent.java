/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.group;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.ui.home.ContentWrapping;
import app.owlcms.ui.home.MainNavigationContent;

/**
 * The Class GroupNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "group", layout = GroupNavigationLayout.class)
public class GroupNavigationContent extends VerticalLayout
		implements ContentWrapping {

	/**
	 * Instantiates a new lifting navigation content.
	 */
	public GroupNavigationContent() {
		Button announcer = new Button("Announcer",
				buttonClickEvent -> UI.getCurrent()
					.navigate(AnnouncerContent.class));
		Button marshall = new Button("Marshall",
				buttonClickEvent -> UI.getCurrent()
					.navigate(MarshallContent.class));
		Button timekeeper = new Button("Timekeeper",
				buttonClickEvent -> UI.getCurrent()
					.navigate(TimekeeperContent.class));
		Button results = new Button("Results",
				buttonClickEvent -> UI.getCurrent()
					.navigate(ResultsContent.class));
		
		FlexibleGridLayout grid = MainNavigationContent.navigationGrid(
			announcer,
			marshall,
			timekeeper,
			results
			);
		
		fillH(grid, this);
	}

}
