package app.owlcms.nui.admin;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.access.AccessUi;
import app.owlcms.access.HasPrincipal;
import app.owlcms.access.Principal;
import app.owlcms.access.Access;
import app.owlcms.access.RequiresAccess;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditSnapshot;
import app.owlcms.audit.AuditSnapshot.Scope;
import app.owlcms.components.elements.LazyDownloadButton;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@RequiresAccess({ Access.ADMINISTRATOR, Access.SUPERVISOR })
@Route(value = "admin/backup", layout = OwlcmsLayout.class)
public class BackupView extends Composite<VerticalLayout>
		implements HasDynamicTitle, HasPrincipal {

	/** Captured during the navigation that creates the page. */
	private final Principal principal = AccessUi.currentPrincipal();

	@Override
	public Principal getPrincipal() {
		return this.principal;
	}

	private final RadioButtonGroup<Scope> scope = new RadioButtonGroup<>();

	public BackupView() {
		this.scope.setItems(Scope.values());
		this.scope.setItemLabelGenerator(value -> Translator.translate("Backup." + value.name()));
		this.scope.setValue(Scope.LOGS_AND_EXPORT);
		this.scope.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);

		// the zip is built on a request thread with no current UI; capture the actor and scope on click
		AtomicReference<AuditActor> actor = new AtomicReference<>();
		AtomicReference<Scope> selected = new AtomicReference<>(this.scope.getValue());
		Div download = DownloadButtonFactory.createDynamicZipDownloadButton("owlcms_backup",
				Translator.translate("Backup.Download"),
				() -> AuditSnapshot.stream(selected.get(), actor.get()));
		download.getChildren().filter(LazyDownloadButton.class::isInstance).findFirst()
				.ifPresent(button -> ((LazyDownloadButton) button).addClickListener(event -> {
					actor.set(AuditActor.capture(this).atStation("ADMIN", null, false));
					selected.set(this.scope.getValue());
				}));

		// the description holds a <br> between its two sentences
		Paragraph description = new Paragraph();
		description.getElement().setProperty("innerHTML", Translator.translate("Backup.Description"));
		getContent().add(new H2(getPageTitle()), description, this.scope, download);
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Backup.Title");
	}
}
