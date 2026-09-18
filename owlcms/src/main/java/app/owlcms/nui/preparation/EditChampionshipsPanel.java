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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.component.UI;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.agegroup.ChampionshipType;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.monitors.WebSocketEventForwarder;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class EditChampionshipsPanel extends VerticalLayout {
	static final String DIALOG_TABLE_WIDTH = "76em";
	private static final String NAME_COLUMN_WIDTH = "20em";
	private static final String TYPE_COLUMN_WIDTH = "34em";
	private static final String ACTIONS_COLUMN_WIDTH = "26em";
	private static final Logger logger = (Logger) LoggerFactory.getLogger(EditChampionshipsPanel.class);

	private final boolean fullWidth;
	private Grid<ChampionshipRow> championshipsTable = new Grid<>(ChampionshipRow.class, false);
	private Checkbox hideEmptyChampionships;
	private Checkbox hideCompetitionDefaults;
	private ChampionshipRow draggedChampionship;
	private List<ChampionshipRow> displayedRows = new ArrayList<>();

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
		this.hideEmptyChampionships = new Checkbox(Translator.translate("Championship.HideEmpty"));
		this.hideEmptyChampionships.setValue(Competition.getCurrent().isHideEmptyChampionships());
		this.hideEmptyChampionships.addValueChangeListener(e -> {
			Competition competition = Competition.getCurrent();
			competition.setHideEmptyChampionships(Boolean.TRUE.equals(e.getValue()));
			CompetitionRepository.save(competition);
			updateChampionshipsTable();
		});
		this.hideCompetitionDefaults = new Checkbox(Translator.translate("EditChampionships.HideCompetitionDefaults"));
		this.hideCompetitionDefaults.setValue(false);
		this.hideCompetitionDefaults
		        .addValueChangeListener(e -> updateChampionshipsTable(Boolean.TRUE.equals(e.getValue())));

		updateChampionshipsTable();
		add(createGridLayout());
	}

	private OwlcmsGridLayout createGridLayout() {
		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Championship.class);
		gridLayout.setMainComponent(this.championshipsTable);
		gridLayout.addToolbarComponent(createRefreshButton());
		gridLayout.addToolbarComponent(createAddButton());
		gridLayout.addFilterComponent(this.hideEmptyChampionships);
		gridLayout.addFilterComponent(this.hideCompetitionDefaults);
		gridLayout.getHeaderLayout().setVisible(true);
		gridLayout.getToolbarLayout().setVisible(true);
		gridLayout.getFilterLayout().setVisible(true);
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
		this.championshipsTable.addComponentColumn(row -> {
			Icon dragHandle = VaadinIcon.MENU.create();
			dragHandle.getStyle().set("color", "var(--lumo-secondary-text-color)");
			return dragHandle;
		}).setHeader("").setWidth("2.5em").setFlexGrow(0);
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

		this.championshipsTable.addDragStartListener(event -> {
			this.draggedChampionship = event.getDraggedItems().get(0);
			this.championshipsTable.setDropMode(GridDropMode.BETWEEN);
		});
		this.championshipsTable.addDragEndListener(event -> {
			this.draggedChampionship = null;
			this.championshipsTable.setDropMode(null);
		});
		this.championshipsTable.addDropListener(event -> reorderChampionships(
		        event.getDropTargetItem().orElse(null), event.getDropLocation()));
	}

	public void updateChampionshipsTable() {
		updateChampionshipsTable(false);
	}

	private void updateChampionshipsTable(boolean traceDifferentChampionships) {
		Championship.recomputeParticipantCounts();
		boolean hideDefaultRows = this.hideCompetitionDefaults == null
		        || Boolean.TRUE.equals(this.hideCompetitionDefaults.getValue());
		boolean hideEmptyRows = this.hideEmptyChampionships == null
		        || Boolean.TRUE.equals(this.hideEmptyChampionships.getValue());
		Map<String, ChampionshipCandidate> candidates = championshipCandidates(false);
		Map<String, ChampionshipCandidate> activeCandidates = championshipCandidates(true);
		Map<String, Championship> explicitChampionships = explicitChampionships();
		List<ChampionshipRow> rows = new ArrayList<>();

		for (ChampionshipCandidate candidate : candidates.values()) {
			Championship existing = explicitChampionships.remove(candidate.name);
			if (hideEmptyRows && Championship.getParticipantCount(candidate.name) == 0) {
				continue;
			}
			boolean usesDefaults = existing == null || existing.computeUsesCompetitionDefaults();
			if (traceDifferentChampionships && hideDefaultRows && existing != null && !usesDefaults) {
				warnCompetitionDefaultDifferences(existing);
			}
			if (hideDefaultRows && usesDefaults) {
				continue;
			}
			boolean canDelete = existing != null && !activeCandidates.containsKey(candidate.name);
			rows.add(new ChampionshipRow(candidate.name, candidate.type, existing, canDelete));
		}

		explicitChampionships.values().stream().sorted((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName())).forEach(c -> {
				if (hideEmptyRows && Championship.getParticipantCount(c.getName()) == 0) {
					return;
				}
				boolean usesDefaults = c.computeUsesCompetitionDefaults();
				if (traceDifferentChampionships && hideDefaultRows && !usesDefaults) {
					warnCompetitionDefaultDifferences(c);
				}
				if (hideDefaultRows && usesDefaults) {
					return;
				}
				boolean canDelete = !activeCandidates.containsKey(c.getName());
				rows.add(new ChampionshipRow(c.getName(), c.getType(), c, canDelete));
		});
		rows.sort((first, second) -> {
			Integer firstOrder = first.championship != null ? first.championship.getOrder() : null;
			Integer secondOrder = second.championship != null ? second.championship.getOrder() : null;
			if (firstOrder != null && secondOrder != null) return Integer.compare(firstOrder, secondOrder);
			if (firstOrder != null) return -1;
			if (secondOrder != null) return 1;
			return first.name.compareToIgnoreCase(second.name);
		});
		this.displayedRows = rows;
		this.championshipsTable.setRowsDraggable(true);
		this.championshipsTable.setItems(this.displayedRows);
	}

	private void reorderChampionships(ChampionshipRow dropTarget, GridDropLocation dropLocation) {
		if (this.draggedChampionship == null || dropTarget == null || this.draggedChampionship == dropTarget) {
			return;
		}
		this.displayedRows.remove(this.draggedChampionship);
		int dropIndex = this.displayedRows.indexOf(dropTarget);
		if (dropLocation == GridDropLocation.BELOW) {
			dropIndex++;
		}
		this.displayedRows.add(dropIndex, this.draggedChampionship);

		for (ChampionshipRow row : this.displayedRows) {
			if (row.championship == null) {
				Championship.addChampionship(row.name, row.type);
			}
		}
		ChampionshipRepository.updateDisplayOrder(this.displayedRows.stream().map(row -> row.name).toList());
		WebSocketEventForwarder.sendDatabaseToAll();
		updateChampionshipsTable();
	}

	private void warnCompetitionDefaultDifferences(Championship championship) {
		List<String> differences = championship.computeCompetitionDefaultDifferences(Championship.of(null));
		if (!differences.isEmpty()) {
			logger.debug("CHAMPIONSHIP_DEFAULT_TRACE '{}' differs from competition defaults: {} {}",
			        championship.getName(), differences, LoggerUtils.whereFrom());
		}
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
		actions.add(editAgeGroupsButton(row.name));
		if (row.championship != null && !row.championship.computeUsesCompetitionDefaults()) {
			actions.add(resetButton(() -> {
				new ConfirmationDialog(
				        Translator.translate("Championship.ResetToDefaults"),
				        Translator.translate("Championship.ResetToDefaultsWarning", row.name),
				        null, () -> {
					        ChampionshipRepository.resetToCompetitionDefaults(row.championship);
					        updateChampionshipsTable();
				        }).open();
			}));
		}
		if (row.canDelete) {
			actions.add(deleteButton(() -> {
				new ConfirmationDialog(
				        Translator.translate("Delete"),
				        Translator.translate("Championship.DeleteWithAgeGroupsWarning", row.name),
				        null, () -> {
					        Championship.removeWithAssociatedAgeGroups(row.championship, associatedAgeGroups(row.name));
					        updateChampionshipsTable();
				        }).open();
			}));
		}
		return actions;
	}

	private Button championshipButton(String name, Runnable action) {
		Button button = new Button(Translator.translate("Edit"), VaadinIcon.PENCIL.create(), e -> action.run());
		button.addThemeVariants(ButtonVariant.LUMO_SMALL);
		return button;
	}

	private Button editAgeGroupsButton(String championshipName) {
		Button button = new Button(Translator.translate("EditAgeGroups"), VaadinIcon.PENCIL.create(), e -> {
			QueryParameters parameters = new QueryParameters(Map.of("championship", List.of(championshipName)));
			String url = URLUtils.getUrlFromTargetClass(AgeGroupContent.class, null, parameters);
			UI.getCurrent().getPage().executeJs("window.open($0, $1)", url, AgeGroupContent.class.getSimpleName());
		});
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

	private List<AgeGroup> associatedAgeGroups(String championshipName) {
		List<AgeGroup> associatedAgeGroups = new ArrayList<>();
		for (AgeGroup ageGroup : AgeGroupRepository.findAll()) {
			if (championshipName.equalsIgnoreCase(effectiveChampionshipName(ageGroup))) {
				associatedAgeGroups.add(ageGroup);
			}
		}
		return associatedAgeGroups;
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
		private final boolean canDelete;

		ChampionshipRow(String name, ChampionshipType type, Championship championship, boolean canDelete) {
			this.name = name;
			this.type = type != null ? type : ChampionshipType.U;
			this.championship = championship;
			this.canDelete = canDelete;
		}
	}
}