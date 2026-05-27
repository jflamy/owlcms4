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

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class ChampionshipDetailsDialog extends Dialog {

	public ChampionshipDetailsDialog(Championship championship, Runnable onSave) {
		championship = normalizeForEditing(championship);
		setCloseOnEsc(true);
		setCloseOnOutsideClick(true);
		boolean templateMode = championship.isCompetitionTemplate();
		setHeaderTitle((templateMode ? Translator.translate("Competition.Defaults") : championship.getName()) + " — " + Translator.translate("Sessions.EditDetails"));
		setWidth("80em");

		ChampionshipDetailsForm form = new ChampionshipDetailsForm(championship);
		add(form);

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.END);

		Button saveButton = new Button(Translator.translate("Update"), event -> {
			if (!form.save()) {
				return;
			}
			if (onSave != null) {
				onSave.run();
			}
			close();
		});
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		Button cancelButton = new Button(Translator.translate("Close"), event -> close());
		buttons.add(cancelButton, saveButton);
		getFooter().add(buttons);
	}

	private Championship normalizeForEditing(Championship championship) {
		if (championship == null || championship.isCompetitionTemplate()) {
			return championship;
		}
		ChampionshipRepository.normalizeCompetitionDefaultFlags();
		Championship refreshed = Championship.findStored(championship.getName());
		return refreshed != null ? refreshed : championship;
	}
}