package org.ledocte.owlcms.ui.home;

import com.vaadin.flow.router.Route;

@Route(value = "view7", layout = MainLayout.class)
public class View7 extends AbstractView {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    String getViewName() {
        return getClass().getName();
    }
}