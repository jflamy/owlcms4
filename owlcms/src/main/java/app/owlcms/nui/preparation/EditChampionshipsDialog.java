/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Arrays;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class EditChampionshipsDialog extends Dialog {
	VerticalLayout championshipsTable = new VerticalLayout();

	public EditChampionshipsDialog(AgeGroupContent ageGroupContent) {
		Dialog dialog = this;
		dialog.setCloseOnEsc(true);
		dialog.setCloseOnOutsideClick(true);

		dialog.setHeaderTitle(Translator.translate("EditChampionships.Title"));
		VerticalLayout content = new VerticalLayout();

		updateChampionshipsTable(this.championshipsTable);
		content.add(this./* paragraph, */championshipsTable);

		HorizontalLayout buttons = new HorizontalLayout();
		Button closeButton = new Button(Translator.translate("Close"), event -> {
			dialog.close();
			ageGroupContent.getCrud().refreshGrid();

		});
		closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		buttons.add(closeButton);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(JustifyContentMode.END);

		dialog.add(content);
		dialog.getFooter().add(buttons);

	}

	public void updateChampionshipsTable(VerticalLayout championshipsTable) {
		championshipsTable.removeAll();
		Championship.getMap().values().stream().sorted((o1,o2)-> o1.getName().compareToIgnoreCase(o2.getName())).forEach(c -> {
			TextField nameField = new TextField();
			nameField.setValue(c.getName());
			ComboBox<ChampionshipType> typeField = createTypeField();
			typeField.setValue(c.getType());
			Button update = new Button(Translator.translate("Update"), e -> {
				c.setType(typeField.getValue());
				c.setName(nameField.getValue());
				updateChampionshipsTable(championshipsTable);
			});
			Button delete = new Button(Translator.translate("Delete"), VaadinIcon.TRASH.create(), e -> {
				Championship.remove(c);
				updateChampionshipsTable(championshipsTable);
			});
			delete.addThemeVariants(ButtonVariant.LUMO_ERROR);
			HorizontalLayout ctRow = new HorizontalLayout(nameField, typeField, update, delete);
			championshipsTable.add(ctRow);
		});
		HorizontalLayout addRow = new HorizontalLayout();
		TextField nameField = new TextField();
		ComboBox<ChampionshipType> typeField = createTypeField();
		typeField.setValue(ChampionshipType.U);
		Button addButton = new Button(Translator.translate("Add"), VaadinIcon.PLUS.create(), e -> {
			Championship.addChampionship(nameField.getValue(), typeField.getValue());
			updateChampionshipsTable(championshipsTable);
		});
		addRow.add(nameField, typeField, addButton);
		championshipsTable.add(addRow);

	}

	private ComboBox<ChampionshipType> createTypeField() {
		ComboBox<ChampionshipType> typeField = new ComboBox<>();
		typeField.setItems(Arrays.asList(ChampionshipType.values()));
		typeField.setItemLabelGenerator(type -> {
			String translated = Translator.translateOrElseNull("Division." + type.name());
			return translated != null ? translated : type.name();
		});
		return typeField;
	}
}