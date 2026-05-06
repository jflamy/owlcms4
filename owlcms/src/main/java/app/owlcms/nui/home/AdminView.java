package app.owlcms.nui.home;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.Main;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.AuthorizationDispatch;
import app.owlcms.utils.RestartUtils;

@SuppressWarnings("serial")
@Route(value = "admin", layout = OwlcmsLayout.class)
public class AdminView extends Composite<VerticalLayout> implements HasDynamicTitle, AuthorizationDispatch {

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		AuthorizationDispatch.super.beforeEnter(event);
		if (!OwlcmsSession.isAuthenticated()) {
			return;
		}
		String clientIp = AccessUtils.getClientIp();
		if (!AccessUtils.checkBackdoor(clientIp)) {
			throw new AccessDeniedException();
		}
	}

	public AdminView() {
		VerticalLayout content = getContent();
		content.setPadding(true);
		content.setSpacing(true);

		H2 title = new H2("Admin");
		Button stop = new Button("Stop", event -> {
			Main.prepareForExit();
			System.exit(0);
		});
		Button restart = new Button("Restart", event -> RestartUtils.triggerRestart("Admin restart requested"));

		content.add(title, stop, restart);
	}

	@Override
	public String getPageTitle() {
		return "Admin";
	}
}