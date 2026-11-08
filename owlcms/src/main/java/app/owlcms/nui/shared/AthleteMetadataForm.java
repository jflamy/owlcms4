/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.access.AccessUi;
import app.owlcms.access.Principal;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteEditAccess;
import app.owlcms.data.athlete.AthleteMetadata;
import app.owlcms.data.athlete.AthleteMetadataEdit;
import app.owlcms.data.athlete.AthleteMetadataEdit.Opening;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.EligibleForIndividualRankingStatus;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Logger;

/**
 * The restricted athlete form: first name, last name, team, record federations and individual eligibility, and nothing
 * else. Replaces the full registration form while the athlete's session is in progress.
 * Lifting cards remain separate and never open this form.
 * <p>
 * Everything the save needs is captured here, on the UI thread, when the form is opened. The save itself runs in a
 * background task and the result is applied back on the UI thread. Unexpected errors are reported, not recovered.
 */
public final class AthleteMetadataForm {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AthleteMetadataForm.class);

	public record EmbeddedEditor(Component component, BooleanSupplier hasChanges) {
	}

	private AthleteMetadataForm() {
	}

	/**
	 * @param stored freshly loaded athlete; the metadata and timestamp must come from the same row
	 * @param cancel closes the containing form without saving
	 * @param saved runs the containing grid's normal post-save callback
	 */
	public static Component create(Athlete stored, Runnable cancel, Runnable saved) {
		return create(stored, cancel, saved, false);
	}

	public static Component create(Athlete stored, Runnable cancel, Runnable saved, boolean readOnly) {
		return createEditor(stored, cancel, saved, readOnly, null).component();
	}

	/** Shares the owning editor's save lock and keeps both drafts open after an information save. */
	public static EmbeddedEditor createEmbedded(Athlete stored, Runnable cancel, Runnable saved, boolean readOnly,
	        FormSaveTask task) {
		return createEditor(stored, cancel, saved, readOnly, task);
	}

	private static EmbeddedEditor createEditor(Athlete stored, Runnable cancel, Runnable saved, boolean readOnly,
	        FormSaveTask sharedTask) {
		Principal editor = AccessUi.currentPrincipal();
		boolean editable = !readOnly && AthleteEditAccess.metadata(editor);
		Long athleteId = stored.getId();
		Opening opening = new Opening(athleteId, stored.getFullName(),
		        AthleteMetadataEdit.normalize(AthleteMetadata.from(stored)), stored.getMetadataUpdatedAt());
		AuditOrigin origin = AuditOrigin.fromUi("Athlete name, team and records form");
		AthleteMetadata initial = opening.values();
		AtomicReference<Opening> baseline = new AtomicReference<>(opening);
		boolean keepOpen = sharedTask != null;

		TextField lastName = new TextField();
		lastName.setValue(initial.lastName());
		lastName.setWidthFull();
		TextField firstName = new TextField();
		firstName.setValue(initial.firstName());
		firstName.setWidthFull();

		ComboBox<String> team = new ComboBox<>();
		List<String> allTeams = new ArrayList<>(AthleteRepository.findAllTeams());
		allTeams.sort(new NaturalOrderComparator<>());
		team.setItems(allTeams);
		team.setAllowCustomValue(true);
		team.addCustomValueSetListener(e -> team.setValue(e.getDetail()));
		team.setClearButtonVisible(true);
		team.getStyle().set("--vaadin-combo-box-overlay-width", "30em");
		team.setWidthFull();
		team.setValue(initial.team());

		TextField federationCodes = new TextField();
		federationCodes.setValue(initial.federationCodes());
		federationCodes.setHelperText(Translator.translate("Registration.FederationCodes"));
		federationCodes.setWidthFull();

		ComboBox<EligibleForIndividualRankingStatus> eligibility = new ComboBox<>();
		eligibility.setItems(EligibleForIndividualRankingStatus.values());
		eligibility.setItemLabelGenerator(s -> Translator.translate("Eligibility." + s.name()));
		eligibility.setAllowCustomValue(false);
		eligibility.setValue(initial.individualEligibilityStatus());
		eligibility.setWidthFull();
		Supplier<AthleteMetadata> currentValues = () -> AthleteMetadataEdit.fromForm(firstName.getValue(),
		        lastName.getValue(), team.getValue(), federationCodes.getValue(), eligibility.getValue());

		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new ResponsiveStep("0", 1));
		layout.addFormItem(lastName, Translator.translate("LastName"));
		layout.addFormItem(firstName, Translator.translate("FirstName"));
		layout.addFormItem(team, Translator.translate("Team"));
		layout.addFormItem(federationCodes, Translator.translate("Registration.Federations"));
		layout.addFormItem(eligibility, Translator.translate("Eligibility.Status"));

		VerticalLayout content = new VerticalLayout();
		FieldOfPlay lifting = OwlcmsFactory.getFOPSessionInProgress(stored.getGroup());
		if (lifting != null) {
			content.add(new Paragraph(LiftingSessionGuard.message(lifting, LiftingSessionGuard.SESSION_IN_PROGRESS_NOTICE)));
		}
		content.add(new H3(Translator.translate("Athlete.InformationTab")), layout);
		content.setPadding(false);
		content.setWidth("40em");
		content.setMaxWidth("100%");
		FormSaveTask task = keepOpen ? sharedTask : new FormSaveTask(content);

		Button cancelButton = new Button(Translator.translate("Cancel"), e -> cancel.run());
		Button save = new Button(Translator.translate("Save"), e -> {
			Opening submitted = baseline.get();
			if (eligibility.isEmpty()) {
				eligibility.setInvalid(true);
				eligibility.setErrorMessage(Translator.translate("ThisFieldIsRequired"));
				return;
			}
			AthleteMetadata values = currentValues.get();
			if (values.equals(submitted.values())) {
				if (!keepOpen) {
					cancel.run();
				}
				return;
			}
			task.execute(() -> {
				AthleteEditAccess.requireMetadata(editor);
				if (!editable) {
					throw new SecurityException("Athlete metadata is read-only");
				}
				return AthleteMetadataEdit.save(submitted, values, origin);
			}, result -> {
				if (result.stale()) {
					if (!keepOpen) {
						cancel.run();
					}
					NotificationUtils.centeredError(Translator.translate("Athlete.MetadataStale", submitted.fullName()));
				} else {
					Athlete committed = result.athlete();
					baseline.set(new Opening(athleteId, committed.getFullName(),
					        AthleteMetadataEdit.normalize(AthleteMetadata.from(committed)), committed.getMetadataUpdatedAt()));
					NotificationUtils.crudResult(Translator.translate("Athlete.MetadataSaved", committed.getFullName()));
					saved.run();
				}
			}, error -> NotificationUtils.unexpectedError(logger, error, null, athleteId));
		});
		save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		save.setVisible(editable);
		lastName.setReadOnly(!editable);
		firstName.setReadOnly(!editable);
		team.setReadOnly(!editable);
		federationCodes.setReadOnly(!editable);
		eligibility.setReadOnly(!editable);
		HorizontalLayout footer = new HorizontalLayout(cancelButton, save);
		footer.setWidthFull();
		footer.setJustifyContentMode(JustifyContentMode.END);
		footer.getStyle().set("margin-top", "auto");
		content.add(footer);
		if (!keepOpen) {
			content.addAttachListener(e -> lastName.focus());
		}
		return new EmbeddedEditor(content, () -> !currentValues.get().equals(baseline.get().values()));
	}
}
