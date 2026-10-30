/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components;

import java.util.function.BooleanSupplier;

import com.vaadin.flow.component.Component;
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
	private final VerticalLayout content = new VerticalLayout();
	private BooleanSupplier confirmCheck;
	private Runnable cancelAction;

	/**
	 * Creates a confirmation dialog with a custom confirm button label.
	 *
	 * @param title              the dialog title
	 * @param question           the confirmation question/warning message, as trusted HTML (may contain
	 *                           formatting and icons). Values inserted by {@link Translator#translate} have their
	 *                           tags removed.
	 * @param confirmButtonLabel the label for the confirm button (red, dangerous action)
	 * @param cancelButtonLabel  the label for the cancel button (null uses translated default)
	 * @param successNotification the notification message shown after successful action (can be null)
	 * @param pAction            the action to run when confirmed
	 */
	public ConfirmationDialog(String title, String question, String confirmButtonLabel, String cancelButtonLabel,
	        String successNotification, Runnable pAction) {
		Dialog dialog = this;
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);

		VerticalLayout content = this.content;
		H3 title1 = new H3(title);
		title1.getStyle().set("margin-top", "0px");
		title1.getStyle().set("padding-top", "0px");

		Paragraph paragraph = new Paragraph();
		content.add(title1);
		// null when the caller adds its own body, for example markdown
		if (question != null) {
			paragraph.getElement().setProperty("innerHTML", question);
			paragraph.setWidth("550px");
			content.add(paragraph);
		}

		HorizontalLayout buttons = new HorizontalLayout();
		Button confirmButton = new Button(confirmButtonLabel, event -> {
			if (this.confirmCheck != null && !this.confirmCheck.getAsBoolean()) {
				// the check shows its own error; the dialog stays open
				return;
			}
			if (pAction != null) {
				pAction.run();
			} else if (this.action != null) {
				this.action.run();
			}
			if (successNotification != null) {
				Notification.show(successNotification);
			}
			dialog.close();
		});
		confirmButton.getElement().setAttribute("theme", "primary error");

		String effectiveCancelLabel = cancelButtonLabel != null ? cancelButtonLabel : Translator.translate("Cancel");
		Button cancelButton = new Button(effectiveCancelLabel, event -> {
			if (this.cancelAction != null) {
				this.cancelAction.run();
			}
			dialog.close();
		});
		cancelButton.getElement().setAttribute("theme", "primary");
		cancelButton.focus();
		buttons.add(confirmButton, cancelButton);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.CENTER);

		dialog.add(content);
		dialog.add(buttons);
	}

	public ConfirmationDialog(String title, String question, String confirmButtonLabel, String successNotification,
	        Runnable pAction) {
		this(title, question, confirmButtonLabel, null, successNotification, pAction);
	}

	/**
	 * Creates a confirmation dialog with default "Confirm" button label.
	 *
	 * @param title              the dialog title
	 * @param question           the confirmation question/warning message, as trusted HTML (may contain
	 *                           formatting and icons). Values inserted by {@link Translator#translate} have their
	 *                           tags removed.
	 * @param successNotification the notification message shown after successful action (can be null)
	 * @param pAction            the action to run when confirmed
	 */
	public ConfirmationDialog(String title, String question, String successNotification, Runnable pAction) {
		this(title, question, Translator.translate("Confirm"), successNotification, pAction);
	}

	public Runnable getAction() {
		return this.action;
	}

	/** Adds a component, such as an input field, below the question. */
	public void addBody(Component component) {
		this.content.add(component);
	}

	/** @param confirmCheck runs before the action; when it returns false, nothing is done and the dialog stays open */
	public void setConfirmCheck(BooleanSupplier confirmCheck) {
		this.confirmCheck = confirmCheck;
	}

	/** @param cancelAction runs when the cancel button is used, for example to undo what led to the dialog */
	public void setCancelAction(Runnable cancelAction) {
		this.cancelAction = cancelAction;
	}

	public void setAction(Runnable action) {
		this.action = action;
	}

}