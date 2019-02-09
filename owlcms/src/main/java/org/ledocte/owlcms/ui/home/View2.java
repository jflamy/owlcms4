package org.ledocte.owlcms.ui.home;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.router.Route;

@Route(value = "view2", layout = MainLayout.class)
public class View2 extends AbstractView {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public View2() {
		add(new FullIronIcon("maps", "local-printshop"));
		add(new FullIronIcon("icons", "expand-more"));
        add(new Button("SubContent", buttonClickEvent -> UI.getCurrent().navigate(SubContent.class)));
    }

    @Override
    String getViewName() {
        return getClass().getName();
    }
}