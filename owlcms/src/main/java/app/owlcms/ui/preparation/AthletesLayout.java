/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.ui.preparation;

import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.html.Label;

import app.owlcms.ui.group.UIEventProcessor;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.SafeEventBusRegistration;

/**
 * The Class CategoryLayout.
 */
@SuppressWarnings("serial")
public class AthletesLayout extends OwlcmsRouterLayout implements SafeEventBusRegistration, UIEventProcessor  {

	/* (non-Javadoc)i
	 * @see app.owlcms.ui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		variant = Behaviour.LEFT;
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		appLayout.closeDrawer();
		appLayout.setTitleComponent(new Label("Edit Athletes"));
		return appLayout;
	}
}
