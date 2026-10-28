/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

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

public class NotificationUtils {

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

	/** Confirms a save or delete made in an editing form; one notification per operation, bottom left. */
	public static void crudResult(String text) {
		Notification.show(text, 3000, Position.BOTTOM_START);
	}

	/** Confirms an update made in an editing form. */
	public static void saved() {
		crudResult(Translator.translate("Crud.Saved"));
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
