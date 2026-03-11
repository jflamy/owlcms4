/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

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
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSExportRecords;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Public read-only records page for recordsOnly mode.
 *
 * Shows the records grid with only Export and Login buttons.
 * No editing, deleting, recomputing, or other admin actions.
 *
 * This page overrides the inherited AuthorizationDispatch behavior so it remains publicly accessible.
 */
@SuppressWarnings("serial")
@Route(value = "publicRecords", layout = OwlcmsLayout.class)
public class PublicRecordsContent extends BaseContent implements CrudListener<RecordEvent>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(PublicRecordsContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	// Filter fields
	private ComboBox<String> federationFilter = new ComboBox<>();
	private ComboBox<String> recordNameFilter = new ComboBox<>();
	private ComboBox<String> ageGroupFilter = new ComboBox<>();
	private ComboBox<Gender> genderFilter = new ComboBox<>();
	private TextField nameFilter = new TextField();

	// Filter values
	private String federation;
	private String recordName;
	private String ageGroup;
	private Gender gender;
	private String name;

	private RecordGrid crud;
	private OwlcmsLayout routerLayout;
	private FlexLayout topBar;

	public PublicRecordsContent() {
		OwlcmsCrudFormFactory<RecordEvent> formFactory = new RecordEditingFormFactory(RecordEvent.class, null);
		GridCrud<RecordEvent> crud = createGrid(formFactory);
		defineFilters(crud);
		fillHW(crud, this);
	}

	/**
	 * Override the inherited AuthorizationDispatch behavior so this page stays public.
	 */
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		// Public page — no login required
	}

	@Override
	public RecordEvent add(RecordEvent domainObjectToAdd) {
		// read-only — should not be called
		return domainObjectToAdd;
	}

	@Override
	public void delete(RecordEvent domainObjectToDelete) {
		// read-only — should not be called
	}

	@Override
	public RecordEvent update(RecordEvent domainObjectToUpdate) {
		// read-only — should not be called
		return domainObjectToUpdate;
	}

	@Override
	public Collection<RecordEvent> findAll() {
		return getFilteredRecords();
	}

	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();
		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		Button exportRecordsButton = createExportRecordsButton();

		Button loginButton = new Button(Translator.translate("Edit"),
		        e -> UI.getCurrent().navigate(RecordContent.class));
		loginButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
		loginButton.getElement().getStyle().set("margin-right", "1em");

		this.topBar.add(exportRecordsButton, loginButton);
		return this.topBar;
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	@Override
	public String getPageTitle() {
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
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// read-only, no special attach behavior needed
	}

	private Button createExportRecordsButton() {
		JXLSDownloader downloadDialog = new JXLSDownloader(
		        () -> {
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

	private List<RecordEvent> getFilteredRecords() {
		return RecordRepository.findWithFilters(
		        federation, recordName, ageGroup, gender, name,
		        "ALL",
		        RecordRepository.normalizeCurrentHistoryFilter("ALL", "CURRENT"),
		        null);
	}

	private void defineFilters(GridCrud<RecordEvent> crud) {
		this.federationFilter.setPlaceholder(Translator.translate("RecordEvent.Federation"));
		this.federationFilter.setItems(RecordRepository.findDistinctFederations());
		this.federationFilter.setClearButtonVisible(true);
		this.federationFilter.addValueChangeListener(e -> {
			this.federation = e.getValue();
			crud.refreshGrid();
		});
		this.federationFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.federationFilter);

		this.recordNameFilter.setPlaceholder(Translator.translate("Records.RecordName"));
		this.recordNameFilter.setItems(RecordRepository.findDistinctRecordNames());
		this.recordNameFilter.setClearButtonVisible(true);
		this.recordNameFilter.addValueChangeListener(e -> {
			this.recordName = e.getValue();
			crud.refreshGrid();
		});
		this.recordNameFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.recordNameFilter);

		this.ageGroupFilter.setPlaceholder(Translator.translate("AgeGroup"));
		this.ageGroupFilter.setItems(RecordRepository.findDistinctAgeGroups());
		this.ageGroupFilter.setClearButtonVisible(true);
		this.ageGroupFilter.addValueChangeListener(e -> {
			this.ageGroup = e.getValue();
			crud.refreshGrid();
		});
		this.ageGroupFilter.setWidth("10em");
		crud.getCrudLayout().addFilterComponent(this.ageGroupFilter);

		this.genderFilter.setPlaceholder(Translator.translate("Gender"));
		this.genderFilter.setItems(Gender.M, Gender.F);
		this.genderFilter.setItemLabelGenerator(g -> g.asGenderName());
		this.genderFilter.setClearButtonVisible(true);
		this.genderFilter.addValueChangeListener(e -> {
			this.gender = e.getValue();
			crud.refreshGrid();
		});
		this.genderFilter.setWidth("8em");
		crud.getCrudLayout().addFilterComponent(this.genderFilter);

		this.nameFilter.setPlaceholder(Translator.translate("Name"));
		this.nameFilter.setClearButtonVisible(true);
		this.nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		this.nameFilter.addValueChangeListener(e -> {
			this.name = e.getValue();
			crud.refreshGrid();
		});
		this.nameFilter.setWidth("12em");
		crud.getCrudLayout().addFilterComponent(this.nameFilter);

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			this.federationFilter.clear();
			this.recordNameFilter.clear();
			this.ageGroupFilter.clear();
			this.genderFilter.clear();
			this.nameFilter.clear();
			crud.refreshGrid();
		});
		crud.getCrudLayout().addFilterComponent(clearFilters);
	}

	private GridCrud<RecordEvent> createGrid(OwlcmsCrudFormFactory<RecordEvent> crudFormFactory) {
		Grid<RecordEvent> grid = new Grid<>(RecordEvent.class, false);
		this.crud = new RecordGrid(RecordEvent.class, new OwlcmsGridLayout(RecordEvent.class), crudFormFactory, grid);
		grid.getThemeNames().add("row-stripes");

		grid.addColumn(RecordEvent::getRecordFederation).setHeader(Translator.translate("Competition.federationTitle")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getRecordName).setHeader(Translator.translate("RecordEvent.Name")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getAgeGrp).setHeader(Translator.translate("AgeGroup")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getGender).setHeader(Translator.translate("Gender")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getBwCatString).setHeader(Translator.translate("Category")).setAutoWidth(true);
		grid.addColumn(re -> re.getRecordLift() != null ? Translator.translate("Record." + re.getRecordLift()) : "")
		        .setHeader(Translator.translate("RecordEvent.Lift")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getRecordValue).setHeader(Translator.translate("RecordEvent.Value")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getAthleteName).setHeader(Translator.translate("RecordEvent.AthleteName")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getNation).setHeader(Translator.translate("RecordEvent.Nation")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getRecordDate).setHeader(Translator.translate("RecordEvent.Date")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getEvent).setHeader(Translator.translate("RecordEvent.Event")).setAutoWidth(true);
		grid.addColumn(RecordEvent::getEventLocation).setHeader(Translator.translate("RecordEvent.EventLocation")).setAutoWidth(true);
		grid.addColumn(recordEvent -> {
			String groupNameString = recordEvent.getGroupNameString();
			boolean isProvisional = groupNameString != null && !groupNameString.trim().isEmpty();
			return isProvisional
			        ? Translator.translate("RecordEvent.PROVISIONAL")
			        : Translator.translate("RecordEvent.OFFICIAL");
		}).setHeader(Translator.translate("RecordEvent.Status")).setAutoWidth(true);

		for (Column<RecordEvent> c : grid.getColumns()) {
			c.setResizable(true);
		}

		this.crud.setCrudListener(this);
		this.crud.setClickable(false);
		this.crud.setClickRowToUpdate(false);
		grid.setSelectionMode(SelectionMode.NONE);

		// Hide add/edit/delete buttons — read-only
		this.crud.setAddOperationVisible(false);
		this.crud.setUpdateOperationVisible(false);
		this.crud.setDeleteOperationVisible(false);

		return this.crud;
	}
}
