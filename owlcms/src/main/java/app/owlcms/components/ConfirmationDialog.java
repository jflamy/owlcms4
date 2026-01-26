/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class ConfirmationDialog extends Dialog {

	Runnable action;

	public ConfirmationDialog(String title, String question, String confirmation, Runnable pAction) {
		Dialog dialog = this;
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);

		VerticalLayout content = new VerticalLayout();
		H3 title1 = new H3(title);
		title1.getStyle().set("margin-top", "0px");
		title1.getStyle().set("padding-top", "0px");

		Paragraph paragraph = new Paragraph();
		paragraph.getElement().setProperty("innerHTML", question);
		paragraph.setWidth("550px");
		content.add(title1, paragraph);

		HorizontalLayout buttons = new HorizontalLayout();
		Button confirmButton = new Button(Translator.translate("Confirm"), event -> {
			if (pAction != null) {
				pAction.run();
			} else if (this.action != null) {
				this.action.run();
			}
			if (confirmation != null) {
				Notification.show(confirmation);
			}
			dialog.close();
		});
		confirmButton.getElement().setAttribute("theme", "primary");

		Button cancelButton = new Button(Translator.translate("Cancel"), event -> {
			dialog.close();
		});
		cancelButton.getElement().setAttribute("theme", "primary error");
		cancelButton.focus();
		buttons.add(confirmButton, cancelButton);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.CENTER);

		dialog.add(content);
		dialog.add(buttons);
	}

	public Runnable getAction() {
		return this.action;
	}

	public void setAction(Runnable action) {
		this.action = action;
	}

}