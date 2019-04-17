/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.ui.preparation;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation", layout = OwlcmsRouterLayout.class)
public class PreparationNavigationContent extends BaseNavigationContent {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(PreparationNavigationContent.class);
	static { logger.setLevel(Level.INFO);}

	/**
	 * Instantiates a new preparation navigation content.
	 */
	public PreparationNavigationContent() {
		Button competition = new Button("Competition Information",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CompetitionContent.class));
		Button categories = new Button("Define Categories",
				buttonClickEvent -> UI.getCurrent()
					.navigate(CategoryContent.class));
		Button groups = new Button("Define Groups",
				buttonClickEvent -> UI.getCurrent()
					.navigate(GroupContent.class));
		Button upload = new Button("Upload Registration File",
				buttonClickEvent -> new UploadDialog().open());
		Button athletes = new Button("Edit Athlete Entries",
				buttonClickEvent -> UI.getCurrent()
					.navigate(AthletesContent.class));
		Button weighIn = new Button("Weigh-In and Start Numbers",
			buttonClickEvent -> UI.getCurrent()
				.navigate(WeighinContent.class));
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			competition,
			categories,
			groups,
			upload,
			athletes,
			weighIn);
		
		fillH(grid, this);
	}
	
	@Override
	protected String getTitle() {
		return "Prepare Competition";
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
