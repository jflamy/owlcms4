/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Affero GNU License amended with the
 * Commons Clause.
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.wrapup;

import org.ledocte.owlcms.displays.attemptboard.AttemptBoard;
import org.ledocte.owlcms.ui.home.MainNavigationContent;
import org.ledocte.owlcms.ui.preparation.CategoryContent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

/**
 * The Class WrapupNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "wrapup", layout = WrapupNavigationLayout.class)
public class WrapupNavigationContent extends VerticalLayout {

	/**
	 * Instantiates a new wrapup navigation content.
	 */
	public WrapupNavigationContent() {
		add(MainNavigationContent.navigationGrid(
			new Button("Competition Book", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)),
			new Button("Timing Statistics", buttonClickEvent -> UI.getCurrent().navigate(AttemptBoard.class))));
    }

}
