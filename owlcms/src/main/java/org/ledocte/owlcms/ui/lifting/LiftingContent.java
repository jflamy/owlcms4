package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.ui.preparation.CategoryContent;

import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.layout.FluentGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "groupLifting", layout = LiftingLayout.class)
public class LiftingContent extends VerticalLayout {

	public LiftingContent() {
        FluentGridLayout layout = new FluentGridLayout();
        layout.withTemplateRows(new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withTemplateColumns(new Flex(1), new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withGap(new Length("1em"))
                .withMargin(false)
                .withRowAndColumn(new Button("Weigh-In and Start Numbers", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)), 2, 2)
                .withRowAndColumn(new Button("Announcer", buttonClickEvent -> UI.getCurrent().navigate(AnnouncerContent.class)), 2, 3)
                .withRowAndColumn(new Button("Marshall", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)), 2, 4)
                .withRowAndColumn(new Button("Timekeeper", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)), 3, 2)
                .withRowAndColumn(new Button("Current Results", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)), 3, 3)
                .withRowAndColumn(new Button("Print Results", buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class)), 3, 4)
                ;
        layout.setWidth("100%");
        layout.setHeight("100%");
		add(layout);
    }

}
