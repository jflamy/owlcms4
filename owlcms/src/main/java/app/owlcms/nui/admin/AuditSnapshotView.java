package app.owlcms.nui.admin;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditSnapshot;
import app.owlcms.audit.AuditSnapshot.Scope;
import app.owlcms.components.elements.LazyDownloadButton;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@RequiresRole(Role.ADMIN_PAGES)
@Route(value = "admin/audit/snapshot", layout = OwlcmsLayout.class)
public class AuditSnapshotView extends Composite<VerticalLayout>
		implements BeforeEnterObserver, HasDynamicTitle {
	private final RadioButtonGroup<Scope> scope = new RadioButtonGroup<>();

	public AuditSnapshotView() {
		this.scope.setItems(Scope.values());
		this.scope.setItemLabelGenerator(value -> Translator.translate("AuditSnapshot." + value.name()));
		this.scope.setValue(Scope.LOGS_AND_EXPORT);
		this.scope.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		// the zip is built on a request thread with no current UI; capture the actor and scope on click
		AtomicReference<AuditActor> actor = new AtomicReference<>();
		AtomicReference<Scope> selected = new AtomicReference<>(this.scope.getValue());
		Div download = DownloadButtonFactory.createDynamicZipDownloadButton("audit_snapshot",
				Translator.translate("AuditSnapshot.Download"),
				() -> AuditSnapshot.stream(selected.get(), actor.get()));
		download.getChildren().filter(LazyDownloadButton.class::isInstance).findFirst()
				.ifPresent(button -> ((LazyDownloadButton) button).addClickListener(event -> {
					actor.set(AuditActor.capture(this).atStation("ADMIN", null, false));
					selected.set(this.scope.getValue());
				}));

		getContent().add(new H2(getPageTitle()),
				new Paragraph(Translator.translate("AuditSnapshot.Description")),
				this.scope, download);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		AdminView.checkLocalAccess();
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("AuditSnapshot.Title");
	}
}
