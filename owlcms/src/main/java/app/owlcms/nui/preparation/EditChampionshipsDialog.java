/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class EditChampionshipsDialog extends Dialog {

	public EditChampionshipsDialog() {
		this(null);
	}

	public EditChampionshipsDialog(Runnable closeAction) {
		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
		setWidth(EditChampionshipsPanel.DIALOG_TABLE_WIDTH);
		setMaxWidth("calc(100vw - 2rem)");
		setHeaderTitle(Translator.translate("EditChampionships.Title"));
		if (closeAction != null) {
			addOpenedChangeListener(event -> {
				if (!event.isOpened()) {
					closeAction.run();
				}
			});
		}

		add(new EditChampionshipsPanel(false));

		HorizontalLayout buttons = new HorizontalLayout();
		Button closeButton = new Button(Translator.translate("Close"), event -> close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		buttons.add(closeButton);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.END);

		getFooter().add(buttons);
	}
}