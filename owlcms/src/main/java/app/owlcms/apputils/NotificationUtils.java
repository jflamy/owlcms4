/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.i18n.Translator;
import app.owlcms.fieldofplay.FieldOfPlay;

public class NotificationUtils {

	/**
	 * Call on the UI thread after leaving the FOP monitor. Reports an unexpected failure without claiming whether
	 * persistence completed or attempting recovery. The displayed timestamp is also included in the error log.
	 */
	public static void unexpectedError(Logger logger, Throwable error, FieldOfPlay fop, Long athleteId) {
		String occurredAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
		logger.error("{}Unexpected error at {} athleteId={}", FieldOfPlay.getLoggingName(fop), occurredAt, athleteId,
		        error);
		centeredError(Translator.translate("Error.UnexpectedAt", occurredAt));
	}

	public static void errorNotification(String labelText) {
		Notification error = new Notification();
		error.addThemeVariants(NotificationVariant.LUMO_ERROR);
		Button button = new Button(Translator.translate("GotIt"), (c) -> {
			error.close();
		});
		button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		NativeLabel label = new NativeLabel(labelText);
		HorizontalLayout layout = new HorizontalLayout(label, button);
		layout.setSpacing(true);
		error.add(layout);
		error.setPosition(Position.MIDDLE);
		error.open();
	}

	/**
	 * Plain bottom-left notification that stays up until the server goes down. Call on the UI thread; it is pushed
	 * immediately because the caller usually blocks or exits right after.
	 */
	public static void restarting(UI ui) {
		Notification n = new Notification(Translator.translate("Platform.Restarting"));
		n.setPosition(Position.BOTTOM_START);
		n.setDuration(0);
		n.open();
		if (ui != null) {
			ui.push();
		}
	}

	/**
	 * Centered, large notification for a step that the user must not miss, such as the restart that follows an
	 * import. It stays until the caller closes it or the server goes down. Call on the UI thread or from
	 * {@code ui.access}.
	 *
	 * @param text already translated, with the locale captured where the action was started
	 */
	public static Notification centeredStatus(String text) {
		Notification n = new Notification();
		Div div = new Div();
		div.setText(text);
		div.getStyle().set("font-size", "x-large");
		div.getStyle().set("font-weight", "bold");
		div.getStyle().set("padding", "1em 2em");
		n.add(div);
		n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
		n.setPosition(Position.MIDDLE);
		n.setDuration(0);
		n.open();
		return n;
	}

	/** Confirms a save or delete made in an editing form; one notification per operation, bottom left. */
	public static void crudResult(String text) {
		Notification.show(text, 3000, Position.BOTTOM_START);
	}

	/** Confirms an update made in an editing form. */
	public static void saved() {
		crudResult(Translator.translate("Crud.Saved"));
	}

	/**
	 * A save refused because the same settings were saved after the page was opened. Nothing was written.
	 *
	 * @param savedBy who made that save; null or blank when unknown
	 */
	public static void staleSettings(String savedBy) {
		centeredError(savedBy == null || savedBy.isBlank()
		        ? Translator.translate("ConcurrentEdit.Settings.Changed")
		        : Translator.translate("ConcurrentEdit.Settings.SavedBy", savedBy));
	}

	/**
	 * Centered red notification that stays until the user clicks it (dismissal ✕ shown).
	 */
	public static Notification centeredError(String text) {
		Notification n = new Notification();
		Div div = new Div();
		div.setText(text + "\u00A0\u00A0\u00A0\u2715");
		div.getStyle().set("font-size", "large");
		div.getStyle().set("cursor", "pointer");
		div.addClickListener(click -> n.close());
		n.add(div);
		n.addThemeVariants(NotificationVariant.LUMO_ERROR);
		n.setPosition(Position.MIDDLE);
		n.setDuration(0);
		n.open();
		return n;
	}

}
