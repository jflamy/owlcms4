package org.ledocte.owlcms.ui.displaySetup;

import org.ledocte.owlcms.displays.attemptboard.AttemptBoard;
import org.ledocte.owlcms.ui.home.MainNavigationContent;
import org.ledocte.owlcms.ui.preparation.CategoryContent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "displays", layout = DisplayNavigationLayout.class)
public class DisplayNavigationContent extends VerticalLayout {

	public DisplayNavigationContent() {
		add(MainNavigationContent.navigationGrid(
			new Button("Attempt Board",
					buttonClickEvent -> UI.getCurrent()
						.navigate(AttemptBoard.class)),
			new Button("Referee Decision Display",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Jury Display",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class)),
			new Button("Plates Display",
					buttonClickEvent -> UI.getCurrent()
						.navigate(CategoryContent.class))));

	}

}
