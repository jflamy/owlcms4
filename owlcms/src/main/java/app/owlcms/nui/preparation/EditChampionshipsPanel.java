/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Map;
import java.util.TreeMap;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class EditChampionshipsPanel extends VerticalLayout {
	private VerticalLayout championshipsTable = new VerticalLayout();
	private Checkbox hideCompetitionDefaults;

	public EditChampionshipsPanel() {
		setPadding(false);
		setSpacing(false);
		setWidthFull();
		this.championshipsTable.setPadding(false);
		this.championshipsTable.setSpacing(false);
		this.championshipsTable.setWidthFull();
		this.championshipsTable.getStyle().set("gap", "0.25rem");

		ChampionshipRepository.normalizeCompetitionDefaultFlags();
		this.hideCompetitionDefaults = new Checkbox(Translator.translate("EditChampionships.HideCompetitionDefaults"));
		this.hideCompetitionDefaults.setValue(true);
		this.hideCompetitionDefaults.addValueChangeListener(e -> updateChampionshipsTable());

		updateChampionshipsTable();
		add(this.hideCompetitionDefaults, this.championshipsTable);
	}

	public void updateChampionshipsTable() {
		this.championshipsTable.removeAll();
		boolean hideDefaultRows = this.hideCompetitionDefaults == null
		        || Boolean.TRUE.equals(this.hideCompetitionDefaults.getValue());
		Map<String, ChampionshipCandidate> candidates = championshipCandidates();
		Map<String, Championship> explicitChampionships = explicitChampionships();

		for (ChampionshipCandidate candidate : candidates.values()) {
			Championship existing = explicitChampionships.remove(candidate.name);
			boolean usesDefaults = existing == null || existing.usesCompetitionDefaults();
			if (hideDefaultRows && usesDefaults) {
				continue;
			}
			Button championshipButton = championshipButton(candidate.name, () -> {
				Championship championship = Championship.findStored(candidate.name);
				if (championship == null) {
					championship = Championship.addChampionship(candidate.name, candidate.type);
				}
				new ChampionshipDetailsDialog(championship, this::updateChampionshipsTable).open();
			});
			HorizontalLayout ctRow = championshipRow(candidate.name, candidate.type, championshipButton);
			if (existing != null) {
				if (!existing.usesCompetitionDefaults()) {
					Button reset = resetButton(() -> {
						ChampionshipRepository.resetToCompetitionDefaults(existing);
						updateChampionshipsTable();
					});
					ctRow.add(reset);
				}
			}
			this.championshipsTable.add(ctRow);
		}

		explicitChampionships.values().stream().sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).forEach(c -> {
			if (hideDefaultRows && c.usesCompetitionDefaults()) {
				return;
			}
			Button championshipButton = championshipButton(c.getName(), () -> {
				new ChampionshipDetailsDialog(c, this::updateChampionshipsTable).open();
			});
			Button reset = resetButton(() -> {
				ChampionshipRepository.resetToCompetitionDefaults(c);
				updateChampionshipsTable();
			});
			Button delete = deleteButton(() -> {
				Championship.remove(c);
				updateChampionshipsTable();
			});
			HorizontalLayout ctRow = championshipRow(c.getName(), c.getType(), championshipButton);
			if (!c.usesCompetitionDefaults()) {
				ctRow.add(reset);
			}
			ctRow.add(delete);
			this.championshipsTable.add(ctRow);
		});
		HorizontalLayout addRow = new HorizontalLayout();
		addRow.setAlignItems(FlexComponent.Alignment.CENTER);
		addRow.setWidthFull();
		addRow.getStyle().set("margin-top", "0.5rem");
		TextField nameField = new TextField();
		nameField.setWidth("12em");
		ComboBox<ChampionshipType> typeField = createTypeField();
		typeField.setValue(ChampionshipType.U);
		Button addButton = new Button(Translator.translate("Add"), VaadinIcon.PLUS.create(), e -> {
			Championship.addChampionship(nameField.getValue(), typeField.getValue());
			updateChampionshipsTable();
		});
		addRow.add(nameField, typeField, addButton);
		this.championshipsTable.add(addRow);

	}

	private HorizontalLayout championshipRow(String name, ChampionshipType type, Button editButton) {
		Span nameText = fixedText(name, "12em");
		Span typeText = fixedText(championshipListTypeLabel(type), "7em");

		HorizontalLayout row = new HorizontalLayout(nameText, typeText, editButton);
		row.setAlignItems(FlexComponent.Alignment.CENTER);
		row.setWidthFull();
		row.getStyle().set("column-gap", "0.75rem");
		row.getStyle().set("min-height", "2.25rem");
		return row;
	}

	private Button championshipButton(String name, Runnable action) {
		Button button = new Button(Translator.translate("Edit"), VaadinIcon.PENCIL.create(), e -> action.run());
		button.addThemeVariants(ButtonVariant.LUMO_SMALL);
		return button;
	}

	private Button resetButton(Runnable action) {
		Button button = new Button(Translator.translate("Championship.ResetToDefaults"), VaadinIcon.REFRESH.create(), e -> action.run());
		button.addThemeVariants(ButtonVariant.LUMO_SMALL);
		return button;
	}

	private Button deleteButton(Runnable action) {
		Button button = new Button(Translator.translate("Delete"), VaadinIcon.TRASH.create(), e -> action.run());
		button.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
		button.setAriaLabel(Translator.translate("Delete"));
		return button;
	}

	private Span fixedText(String text, String width) {
		Span span = new Span(text);
		span.setWidth(width);
		span.getStyle().set("flex-shrink", "0");
		return span;
	}

	private String championshipListTypeLabel(ChampionshipType type) {
		return switch (ChampionshipType.normalizeOrDefault(type)) {
			case MASTERS -> Translator.translate("Championship.TypeShort.MASTERS");
			case DEFAULT -> Translator.translate("Championship.TypeShort.DEFAULT");
			default -> "";
		};
	}

	private Map<String, ChampionshipCandidate> championshipCandidates() {
		Map<String, ChampionshipCandidate> candidates = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (AgeGroup ageGroup : AgeGroupRepository.findAll()) {
			String name = Championship.canonicalizeChampionshipName(ageGroup.computeChampionshipName());
			if (name == null || name.isBlank()) {
				continue;
			}
			ChampionshipType type = ageGroup.getChampionshipType();
			candidates.putIfAbsent(name, new ChampionshipCandidate(name, type));
		}
		return candidates;
	}

	private Map<String, Championship> explicitChampionships() {
		Map<String, Championship> explicitChampionships = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Championship championship : Championship.getMap().values()) {
			if (!championship.isCompetitionTemplate()) {
				explicitChampionships.put(championship.getName(), championship);
			}
		}
		return explicitChampionships;
	}

	private static class ChampionshipCandidate {
		private final String name;
		private final ChampionshipType type;

		ChampionshipCandidate(String name, ChampionshipType type) {
			this.name = name;
			this.type = type != null ? type : ChampionshipType.U;
		}
	}

	private ComboBox<ChampionshipType> createTypeField() {
		ComboBox<ChampionshipType> typeField = new ComboBox<>();
		typeField.setItems(ChampionshipType.selectableValues());
		typeField.setItemLabelGenerator(type -> Translator.translate(type.labelKey()));
		typeField.setWidth("28em");
		return typeField;
	}
}