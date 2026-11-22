/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditActor;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.SessionReadiness;
import app.owlcms.fieldofplay.SessionReadiness.Availability;
import app.owlcms.fieldofplay.SessionReadiness.Evaluation;
import app.owlcms.fieldofplay.SessionReadiness.Status;
import app.owlcms.fieldofplay.SessionSelectionOrder;
import app.owlcms.i18n.Translator;

/**
 * UI gate ensuring a ready session is selected before starting introductions, countdowns, or lifting.
 * Errors with no interactive choice use {@link NotificationUtils#centeredError(String)}; missing or finished sessions
 * with ready candidates open a modal selection dialog proposing the current platform's sessions first.
 */
public final class SessionSelectionGuard {

	public enum ActionKind {
		INTRODUCTION,
		START
	}

	private SessionSelectionGuard() {
	}

	/**
	 * Verifies that {@code fop} has a ready session before running {@code onReady}.
	 * <ul>
	 * <li>{@code READY}: reloads the session into {@code fop} if weigh-in completed since load, then runs
	 * {@code onReady}.</li>
	 * <li>{@code NO_SESSION} or {@code DONE}: opens a dialog to pick a ready session (current platform first) and, on
	 * confirm, switches to it and runs {@code onReady}; if no ready session exists, shows a centered error.</li>
	 * <li>{@code NOT_READY}: refuses and shows a centered error without mutating {@code fop}.</li>
	 * </ul>
	 */
	public static void ensureReadySession(FieldOfPlay fop, Object origin, AuditActor actor, ActionKind kind,
	        Runnable onReady) {
		if (fop == null) {
			return;
		}
		Evaluation evaluation = SessionReadiness.evaluate(fop);
		switch (evaluation.status()) {
			case READY -> {
				if (evaluation.needsReload()) {
					fop.switchGroupAndApply(evaluation.group(), origin, actor, true, onReady);
				} else {
					onReady.run();
				}
			}
			case NOT_READY -> NotificationUtils.centeredError(
			        Translator.translate("SessionReadiness.NotReady", evaluation.group().getName()));
			case NO_SESSION, DONE -> promptNextSession(fop, origin, actor, kind, evaluation, onReady);
		}
	}

	/**
	 * Non-blocking warning shown when an operator directly selects a session that has no weighed-in athletes ready to
	 * lift. Finished sessions are allowed silently (e.g., for medals or corrections).
	 */
	public static void warnIfNotReady(Group selected) {
		if (selected == null) {
			return;
		}
		Evaluation evaluation = SessionReadiness.evaluate(selected);
		if (evaluation.status() == Status.NOT_READY) {
			NotificationUtils.centeredError(
			        Translator.translate("SessionReadiness.NotReady", selected.getName()));
		}
	}

	private static void promptNextSession(FieldOfPlay fop, Object origin, AuditActor actor, ActionKind kind,
	        Evaluation evaluation, Runnable onReady) {
		List<Group> candidates = SessionReadiness.findReadySessions(fop);
		if (candidates.isEmpty()) {
			NotificationUtils.centeredError(Translator.translate("SessionReadiness.NoReadySession"));
			return;
		}

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(Translator.translate("SessionReadiness.SelectSessionTitle"));
		dialog.setCloseOnOutsideClick(false);

		Paragraph prompt = new Paragraph(promptMessage(kind, evaluation));
		prompt.getStyle().set("margin-top", "0");

		Select<Group> sessionSelect = new Select<>();
		sessionSelect.setWidthFull();
		sessionSelect.setAriaLabel(Translator.translate("SessionReadiness.SelectSessionTitle"));
		showCandidates(sessionSelect, candidates, fop);

		VerticalLayout content = new VerticalLayout(prompt, sessionSelect);
		content.setPadding(false);
		content.setSpacing(true);
		content.setWidthFull();
		dialog.add(content);

		Button cancelButton = new Button(Translator.translate("Cancel"), event -> dialog.close());
		Button confirmButton = new Button(Translator.translate("Confirm"), event -> {
			Group chosen = sessionSelect.getValue();
			if (chosen == null) {
				return;
			}
			// another operator may have changed the platform's session while the dialog was open
			Evaluation platformNow = SessionReadiness.evaluate(fop);
			if (platformNow.status() != evaluation.status()
			        || !Objects.equals(platformNow.group(), evaluation.group())) {
				dialog.close();
				NotificationUtils.centeredError(
				        Translator.translate("SessionReadiness.PlatformSessionChanged", fop.getName()));
				return;
			}
			Availability availability = SessionReadiness.availabilityFor(fop, chosen);
			if (!availability.isAvailable()) {
				NotificationUtils.centeredError(unavailableMessage(chosen, availability));
				List<Group> refreshed = SessionReadiness.findReadySessions(fop);
				if (refreshed.isEmpty()) {
					dialog.close();
				} else {
					showCandidates(sessionSelect, refreshed, fop);
				}
				return;
			}
			dialog.close();
			fop.switchGroupAndApply(chosen, origin, actor, false, onReady);
		});
		confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		dialog.getFooter().add(cancelButton, confirmButton);
		dialog.open();
	}

	private static void showCandidates(Select<Group> sessionSelect, List<Group> candidates, FieldOfPlay fop) {
		String currentPlatformName = fop != null ? fop.getName() : null;
		List<List<Group>> sections = SessionSelectionOrder.groupSections(candidates, currentPlatformName, g -> false);
		List<Group> ordered = sections.stream().flatMap(List::stream).toList();
		boolean multiplePlatforms = SessionSelectionOrder.hasMultiplePlatforms(ordered);

		sessionSelect.removeAll();
		sessionSelect.setItemLabelGenerator(g -> SessionSelectionOrder.formatLabel(g, multiplePlatforms));
		sessionSelect.setItems(ordered);
		for (int i = 0; i < sections.size() - 1; i++) {
			List<Group> section = sections.get(i);
			sessionSelect.addComponents(section.get(section.size() - 1), new Hr());
		}
		if (!ordered.isEmpty()) {
			sessionSelect.setValue(ordered.get(0));
		}
	}

	private static String unavailableMessage(Group chosen, Availability availability) {
		return switch (availability.reason()) {
			case SELECTED_ELSEWHERE -> Translator.translate("SessionReadiness.SelectedOnOtherPlatform",
			        chosen.getName(), availability.holder().getName());
			case FINISHED -> Translator.translate("SessionReadiness.Finished", chosen.getName());
			default -> Translator.translate("SessionReadiness.NotReady", chosen.getName());
		};
	}

	private static String promptMessage(ActionKind kind, Evaluation evaluation) {
		if (evaluation.status() == Status.DONE && evaluation.group() != null) {
			String key = kind == ActionKind.INTRODUCTION
			        ? "SessionReadiness.FinishedForIntro"
			        : "SessionReadiness.FinishedForStart";
			return Translator.translate(key, evaluation.group().getName());
		}
		String key = kind == ActionKind.INTRODUCTION
		        ? "SessionReadiness.NotSelectedForIntro"
		        : "SessionReadiness.NotSelectedForStart";
		return Translator.translate(key);
	}
}
