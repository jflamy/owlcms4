package org.ledocte.owlcms.ui.displaySetup;

import org.ledocte.owlcms.displays.attemptboard.AttemptBoard;
import org.ledocte.owlcms.ui.preparation.CategorySubLayout;

import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.layout.FluentGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "displays", layout = DisplayLayout.class)
public class DisplaySubLayout extends VerticalLayout {

	public DisplaySubLayout() {
        FluentGridLayout layout = new FluentGridLayout();
        layout.withTemplateRows(new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withTemplateColumns(new Flex(1), new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withGap(new Length("1em"))
                .withMargin(false)
                .withRowAndColumn(new Button("Results Screen", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 2, 2)
                .withRowAndColumn(new Button("Attempt Board", buttonClickEvent -> UI.getCurrent().navigate(AttemptBoard.class)), 2, 3)
                .withRowAndColumn(new Button("Referee Decision Display", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 2, 4)
                .withRowAndColumn(new Button("Jury Display", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 3, 2)
                .withRowAndColumn(new Button("Plates Display", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 3, 3)
                ;
        layout.setWidth("100%");
        layout.setHeight("100%");
		add(layout);
    }

}
