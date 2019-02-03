package org.ledocte.owlcms.ui.home;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
public class View1 extends VerticalLayout {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public View1() {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.getStyle().set("border", "1px black solid").set("padding", "10px").set("margin", "0px");
        add(horizontalLayout);
        horizontalLayout.add(new Button("Test"), new Checkbox("My Checkbox"));
        setMargin(false);
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        setFlexGrow(1, horizontalLayout);
        setSizeFull();
    }

}