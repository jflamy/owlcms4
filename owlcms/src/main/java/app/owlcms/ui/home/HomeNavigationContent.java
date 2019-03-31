/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.home;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.css.grid.GridLayoutComponent.AutoFlow;
import com.github.appreciated.css.grid.GridLayoutComponent.Overflow;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.MinMax;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.ui.displays.DisplayNavigationContent;
import app.owlcms.ui.group.GroupNavigationContent;
import app.owlcms.ui.preparation.PreparationNavigationContent;
import app.owlcms.ui.results.ResultsNavigationContent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class HomeNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "", layout = OwlcmsRouterLayout.class)
public class HomeNavigationContent extends BaseNavigationContent {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(HomeNavigationContent.class);
	static { logger.setLevel(Level.INFO);}

	/**
	 * Instantiates a new main navigation content.
	 */
	public HomeNavigationContent() {
		Button prepare = new Button("Prepare Competition",
				buttonClickEvent -> UI.getCurrent()
					.navigate(PreparationNavigationContent.class));
		Button lifting = new Button("Run Lifting Group",
				buttonClickEvent -> UI.getCurrent()
					.navigate(GroupNavigationContent.class));
		Button displays = new Button("Start Displays",
			buttonClickEvent -> UI.getCurrent()
				.navigate(DisplayNavigationContent.class));
		Button documents = new Button("Final Documents",
				buttonClickEvent -> UI.getCurrent()
					.navigate(ResultsNavigationContent.class));
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			prepare,
			lifting,
			displays,
			documents);

		documents.setEnabled(false);
		
		fillH(grid, this);
	}

	/**
	 * Navigation grid.
	 *
	 * @param items the items
	 * @return the flexible grid layout
	 */
	public static FlexibleGridLayout navigationGrid(Component... items) {
		FlexibleGridLayout layout = new FlexibleGridLayout();
		layout.withColumns(Repeat.RepeatMode.AUTO_FILL, new MinMax(new Length("300px"), new Flex(1)))
			.withAutoRows(new Length("1fr"))
			.withItems(items)
			.withGap(new Length("2vmin"))
			.withOverflow(Overflow.AUTO)
			.withAutoFlow(AutoFlow.ROW)
			.withMargin(false)
			.withPadding(true)
			.withSpacing(false);
		layout.setSizeUndefined();
		layout.setWidth("80%");
		layout.setBoxSizing(BoxSizing.BORDER_BOX);
		return layout;
	}
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.BaseNavigationContent#configureTopBar(java.lang.String, com.github.appreciated.app.layout.behaviour.AppLayout)
	 */
	@Override
	protected void createTopBar(String title) {
		super.createTopBar("OWLCMS - Olympic Weightlifting Competition Management System");
	}

	/**
	 * The left part of the top bar.
	 * @param topBarTitle
	 * @param appLayoutComponent
	 */
	@Override
	protected void configureTopBarTitle(String topBarTitle) {
		AbstractLeftAppLayoutBase appLayout = getAppLayout();
		appLayout.getTitleWrapper()
		.getElement()
		.getStyle()
		.set("flex", "0 1 40em");
		Label label = new Label(topBarTitle);
		appLayout.setTitleComponent(label);
	}
	
	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		return null;
	}
	
	@Override
	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		return null;
	}
	
	@Override
	public boolean isIgnoreFop() {
		return true;
	}

}