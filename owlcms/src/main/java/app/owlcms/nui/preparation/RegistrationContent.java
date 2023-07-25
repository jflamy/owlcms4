/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.components.DownloadDialog;
import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.NAthleteRegistrationFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCardsDocs;
import app.owlcms.spreadsheet.JXLSCategoriesListDocs;
import app.owlcms.spreadsheet.JXLSStartingListDocs;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.NaturalOrderComparator;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class RegistrationContent
 *
 * Defines the toolbar and the table for editing registration data on athletes.
 *
 */
@SuppressWarnings("serial")
@Route(value = "preparation/athletes", layout = OwlcmsLayout.class)
@CssImport(value = "./styles/shared-styles.css")
public class RegistrationContent extends VerticalLayout implements CrudListener<Athlete>, OwlcmsContent, FOPParameters {

	final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
	private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	protected OwlcmsCrudGrid<Athlete> crudGrid;
	private Group group;
	protected ComboBox<Gender> genderFilter = new ComboBox<>();
	protected ComboBox<String> teamFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();
	protected TextField lastNameFilter = new TextField();
	private Location location;
	private UI locationUI;
	private OwlcmsLayout routerLayout;

	protected JXLSStartingListDocs startingXlsWriter;
	protected JXLSCategoriesListDocs categoriesXlsWriter;
	protected JXLSCardsDocs cardsXlsWriter;

	private ComboBox<Boolean> weighedInFilter = new ComboBox<>();
//    private Group group;
	private ComboBox<Group> groupSelect;
	protected GroupSelectionMenu topBarMenu;

	protected FlexLayout topBar;

	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Category category;
	private Gender gender;
	private Platform platform;
	private AgeDivision ageDivision;
	private String ageGroupPrefix;
	private String lastName;
	private AgeGroup ageGroup;
	private Boolean weighedIn;
	private String team;

	/**
	 * Instantiates the athlete crudGrid
	 */
	public RegistrationContent() {
		init();
	}

	@Override
	public Athlete add(Athlete athlete) {
		if (athlete.getGroup() == null && getGroup() != null) {
			athlete.setGroup(getGroup());
		}
		((OwlcmsCrudFormFactory<Athlete>) crudGrid.getCrudFormFactory()).add(athlete);
		return athlete;
	}

	@Override
	public FlexLayout createMenuArea() {
		createTopBarGroupSelect();

		Button bwButton = createBWButton();
		Button categoriesListButton = createCategoriesListButton();
		Button teamsListButton = createTeamsListButton();

		Button drawLots = new Button(Translator.translate("DrawLotNumbers"), (e) -> {
			drawLots();
		});

		Button deleteAthletes = new Button(Translator.translate("DeleteAthletes"), (e) -> {
			new ConfirmationDialog(Translator.translate("DeleteAthletes"),
			        Translator.translate("Warning_DeleteAthletes"),
			        Translator.translate("Done_period"), () -> {
				        deleteAthletes();
			        }).open();

		});
		deleteAthletes.getElement().setAttribute("title", Translator.translate("DeleteAthletes_forListed"));

		Button clearLifts = new Button(Translator.translate("ClearLifts"), (e) -> {
			new ConfirmationDialog(Translator.translate("ClearLifts"),
			        Translator.translate("Warning_ClearAthleteLifts"),
			        Translator.translate("LiftsCleared"), () -> {
				        clearLifts();
			        }).open();
		});
		deleteAthletes.getElement().setAttribute("title", Translator.translate("ClearLifts_forListed"));

		Button resetCats = new Button(Translator.translate("ResetCategories.ResetAthletes"), (e) -> {
			new ConfirmationDialog(
			        Translator.translate("ResetCategories.ResetCategories"),
			        Translator.translate("ResetCategories.Warning_ResetCategories"),
			        Translator.translate("ResetCategories.CategoriesReset"), () -> {
				        resetCategories();
			        }).open();
		});
		resetCats.getElement().setAttribute("title", Translator.translate("ResetCategories.ResetCategoriesMouseOver"));

		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		FlexLayout buttons = new FlexLayout(
				new Label(Translator.translate("Preparation")),
				drawLots, deleteAthletes, clearLifts,
		        resetCats, hr, 
		        new Label(Translator.translate("Entries")),
		        bwButton, categoriesListButton, teamsListButton);
		buttons.getStyle().set("flex-wrap", "wrap");
		buttons.getStyle().set("gap", "1ex");
		buttons.getStyle().set("margin-left", "5em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar = new FlexLayout();
		topBar.getStyle().set("flex", "100 1");
		topBar.removeAll();
		topBar.add(topBarMenu, buttons);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		return topBar;
	}

	@Override
	public void delete(Athlete athlete) {
		((OwlcmsCrudFormFactory<Athlete>) crudGrid.getCrudFormFactory()).delete(athlete);
		return;
	}

	/**
	 * The refresh button on the toolbar; also called by refreshGrid when the group
	 * is changed.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		List<Athlete> findFiltered = AthleteRepository.findFiltered(getLastName(), getGroup(),
		        getCategory(), getAgeGroup(), getAgeDivision(),
		        getGender(), getWeighedIn(), getTeam(), -1, -1);
		AthleteSorter.registrationOrder(findFiltered);
		startingXlsWriter.setSortedAthletes(findFiltered);
		List<Athlete> c = AthleteSorter.displayOrderCopy(findFiltered);
		categoriesXlsWriter.setSortedAthletes(c);
		updateURLLocations();
		return findFiltered;
	}

	/**
	 * @return the groupFilter
	 */
	public ComboBox<Group> getGroupFilter() {
		return groupFilter;
	}

	public ComboBox<Group> getGroupSelect() {
		return groupSelect;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
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
		return Translator.translate("EditRegisteredAthletes");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	public void refresh() {
		crudGrid.refreshGrid();
	}

	public void refreshCrudGrid() {
		crudGrid.refreshGrid();
	}

	@Override
	public void setHeaderContent() {
		routerLayout.setMenuTitle(getPageTitle());
		routerLayout.setMenuArea(createMenuArea());
		routerLayout.showLocaleDropdown(false);
		routerLayout.setDrawerOpened(false);
		routerLayout.updateHeader(true);
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	/**
	 * Parse the http query parameters
	 *
	 * Note: because we have the @Route, the parameters are parsed *before* our
	 * parent layout is created.
	 *
	 * @param event     Vaadin navigation event
	 * @param parameter null in this case -- we don't want a vaadin "/" parameter.
	 *                  This allows us to add query parameters instead.
	 *
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
	 *      java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		setLocation(event.getLocation());
		setLocationUI(event.getUI());
		QueryParameters queryParameters = getLocation().getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters(); // immutable
		HashMap<String, List<String>> params = new HashMap<>(parametersMap);

		// logger.trace("parsing query parameters RegistrationContent");
		List<String> groupNames = params.get("group");
		// logger.trace("groupNames = {}", groupNames);
		if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
			String groupName = groupNames.get(0);
			groupName = URLDecoder.decode(groupName, StandardCharsets.UTF_8);
			setGroup(GroupRepository.findByName(groupName));
		} else {
			setGroup(null);
		}
		if (getGroup() != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(getGroup().getName())));
			OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
			crudGrid.setCrudFormFactory(crudFormFactory);
		} else {
			params.remove("group");
		}

		params.remove("fop");

		// change the URL to reflect group
		event.getUI().getPage().getHistory().replaceState(null,
		        new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Override
	public Athlete update(Athlete athlete) {
		OwlcmsSession.setAttribute("weighIn", athlete);
		Athlete a = ((OwlcmsCrudFormFactory<Athlete>) crudGrid.getCrudFormFactory()).update(athlete);
		OwlcmsSession.setAttribute("weighIn", null);
		return a;
	}

	protected Button createBWButton() {
		String resourceDirectoryLocation = "/templates/bwStart";
		String title = Translator.translate("BodyWeightCategories");
		String downloadedFilePrefix = "bwStartingList";

		DownloadDialog startingListFactory = new DownloadDialog(
		        () -> {
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        findAll();
			        List<Athlete> sortedAthletes = startingXlsWriter.getSortedAthletes();
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationBWCopy(sortedAthletes));
			        startingXlsWriter.createAgeGroupColumns(10, 7);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedStartListTemplateFileName,
		        Competition::setStartListTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return startingListFactory.createTopBarDownloadButton();
	}

	protected Button createCategoriesListButton() {
		String resourceDirectoryLocation = "/templates/categories";
		String title = Translator.translate("StartingList.Categories");
		String downloadedFilePrefix = "categories";

		DownloadDialog startingListFactory = new DownloadDialog(
		        () -> {
			        // group may have been edited since the page was loaded
			        categoriesXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        participationFindAll();
			        return categoriesXlsWriter;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedCategoriesListTemplateFileName,
		        Competition::setCategoriesListTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return startingListFactory.createTopBarDownloadButton();
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected OwlcmsCrudGrid<Athlete> createCrudGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		Grid<Athlete> grid = new Grid<>(Athlete.class, false);

		grid.getThemeNames().add("row-stripes");
		grid.getThemeNames().add("compact");
		grid.addColumn("lotNumber").setHeader(Translator.translate("Lot")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("lastName").setHeader(Translator.translate("LastName")).setWidth("20ch");
		grid.addColumn("firstName").setHeader(Translator.translate("FirstName"));
		grid.addColumn("team").setHeader(Translator.translate("Team")).setAutoWidth(true);
		grid.addColumn("yearOfBirth").setHeader(Translator.translate("BirthDate")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("gender").setHeader(Translator.translate("Gender")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("ageGroup").setHeader(Translator.translate("AgeGroup")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("category").setHeader(Translator.translate("Category")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(new NumberRenderer<>(Athlete::getBodyWeight, "%.2f", this.getLocale()))
		        .setSortProperty("bodyWeight")
		        .setHeader(Translator.translate("BodyWeight")).setAutoWidth(true).setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("group").setHeader(Translator.translate("Group")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("eligibleCategories").setHeader(Translator.translate("Registration.EligibleCategories"))
		        .setAutoWidth(true);
		grid.addColumn("entryTotal").setHeader(Translator.translate("EntryTotal")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("federationCodes").setHeader(Translator.translate("Registration.FederationCodesShort"))
		        .setAutoWidth(true);

		OwlcmsCrudGrid<Athlete> crudGrid = new OwlcmsCrudGrid<>(Athlete.class, new OwlcmsGridLayout(Athlete.class) {

			@Override
			public void hideForm() {
				// registration should be the same as weigh-in (set an attribute to prevent
				// interference with validations)
				super.hideForm();
				logger.trace("clearing {}", OwlcmsSession.getAttribute("weighIn"));
				OwlcmsSession.setAttribute("weighIn", null);
			}
		},
		        crudFormFactory, grid);
		crudGrid.setCrudListener(this);
		crudGrid.setClickRowToUpdate(true);
		return crudGrid;
	}

	/**
	 * Define the form used to edit a given athlete.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
		OwlcmsCrudFormFactory<Athlete> athleteEditingFormFactory;
		athleteEditingFormFactory = new NAthleteRegistrationFormFactory(Athlete.class,
		        group);
		// createFormLayout(athleteEditingFormFactory);
		return athleteEditingFormFactory;
	}

	protected Button createTeamsListButton() {
		String resourceDirectoryLocation = "/templates/teams";
		String title = Translator.translate("StartingList.Teams");
		String downloadedFilePrefix = "teams";

		DownloadDialog startingListFactory = new DownloadDialog(
		        () -> {
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        findAll();
			        // List<Athlete> sortedAthletes = startingXlsWriter.getSortedAthletes();
			        // startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(sortedAthletes));
			        startingXlsWriter.createTeamColumns(9, 6);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedTeamsListTemplateFileName,
		        Competition::setTeamsListTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return startingListFactory.createTopBarDownloadButton();
	}

	protected void createTopBarGroupSelect() {
		// there is already all the SQL filtering logic for the group attached
		// hidden field in the crudGrid part of the page so we just set that
		// filter.

		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());

		OwlcmsSession.withFop(fop -> {
			// logger.debug("initial setting group to {} {}", getCurrentGroup(),
			// LoggerUtils.whereFrom());
			getGroupFilter().setValue(getGroup());
			// switching to group "*" is understood to mean all groups
			topBarMenu = new GroupSelectionMenu(groups, getGroup(),
			        fop,
			        (g1) -> doSwitchGroup(g1),
			        (g1) -> doSwitchGroup(new Group("*")),
			        null,
			        Translator.translate("AllGroups"));
		});
	}

	/**
	 * The filters at the top of the crudGrid
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */

	protected void defineFilters(OwlcmsCrudGrid<Athlete> crudGrid) {
		lastNameFilter.setPlaceholder(Translator.translate("LastName"));
		lastNameFilter.setClearButtonVisible(true);
		lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		lastNameFilter.addValueChangeListener(e -> {
			setLastName(e.getValue());
			crudGrid.refreshGrid();
		});
		lastNameFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(lastNameFilter);

		teamFilter.setPlaceholder(Translator.translate("Team"));
		teamFilter.setItems(AthleteRepository.findAllTeams());
		teamFilter.setClearButtonVisible(true);
		teamFilter.addValueChangeListener(e -> {
			setTeam(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(teamFilter);

		ageDivisionFilter.setPlaceholder(Translator.translate("AgeDivision"));
		ageDivisionFilter.setItems(AgeDivision.findAll());
		ageDivisionFilter.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
		ageDivisionFilter.setClearButtonVisible(true);
		ageDivisionFilter.addValueChangeListener(e -> {
			setAgeDivision(e.getValue());
			crudGrid.refreshGrid();
		});
		ageDivisionFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(ageDivisionFilter);

		ageGroupFilter.setPlaceholder(Translator.translate("AgeGroup"));
		ageGroupFilter.setItems(AgeGroupRepository.findAll());
		// ageGroupFilter.setItemLabelGenerator(AgeDivision::name);
		ageGroupFilter.setClearButtonVisible(true);
		ageGroupFilter.addValueChangeListener(e -> {
			setAgeGroup(e.getValue());
			crudGrid.refreshGrid();
		});
		ageGroupFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(ageGroupFilter);

		categoryFilter.setPlaceholder(Translator.translate("Category"));
		categoryFilter.setItems(CategoryRepository.findActive());
		categoryFilter.setItemLabelGenerator(Category::getTranslatedName);
		categoryFilter.setClearButtonVisible(true);
		categoryFilter.addValueChangeListener(e -> {
			setCategory(e.getValue());
			crudGrid.refreshGrid();
		});
		categoryFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(categoryFilter);

		groupFilter.setPlaceholder(Translator.translate("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());
		groupFilter.setItems(groups);
		groupFilter.setItemLabelGenerator(Group::getName);
		groupFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
			setGroup(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), e.getValue());
		});
		groupFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(groupFilter);
		groupFilter.getStyle().set("display", "none");

		weighedInFilter.setPlaceholder(Translator.translate("Weighed_in_p"));
		weighedInFilter.setItems(Boolean.TRUE, Boolean.FALSE);
		weighedInFilter.setItemLabelGenerator((i) -> {
			return i ? Translator.translate("Weighed") : Translator.translate("Not_weighed");
		});
		weighedInFilter.setClearButtonVisible(true);
		weighedInFilter.addValueChangeListener(e -> {
			setWeighedIn(e.getValue());
			crudGrid.refreshGrid();
		});
		weighedInFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(weighedInFilter);

		genderFilter.setPlaceholder(Translator.translate("Gender"));
		genderFilter.setItems(Gender.M, Gender.F);
		genderFilter.setItemLabelGenerator((i) -> {
			return i == Gender.M ? Translator.translate("Gender.Men") : Translator.translate("Gender.Women");
		});
		genderFilter.setClearButtonVisible(true);
		genderFilter.addValueChangeListener(e -> {
			setGender(e.getValue());
			crudGrid.refreshGrid();
		});
		genderFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(genderFilter);

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			lastNameFilter.clear();
			ageGroupFilter.clear();
			ageDivisionFilter.clear();
			categoryFilter.clear();
			// groupFilter.clear();
			weighedInFilter.clear();
			genderFilter.clear();
		});
		lastNameFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	protected void errorNotification() {
		Label content = new Label(Translator.translate("Select_group_first"));
		content.getElement().setAttribute("theme", "error");
		Button buttonInside = new Button(Translator.translate("GotIt"));
		buttonInside.getElement().setAttribute("theme", "error primary");
		VerticalLayout verticalLayout = new VerticalLayout(content, buttonInside);
		verticalLayout.setAlignItems(Alignment.CENTER);
		Notification notification = new Notification(verticalLayout);
		notification.setDuration(3000);
		buttonInside.addClickListener(event -> notification.close());
		notification.setPosition(Position.MIDDLE);
		notification.open();
	}

	/**
	 * @return the ageDivision
	 */
	protected AgeDivision getAgeDivision() {
		return ageDivision;
	}

	protected AgeGroup getAgeGroup() {
		return ageGroup;
	}

	/**
	 * @return the ageGroupPrefix
	 */
	protected String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	protected Category getCategory() {
		return category;
	}

	protected Category getCategoryValue() {
		return getCategory();
	}

	protected Gender getGender() {
		return gender;
	}

	protected Group getGroup() {
		return group;
	}

	protected String getLastName() {
		return lastName;
	}

	protected Platform getPlatform() {
		return platform;
	}

	protected String getTeam() {
		return team;
	}

	protected Boolean getWeighedIn() {
		return weighedIn;
	}

	protected void init() {
		cardsXlsWriter = new JXLSCardsDocs();
		startingXlsWriter = new JXLSStartingListDocs();
		categoriesXlsWriter = new JXLSCategoriesListDocs();
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
		crudGrid = createCrudGrid(crudFormFactory);
		defineFilters(crudGrid);
		fillHW(crudGrid, this);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	protected void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	protected void setAgeGroup(AgeGroup value) {
		this.ageGroup = value;

	}

	protected void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	protected void setCategory(Category category) {
		this.category = category;
	}

	protected void setGender(Gender value) {
		this.gender = value;
	}

	protected void setGroup(Group currentGroup) {
		this.group = currentGroup;
	}

	/**
	 * @param groupSelect the groupSelect to set
	 */
	protected void setGroupSelect(ComboBox<Group> groupSelect) {
		this.groupSelect = groupSelect;
	}

	protected void setLastName(String value) {
		this.lastName = value;

	}

	protected void setPlatform(Platform platformValue) {
		this.platform = platformValue;
	}

	protected void setTeam(String value) {
		team = value;
	}

	protected void updateURLLocations() {
		updateURLLocation(UI.getCurrent(), getLocation(), "fop", null);

		String ag = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "ag",
		        ag);
		String ad = getAgeDivision() != null ? getAgeDivision().name() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "ad",
		        ad);
		String cat = getCategoryValue() != null ? getCategoryValue().getComputedCode() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "cat",
		        cat);
		String platformName = getPlatform() != null ? getPlatform().getName() : null;
		// logger.debug("updating platform {}", platformName);
		updateURLLocation(UI.getCurrent(), getLocation(), "platform",
		        platformName);
		String group = getGroup() != null ? getGroup().getName() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "group",
		        group);
		String gender = getGender() != null ? getGender().name() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "gender",
		        gender);
	}

	private void clearLifts() {
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = (List<Athlete>) doFindAll(em);
			for (Athlete a : athletes) {
				a.clearLifts();
				em.merge(a);
			}
			em.flush();
			return null;
		});
	}

	private void deleteAthletes() {
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = (List<Athlete>) doFindAll(em);
			for (Athlete a : athletes) {
				em.remove(a);
			}
			em.flush();
			return null;
		});
		refreshCrudGrid();
	}

	private Collection<Athlete> doFindAll(EntityManager em) {
		List<Athlete> all = AthleteRepository.doFindFiltered(em, getLastName(), getGroup(),
		        getCategory(), getAgeGroup(), getAgeDivision(),
		        getGender(), getWeighedIn(), getTeam(), -1, -1);
		return all;
	}

	private void doSwitchGroup(Group newCurrentGroup) {
		if (newCurrentGroup != null && newCurrentGroup.getName() == "*") {
			setGroup(null);
		} else {
			setGroup(newCurrentGroup);
		}
		getRouterLayout().updateHeader(true);
		getGroupFilter().setValue(newCurrentGroup);
	}

	private void drawLots() {
		JPAService.runInTransaction(em -> {
			List<Athlete> toBeShuffled = AthleteRepository.doFindAll(em);
			AthleteSorter.drawLots(toBeShuffled);
			for (Athlete a : toBeShuffled) {
				em.merge(a);
			}
			em.flush();
			return null;
		});
		refreshCrudGrid();
	}

	private void resetCategories() {
		AthleteRepository.resetParticipations();
		refreshCrudGrid();
	}

	private void setWeighedIn(Boolean value) {
		this.weighedIn = value;
	}

	private void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(
		        location.getQueryParameters().getParameters());
		if (!isIgnoreGroupFromURL() && newGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
			if (newGroup != null) {
				params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
				setGroup(newGroup);
				OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
				crudGrid.setCrudFormFactory(crudFormFactory);
			}
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}
	
	protected List<Athlete> participationFindAll() {
		List<Athlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(getAgeGroupPrefix(),
		        getAgeDivision());

		Category catFilterValue = getCategoryValue();
		Stream<Athlete> stream = athletes.stream()
		        .filter(a -> {
			        Platform platformFilterValue = getPlatform();
			        if (platformFilterValue == null) {
				        return true;
			        }
			        Platform athletePlaform = a.getGroup() != null
			                ? (a.getGroup().getPlatform() != null ? a.getGroup().getPlatform() : null)
			                : null;
			        return platformFilterValue.equals(athletePlaform);
		        })
		        .filter(a -> a.getCategory() != null)
		        .filter(a -> {
			        Gender genderFilterValue = getGender();
			        Gender athleteGender = a.getGender();
			        boolean catOk = (catFilterValue == null
			                || catFilterValue.toString().equals(a.getCategory().toString()))
			                && (genderFilterValue == null || genderFilterValue == athleteGender);
			        return catOk;
		        })
		        .filter(a -> getGroup() != null ? getGroup().equals(a.getGroup())
		                : true)
		        .filter(a -> getTeam() != null ? getTeam().contentEquals(a.getTeam())
		                : true)
		        .map(a -> {
			        if (a.getTeam() == null) {
				        a.setTeam("-");
			        }
			        return a;
		        });

		// for categories listing we want all the participation categories
		Comparator<? super Athlete> groupCategoryComparator = (a1, a2) -> {
			int compare;
			compare = ObjectUtils.compare(a1.getGroup(), a2.getGroup(), true);
			if (compare != 0) {
				return compare;
			}
			compare = ObjectUtils.compare(a1.getCategory(), a2.getCategory(), true);
			return compare;
		};
		List<Athlete> found = stream.sorted(
		        groupCategoryComparator)
		        .collect(Collectors.toList());
		categoriesXlsWriter.setSortedAthletes(found);

		// cards and starting we only want the actual athlete, without duplicates
		Set<Athlete> regCatAthletes = found.stream().map(pa -> ((PAthlete) pa)._getAthlete())
		        .collect(Collectors.toSet());
		List<Athlete> regCatAthletesList = new ArrayList<>(regCatAthletes);
		regCatAthletesList.sort(groupCategoryComparator);

		cardsXlsWriter.setSortedAthletes(regCatAthletesList);
		startingXlsWriter.setSortedAthletes(regCatAthletesList);

		updateURLLocations();
		return regCatAthletesList;
	}
}
