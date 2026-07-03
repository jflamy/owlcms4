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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;

import app.owlcms.data.competition.Competition;
import app.owlcms.components.fields.GridField;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.spreadsheet.JXLSExportRecords;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.utils.URLUtils;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.config.Config;
import app.owlcms.data.records.RecordConfig;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.admin.RecordFederationComparisonReport;
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
	protected RadioButtonGroup<RecordFilters.TimeRange> timeRangeFilter = new RadioButtonGroup<>();
	protected CheckboxGroup<RecordFilters.ProvisionalFilter> approvalStatusFilter = new CheckboxGroup<>();
	protected RadioButtonGroup<RecordFilters.CurrentHistoryFilter> historicalFilter = new RadioButtonGroup<>();
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
			String timeRange = getFirstParam(params, "timeRange");
			String approval = getFirstParam(params, "approval");
			String historical = getFirstParam(params, "historical");

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
			if (timeRange != null) {
				try {
					this.timeRangeFilter.setValue(RecordFilters.TimeRange.valueOf(timeRange));
				} catch (IllegalArgumentException ignored) {
				}
			}
			if (approval != null) {
				this.approvalStatusFilter.setValue(parseApprovalParam(approval));
			}
			if (historical != null) {
				try {
					this.historicalFilter.setValue(RecordFilters.CurrentHistoryFilter.valueOf(historical));
				} catch (IllegalArgumentException ignored) {
				}
			}
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
				createImportButton(),
				createDisplayOrderButton(),
				createManageRecordsButton(),
				createEligibilityReportButton());
		row1.setAlignItems(FlexComponent.Alignment.CENTER);
		row1.setPadding(false);
		row1.setSpacing(true);
		if (Config.getCurrent().isRecordRepository()) {
			row1.add(createLogoutButton());
		}

		// Row 2: Provisional / computation actions
		HorizontalLayout row2 = new HorizontalLayout(
				createRecomputeRecordsButton(),
				createAcceptProvisionalRecordsButton(),
				createKeepLatestOfficialRecordsButton());
		row2.setAlignItems(FlexComponent.Alignment.CENTER);
		row2.setPadding(false);
		row2.setSpacing(true);

		this.topBar.add(row1, row2);
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

	protected Button createDisplayOrderButton() {
		Button displayOrderButton = new Button(Translator.translate("Records.DisplayOptions"),
		        buttonClickEvent -> openDisplayOrderDialog());
		displayOrderButton.getElement().getStyle().set("margin-right", "1em");
		displayOrderButton.getElement().setAttribute("title", Translator.translate("Records.DisplayOptions"));
		return displayOrderButton;
	}

	protected Button createManageRecordsButton() {
		Button manageButton = new Button(Translator.translate("Records.ManageButton"),
		        buttonClickEvent -> new RecordManagementDialog(() -> {
			        refreshFilterOptionsFromRepository();
			        this.crud.refreshGrid();
		        }).open());
		manageButton.setIcon(VaadinIcon.LIST_OL.create());
		manageButton.getElement().getStyle().set("margin-right", "1em");
		manageButton.getElement().setAttribute("title", Translator.translate("Records.ManageExplanation"));
		return manageButton;
	}

	protected Button createEligibilityReportButton() {
		Button eligibilityReportButton = new Button(Translator.translate("Records.EligibilityReport"),
		        buttonClickEvent -> {
			        String url = RouteConfiguration.forSessionScope()
			                .getUrl(RecordFederationComparisonReport.class);
			        UI.getCurrent().getPage().open(url, "_blank");
		        });
		eligibilityReportButton.setIcon(VaadinIcon.TABLE.create());
		eligibilityReportButton.getElement().getStyle().set("margin-right", "1em");
		eligibilityReportButton.getElement().setAttribute("title",
		        Translator.translate("Records.EligibilityReportTooltip"));
		return eligibilityReportButton;
	}

	private void openDisplayOrderDialog() {
		RecordConfig current = RecordConfig.getCurrent();

		GridField<String> orderingField = new GridField<>(getOrderedActiveRecordNames(current), true,
		        Translator.translate("Records.NoDisplayOrderRecords"));
		Checkbox showAllCategoriesField = new Checkbox(Translator.translate("Records.AllCategories"));
		showAllCategoriesField.setValue(Boolean.TRUE.equals(current.getShowAllCategoryRecords()));
		Checkbox showAllFederationsField = new Checkbox(Translator.translate("Records.AllFederations"));
		showAllFederationsField.setValue(Boolean.TRUE.equals(current.getShowAllFederations()));

		Dialog dialog = new Dialog();
		dialog.setHeaderTitle(Translator.translate("Records.DisplayOptions"));
		dialog.getHeader().add(createDialogHeaderCloseButton(dialog));
		dialog.setWidth("60em");
		dialog.setCloseOnEsc(true);
		dialog.setCloseOnOutsideClick(true);

		Paragraph instructions = new Paragraph(Translator.translate("Records.OrderingField"));
		instructions.getStyle().set("margin-top", "0");

		Button cancelButton = new Button(Translator.translate("Cancel"), event -> dialog.close());
		Button saveButton = new Button(Translator.translate("Records.UpdateDisplayOptions"), event -> {
			current.setRecordOrder(new ArrayList<>(orderingField.getValue()));
			current.setShowAllCategoryRecords(showAllCategoriesField.getValue());
			current.setShowAllFederations(showAllFederationsField.getValue());
			RecordConfig.setCurrent(current);
			dialog.close();
		});
		saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

		HorizontalLayout buttons = new HorizontalLayout(cancelButton, saveButton);
		buttons.setWidthFull();
		buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

		VerticalLayout content = new VerticalLayout(
		        instructions,
		        orderingField,
		        showAllCategoriesField,
		        showAllFederationsField,
		        buttons);
		content.setPadding(false);
		content.setSpacing(true);
		content.setAlignItems(FlexComponent.Alignment.STRETCH);

		dialog.add(content);
		dialog.open();
	}

	private Button createDialogHeaderCloseButton(Dialog dialog) {
		Button closeButton = new Button(VaadinIcon.CLOSE.create(), event -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		closeButton.getElement().setAttribute("aria-label", Translator.translate("Close"));
		closeButton.getElement().setAttribute("title", Translator.translate("Close"));
		return closeButton;
	}

	private ArrayList<String> getOrderedActiveRecordNames(RecordConfig current) {
		ArrayList<String> activeRecordNames = new ArrayList<>(RecordRepository.findDistinctRecordNames());
		ArrayList<String> orderedActiveNames = current.getRecordOrder() == null
		        ? new ArrayList<>()
		        : new ArrayList<>(current.getRecordOrder());

		orderedActiveNames.removeIf(name -> !activeRecordNames.contains(name));
		for (String activeName : activeRecordNames) {
			if (!orderedActiveNames.contains(activeName)) {
				orderedActiveNames.add(activeName);
			}
		}

		return orderedActiveNames;
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
				List<RecordEvent> visibleRecords = getFilteredRecords();
				ArrayList<Long> visibleRecordIds = new ArrayList<>();
				for (RecordEvent visibleRecord : visibleRecords) {
					if (visibleRecord.getId() != null) {
						visibleRecordIds.add(visibleRecord.getId());
					}
				}

				RecordRepository.acceptProvisionalRecordsByIds(visibleRecordIds);
				this.crud.refreshGrid();
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
			this.timeRangeFilter.setValue(RecordFilters.TimeRange.ALL_RECORDS);
			this.approvalStatusFilter.setValue(defaultApprovalSelection());
			this.historicalFilter.setValue(RecordFilters.CurrentHistoryFilter.CURRENT);
			this.nameFilter.clear();
			setName(null);
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

	public Set<RecordFilters.ProvisionalFilter> getApprovalStatus() {
		return this.approvalStatusFilter.getValue();
	}

	/**
	 * Map the approval-status checkbox selection to the repository provisional-filter string.
	 *
	 * @return "ALL" when both are selected, "PROVISIONAL"/"OFFICIAL" when only one is selected, or
	 *         {@code null} when neither is selected (no records should be shown).
	 */
	private String getProvisionalFilterName() {
		Set<RecordFilters.ProvisionalFilter> selection = this.approvalStatusFilter != null
		        ? this.approvalStatusFilter.getValue()
		        : null;
		boolean provisional = selection != null && selection.contains(RecordFilters.ProvisionalFilter.PROVISIONAL);
		boolean official = selection != null && selection.contains(RecordFilters.ProvisionalFilter.OFFICIAL);
		if (provisional && official) {
			return RecordFilters.ProvisionalFilter.ALL.name();
		}
		if (provisional) {
			return RecordFilters.ProvisionalFilter.PROVISIONAL.name();
		}
		if (official) {
			return RecordFilters.ProvisionalFilter.OFFICIAL.name();
		}
		return null;
	}

	private boolean isThisCompetitionOnly() {
		return this.timeRangeFilter != null
		        && this.timeRangeFilter.getValue() == RecordFilters.TimeRange.THIS_COMPETITION;
	}

	public RecordFilters.CurrentHistoryFilter getCurrentHistoryFilter() {
		return this.historicalFilter.getValue();
	}

	public void setCurrentHistoryFilter(RecordFilters.CurrentHistoryFilter currentHistoryFilter) {
		this.historicalFilter.setValue(currentHistoryFilter);
	}

	private String getCurrentHistoryFilterName() {
		RecordFilters.CurrentHistoryFilter currentHistory = this.historicalFilter != null
		        ? this.historicalFilter.getValue()
		        : null;
		return currentHistory != null ? currentHistory.name() : RecordFilters.CurrentHistoryFilter.CURRENT.name();
	}

	/**
	 * Default approval selection: both provisional and official records.
	 */
	private Set<RecordFilters.ProvisionalFilter> defaultApprovalSelection() {
		Set<RecordFilters.ProvisionalFilter> selection = new LinkedHashSet<>();
		selection.add(RecordFilters.ProvisionalFilter.PROVISIONAL);
		selection.add(RecordFilters.ProvisionalFilter.OFFICIAL);
		return selection;
	}

	/**
	 * Parse the comma-separated {@code approval} URL parameter into a selection set.
	 */
	private Set<RecordFilters.ProvisionalFilter> parseApprovalParam(String approval) {
		Set<RecordFilters.ProvisionalFilter> selection = new LinkedHashSet<>();
		if (approval == null || approval.isBlank()) {
			return selection;
		}
		for (String part : approval.split(",")) {
			try {
				RecordFilters.ProvisionalFilter value = RecordFilters.ProvisionalFilter.valueOf(part.trim());
				if (value == RecordFilters.ProvisionalFilter.PROVISIONAL
				        || value == RecordFilters.ProvisionalFilter.OFFICIAL) {
					selection.add(value);
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		return selection;
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
		RecordFilters.TimeRange tr = this.timeRangeFilter.getValue();
		if (tr != null && tr != RecordFilters.TimeRange.ALL_RECORDS) {
			params.put("timeRange", List.of(tr.name()));
		}
		Set<RecordFilters.ProvisionalFilter> approval = this.approvalStatusFilter.getValue();
		if (approval != null && !approval.equals(defaultApprovalSelection())) {
			List<String> approvalNames = new ArrayList<>();
			if (approval.contains(RecordFilters.ProvisionalFilter.PROVISIONAL)) {
				approvalNames.add(RecordFilters.ProvisionalFilter.PROVISIONAL.name());
			}
			if (approval.contains(RecordFilters.ProvisionalFilter.OFFICIAL)) {
				approvalNames.add(RecordFilters.ProvisionalFilter.OFFICIAL.name());
			}
			params.put("approval", List.of(String.join(",", approvalNames)));
		}
		RecordFilters.CurrentHistoryFilter cv = this.historicalFilter.getValue();
		if (cv != null && cv != RecordFilters.CurrentHistoryFilter.CURRENT) {
			params.put("historical", List.of(cv.name()));
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

		// Neither approval status selected: show nothing.
		if (provisionalFilterStr == null) {
			return List.of();
		}

		String currentHistoryFilterStr = getCurrentHistoryFilterName();

		return RecordRepository.findWithFilters(
			getFederation(),
			getRecordName(),
			getAgeGroup(),
			getGender(),
			getName(),
			provisionalFilterStr,
			currentHistoryFilterStr, null,
			getActiveFilterStr(),
			isThisCompetitionOnly()
		);
	}

	/**
	 * Define the filters for the record grid
	 */
	protected void defineFilters(GridCrud<RecordEvent> crud) {
		// All filter controls are laid out ourselves in a VerticalLayout (row1, row2)
		// so that row2 (Time Range / Approval Status / Historical Data) is guaranteed
		// to appear on its own line, regardless of the crudui library's own filter
		// row wrap behavior.
		HorizontalLayout filterRow1 = new HorizontalLayout();
		filterRow1.setPadding(false);
		filterRow1.setSpacing(true);
		filterRow1.setAlignItems(FlexComponent.Alignment.CENTER);
		filterRow1.getStyle().set("flex-wrap", "wrap");

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
		filterRow1.add(this.federationFilter);
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
		filterRow1.add(this.recordNameFilter);

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
		filterRow1.add(this.ageGroupFilter);
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
		filterRow1.add(this.genderFilter);

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
		filterRow1.add(this.nameFilter);

		// Second filter row: Time Range, Approval Status, Historical Data laid out
		// horizontally. The three axes are independent of each other.
		HorizontalLayout filterRow2 = new HorizontalLayout();
		filterRow2.setPadding(false);
		filterRow2.setSpacing(true);
		filterRow2.setAlignItems(FlexComponent.Alignment.CENTER);
		filterRow2.getStyle().set("column-gap", "4.5em");
		filterRow2.getStyle().set("flex-wrap", "wrap");

		// Row 1: Time Range (only meaningful when there is a current competition).
		// Record-repository mode has no notion of a competition, so hide the whole row.
		if (!Config.getCurrent().isRecordRepository()) {
			this.timeRangeFilter.setItems(RecordFilters.TimeRange.values());
			this.timeRangeFilter.setItemLabelGenerator(filter -> Translator.translate(
			        filter == RecordFilters.TimeRange.THIS_COMPETITION
			                ? "RecordEvent.ThisCompetitionOnly"
			                : "RecordEvent.AllRecords"));
			this.timeRangeFilter.setValue(RecordFilters.TimeRange.ALL_RECORDS);
			this.timeRangeFilter.addValueChangeListener(e -> {
				if (!this.updatingFilters) {
					crud.refreshGrid();
					updateUrlParameters();
				}
			});
			filterRow2.add(buildFilterBlock("RecordEvent.TimeRangeTitle", this.timeRangeFilter));
		} else {
			this.timeRangeFilter.setValue(RecordFilters.TimeRange.ALL_RECORDS);
		}

		// Row 2: Approval Status (provisional / official), both selected by default.
		this.approvalStatusFilter.setItems(
		        RecordFilters.ProvisionalFilter.PROVISIONAL, RecordFilters.ProvisionalFilter.OFFICIAL);
		this.approvalStatusFilter.setItemLabelGenerator(filter -> Translator.translate(filter.getKey()));
		this.approvalStatusFilter.setValue(defaultApprovalSelection());
		this.approvalStatusFilter.addValueChangeListener(e -> {
			if (!this.updatingFilters) {
				crud.refreshGrid();
				updateUrlParameters();
			}
		});
		filterRow2.add(buildFilterBlock("RecordEvent.ApprovalStatusTitle", this.approvalStatusFilter));

		// Row 3: Historical Data (only current vs. include superseded).
		this.historicalFilter.setItems(
		        RecordFilters.CurrentHistoryFilter.CURRENT, RecordFilters.CurrentHistoryFilter.HISTORY);
		this.historicalFilter.setItemLabelGenerator(filter -> Translator.translate(
		        filter == RecordFilters.CurrentHistoryFilter.CURRENT
		                ? "RecordEvent.OnlyCurrentRecords"
		                : "RecordEvent.IncludeSuperseded"));
		this.historicalFilter.setValue(RecordFilters.CurrentHistoryFilter.CURRENT);
		this.historicalFilter.addValueChangeListener(e -> {
			if (!this.updatingFilters) {
				crud.refreshGrid();
				updateUrlParameters();
			}
		});
		filterRow2.add(buildFilterBlock("RecordEvent.HistoricalDataTitle", this.historicalFilter));

		// Clear filters button (stays on row 1)
		Button clearFiltersButton = new Button(null, VaadinIcon.CLOSE.create());
		clearFiltersButton.addClickListener(event -> {
			clearFilters();
			crud.refreshGrid();
			updateUrlParameters();
		});
		filterRow1.add(clearFiltersButton);

		// Stack row1 above row2 ourselves so the new block is always on its own line.
		VerticalLayout filterRows = new VerticalLayout(filterRow1, filterRow2);
		filterRows.setPadding(false);
		filterRows.setSpacing(false);
		filterRows.getStyle().set("gap", "0.5em");
		crud.getCrudLayout().addFilterComponent(filterRows);
		hideCrudFilterIcon();
	}

	private void hideCrudFilterIcon() {
		com.vaadin.flow.component.Component filterLayout = this.crud.getOwlcmsGridLayout().getFilterLayout();
		filterLayout.getChildren()
		        .filter(component -> "vaadin-icon".equals(component.getElement().getTag()))
		        .forEach(component -> component.getElement().getStyle().set("visibility", "hidden"));
		filterLayout.getElement().getChildren()
		        .filter(element -> "vaadin-icon".equals(element.getTag()))
		        .forEach(element -> {
			        element.getStyle().set("visibility", "hidden");
			        element.getStyle().remove("display");
			        element.getStyle().remove("width");
			        element.getStyle().remove("min-width");
			        element.getStyle().remove("margin");
			        element.getStyle().remove("padding");
		        });
	}

	private String getActiveFilterStr() {
		return "ACTIVE";
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
	}

	/**
	 * Build one filter block: label above the filter control.
	 */
	private VerticalLayout buildFilterBlock(String labelKey, com.vaadin.flow.component.Component control) {
		NativeLabel label = new NativeLabel(Translator.translate(labelKey));
		label.getStyle().set("font-weight", "600");
		VerticalLayout block = new VerticalLayout(label, control);
		block.setWidth(null);
		block.setAlignItems(FlexComponent.Alignment.START);
		block.setSpacing(false);
		block.setPadding(false);
		block.getStyle().set("flex", "0 0 auto");
		control.getElement().getStyle().set("width", "auto");
		return block;
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
			grid.setSelectionMode(SelectionMode.NONE);
		}
		return this.crud;
	}

}
