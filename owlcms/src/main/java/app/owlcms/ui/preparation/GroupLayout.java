/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.html.Label;

import app.owlcms.ui.shared.OwlcmsRouterLayout;

/**
 * The Class CategoryLayout.
 */
@SuppressWarnings("serial")
public class GroupLayout extends OwlcmsRouterLayout {

	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		variant = Behaviour.LEFT;
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		appLayout.closeDrawer();
		appLayout.setTitleComponent(new Label(getTranslation("Edit_Groups"))); //$NON-NLS-1$
		return appLayout;
	}
}
