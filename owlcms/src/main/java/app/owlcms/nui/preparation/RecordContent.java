/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
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
	private ComboBox<String> federationFilter = new ComboBox<>();
	private ComboBox<String> ageGroupFilter = new ComboBox<>();
	private ComboBox<Gender> genderFilter = new ComboBox<>();
	private ComboBox<RecordFilters.ProvisionalFilter> provisionalFilter = new ComboBox<>();
	private ComboBox<RecordFilters.CurrentHistoryFilter> currentHistoryFilter = new ComboBox<>();
	private TextField nameFilter = new TextField();

	// Filter values
	private String federation;
	private String ageGroup;
	private Gender gender;
	private String name;
	
	boolean documentPage;
	private RecordGrid crud;
	private OwlcmsCrudFormFactory<RecordEvent> editingFormFactory;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;

	/**
	 * Instantiates the RecordEvent crudGrid.
	 */
	public RecordContent() {
		this.editingFormFactory = new RecordEditingFormFactory(RecordEvent.class, this);
		GridCrud<RecordEvent> crud = createGrid(this.editingFormFactory);
		defineFilters(crud);
		fillHW(crud, this);
	}

	@Override
	public RecordEvent add(RecordEvent domainObjectToAdd) {
		return this.editingFormFactory.add(domainObjectToAdd);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		String path = event.getLocation().getPath();
		this.documentPage = path.contains("documents");
	}

	public void closeDialog() {
	}

	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		// Add export button for filtered records
		Button exportRecordsButton = createExportRecordsButton();
		
		// Add Clear New Records button (Accept Provisional Records)
		Button clearNewRecordsButton = createClearNewRecordsButton();
		
		// Add Recompute Records button
		Button recomputeRecordsButton = createRecomputeRecordsButton();
		
		// Add Keep Current Records button
		Button keepCurrentRecordsButton = createKeepCurrentRecordsButton();
		
		this.topBar.add(exportRecordsButton, recomputeRecordsButton, clearNewRecordsButton, keepCurrentRecordsButton);

		return this.topBar;
	}

	private Button createExportRecordsButton() {
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
		Button exportButton = downloadDialog.createDownloadButton();
		exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		exportButton.getElement().getStyle().set("margin-right", "1em");
		return exportButton;
	}

	private Button createClearNewRecordsButton() {
		Button clearNewRecordsButton = new Button(Translator.translate("Preparation.ClearNewRecords"),
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
					
					// Clear provisional flags only for the filtered records
					RecordRepository.clearNewRecordsWithFilters(
						getFederation(),
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
		clearNewRecordsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		clearNewRecordsButton.getElement().getStyle().set("margin-right", "1em");
		clearNewRecordsButton.getElement().setAttribute("title", Translator.translate("Preparation.ClearNewRecordsExplanation"));
		return clearNewRecordsButton;
	}

	private Button createRecomputeRecordsButton() {
		Button recomputeRecordsButton = new Button(Translator.translate("Preparation.RecomputeNewRecords"),
			buttonClickEvent -> {
				RecordRepository.recomputeNewRecords();
				// Refresh the grid to show the updated records
				this.crud.refreshGrid();
			});
		recomputeRecordsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		recomputeRecordsButton.getElement().getStyle().set("margin-right", "1em");
		recomputeRecordsButton.getElement().setAttribute("title", Translator.translate("Preparation.RecomputeNewRecordsExplanation"));
		return recomputeRecordsButton;
	}

	private Button createKeepCurrentRecordsButton() {
		Button keepCurrentRecordsButton = new Button(Translator.translate("RecordEvent.KeepCurrentRecords"),
			buttonClickEvent -> {
				try {
					// Use the same filter parameters as the grid display
					String provisionalFilterStr = "ALL";
					if (this.provisionalFilter != null && this.provisionalFilter.getValue() != null) {
						provisionalFilterStr = this.provisionalFilter.getValue().name();
					}
					
					// Keep only current records within the filtered subset
					RecordRepository.keepOnlyCurrentRecordsWithFilters(
						getFederation(),
						getAgeGroup(),
						getGender(),
						getName(),
						provisionalFilterStr
					);
					
					// Refresh the grid to show the updated records
					this.crud.refreshGrid();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		keepCurrentRecordsButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
		keepCurrentRecordsButton.getElement().getStyle().set("margin-right", "1em");
		keepCurrentRecordsButton.getElement().setAttribute("title", Translator.translate("RecordEvent.KeepCurrentRecordsExplanation"));
		return keepCurrentRecordsButton;
	}

	@Override
	public void delete(RecordEvent domainObjectToDelete) {
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
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public RecordEvent update(RecordEvent domainObjectToUpdate) {
		return this.editingFormFactory.update(domainObjectToUpdate);
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
		this.federationFilter.clear();
		this.ageGroupFilter.clear();
		this.genderFilter.clear();
		this.provisionalFilter.setValue(RecordFilters.ProvisionalFilter.ALL);
		this.currentHistoryFilter.setValue(RecordFilters.CurrentHistoryFilter.CURRENT);
		this.nameFilter.clear();
	}

	public String getFederation() {
		return federation;
	}

	public void setFederation(String federation) {
		this.federation = federation;
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

	public RecordFilters.CurrentHistoryFilter getCurrentHistoryFilter() {
		return this.currentHistoryFilter.getValue();
	}

	public void setCurrentHistoryFilter(RecordFilters.CurrentHistoryFilter currentHistoryFilter) {
		this.currentHistoryFilter.setValue(currentHistoryFilter);
	}

	/**
	 * Get filtered records using RecordRepository to ensure consistency between grid and export
	 */
	private List<RecordEvent> getFilteredRecords() {
		// Convert enum values to strings for the repository method
		String provisionalFilterStr = "ALL";
		if (this.provisionalFilter != null && this.provisionalFilter.getValue() != null) {
			provisionalFilterStr = this.provisionalFilter.getValue().name();
		}
		
		String currentHistoryFilterStr = "HISTORY"; // Default to showing all records
		if (this.currentHistoryFilter != null && this.currentHistoryFilter.getValue() != null) {
			currentHistoryFilterStr = this.currentHistoryFilter.getValue().name();
		}
		
		return RecordRepository.findWithFilters(
			getFederation(),
			getAgeGroup(),
			getGender(),
			getName(),
			provisionalFilterStr,
			currentHistoryFilterStr
		);
	}

	/**
	 * Define the filters for the record grid
	 */
	private void defineFilters(GridCrud<RecordEvent> crud) {
		// Federation filter
		this.federationFilter.setPlaceholder(Translator.translate("RecordEvent.Federation"));
		this.federationFilter.setItems(RecordRepository.findDistinctFederations());
		this.federationFilter.setClearButtonVisible(true);
		this.federationFilter.addValueChangeListener(e -> {
			setFederation(e.getValue());
			crud.refreshGrid();
		});
		this.federationFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.federationFilter);

		// Age Group filter
		this.ageGroupFilter.setPlaceholder(Translator.translate("AgeGroup"));
		this.ageGroupFilter.setItems(RecordRepository.findDistinctAgeGroups());
		this.ageGroupFilter.setClearButtonVisible(true);
		this.ageGroupFilter.addValueChangeListener(e -> {
			setAgeGroup(e.getValue());
			crud.refreshGrid();
		});
		this.ageGroupFilter.setWidth("10em");
		crud.getCrudLayout().addFilterComponent(this.ageGroupFilter);

		// Gender filter
		this.genderFilter.setPlaceholder(Translator.translate("Gender"));
		this.genderFilter.setItems(Gender.M, Gender.F);
		this.genderFilter.setItemLabelGenerator(g -> g.asGenderName());
		this.genderFilter.setClearButtonVisible(true);
		this.genderFilter.addValueChangeListener(e -> {
			setGender(e.getValue());
			crud.refreshGrid();
		});
		this.genderFilter.setWidth("8em");
		crud.getCrudLayout().addFilterComponent(this.genderFilter);

		// Name filter (for record name or athlete name)
		this.nameFilter.setPlaceholder(Translator.translate("Name"));
		this.nameFilter.setClearButtonVisible(true);
		this.nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		this.nameFilter.addValueChangeListener(e -> {
			setName(e.getValue());
			crud.refreshGrid();
		});
		this.nameFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.nameFilter);

		// Provisional filter
		NativeLabel provisionalLabel = new NativeLabel(Translator.translate("RecordEvent.Status"));
		this.provisionalFilter.setItems(RecordFilters.ProvisionalFilter.values());
		this.provisionalFilter.setItemLabelGenerator(filter -> Translator.translate(filter.getKey()));
		this.provisionalFilter.setValue(RecordFilters.ProvisionalFilter.ALL);
		this.provisionalFilter.setClearButtonVisible(true);
		this.provisionalFilter.addValueChangeListener(e -> {
			setProvisionalFilter(e.getValue());
			crud.refreshGrid();
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
		this.currentHistoryFilter.setClearButtonVisible(true);
		this.currentHistoryFilter.addValueChangeListener(e -> {
			setCurrentHistoryFilter(e.getValue());
			crud.refreshGrid();
		});
		this.currentHistoryFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.currentHistoryFilter);

		// Clear filters button
		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			clearFilters();
			crud.refreshGrid();
		});
		crud.getCrudLayout().addFilterComponent(clearFilters);
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing a record event
	 * @return
	 */
	private GridCrud<RecordEvent> createGrid(OwlcmsCrudFormFactory<RecordEvent> crudFormFactory) {
		Grid<RecordEvent> grid = new Grid<>(RecordEvent.class, false);
		this.crud = new RecordGrid(RecordEvent.class, new OwlcmsGridLayout(RecordEvent.class), crudFormFactory, grid);
		grid.getThemeNames().add("row-stripes");
		
		// Record identification columns
		grid.addColumn(RecordEvent::getRecordFederation).setHeader(Translator.translate("Competition.federationTitle")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getRecordName).setHeader(Translator.translate("RecordEvent.Name")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getAgeGrp).setHeader(Translator.translate("AgeGroup")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getGender).setHeader(Translator.translate("Gender")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getBwCatString).setHeader(Translator.translate("Category")).setAutoWidth(true);
		
		// Record details columns
		grid.addColumn(re -> re.getRecordLift() != null ? Translator.translate("Record." + re.getRecordLift()) : "")
		    .setHeader(Translator.translate("RecordEvent.Lift")).setAutoWidth(true);
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
		this.crud.setClickRowToUpdate(true);
		grid.setSelectionMode(SelectionMode.SINGLE);
		return this.crud;
	}

}
