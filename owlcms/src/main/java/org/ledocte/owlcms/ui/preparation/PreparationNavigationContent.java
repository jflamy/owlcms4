/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.preparation;

import org.ledocte.owlcms.ui.home.MainNavigationContent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * The Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation", layout = PreparationNavigationLayout.class)
public class PreparationNavigationContent extends VerticalLayout {

	/**
	 * Instantiates a new preparation navigation content.
	 */
	public PreparationNavigationContent() {
		add(MainNavigationContent.navigationGrid(
			new Button("Competition Information",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Define Categories",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Define Groups",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Upload Registration File",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Edit Athlete Entries",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class))));
	}

}
