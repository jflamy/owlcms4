package app.owlcms.nui.home;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireLogin;

@SuppressWarnings("serial")
@Route(value = "admin", layout = OwlcmsLayout.class)
public class AdminView extends Composite<VerticalLayout> implements HasDynamicTitle, RequireLogin {

	public AdminView() {
		VerticalLayout content = getContent();
		content.setPadding(true);
		content.setSpacing(true);

		H2 title = new H2("Admin");
		Button stop = new Button("Stop", event -> System.exit(0));
		Button restart = new Button("Restart", event -> System.exit(1));

		content.add(title, stop, restart);
	}

	@Override
	public String getPageTitle() {
		return "Admin";
	}
}