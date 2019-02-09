package org.ledocte.owlcms.ui.preparation;

import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.layout.FluentGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@SuppressWarnings("serial")
@Route(value = "preparation", layout = PreparationLayout.class)
public class PreparationSubLayout extends VerticalLayout {

	public PreparationSubLayout() {
        FluentGridLayout layout = new FluentGridLayout();
        layout.withTemplateRows(new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withTemplateColumns(new Flex(1), new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withGap(new Length("1em"))
                .withMargin(false)
                .withRowAndColumn(new Button("Competition Information", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 2, 2)
                .withRowAndColumn(new Button("Define Categories", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 2, 3)
                .withRowAndColumn(new Button("Define Groups", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 2, 4)
                .withRowAndColumn(new Button("Upload Registration File", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 3, 2)
                .withRowAndColumn(new Button("Edit Athlete Entries", buttonClickEvent -> UI.getCurrent().navigate(CategorySubLayout.class)), 3, 3)
                ;
        layout.setWidth("100%");
        layout.setHeight("100%");
		add(layout);
    }

}
