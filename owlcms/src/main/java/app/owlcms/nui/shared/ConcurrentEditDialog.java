/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.ConcurrentEditCheck;
import app.owlcms.data.athlete.ConcurrentEditCheck.Conflict;
import app.owlcms.data.athlete.ConcurrentEditCheck.SaveAttempt;
import app.owlcms.audit.AthleteDiff.Snapshot;
import app.owlcms.i18n.Translator;

/**
 * Confirmation shown when an athlete card is saved after the athlete was changed elsewhere.
 */
public final class ConcurrentEditDialog {

	private static final Map<String, String> LABEL_KEYS = Map.ofEntries(
	        Map.entry("lastName", "LastName"),
	        Map.entry("firstName", "FirstName"),
	        Map.entry("birthDate", "BirthDate"),
	        Map.entry("birthYear", "YearOfBirth"),
	        Map.entry("session", "Group"),
	        Map.entry("category", "Category"),
	        Map.entry("startNumber", "StartNumber"),
	        Map.entry("lotNumber", "Lot"),
	        Map.entry("entryTotal", "EntryTotal"),
	        Map.entry("bodyWeight", "BodyWeight"),
	        Map.entry("forcedAsCurrent", "ForcedAsCurrent"),
	        Map.entry("coach", "Coach"),
	        Map.entry("custom1", "Custom1.Title"),
	        Map.entry("custom2", "Custom2.Title"),
	        Map.entry("federationCodes", "Registration.Federations"),
	        Map.entry("customScore", "Score"),
	        Map.entry("declaration", "Declaration"),
	        Map.entry("change1", "Change_1"),
	        Map.entry("change2", "Change_2"),
	        Map.entry("actualLift", "WeightLifted"));

	private ConcurrentEditDialog() {
	}

	/**
	 * Run the save unless it would overwrite changes made elsewhere since the form was opened; otherwise list those
	 * fields and let the user save anyway, discard the form, or go back to it. Going back keeps the form as is: values
	 * re-entered to match the stored ones are no longer conflicts on the next save.
	 *
	 * @param check       created when the persisted athlete card was opened
	 * @param formValues  the athlete holding the values that the save will write
	 * @param athleteName shown as the dialog heading
	 * @param save        the normal save action of the form
	 * @param discard     closes the form without saving
	 */
	public static void saveUnlessConflicting(FormSaveTask task, ConcurrentEditCheck check, Object monitor,
	        Athlete formValues, String athleteName, BooleanSupplier valid, Runnable save, Runnable saved, Runnable discard,
	        Consumer<Exception> failed) {
		attemptSave(task, check, monitor, formValues, athleteName, null, valid, save, saved, discard, failed);
	}

	private static void attemptSave(FormSaveTask task, ConcurrentEditCheck check, Object monitor, Athlete formValues,
	        String athleteName, Snapshot accepted, BooleanSupplier valid, Runnable save, Runnable saved, Runnable discard,
	        Consumer<Exception> failed) {
		if (!valid.getAsBoolean()) {
			return;
		}
		task.execute(() -> check.trySave(monitor, formValues, accepted, task::isActive, save), result -> {
			if (result.saved()) {
				saved.run();
			} else if (!result.conflicts().isEmpty()) {
				showConflicts(task, check, monitor, formValues, athleteName, result, valid, save, saved, discard, failed);
			}
		}, failed);
	}

	private static void showConflicts(FormSaveTask task, ConcurrentEditCheck check, Object monitor, Athlete formValues,
	        String athleteName, SaveAttempt attempt, BooleanSupplier valid, Runnable save, Runnable saved, Runnable discard,
	        Consumer<Exception> failed) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(Translator.translate("ConcurrentEdit.Title"));
		dialog.setCloseOnOutsideClick(false);
		dialog.setCloseOnEsc(true);
		dialog.setWidth("60em");

		Grid<Conflict> grid = new Grid<>();
		grid.addColumn(c -> label(c.field())).setHeader(Translator.translate("ConcurrentEdit.Field"))
		        .setAutoWidth(true);
		grid.addColumn(c -> display(c.whenOpened())).setHeader(Translator.translate("ConcurrentEdit.WhenOpened"))
		        .setAutoWidth(true);
		grid.addColumn(c -> display(c.current())).setHeader(Translator.translate("ConcurrentEdit.Now"))
		        .setAutoWidth(true);
		grid.addColumn(c -> display(c.inForm())).setHeader(Translator.translate("ConcurrentEdit.InForm"))
		        .setAutoWidth(true);
		grid.setItems(attempt.conflicts());
		grid.setAllRowsVisible(true);

		VerticalLayout content = new VerticalLayout(new H3(athleteName),
		        new Paragraph(Translator.translate("ConcurrentEdit.Explanation")), grid);
		content.setPadding(false);
		dialog.add(content);

		Button saveAnyway = new Button(Translator.translate("ConcurrentEdit.SaveAnyway"), e -> {
			dialog.close();
			attemptSave(task, check, monitor, formValues, athleteName, attempt.current(), valid, save, saved, discard, failed);
		});
		saveAnyway.addThemeVariants(ButtonVariant.LUMO_ERROR);
		Button discardChanges = new Button(Translator.translate("ConcurrentEdit.Discard"), e -> {
			dialog.close();
			discard.run();
		});
		Button back = new Button(Translator.translate("ConcurrentEdit.Back"), e -> dialog.close());
		back.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		HorizontalLayout buttons = new HorizontalLayout(saveAnyway, discardChanges, back);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.END);
		dialog.getFooter().add(buttons);
		dialog.open();
		back.focus();
	}

	public static void refuseRegistration(String athleteName, Runnable closeForm) {
		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(Translator.translate("ConcurrentEdit.Title"));
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.add(new H3(athleteName), new Paragraph(Translator.translate("ConcurrentEdit.RegistrationStale")));
		Button close = new Button(Translator.translate("Close"), e -> {
			dialog.close();
			closeForm.run();
		});
		close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		dialog.getFooter().add(close);
		dialog.open();
		close.focus();
	}

	private static String display(Object value) {
		if (value == null) {
			return "–";
		}
		if (value instanceof Boolean b) {
			return b ? "✔" : "✘";
		}
		String s = String.valueOf(value);
		return s.isBlank() ? "–" : s;
	}

	private static String label(String field) {
		int dot = field.indexOf('.');
		if (dot > 0) {
			String attempt = field.substring(0, dot);
			String part = field.substring(dot + 1);
			String attemptKey = attempt.startsWith("snatch") ? "Snatch" + attempt.substring(6)
			        : "C_and_J_" + attempt.substring(9);
			return Translator.translate(attemptKey) + " – " + translateField(part);
		}
		return translateField(field);
	}

	private static String translateField(String field) {
		String key = LABEL_KEYS.get(field);
		return key != null ? Translator.translate(key) : field;
	}

}
