package org.ledocte.owlcms.ui.wrapup;

import org.ledocte.owlcms.ui.home.MainLayout;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.vaadin.flow.component.html.Label;

@SuppressWarnings("serial")
public class WrapupLayout extends MainLayout {

	/* (non-Javadoc)
	 * @see org.ledocte.owlcms.ui.home.MainLayout#createAppLayoutInstance()
	 */
	@Override
	public AppLayout createAppLayoutInstance() {
		AppLayout appLayout = super.createAppLayoutInstance();
		appLayout.setTitleComponent(new Label("Wrap-up Competition"));
		return appLayout;
	}
	
	

}
