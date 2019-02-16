package org.ledocte.owlcms.ui.home;

import org.ledocte.owlcms.ui.displaySetup.DisplayNavigationContent;
import org.ledocte.owlcms.ui.lifting.LiftingNavigationContent;
import org.ledocte.owlcms.ui.preparation.PreparationNavigationContent;
import org.ledocte.owlcms.ui.wrapup.WrapupNavigationContent;

import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.MinMax;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "", layout = MainNavigationLayout.class)
public class MainNavigationContent extends VerticalLayout {

	public MainNavigationContent() {
		add(MainNavigationContent.navigationGrid(new Button("Prepare Competition",
				buttonClickEvent -> UI.getCurrent()
					.navigate(PreparationNavigationContent.class)),
			new Button("Setup Displays",
					buttonClickEvent -> UI.getCurrent()
						.navigate(DisplayNavigationContent.class)),
			new Button("Run Lifting Group",
					buttonClickEvent -> UI.getCurrent()
						.navigate(LiftingNavigationContent.class)),
			new Button("Competition Documents",
					buttonClickEvent -> UI.getCurrent()
						.navigate(WrapupNavigationContent.class))));
	}

	public static FlexibleGridLayout navigationGrid(Component... items) {
		FlexibleGridLayout layout = new FlexibleGridLayout();
		layout.withColumns(Repeat.RepeatMode.AUTO_FIT, new MinMax(new Length("350px"), new Flex(1)))
			.withAutoRows(new Length("1fr"))
			.withMargin(false)
			.withItems(items);
		layout.getStyle()
			.set("column-gap", "4vmin");
		layout.getStyle()
			.set("row-gap", "2vmin");
		layout.getStyle()
			.set("padding", "5vmin");
		layout.setWidth("100%");
		layout.setHeight("100%");
		return layout;
	
	}

}