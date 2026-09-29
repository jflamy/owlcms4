/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import com.vaadin.flow.component.UI;
import app.owlcms.access.PublicPage;
import app.owlcms.access.Principal;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.DisplayNavigationContent;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@PublicPage
@Route(value = DisplayLoginView.LOGIN, layout = OwlcmsLayout.class)
public class DisplayLoginView extends LoginView implements BeforeEnterObserver {

	public static final String LOGIN = "displaylogin";

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (Config.getCurrent().isAccountsMode()) {
			event.forwardTo(LoginView.LOGIN);
		}
	}

	@Override
	protected String loginKind() {
		return "display pin";
	}

	@Override
	protected Principal authenticatedPrincipal() {
		return Principal.displays();
	}

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
