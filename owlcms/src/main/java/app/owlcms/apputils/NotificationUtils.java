/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

}
