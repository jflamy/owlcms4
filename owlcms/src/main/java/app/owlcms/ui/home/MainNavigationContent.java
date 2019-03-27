/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.home;

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
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.ui.displays.DisplayNavigationContent;
import app.owlcms.ui.finalresults.WrapupNavigationContent;
import app.owlcms.ui.group.GroupNavigationContent;
import app.owlcms.ui.preparation.PreparationNavigationContent;

/**
 * The Class MainNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "", layout = MainNavigationLayout.class)
public class MainNavigationContent extends VerticalLayout implements ContentWrapping {

	/**
	 * Instantiates a new main navigation content.
	 */
	public MainNavigationContent() {
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
					.navigate(WrapupNavigationContent.class));
		FlexibleGridLayout grid = MainNavigationContent.navigationGrid(
			prepare,
			displays,
			lifting,
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

}