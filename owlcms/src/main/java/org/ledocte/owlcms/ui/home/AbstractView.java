package org.ledocte.owlcms.ui.home;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

@SuppressWarnings("serial")
abstract class AbstractView extends HorizontalLayout {
    public AbstractView() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSizeFull();
        layout.setMargin(false);
        setMargin(false);
        Label label = new Label("< " + getViewName() + " >");
        layout.add(label);
        layout.setAlignItems(Alignment.CENTER);
        add(layout);
        setSizeFull();
        getElement().getStyle().set("overflow", "auto");
    }

    abstract String getViewName();
}