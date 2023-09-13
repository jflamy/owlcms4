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
		if (requestedUrl != null && requestedUrl.startsWith("displays/")) {
			// logger.debug("requestedURL starts with displays/ {}", requestedUrl);
			UI.getCurrent().navigate(requestedUrl, OwlcmsSession.getRequestedQueryParameters());
		} else {
			// logger.debug("requestedURL does NOT start with displays/ {}", requestedUrl);
			UI.getCurrent().navigate(DisplayNavigationContent.class);
		}
	}
}
