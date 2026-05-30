/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class EditChampionshipsPanel extends VerticalLayout {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(EditChampionshipsPanel.class);
	static final String DIALOG_TABLE_WIDTH = "76em";
	private static final String NAME_COLUMN_WIDTH = "12em";
	private static final String TYPE_COLUMN_WIDTH = "34em";
	private static final String ACTIONS_COLUMN_WIDTH = "26em";

	private final boolean fullWidth;
	private Grid<ChampionshipRow> championshipsTable = new Grid<>(ChampionshipRow.class, false);
	private Checkbox hideCompetitionDefaults;

	public EditChampionshipsPanel() {
		this(true);
	}

	public EditChampionshipsPanel(boolean fullWidth) {
		this.fullWidth = fullWidth;
		setPadding(false);
		setSpacing(false);
		if (this.fullWidth) {
			setWidthFull();
		} else {
			setWidth(DIALOG_TABLE_WIDTH);
			setMaxWidth("calc(100vw - 4rem)");
		}
		configureChampionshipsTable();

		ChampionshipRepository.normalizeDefaultTypes();
		ChampionshipRepository.normalizeCompetitionDefaultFlags();
		this.hideCompetitionDefaults = new Checkbox(Translator.translate("EditChampionships.HideCompetitionDefaults"));
		this.hideCompetitionDefaults.setValue(true);
		this.hideCompetitionDefaults.addValueChangeListener(e -> updateChampionshipsTable());

		updateChampionshipsTable();
		add(this.hideCompetitionDefaults, this.championshipsTable);
	}

	private void configureChampionshipsTable() {
		if (this.fullWidth) {
			this.championshipsTable.setWidthFull();
		} else {
			this.championshipsTable.setWidth(DIALOG_TABLE_WIDTH);
			this.championshipsTable.setMaxWidth("calc(100vw - 4rem)");
		}
		this.championshipsTable.getStyle().set("margin-top", "1em");
		this.championshipsTable.setAllRowsVisible(true);
		this.championshipsTable.getThemeNames().add("row-stripes");
		Column<ChampionshipRow> nameColumn = this.championshipsTable.addColumn(new ComponentRenderer<>(this::nameCell))
		        .setHeader(Translator.translate("Name"))
		        .setWidth(NAME_COLUMN_WIDTH)
		        .setFlexGrow(0);
		Column<ChampionshipRow> typeColumn = this.championshipsTable.addColumn(new ComponentRenderer<>(this::typeCell))
		        .setHeader(Translator.translate("Championship.Type"))
		        .setFlexGrow(0);
		Column<ChampionshipRow> actionsColumn = this.championshipsTable.addColumn(new ComponentRenderer<>(this::actionsCell))
		        .setHeader("")
		        .setFlexGrow(this.fullWidth ? 1 : 0);
		if (this.fullWidth) {
			typeColumn.setAutoWidth(true);
			actionsColumn.setAutoWidth(true);
		} else {
			typeColumn.setWidth(TYPE_COLUMN_WIDTH);
			actionsColumn.setWidth(ACTIONS_COLUMN_WIDTH);
		}

		for (Column<ChampionshipRow> column : List.of(nameColumn, typeColumn, actionsColumn)) {
			column.setResizable(true);
		}
	}

	public void updateChampionshipsTable() {
		boolean hideDefaultRows = this.hideCompetitionDefaults == null
		        || Boolean.TRUE.equals(this.hideCompetitionDefaults.getValue());
		Map<String, ChampionshipCandidate> candidates = championshipCandidates();
		Map<String, Championship> explicitChampionships = explicitChampionships();
		List<ChampionshipRow> rows = new ArrayList<>();

		for (ChampionshipCandidate candidate : candidates.values()) {
			Championship existing = explicitChampionships.remove(candidate.name);
			boolean usesDefaults = existing == null || existing.usesCompetitionDefaults();
			if (hideDefaultRows && usesDefaults) {
				continue;
			}
			rows.add(new ChampionshipRow(candidate.name, candidate.type, existing, false));
		}

		explicitChampionships.values().stream().sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).forEach(c -> {
			if (hideDefaultRows && c.usesCompetitionDefaults()) {
				return;
			}
			rows.add(new ChampionshipRow(c.getName(), c.getType(), c, true));
		});
		rows.add(ChampionshipRow.addRow());
		this.championshipsTable.setItems(rows);
	}

	private Component nameCell(ChampionshipRow row) {
		if (!row.addRow) {
			return new Span(row.name);
		}
		TextField nameField = new TextField();
		nameField.setAriaLabel(Translator.translate("Name"));
		nameField.setPlaceholder(Translator.translate("Name"));
		nameField.setWidthFull();
		nameField.addValueChangeListener(e -> row.name = e.getValue());
		return nameField;
	}

	private Component typeCell(ChampionshipRow row) {
		if (!row.addRow) {
			return new Span(championshipListTypeLabel(row.type));
		}
		ComboBox<ChampionshipType> typeField = createTypeField();
		typeField.setAriaLabel(Translator.translate("Championship.Type"));
		typeField.setValue(row.type);
		typeField.addValueChangeListener(e -> row.type = e.getValue());
		return typeField;
	}

	private Component actionsCell(ChampionshipRow row) {
		if (!row.addRow) {
			return championshipActions(row);
		}
		Button addButton = new Button(Translator.translate("Add"), VaadinIcon.PLUS.create(), e -> {
			String rawName = row.name != null ? row.name.trim() : "";
			if (rawName.isBlank()) {
				showAddError(Translator.translate("ThisFieldIsRequired"));
				return;
			}
			String canonicalName = Championship.canonicalizeChampionshipName(rawName);
			if (findExistingChampionship(canonicalName) != null) {
				logger.warn("Rejected new championship '{}': duplicate of existing championship", canonicalName);
				showAddError(Translator.translate("Championship.NameAlreadyExists", canonicalName));
				return;
			}
			if (implicitChampionshipNameExists(canonicalName)) {
				logger.warn("Rejected new championship '{}': implicit championship from age groups already exists",
				        canonicalName);
				showAddError(Translator.translate("Championship.NameAlreadyExists", canonicalName));
				return;
			}
			Championship championship = Championship.addChampionship(canonicalName, row.type);
			new ChampionshipDetailsDialog(championship, this::updateChampionshipsTable).open();
		});
		return addButton;
	}

	private HorizontalLayout championshipActions(ChampionshipRow row) {
		HorizontalLayout actions = new HorizontalLayout();
		actions.setAlignItems(FlexComponent.Alignment.CENTER);
		actions.setSpacing(true);
		Button championshipButton = championshipButton(row.name, () -> {
			Championship championship = row.championship != null ? row.championship : Championship.findStored(row.name);
			if (championship == null) {
				championship = Championship.addChampionship(row.name, row.type);
			}
			new ChampionshipDetailsDialog(championship, this::updateChampionshipsTable).open();
		});
		actions.add(championshipButton);
		if (row.championship != null && !row.championship.usesCompetitionDefaults()) {
			actions.add(resetButton(() -> {
				ChampionshipRepository.resetToCompetitionDefaults(row.championship);
				updateChampionshipsTable();
			}));
		}
		if (row.extraExplicit) {
			actions.add(deleteButton(() -> {
				Championship.remove(row.championship);
				updateChampionshipsTable();
			}));
		}
		return actions;
	}

	private Button championshipButton(String name, Runnable action) {
		Button button = new Button(Translator.translate("Edit"), VaadinIcon.PENCIL.create(), e -> action.run());
		button.addThemeVariants(ButtonVariant.LUMO_SMALL);
		return button;
	}

	private Championship findExistingChampionship(String canonicalName) {
		for (Championship existing : Championship.findAllIncludingTemplate()) {
			if (existing.getName() != null
			        && existing.getName().trim().equalsIgnoreCase(canonicalName.trim())) {
				return existing;
			}
		}
		return null;
	}

	private boolean implicitChampionshipNameExists(String canonicalName) {
		for (AgeGroup ageGroup : AgeGroupRepository.findAll()) {
			String implicitName = Championship.canonicalizeChampionshipName(ageGroup.computeChampionshipName());
			if (implicitName != null && implicitName.trim().equalsIgnoreCase(canonicalName.trim())) {
				return true;
			}
		}
		return false;
	}

	private void showAddError(String message) {
		NotificationUtils.errorNotification(message);
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

	private String championshipListTypeLabel(ChampionshipType type) {
		return Translator.translate(ChampionshipType.normalizeOrDefault(type).labelKey());
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

	private static class ChampionshipRow {
		private String name;
		private ChampionshipType type;
		private final Championship championship;
		private final boolean extraExplicit;
		private final boolean addRow;

		static ChampionshipRow addRow() {
			return new ChampionshipRow("", ChampionshipType.U, null, false, true);
		}

		ChampionshipRow(String name, ChampionshipType type, Championship championship, boolean extraExplicit) {
			this(name, type, championship, extraExplicit, false);
		}

		ChampionshipRow(String name, ChampionshipType type, Championship championship, boolean extraExplicit, boolean addRow) {
			this.name = name;
			this.type = type != null ? type : ChampionshipType.U;
			this.championship = championship;
			this.extraExplicit = extraExplicit;
			this.addRow = addRow;
		}
	}

	private ComboBox<ChampionshipType> createTypeField() {
		ComboBox<ChampionshipType> typeField = new ComboBox<>();
		typeField.setItems(ChampionshipType.selectableValues());
		typeField.setItemLabelGenerator(type -> Translator.translate(type.labelKey()));
		typeField.setWidthFull();
		return typeField;
	}
}