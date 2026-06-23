package app.owlcms.nui.admin;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;

import app.owlcms.Main;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.websocket.WebSocketEventSender;
import app.owlcms.nui.shared.AuthorizationDispatch;
import app.owlcms.nui.shared.OwlcmsLayout;
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
		Button reloadTranslations = new Button("Reload Translation Strings", event -> {
			Translator.reset();
			WebSocketEventSender.sendTranslationsToAll();
		});
		Span reloadTranslationsNote = new Span(
		        "Reload the translation file and push updated strings to connected clients. Refresh browser pages to see the new text.");
		reloadTranslationsNote.getStyle().set("color", "var(--lumo-secondary-text-color)");
		HorizontalLayout reloadTranslationsAction = new HorizontalLayout(reloadTranslations, reloadTranslationsNote);
		reloadTranslationsAction.setAlignItems(FlexComponent.Alignment.CENTER);
		Button federationReport = new Button("Record Federation Report", event -> {
			String url = RouteConfiguration.forSessionScope().getUrl(RecordFederationComparisonReport.class);
			UI.getCurrent().getPage().open(url, "_blank");
		});
		Span federationReportNote = new Span(
		        "Show how many records are loaded for each federation, and how many athletes are eligible.");
		federationReportNote.getStyle().set("color", "var(--lumo-secondary-text-color)");
		HorizontalLayout federationReportAction = new HorizontalLayout(federationReport, federationReportNote);
		federationReportAction.setAlignItems(FlexComponent.Alignment.CENTER);
		Button repairBirthDates = new Button("Repair Birth Dates", event -> {
			BirthDateRepairDialog dialog = new BirthDateRepairDialog(event.getSource());
			dialog.open();
		});
		Span repairBirthDatesNote = new Span(
		        "Repair dates that were set in the past (previous day or previous year) during the registration process");
		repairBirthDatesNote.getStyle().set("color", "var(--lumo-secondary-text-color)");
		HorizontalLayout repairBirthDatesAction = new HorizontalLayout(repairBirthDates, repairBirthDatesNote);
		repairBirthDatesAction.setAlignItems(FlexComponent.Alignment.CENTER);

		content.add(title, stop, restart, reloadTranslationsAction, federationReportAction, repairBirthDatesAction);
	}

	@Override
	public String getPageTitle() {
		return "Admin";
	}
}