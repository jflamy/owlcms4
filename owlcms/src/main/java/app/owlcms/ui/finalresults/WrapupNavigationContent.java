/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.finalresults;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.ui.home.BaseNavigationContent;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.home.NavigationLayout;
import app.owlcms.ui.preparation.CategoryContent;

/**
 * The Class WrapupNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "wrapup", layout = NavigationLayout.class)
public class WrapupNavigationContent extends BaseNavigationContent implements NavigationPage {

	/**
	 * Instantiates a new wrapup navigation content.
	 */
	public WrapupNavigationContent() {
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			new Button("Competition Book", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)),
			new Button("Timing Statistics", buttonClickEvent -> UI.getCurrent().navigate(AttemptBoard.class)));
		fillH(grid, this);
    }

	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.BaseNavigationContent#configureTopBar(java.lang.String, com.github.appreciated.app.layout.behaviour.AppLayout)
	 */
	@Override
	protected void configureTopBar(String title, AppLayout appLayout) {
		super.configureTopBar("Produce Results", appLayout);
	}

	
	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		return null;
	}
	
	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}
}
