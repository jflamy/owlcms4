/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.DisplayNavigationContent;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@Route(value = DisplayLoginView.LOGIN, layout = OwlcmsLayout.class)
public class DisplayLoginView extends LoginView {

	public static final String LOGIN = "displaylogin";

	@Override
	protected boolean checkAuthenticated(String value) {
		return !AccessUtils.checkDisplayAuthenticated(value);
	}

	@Override
	protected void redirect() {
		String requestedUrl = OwlcmsSession.getRequestedUrl();
		if (requestedUrl != null) {
			UI.getCurrent().navigate(requestedUrl, OwlcmsSession.getRequestedQueryParameters());
		} else {
			UI.getCurrent().navigate(DisplayNavigationContent.class);
		}
	}
}
