package app.owlcms.nui.admin;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@RequiresRole(Role.ADMIN_PAGES)
@Route(value = "admin/audit/check", layout = OwlcmsLayout.class)
public class AuditIntegrityCheckView extends Composite<VerticalLayout>
		implements BeforeEnterObserver, HasDynamicTitle {
	public AuditIntegrityCheckView() {
		getContent().add(new H2(getPageTitle()),
				new Paragraph(Translator.translate("AuditIntegrity.CheckPending")));
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		AdminView.checkLocalAccess();
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("AuditIntegrity.CheckTitle");
	}
}
