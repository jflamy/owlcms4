package org.ledocte.owlcms.ui.home;

import org.ledocte.owlcms.ui.displaySetup.DisplayContent;
import org.ledocte.owlcms.ui.lifting.LiftingContent;
import org.ledocte.owlcms.ui.preparation.CategoryContent;
import org.ledocte.owlcms.ui.preparation.PreparationContent;
import org.ledocte.owlcms.ui.wrapup.WrapupContent;

import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.layout.FluentGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
public class MainContent extends VerticalLayout {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MainContent() {
        FluentGridLayout layout = new FluentGridLayout();
        layout.withTemplateRows(new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withTemplateColumns(new Flex(1), new Flex(1), new Flex(1), new Flex(1), new Flex(1))
                .withGap(new Length("1em"))
                .withMargin(false)
                .withRowAndColumn(new Button("Prepare Competition", buttonClickEvent -> UI.getCurrent().navigate(PreparationContent.class)), 2, 2)
                .withRowAndColumn(new Button("Setup Displays", buttonClickEvent -> UI.getCurrent().navigate(DisplayContent.class)), 2, 3)
                .withRowAndColumn(new Button("Run Lifting Group", buttonClickEvent -> UI.getCurrent().navigate(LiftingContent.class)), 2, 4)
                .withRowAndColumn(new Button("Competition Documents", buttonClickEvent -> UI.getCurrent().navigate(WrapupContent.class)), 3, 2)
                ;
        layout.setWidth("100%");
        layout.setHeight("100%");
		add(layout);
    }

}