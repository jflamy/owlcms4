/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;

import app.owlcms.data.competition.Competition;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.spreadsheet.JXLSExportRecords;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.utils.URLUtils;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.config.Config;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class RecordContent.
 *
 * Defines the toolbar and the table for editing record events.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/records", layout = OwlcmsLayout.class)
public class RecordContent extends BaseContent implements CrudListener<RecordEvent>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(RecordContent.class);

	static {
		logger.setLevel(Level.INFO);
	}
	
	// Filter fields
	protected ComboBox<String> federationFilter = new ComboBox<>();
	protected ComboBox<String> recordNameFilter = new ComboBox<>();
	protected ComboBox<String> ageGroupFilter = new ComboBox<>();
	protected ComboBox<Gender> genderFilter = new ComboBox<>();
	protected ComboBox<RecordFilters.ProvisionalFilter> provisionalFilter = new ComboBox<>();
	protected ComboBox<RecordFilters.CurrentHistoryFilter> currentHistoryFilter = new ComboBox<>();
	protected Checkbox activeOnlyFilter = new Checkbox();
	protected TextField nameFilter = new TextField();

	// Filter values
	protected String federation;
	protected String recordName;
	protected String ageGroup;
	protected Gender gender;
	protected String name;

	protected boolean readOnly;
	boolean documentPage;
	private boolean updatingFilters;
	protected RecordGrid crud;
	protected OwlcmsCrudFormFactory<RecordEvent> editingFormFactory;
	protected OwlcmsLayout routerLayout;
	protected FlexLayout topBar;

	/**
	 * Instantiates the RecordEvent crudGrid (editing mode).
	 */
	public RecordContent() {
		this(false);
	}

	/**
	 * Instantiates the RecordEvent crudGrid.
	 * @param readOnly true for the public page (no editing, no selection)
	 */
	protected RecordContent(boolean readOnly) {
		this.readOnly = readOnly;
		this.editingFormFactory = new RecordEditingFormFactory(RecordEvent.class, readOnly ? null : this);
		GridCrud<RecordEvent> crud = createGrid(this.editingFormFactory);
		defineFilters(crud);
		fillHW(crud, this);
	}

	@Override
	public RecordEvent add(RecordEvent domainObjectToAdd) {
		if (this.readOnly) return domainObjectToAdd;
		return this.editingFormFactory.add(domainObjectToAdd);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		OwlcmsContent.super.beforeEnter(event);
		String path = event.getLocation().getPath();
		this.documentPage = path.contains("documents");
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		Location location = event.getLocation();
		setLocation(location);
		setLocationUI(event.getUI());

		Map<String, List<String>> params = location.getQueryParameters().getParameters();

		this.updatingFilters = true;
		try {
			String fed = getFirstParam(params, "federation");
			String recName = getFirstParam(params, "recordName");
			String ag = getFirstParam(params, "ageGroup");
			String g = getFirstParam(params, "gender");
			String n = getFirstParam(params, "name");
			String prov = getFirstParam(params, "provisional");
			String curHist = getFirstParam(params, "currentHistory");
			String activeParam = getFirstParam(params, "activeFilter");
			this.activeOnlyFilter.setValue(!"ALL".equals(activeParam));

			// Federation first (dependent filters rely on it)
			if (fed != null && !fed.isBlank()) {
				setFederation(fed);
				this.federationFilter.setValue(fed);
			} else {
				autoSelectSingleFederation();
			}

			// Populate dependent combo items
			refreshDependentFilterOptions();

			if (recName != null && !recName.isBlank()) {
				setRecordName(recName);
				this.recordNameFilter.setValue(recName);
			}
			if (ag != null && !ag.isBlank()) {
				setAgeGroup(ag);
				this.ageGroupFilter.setValue(ag);
			}
			if (g != null) {
				try {
					Gender gv = Gender.valueOf(g);
					setGender(gv);
					this.genderFilter.setValue(gv);
				} catch (IllegalArgumentException ignored) {
				}
			}
			if (n != null && !n.isBlank()) {
				setName(n);
				this.nameFilter.setValue(n);
			}
			if (prov != null) {
				try {
					this.provisionalFilter.setValue(RecordFilters.ProvisionalFilter.valueOf(prov));
				} catch (IllegalArgumentException ignored) {
				}
			}
			if (curHist != null) {
				try {
					this.currentHistoryFilter.setValue(RecordFilters.CurrentHistoryFilter.valueOf(curHist));
				} catch (IllegalArgumentException ignored) {
				}
			}
			syncCurrentHistoryFilterForProvisional();
		} finally {
			this.updatingFilters = false;
		}

		this.crud.refreshGrid();
		updateUrlParameters();
	}

	public void closeDialog() {
	}

	public void updateDialogCaption(String caption) {
		if (this.crud != null) {
			this.crud.updateDialogCaption(caption);
		}
	}

	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();
		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.setFlexDirection(FlexLayout.FlexDirection.COLUMN);
		applyRecordsOnlyToolbarOffset();

		// Row 1: Import / Export
		HorizontalLayout row1 = new HorizontalLayout(
				createExportRecordsButton(),
				createImportButton());
		row1.setAlignItems(FlexComponent.Alignment.CENTER);
		row1.setPadding(false);
		row1.setSpacing(true);
		if (Config.getCurrent().isRecordRepository()) {
			row1.add(createLogoutButton());
		}

		// Row 2: Record-set status mutations (acts on currently filtered records)
		HorizontalLayout row2 = new HorizontalLayout(
				createMarkActiveButton(true),
				createMarkActiveButton(false),
				createRemoveSelectedButton());
		row2.setAlignItems(FlexComponent.Alignment.CENTER);
		row2.setPadding(false);
		row2.setSpacing(true);

		// Row 3: Provisional / computation actions
		HorizontalLayout row3 = new HorizontalLayout(
				createRecomputeRecordsButton(),
				createAcceptProvisionalRecordsButton(),
				createKeepLatestOfficialRecordsButton());
		row3.setAlignItems(FlexComponent.Alignment.CENTER);
		row3.setPadding(false);
		row3.setSpacing(true);

		this.topBar.add(row1, row2, row3);
		return this.topBar;
	}

	protected void applyRecordsOnlyToolbarOffset() {
		this.topBar.getStyle().set("margin-left", "1.5em");
		this.topBar.getStyle().set("padding-left", "0");
	}

	protected Button createImportButton() {
		Button importButton = new Button(Translator.translate("Import"), buttonClickEvent -> {
			new RecordImportDialog(this::refreshAfterRecordImport).open();
		});
		importButton.setIcon(VaadinIcon.UPLOAD_ALT.create());
		importButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
		importButton.getElement().getStyle().set("margin-right", "1em");
		return importButton;
	}

	private void refreshAfterRecordImport() {
		refreshFilterOptionsFromRepository();
		this.crud.refreshGrid();
		updateUrlParameters();
	}

	private Button createLogoutButton() {
		Button logoutButton = new Button("Logout", buttonClickEvent -> {
			UI currentUi = UI.getCurrent();
			OwlcmsSession.invalidate();
			currentUi.getPage().setLocation("publicRecords");
		});
		logoutButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
		logoutButton.getElement().getStyle().set("margin-right", "1em");
		return logoutButton;
	}

	protected Button createExportRecordsButton() {
		JXLSDownloader downloadDialog = new JXLSDownloader(
			() -> {
				// Get the same filtered records that are shown in the grid
				List<RecordEvent> filteredRecords = getFilteredRecords();
				return new JXLSExportRecords(UI.getCurrent(), filteredRecords);
			},
			"/templates/records",
			Competition::getComputedCurrentRecordsTemplateFileName,
			Competition::setCurrentRecordsTemplateFileName,
			Translator.translate("RecordEvent.ExportRecords"),
			Translator.translate("Download"));
		downloadDialog.setFileNamePrefixSupplier(this::getRecordExportFileNamePrefix);
		Button exportButton = downloadDialog.createDownloadButton();
		exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		exportButton.getElement().getStyle().set("margin-right", "1em");
		return exportButton;
	}

	private String getRecordExportFileNamePrefix() {
		StringBuilder prefix = new StringBuilder();
		appendFileNamePart(prefix, getFederation());
		appendFileNamePart(prefix, getRecordName());
		return prefix.toString();
	}

	private static void appendFileNamePart(StringBuilder prefix, String value) {
		if (value == null || value.isBlank()) {
			return;
		}
		if (prefix.length() > 0) {
			prefix.append("_");
		}
		prefix.append(value.trim());
	}

	private Button createAcceptProvisionalRecordsButton() {
		Button acceptProvisionalRecordsButton = new Button(Translator.translate("Preparation.ClearNewRecords"),
			buttonClickEvent -> {
				try {
					// Use the same filter parameters as the grid display
					String provisionalFilterStr = "ALL";
					if (this.provisionalFilter != null && this.provisionalFilter.getValue() != null) {
						provisionalFilterStr = this.provisionalFilter.getValue().name();
					}
					
					String currentHistoryFilterStr = "HISTORY"; // Default to showing all records
					if (this.currentHistoryFilter != null && this.currentHistoryFilter.getValue() != null) {
						currentHistoryFilterStr = this.currentHistoryFilter.getValue().name();
					}
					
					// Accept provisional rows only for the filtered records.
					RecordRepository.acceptProvisionalRecordsWithFilters(
						getFederation(),
						getRecordName(),
						getAgeGroup(),
						getGender(),
						getName(),
						provisionalFilterStr,
						currentHistoryFilterStr
					);
					
					// Refresh the grid to show the updated records
					this.crud.refreshGrid();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		acceptProvisionalRecordsButton.getElement().getStyle().set("margin-right", "1em");
		acceptProvisionalRecordsButton.getElement().setAttribute("title", Translator.translate("Preparation.ClearNewRecordsExplanation"));
		return acceptProvisionalRecordsButton;
	}

	private Button createRecomputeRecordsButton() {
		Button recomputeRecordsButton = new Button(Translator.translate("Preparation.RecomputeNewRecords"),
			buttonClickEvent -> {
				RecordRepository.recomputeNewRecords();
				// Refresh the grid to show the updated records
				this.crud.refreshGrid();
			});
		recomputeRecordsButton.getElement().getStyle().set("margin-right", "1em");
		recomputeRecordsButton.getElement().setAttribute("title", Translator.translate("Preparation.RecomputeNewRecordsExplanation"));
		return recomputeRecordsButton;
	}

	private Button createKeepLatestOfficialRecordsButton() {
		Button keepLatestOfficialRecordsButton = new Button(Translator.translate("RecordEvent.KeepCurrentRecords"),
			buttonClickEvent -> {
				try {
					// Prune official history only, keeping the latest official row per logical key.
					RecordRepository.keepLatestOfficialRecordsWithFilters(
						getFederation(),
						getRecordName(),
						getAgeGroup(),
						getGender(),
						getName()
					);
					
					// Refresh the grid to show the updated records
					this.crud.refreshGrid();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		keepLatestOfficialRecordsButton.getElement().getStyle().set("margin-right", "1em");
		keepLatestOfficialRecordsButton.getElement().setAttribute("title", Translator.translate("RecordEvent.KeepCurrentRecordsExplanation"));
		return keepLatestOfficialRecordsButton;
	}

	private Button createRemoveSelectedButton() {
		Button removeSelectedButton = new Button(Translator.translate("RecordEvent.DeleteSelected"),
			buttonClickEvent -> {
				Set<RecordEvent> selected = this.crud.getSelectedItems();
				if (selected.isEmpty()) {
					return;
				}
				ConfirmationDialog confirmDialog = new ConfirmationDialog(
					Translator.translate("RecordEvent.DeleteSelected"),
					Translator.translate("RecordEvent.DeleteSelectedExplanation"),
					null,
					() -> {
						List<Long> ids = selected.stream()
						        .map(RecordEvent::getId)
						        .filter(id -> id != null)
						        .collect(Collectors.toList());
						RecordRepository.deleteRecordsByIds(ids);
						this.crud.refreshGrid();
					}
				);
				confirmDialog.open();
			});
		removeSelectedButton.getElement().getStyle().set("margin-right", "1em");
		removeSelectedButton.getElement().setAttribute("title", Translator.translate("RecordEvent.DeleteSelectedExplanation"));
		return removeSelectedButton;
	}

	private Button createMarkActiveButton(boolean active) {
		String labelKey = active ? "RecordEvent.MarkFilteredActive" : "RecordEvent.MarkFilteredInactive";
		Button btn = new Button(Translator.translate(labelKey), e -> {
			List<RecordEvent> filtered = getFilteredRecords();
			if (filtered.isEmpty()) {
				return;
			}
			List<Long> ids = filtered.stream()
			        .map(RecordEvent::getId)
			        .filter(id -> id != null)
			        .collect(Collectors.toList());
			String confirmKey = active ? "RecordEvent.MarkFilteredActiveConfirm" : "RecordEvent.MarkFilteredInactiveConfirm";
			ConfirmationDialog cd = new ConfirmationDialog(
					Translator.translate(labelKey),
					Translator.translate(confirmKey, ids.size()),
					null,
					() -> {
						RecordRepository.setActiveForRecordIds(ids, active);
						this.crud.refreshGrid();
					});
			cd.open();
		});
		btn.getElement().getStyle().set("margin-right", "1em");
		return btn;
	}

	@Override
	public void delete(RecordEvent domainObjectToDelete) {
		if (this.readOnly) return;
		this.editingFormFactory.delete(domainObjectToDelete);
	}

	/**
	 * The refresh button on the toolbar
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<RecordEvent> findAll() {
		// Use the centralized filtering method from RecordRepository
		// This ensures grid and export show exactly the same data
		return getFilteredRecords();
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		if (this.documentPage) {
			return Translator.translate("Documents.Title");
		}
		return Translator.translate("RecordEvent.PageTitle");
	}

	@Override
	public void setHeaderContent() {
		this.routerLayout.setMenuTitle(getMenuTitle());
		this.routerLayout.setMenuArea(createMenuArea());
		this.routerLayout.showLocaleDropdown(true);
		this.routerLayout.setDrawerOpened(false);
		this.routerLayout.updateHeader(true);
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public RecordEvent update(RecordEvent domainObjectToUpdate) {
		if (this.readOnly) return domainObjectToUpdate;
		return this.editingFormFactory.update(domainObjectToUpdate);
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#isIgnoreFopFromURL()
	 */
	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		if (this.documentPage) {
			this.crud.getAddButton().removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
		}
	}

	/**
	 * Filter methods
	 */
	public void clearFilters() {
		this.updatingFilters = true;
		try {
			this.federationFilter.clear();
			autoSelectSingleFederation();
			this.recordNameFilter.clear();
			this.ageGroupFilter.clear();
			refreshDependentFilterOptions();
			this.genderFilter.clear();
			setGender(null);
			this.provisionalFilter.setValue(RecordFilters.ProvisionalFilter.ALL);
			this.currentHistoryFilter.setValue(RecordFilters.CurrentHistoryFilter.CURRENT);
			syncCurrentHistoryFilterForProvisional();
			this.nameFilter.clear();
			setName(null);
			this.activeOnlyFilter.setValue(true);
		} finally {
			this.updatingFilters = false;
		}
	}

	public String getFederation() {
		return federation;
	}

	public void setFederation(String federation) {
		this.federation = federation;
	}

	public String getRecordName() {
		return recordName;
	}

	public void setRecordName(String recordName) {
		this.recordName = recordName;
	}

	public String getAgeGroup() {
		return ageGroup;
	}

	public void setAgeGroup(String ageGroup) {
		this.ageGroup = ageGroup;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public RecordFilters.ProvisionalFilter getProvisionalFilter() {
		return this.provisionalFilter.getValue();
	}

	public void setProvisionalFilter(RecordFilters.ProvisionalFilter provisionalFilter) {
		this.provisionalFilter.setValue(provisionalFilter);
	}

	private String getProvisionalFilterName() {
		RecordFilters.ProvisionalFilter provisional = this.provisionalFilter != null
		        ? this.provisionalFilter.getValue()
		        : null;
		return provisional != null ? provisional.name() : RecordFilters.ProvisionalFilter.ALL.name();
	}

	public RecordFilters.CurrentHistoryFilter getCurrentHistoryFilter() {
		return this.currentHistoryFilter.getValue();
	}

	public void setCurrentHistoryFilter(RecordFilters.CurrentHistoryFilter currentHistoryFilter) {
		this.currentHistoryFilter.setValue(currentHistoryFilter);
	}

	private String getCurrentHistoryFilterName() {
		RecordFilters.CurrentHistoryFilter currentHistory = this.currentHistoryFilter != null
		        ? this.currentHistoryFilter.getValue()
		        : null;
		return currentHistory != null ? currentHistory.name() : RecordFilters.CurrentHistoryFilter.CURRENT.name();
	}

	// ---- URL parameter persistence ----

	private String getFirstParam(Map<String, List<String>> params, String key) {
		List<String> values = params.get(key);
		if (values == null || values.isEmpty()) {
			return null;
		}
		String val = values.get(0);
		return (val != null && !val.isBlank()) ? URLDecoder.decode(val, StandardCharsets.UTF_8) : null;
	}

	private void updateUrlParameters() {
		if (this.updatingFilters || getLocationUI() == null || getLocation() == null) {
			return;
		}
		HashMap<String, List<String>> params = new HashMap<>();
		if (federation != null && !federation.isBlank()) {
			params.put("federation", List.of(federation));
		}
		if (recordName != null && !recordName.isBlank()) {
			params.put("recordName", List.of(recordName));
		}
		if (ageGroup != null && !ageGroup.isBlank()) {
			params.put("ageGroup", List.of(ageGroup));
		}
		if (gender != null) {
			params.put("gender", List.of(gender.name()));
		}
		if (name != null && !name.isBlank()) {
			params.put("name", List.of(name));
		}
		RecordFilters.ProvisionalFilter pv = this.provisionalFilter.getValue();
		if (pv != null && pv != RecordFilters.ProvisionalFilter.ALL) {
			params.put("provisional", List.of(pv.name()));
		}
		RecordFilters.CurrentHistoryFilter cv = this.currentHistoryFilter.getValue();
		if (cv != null && cv != RecordFilters.CurrentHistoryFilter.CURRENT) {
			params.put("currentHistory", List.of(cv.name()));
		}
		if (Boolean.FALSE.equals(this.activeOnlyFilter.getValue())) {
			params.put("activeFilter", List.of("ALL"));
		}

		Location newLocation = new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params)));
		getLocationUI().getPage().getHistory().replaceState(null, newLocation);
		setLocation(newLocation);
	}

	/**
	 * Get filtered records using RecordRepository to ensure consistency between grid and export
	 */
	protected List<RecordEvent> getFilteredRecords() {
		// Convert enum values to strings for the repository method
		String provisionalFilterStr = getProvisionalFilterName();
		
		String currentHistoryFilterStr = getCurrentHistoryFilterName();
		currentHistoryFilterStr = RecordRepository.normalizeCurrentHistoryFilter(provisionalFilterStr, currentHistoryFilterStr);
		
		return RecordRepository.findWithFilters(
			getFederation(),
			getRecordName(),
			getAgeGroup(),
			getGender(),
			getName(),
			provisionalFilterStr,
			currentHistoryFilterStr, null,
			Boolean.TRUE.equals(this.activeOnlyFilter.getValue()) ? "ACTIVE" : "ALL"
		);
	}

	/**
	 * Define the filters for the record grid
	 */
	protected void defineFilters(GridCrud<RecordEvent> crud) {
		// Federation filter
		this.federationFilter.setPlaceholder(Translator.translate("RecordEvent.Federation"));
		this.federationFilter.setItems(RecordRepository.findDistinctFederations());
		this.federationFilter.setClearButtonVisible(true);
		this.federationFilter.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			setFederation(e.getValue());
			resetFederationCascade();
			refreshDependentFilterOptions();
			crud.refreshGrid();
			updateUrlParameters();
		});
		this.federationFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.federationFilter);
		autoSelectSingleFederation();

		// Record Name filter
		this.recordNameFilter.setPlaceholder(Translator.translate("Records.RecordName"));
		this.recordNameFilter.setClearButtonVisible(true);
		this.recordNameFilter.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			setRecordName(e.getValue());
			refreshDependentFilterOptions();
			crud.refreshGrid();
			updateUrlParameters();
		});
		this.recordNameFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.recordNameFilter);

		// Age Group filter
		this.ageGroupFilter.setPlaceholder(Translator.translate("AgeGroup"));
		this.ageGroupFilter.setClearButtonVisible(true);
		this.ageGroupFilter.addValueChangeListener(e -> {
			if (!e.isFromClient()) {
				return;
			}
			setAgeGroup(e.getValue());
			refreshDependentFilterOptions();
			crud.refreshGrid();
			updateUrlParameters();
		});
		this.ageGroupFilter.setWidth("10em");
		crud.getCrudLayout().addFilterComponent(this.ageGroupFilter);
		refreshDependentFilterOptions();

		// Gender filter
		this.genderFilter.setPlaceholder(Translator.translate("Gender"));
		this.genderFilter.setItems(Gender.M, Gender.F);
		this.genderFilter.setItemLabelGenerator(g -> g.asGenderName());
		this.genderFilter.setClearButtonVisible(true);
		this.genderFilter.addValueChangeListener(e -> {
			setGender(e.getValue());
			if (!this.updatingFilters) {
				crud.refreshGrid();
				updateUrlParameters();
			}
		});
		this.genderFilter.setWidth("8em");
		crud.getCrudLayout().addFilterComponent(this.genderFilter);

		// Name filter (for record name or athlete name)
		this.nameFilter.setPlaceholder(Translator.translate("Name"));
		this.nameFilter.setClearButtonVisible(true);
		this.nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		this.nameFilter.addValueChangeListener(e -> {
			setName(e.getValue());
			if (!this.updatingFilters) {
				crud.refreshGrid();
				updateUrlParameters();
			}
		});
		this.nameFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.nameFilter);

		// Provisional filter
		NativeLabel provisionalLabel = new NativeLabel(Translator.translate("RecordEvent.Status"));
		this.provisionalFilter.setItems(RecordFilters.ProvisionalFilter.values());
		this.provisionalFilter.setItemLabelGenerator(filter -> Translator.translate(filter.getKey()));
		this.provisionalFilter.setValue(RecordFilters.ProvisionalFilter.ALL);
		this.provisionalFilter.setClearButtonVisible(false);
		this.provisionalFilter.addValueChangeListener(e -> {
			setProvisionalFilter(e.getValue());
			syncCurrentHistoryFilterForProvisional();
			if (!this.updatingFilters) {
				crud.refreshGrid();
				updateUrlParameters();
			}
		});
		this.provisionalFilter.setWidth("10em");
		
		HorizontalLayout provisionalLayout = new HorizontalLayout(provisionalLabel, this.provisionalFilter);
		provisionalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		provisionalLayout.setSpacing(false);
		provisionalLabel.getStyle().set("margin-right", "0.5em");
		
		crud.getCrudLayout().addFilterComponent(provisionalLayout);

		// Current/History filter 
		this.currentHistoryFilter.setItems(RecordFilters.CurrentHistoryFilter.values());
		this.currentHistoryFilter.setItemLabelGenerator(filter -> Translator.translate(filter.getKey()));
		this.currentHistoryFilter.setValue(RecordFilters.CurrentHistoryFilter.CURRENT);
		this.currentHistoryFilter.setClearButtonVisible(false);
		this.currentHistoryFilter.addValueChangeListener(e -> {
			setCurrentHistoryFilter(e.getValue());
			syncCurrentHistoryFilterForProvisional();
			if (!this.updatingFilters) {
				crud.refreshGrid();
				updateUrlParameters();
			}
		});
		this.currentHistoryFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.currentHistoryFilter);
		syncCurrentHistoryFilterForProvisional();

		// Active-only filter checkbox
		this.activeOnlyFilter.setLabel(Translator.translate("Active"));
		this.activeOnlyFilter.setValue(true);
		this.activeOnlyFilter.addValueChangeListener(e -> {
			if (!this.updatingFilters) {
				refreshFilterOptionsFromRepository();
				crud.refreshGrid();
				updateUrlParameters();
			}
		});
		crud.getCrudLayout().addFilterComponent(this.activeOnlyFilter);

		// Clear filters button
		Button clearFiltersButton = new Button(null, VaadinIcon.CLOSE.create());
		clearFiltersButton.addClickListener(event -> {
			clearFilters();
			crud.refreshGrid();
			updateUrlParameters();
		});
		crud.getCrudLayout().addFilterComponent(clearFiltersButton);
	}

	private String getActiveFilterStr() {
		return Boolean.TRUE.equals(this.activeOnlyFilter.getValue()) ? "ACTIVE" : "ALL";
	}

	protected void autoSelectSingleFederation() {
		if (this.federationFilter.getValue() != null && !this.federationFilter.getValue().isBlank()) {
			return;
		}

		List<String> availableFederations = RecordRepository.findDistinctFederations(getActiveFilterStr());
		if (availableFederations.size() == 1) {
			this.federationFilter.setValue(availableFederations.get(0));
			setFederation(availableFederations.get(0));
		}
	}

	protected void refreshFilterOptionsFromRepository() {
		String selectedFederation = this.federationFilter.getValue();
		List<String> availableFederations = preserveSelectedValue(RecordRepository.findDistinctFederations(getActiveFilterStr()), selectedFederation);
		this.federationFilter.setItems(availableFederations);

		if (selectedFederation != null && !selectedFederation.isBlank()) {
			this.federationFilter.setValue(selectedFederation);
			setFederation(selectedFederation);
		} else {
			autoSelectSingleFederation();
		}

		refreshDependentFilterOptions();
	}

	protected void refreshDependentFilterOptions() {
		String selectedFederation = this.federationFilter.getValue();
		String selectedRecordName = this.recordNameFilter.getValue();
		String selectedAgeGroup = this.ageGroupFilter.getValue();

		if (selectedFederation == null || selectedFederation.isBlank()) {
			this.recordNameFilter.clear();
			this.ageGroupFilter.clear();
			this.recordNameFilter.setItems(List.of());
			this.ageGroupFilter.setItems(List.of());
			this.recordNameFilter.setReadOnly(true);
			this.ageGroupFilter.setReadOnly(true);
			setRecordName(null);
			setAgeGroup(null);
			return;
		}

		List<String> availableRecordNames = RecordRepository.findDistinctRecordNames(selectedFederation, selectedAgeGroup, getActiveFilterStr());
		List<String> availableAgeGroups = RecordRepository.findDistinctAgeGroups(selectedFederation, selectedRecordName, getActiveFilterStr());

		updateSingleValueFilter(this.recordNameFilter, availableRecordNames, selectedRecordName, this::setRecordName);
		updateSingleValueFilter(this.ageGroupFilter, availableAgeGroups, selectedAgeGroup, this::setAgeGroup);
	}

	private void resetFederationCascade() {
		this.recordNameFilter.clear();
		this.ageGroupFilter.clear();
		setRecordName(null);
		setAgeGroup(null);
	}

	private void updateSingleValueFilter(ComboBox<String> filter, List<String> availableValues, String selectedValue,
	        java.util.function.Consumer<String> setter) {
		List<String> valuesToShow = preserveSelectedValue(availableValues, selectedValue);
		filter.setItems(valuesToShow);

		if (valuesToShow.isEmpty()) {
			filter.clear();
			filter.setReadOnly(true);
			setter.accept(null);
			return;
		}

		if (availableValues.size() == 1 && (selectedValue == null || selectedValue.isBlank() || availableValues.contains(selectedValue))) {
			String onlyValue = availableValues.get(0);
			if (!onlyValue.equals(filter.getValue())) {
				filter.setValue(onlyValue);
			}
			filter.setReadOnly(true);
			setter.accept(onlyValue);
			return;
		}

		filter.setReadOnly(false);
		if (selectedValue != null) {
			filter.setValue(selectedValue);
			setter.accept(selectedValue);
		}
	}

	private List<String> preserveSelectedValue(List<String> availableValues, String selectedValue) {
		List<String> valuesToShow = new ArrayList<>(availableValues);
		if (selectedValue != null && !selectedValue.isBlank() && !valuesToShow.contains(selectedValue)) {
			valuesToShow.add(0, selectedValue);
		}
		return valuesToShow;
	}

	protected void syncCurrentHistoryFilterForProvisional() {
		if (this.currentHistoryFilter != null) {
			this.currentHistoryFilter.setEnabled(true);
		}
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing a record event
	 * @return
	 */
	protected GridCrud<RecordEvent> createGrid(OwlcmsCrudFormFactory<RecordEvent> crudFormFactory) {
		Grid<RecordEvent> grid = new Grid<>(RecordEvent.class, false);
		this.crud = new RecordGrid(RecordEvent.class, new OwlcmsGridLayout(RecordEvent.class), crudFormFactory, grid,
		        this::refreshFilterOptionsFromRepository, this::getFilteredRecords);
		grid.getThemeNames().add("row-stripes");
		
		// Record identification columns
		grid.addColumn(RecordEvent::getRecordFederation).setHeader(Translator.translate("Competition.federationTitle")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getRecordName).setHeader(Translator.translate("RecordEvent.Name")).setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(RecordEvent::getAgeGrp).setHeader(Translator.translate("AgeGroup")).setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(RecordEvent::getGender).setHeader(Translator.translate("Gender")).setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(RecordEvent::getBwCatString).setHeader(Translator.translate("Category")).setAutoWidth(true).setFlexGrow(0);
		
		// Record details columns
		grid.addColumn(re -> re.getRecordLift() != null ? Translator.translate("Record." + re.getRecordLift()) : "")
		    .setHeader(Translator.translate("RecordEvent.Lift")).setAutoWidth(true).setFlexGrow(0);
		grid.addColumn(RecordEvent::getRecordValue).setHeader(Translator.translate("RecordEvent.Value")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getAthleteName).setHeader(Translator.translate("RecordEvent.AthleteName")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getNation).setHeader(Translator.translate("RecordEvent.Nation")).setAutoWidth(true);
		
		// Event details columns
		grid.addColumn(RecordEvent::getRecordDate).setHeader(Translator.translate("RecordEvent.Date")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getEvent).setHeader(Translator.translate("RecordEvent.Event")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getEventLocation).setHeader(Translator.translate("RecordEvent.EventLocation")).setAutoWidth(true);

		// Status column
		grid.addColumn(recordEvent -> {
			String groupNameString = recordEvent.getGroupNameString();
			boolean isProvisional = groupNameString != null && !groupNameString.trim().isEmpty();
			return isProvisional ? 
				Translator.translate("RecordEvent.PROVISIONAL") : 
				Translator.translate("RecordEvent.OFFICIAL");
		}).setHeader(Translator.translate("RecordEvent.Status")).setAutoWidth(true);

		// Active column (read-only checkbox)
		grid.addComponentColumn(recordEvent -> {
			Checkbox cb = new Checkbox();
			cb.setValue(Boolean.TRUE.equals(recordEvent.getActive()));
			cb.setReadOnly(true);
			return cb;
		}).setHeader(Translator.translate("Active")).setAutoWidth(true).setFlexGrow(0);

		for (Column<RecordEvent> c : grid.getColumns()) {
			c.setResizable(true);
		}

		this.crud.setCrudListener(this);
		if (this.readOnly) {
			this.crud.setClickable(false);
			this.crud.setClickRowToUpdate(false);
			grid.setSelectionMode(SelectionMode.NONE);
			this.crud.setAddOperationVisible(false);
			this.crud.setUpdateOperationVisible(false);
			this.crud.setDeleteOperationVisible(false);
		} else {
			this.crud.setClickRowToUpdate(true);
			grid.setSelectionMode(SelectionMode.MULTI);
		}
		return this.crud;
	}

}
