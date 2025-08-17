/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
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
	private ComboBox<ProvisionalFilter> provisionalFilter = new ComboBox<>();
	private TextField nameFilter = new TextField();

	public enum ProvisionalFilter {
		ALL,
		PROVISIONAL,
		OFFICIAL;

		public String getKey() {
			return "RecordEvent." + this.name();
		}
	}
	
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

		return this.topBar;
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
		List<RecordEvent> allRecords = RecordRepository.findAll();
		List<RecordEvent> filteredRecords = filterRecords(allRecords);
		return filteredRecords.stream().sorted().collect(Collectors.toList());
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
		this.provisionalFilter.setValue(ProvisionalFilter.ALL);
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

	public ProvisionalFilter getProvisionalFilter() {
		return this.provisionalFilter.getValue();
	}

	public void setProvisionalFilter(ProvisionalFilter provisionalFilter) {
		this.provisionalFilter.setValue(provisionalFilter);
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

		// Provisional filter
		NativeLabel provisionalLabel = new NativeLabel(Translator.translate("RecordEvent.Status"));
		this.provisionalFilter.setItems(ProvisionalFilter.values());
		this.provisionalFilter.setItemLabelGenerator(filter -> Translator.translate(filter.getKey()));
		this.provisionalFilter.setValue(ProvisionalFilter.ALL);
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

		// Clear filters button
		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			clearFilters();
			crud.refreshGrid();
		});
		crud.getCrudLayout().addFilterComponent(clearFilters);
	}

	/**
	 * Filter the records based on the current filter values
	 */
	private List<RecordEvent> filterRecords(List<RecordEvent> records) {
		Stream<RecordEvent> stream = records.stream()
			.filter(r -> {
				// Filter by federation
				if (getFederation() != null && !getFederation().isEmpty()) {
					String recordFederation = r.getRecordFederation();
					if (recordFederation == null || !recordFederation.equals(getFederation())) {
						return false;
					}
				}
				return true;
			})
			.filter(r -> {
				// Filter by age group
				if (getAgeGroup() != null && !getAgeGroup().isEmpty()) {
					String recordAgeGroup = r.getAgeGrp();
					if (recordAgeGroup == null || !recordAgeGroup.equals(getAgeGroup())) {
						return false;
					}
				}
				return true;
			})
			.filter(r -> {
				// Filter by gender
				if (getGender() != null) {
					Gender recordGender = r.getGender();
					if (recordGender == null || !recordGender.equals(getGender())) {
						return false;
					}
				}
				return true;
			})
			.filter(r -> {
				// Filter by provisional status
				ProvisionalFilter provisionalFilter = getProvisionalFilter();
				if (provisionalFilter != null && provisionalFilter != ProvisionalFilter.ALL) {
					String groupNameString = r.getGroupNameString();
					boolean isProvisional = groupNameString != null && !groupNameString.trim().isEmpty();
					
					if (provisionalFilter == ProvisionalFilter.PROVISIONAL && !isProvisional) {
						return false;
					}
					if (provisionalFilter == ProvisionalFilter.OFFICIAL && isProvisional) {
						return false;
					}
				}
				return true;
			})
			.filter(r -> {
				// Filter by name (search in record name and athlete name)
				if (getName() != null && !getName().isEmpty()) {
					String filterName = getName().toLowerCase();
					String recordName = r.getRecordName();
					String athleteName = r.getAthleteName();
					
					boolean recordNameMatch = recordName != null && recordName.toLowerCase().contains(filterName);
					boolean athleteNameMatch = athleteName != null && athleteName.toLowerCase().contains(filterName);
					
					if (!recordNameMatch && !athleteNameMatch) {
						return false;
					}
				}
				return true;
			});

		return stream.collect(Collectors.toList());
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
