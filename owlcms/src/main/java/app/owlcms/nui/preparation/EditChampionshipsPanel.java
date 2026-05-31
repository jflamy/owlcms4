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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsGridLayout;

@SuppressWarnings("serial")
public class EditChampionshipsPanel extends VerticalLayout {
	static final String DIALOG_TABLE_WIDTH = "76em";
	private static final String NAME_COLUMN_WIDTH = "12em";
	private static final String TYPE_COLUMN_WIDTH = "34em";
	private static final String ACTIONS_COLUMN_WIDTH = "26em";

	private final boolean fullWidth;
	private Grid<ChampionshipRow> championshipsTable = new Grid<>(ChampionshipRow.class, false);
	private Checkbox showActiveChampionshipsOnly;
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
		this.showActiveChampionshipsOnly = new Checkbox(Translator.translate("Active"));
		this.showActiveChampionshipsOnly.setValue(true);
		this.showActiveChampionshipsOnly.addValueChangeListener(e -> updateChampionshipsTable());
		this.hideCompetitionDefaults = new Checkbox(Translator.translate("EditChampionships.HideCompetitionDefaults"));
		this.hideCompetitionDefaults.setValue(true);
		this.hideCompetitionDefaults.addValueChangeListener(e -> updateChampionshipsTable());

		updateChampionshipsTable();
		add(createGridLayout());
	}

	private OwlcmsGridLayout createGridLayout() {
		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Championship.class);
		gridLayout.setMainComponent(this.championshipsTable);
		gridLayout.addToolbarComponent(createRefreshButton());
		gridLayout.addToolbarComponent(createAddButton());
		gridLayout.addFilterComponent(this.showActiveChampionshipsOnly);
		gridLayout.addFilterComponent(this.hideCompetitionDefaults);
		return gridLayout;
	}

	private Button createRefreshButton() {
		Button refreshButton = new Button(Translator.translate("RefreshList"), VaadinIcon.REFRESH.create(),
		        e -> updateChampionshipsTable());
		refreshButton.getElement().setAttribute("title", Translator.translate("RefreshList"));
		return refreshButton;
	}

	private Button createAddButton() {
		Button addButton = new Button(Translator.translate("Add"), VaadinIcon.PLUS.create(),
		        e -> addChampionship());
		addButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		addButton.getElement().setAttribute("title", Translator.translate("Add"));
		return addButton;
	}

	private void addChampionship() {
		Championship championship = new Championship("", ChampionshipType.U);
		championship.populateScoringDefaults();
		new ChampionshipDetailsDialog(championship, this::updateChampionshipsTable).open();
	}

	private void configureChampionshipsTable() {
		if (this.fullWidth) {
			this.championshipsTable.setWidthFull();
		} else {
			this.championshipsTable.setWidth(DIALOG_TABLE_WIDTH);
			this.championshipsTable.setMaxWidth("calc(100vw - 4rem)");
		}
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
		boolean activeOnly = this.showActiveChampionshipsOnly == null
		        || Boolean.TRUE.equals(this.showActiveChampionshipsOnly.getValue());
		boolean hideDefaultRows = this.hideCompetitionDefaults == null
		        || Boolean.TRUE.equals(this.hideCompetitionDefaults.getValue());
		Map<String, ChampionshipCandidate> candidates = championshipCandidates(activeOnly);
		Map<String, Championship> explicitChampionships = explicitChampionships();
		List<ChampionshipRow> rows = new ArrayList<>();

		for (ChampionshipCandidate candidate : candidates.values()) {
			Championship existing = explicitChampionships.remove(candidate.name);
			boolean usesDefaults = existing == null || existing.computeUsesCompetitionDefaults();
			if (hideDefaultRows && usesDefaults) {
				continue;
			}
			rows.add(new ChampionshipRow(candidate.name, candidate.type, existing, false));
		}

		if (!activeOnly) {
			explicitChampionships.values().stream().sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).forEach(c -> {
				if (hideDefaultRows && c.computeUsesCompetitionDefaults()) {
					return;
				}
				rows.add(new ChampionshipRow(c.getName(), c.getType(), c, true));
			});
		}
		this.championshipsTable.setItems(rows);
	}

	private Component nameCell(ChampionshipRow row) {
		return new Span(row.name);
	}

	private Component typeCell(ChampionshipRow row) {
		return new Span(championshipListTypeLabel(row.type));
	}

	private Component actionsCell(ChampionshipRow row) {
		return championshipActions(row);
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
		if (row.championship != null && !row.championship.computeUsesCompetitionDefaults()) {
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

	private Map<String, ChampionshipCandidate> championshipCandidates(boolean activeOnly) {
		Map<String, ChampionshipCandidate> candidates = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (AgeGroup ageGroup : AgeGroupRepository.findAll()) {
			if (activeOnly && !ageGroup.isActive()) {
				continue;
			}
			String name = effectiveChampionshipName(ageGroup);
			if (name == null || name.isBlank()) {
				continue;
			}
			ChampionshipType type = ageGroup.getChampionshipType();
			candidates.putIfAbsent(name, new ChampionshipCandidate(name, type));
		}
		return candidates;
	}

	private String effectiveChampionshipName(AgeGroup ageGroup) {
		String name = ageGroup.computeChampionshipName();
		if (name == null || name.isBlank()
		        || name.trim().equalsIgnoreCase(Championship.COMPETITION_TEMPLATE_NAME)) {
			name = ageGroup.getCode();
		}
		return Championship.canonicalizeChampionshipName(name != null ? name.trim() : null);
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

		ChampionshipRow(String name, ChampionshipType type, Championship championship, boolean extraExplicit) {
			this.name = name;
			this.type = type != null ? type : ChampionshipType.U;
			this.championship = championship;
			this.extraExplicit = extraExplicit;
		}
	}
}