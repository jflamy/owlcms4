package org.ledocte.owlcms.ui.wrapup;

import org.ledocte.owlcms.displays.attemptboard.AttemptBoard;
import org.ledocte.owlcms.ui.home.MainNavigationContent;
import org.ledocte.owlcms.ui.preparation.CategoryContent;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "wrapup", layout = WrapupNavigationLayout.class)
public class WrapupNavigationContent extends VerticalLayout {

	public WrapupNavigationContent() {
		add(MainNavigationContent.navigationGrid(
			new Button("Competition Book", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)),
			new Button("Timing Statistics", buttonClickEvent -> UI.getCurrent().navigate(AttemptBoard.class))));
    }

}
