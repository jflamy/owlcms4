/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.results;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.ui.home.BaseNavigationContent;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.home.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class ResultsNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "results", layout = OwlcmsRouterLayout.class)
public class ResultsNavigationContent extends BaseNavigationContent implements NavigationPage {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(ResultsNavigationContent.class);
	static { logger.setLevel(Level.DEBUG);}

	/**
	 * Instantiates a new wrapup navigation content.
	 */
	public ResultsNavigationContent() {
		Button groupResults = new Button("Group Results",
			buttonClickEvent -> UI.getCurrent().navigate(ResultsContent.class));
		Button finalPackage = new Button("Final Results Package", 
			buttonClickEvent -> UI.getCurrent().navigate(ResultsContent.class));
		Button timingStats = new Button("Timing Statistics", 
			buttonClickEvent -> UI.getCurrent().navigate(ResultsContent.class));
		
		finalPackage.setEnabled(false);
		timingStats.setEnabled(false);
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			groupResults,
			finalPackage,
			timingStats
			);
		fillH(grid, this);
    }

	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.BaseNavigationContent#configureTopBar(java.lang.String, com.github.appreciated.app.layout.behaviour.AppLayout)
	 */
	@Override 
	protected void createTopBar(String title) {
		super.createTopBar("Produce Results");
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
