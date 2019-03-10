/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.wrapup;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.html.Label;

import app.owlcms.ui.home.MainNavigationLayout;

/**
 * The Class WrapupNavigationLayout.
 */
@SuppressWarnings("serial")
public class WrapupNavigationLayout extends MainNavigationLayout {

	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.MainNavigationLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		appLayout.setTitleComponent(new Label("Wrap-up Competition"));
		return appLayout;
	}
	
	

}
