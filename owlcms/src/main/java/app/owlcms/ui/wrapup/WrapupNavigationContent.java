/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.wrapup;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.ui.home.ContentWrapping;
import app.owlcms.ui.home.MainNavigationContent;
import app.owlcms.ui.preparation.CategoryContent;

/**
 * The Class WrapupNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "wrapup", layout = WrapupNavigationLayout.class)
public class WrapupNavigationContent extends VerticalLayout 
implements ContentWrapping {

	/**
	 * Instantiates a new wrapup navigation content.
	 */
	public WrapupNavigationContent() {
		FlexibleGridLayout grid = MainNavigationContent.navigationGrid(
			new Button("Competition Book", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)),
			new Button("Timing Statistics", buttonClickEvent -> UI.getCurrent().navigate(AttemptBoard.class)));
		fillH(grid, this);
    }

}
