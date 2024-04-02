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
import com.vaadin.flow.component.html.NativeLabel;
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

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.NAthleteRegistrationFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
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
public class RegistrationContent extends BaseContent implements CrudListener<Athlete>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private ComboBox<Championship> championshipFilter = new ComboBox<>();
	private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	protected OwlcmsCrudGrid<Athlete> crudGrid;
	private Group group;
	protected ComboBox<Gender> genderFilter = new ComboBox<>();
	protected ComboBox<String> teamFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();
	protected TextField lastNameFilter = new TextField();
	private OwlcmsLayout routerLayout;
	// protected JXLSStartingListDocs startingXlsWriter;
	// protected JXLSCategoriesListDocs categoriesXlsWriter;
	// protected JXLSCardsDocs cardsXlsWriter;
	private ComboBox<Boolean> weighedInFilter = new ComboBox<>();
	// private Group group;
	private ComboBox<Group> groupSelect;
	protected GroupSelectionMenu topBarMenu;
	protected FlexLayout topBar;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Category category;
	private Gender gender;
	private Platform platform;
	private Championship championship;
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
		((OwlcmsCrudFormFactory<Athlete>) this.crudGrid.getCrudFormFactory()).add(athlete);
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
		hr.setWidth("100%");
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("Preparation")),
		        drawLots, deleteAthletes, clearLifts,
		        resetCats, hr,
		        new NativeLabel(Translator.translate("Entries")),
		        bwButton, categoriesListButton, teamsListButton);
		buttons.getStyle().set("flex-wrap", "wrap");
		buttons.getStyle().set("gap", "1ex");
		buttons.getStyle().set("margin-left", "3em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		this.topBar = new FlexLayout();
		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.removeAll();
		this.topBar.add(this.topBarMenu, buttons);
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		return this.topBar;
	}

	@Override
	public void delete(Athlete athlete) {
		((OwlcmsCrudFormFactory<Athlete>) this.crudGrid.getCrudFormFactory()).delete(athlete);
	}

	/**
	 * The refresh button on the toolbar; also called by refreshGrid when the group is changed.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		List<Athlete> findFiltered = AthleteRepository.findFiltered(getLastName(), getGroup(),
		        getCategory(), getAgeGroup(), getChampionship(),
		        getGender(), getWeighedIn(), getTeam(), -1, -1);
		AthleteSorter.registrationOrder(findFiltered);
		// startingXlsWriter.setSortedAthletes(findFiltered);
		// List<Athlete> c = AthleteSorter.displayOrderCopy(findFiltered);
		// categoriesXlsWriter.setSortedAthletes(c);
		updateURLLocations();
		return findFiltered;
		// return athletesFindAll();
	}

	/**
	 * @return the groupFilter
	 */
	public ComboBox<Group> getGroupFilter() {
		return this.groupFilter;
	}

	public ComboBox<Group> getGroupSelect() {
		return this.groupSelect;
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
		return this.routerLayout;
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
		this.crudGrid.refreshGrid();
	}

	public void refreshCrudGrid() {
		this.crudGrid.refreshGrid();
	}

	@Override
	public void setHeaderContent() {
		this.routerLayout.setMenuTitle(getPageTitle());
		this.routerLayout.setMenuArea(createMenuArea());
		this.routerLayout.showLocaleDropdown(false);
		this.routerLayout.setDrawerOpened(false);
		this.routerLayout.updateHeader(true);
	}

	/**
	 * Parse the http query parameters
	 *
	 * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
	 *
	 * @param event     Vaadin navigation event
	 * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query
	 *                  parameters instead.
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
			this.crudGrid.setCrudFormFactory(crudFormFactory);
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
	public Athlete update(Athlete athlete) {
		OwlcmsSession.setAttribute("weighIn", athlete);
		Athlete a = ((OwlcmsCrudFormFactory<Athlete>) this.crudGrid.getCrudFormFactory()).update(athlete);
		OwlcmsSession.setAttribute("weighIn", null);
		return a;
	}

	protected List<Athlete> athletesFindAll() {
		List<Athlete> found = participationFindAll();
		// for cards and starting lists we only want the actual athlete, without duplicates
		Set<Athlete> regCatAthletes = found.stream().map(pa -> ((PAthlete) pa)._getAthlete())
		        .collect(Collectors.toSet());
		List<Athlete> regCatAthletesList = new ArrayList<>(regCatAthletes);
		regCatAthletesList.sort(groupCategoryComparator());

		updateURLLocations();
		return regCatAthletesList;
	}

	protected Button createBWButton() {
		String resourceDirectoryLocation = "/templates/bwStart";
		String title = Translator.translate("BodyWeightCategories");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationBWCopy(athletesFindAll()));
			        startingXlsWriter.createAgeGroupColumns(10, 7);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedStartListTemplateFileName,
		        Competition::setStartListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	protected Button createCategoriesListButton() {
		String resourceDirectoryLocation = "/templates/categories";
		String title = Translator.translate("StartingList.Categories");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSCategoriesListDocs categoriesXlsWriter = new JXLSCategoriesListDocs();
			        // group may have been edited since the page was loaded
			        categoriesXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        categoriesXlsWriter.setSortedAthletes(participationFindAll());
			        return categoriesXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedCategoriesListTemplateFileName,
		        Competition::setCategoriesListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
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
		        this.group, null);
		// createFormLayout(athleteEditingFormFactory);
		return athleteEditingFormFactory;
	}

	protected Button createTeamsListButton() {
		String resourceDirectoryLocation = "/templates/teams";
		String title = Translator.translate("StartingList.Teams");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        // findAll();
			        // List<Athlete> sortedAthletes = startingXlsWriter.getSortedAthletes();
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(participationFindAll()));
			        startingXlsWriter.createTeamColumns(9, 6);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedTeamsListTemplateFileName,
		        Competition::setTeamsListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	protected void createTopBarGroupSelect() {
		// there is already all the SQL filtering logic for the group attached
		// hidden field in the crudGrid part of the page so we just set that
		// filter.

		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());

		OwlcmsSession.withFop(fop -> {
			// logger.debug("initial setting group to {} {}", getCurrentGroup(),
			// LoggerUtils.whereFrom());
			getGroupFilter().setValue(getGroup());
			// switching to group "*" is understood to mean all groups
			this.topBarMenu = new GroupSelectionMenu(groups, getGroup(),
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
		this.lastNameFilter.setPlaceholder(Translator.translate("LastName"));
		this.lastNameFilter.setClearButtonVisible(true);
		this.lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		this.lastNameFilter.addValueChangeListener(e -> {
			setLastName(e.getValue());
			crudGrid.refreshGrid();
		});
		this.lastNameFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.lastNameFilter);

		this.teamFilter.setPlaceholder(Translator.translate("Team"));
		List<String> allTeams = AthleteRepository.findAllTeams();
		this.teamFilter.setItems(allTeams);
		this.teamFilter.setClearButtonVisible(true);
		this.teamFilter.setWidth("10em");
		this.teamFilter.getStyle().set("--vaadin-combo-box-overlay-width", "25em");
		this.teamFilter.addValueChangeListener(e -> {
			String value = e.getValue();
			setTeam(value);
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.teamFilter);

		this.championshipFilter.setPlaceholder(Translator.translate("Championship"));
		this.championshipFilter.setItems(Championship.findAllUsed());
		this.championshipFilter.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.getName()));
		this.championshipFilter.setClearButtonVisible(true);
		this.championshipFilter.addValueChangeListener(e -> {
			setChampionship(e.getValue());
			crudGrid.refreshGrid();
		});
		this.championshipFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.championshipFilter);

		this.ageGroupFilter.setPlaceholder(Translator.translate("AgeGroup"));
		this.ageGroupFilter.setItems(AgeGroupRepository.findAll());
		// ageGroupFilter.setItemLabelGenerator(Championship::name);
		this.ageGroupFilter.setClearButtonVisible(true);
		this.ageGroupFilter.addValueChangeListener(e -> {
			setAgeGroup(e.getValue());
			crudGrid.refreshGrid();
		});
		this.ageGroupFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.ageGroupFilter);

		this.categoryFilter.setPlaceholder(Translator.translate("Category"));
		this.categoryFilter.setItems(CategoryRepository.findActive());
		this.categoryFilter.setItemLabelGenerator(Category::getTranslatedName);
		this.categoryFilter.setClearButtonVisible(true);
		this.categoryFilter.addValueChangeListener(e -> {
			setCategory(e.getValue());
			crudGrid.refreshGrid();
		});
		this.categoryFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.categoryFilter);

		this.groupFilter.setPlaceholder(Translator.translate("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());
		this.groupFilter.setItems(groups);
		this.groupFilter.setItemLabelGenerator(Group::getName);
		this.groupFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
			setGroup(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), e.getValue());
		});
		this.groupFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.groupFilter);
		this.groupFilter.getStyle().set("display", "none");

		this.weighedInFilter.setPlaceholder(Translator.translate("Weighed_in_p"));
		this.weighedInFilter.setItems(Boolean.TRUE, Boolean.FALSE);
		this.weighedInFilter.setItemLabelGenerator((i) -> {
			return i ? Translator.translate("Weighed") : Translator.translate("Not_weighed");
		});
		this.weighedInFilter.setClearButtonVisible(true);
		this.weighedInFilter.addValueChangeListener(e -> {
			setWeighedIn(e.getValue());
			crudGrid.refreshGrid();
		});
		this.weighedInFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.weighedInFilter);

		this.genderFilter.setPlaceholder(Translator.translate("Gender"));
		this.genderFilter.setItems(Gender.M, Gender.F);
		this.genderFilter.setItemLabelGenerator((i) -> {
			return i == Gender.M ? Translator.translate("Gender.Men") : Translator.translate("Gender.Women");
		});
		this.genderFilter.setClearButtonVisible(true);
		this.genderFilter.addValueChangeListener(e -> {
			setGender(e.getValue());
			crudGrid.refreshGrid();
		});
		this.genderFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.genderFilter);

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			this.lastNameFilter.clear();
			this.ageGroupFilter.clear();
			this.championshipFilter.clear();
			this.categoryFilter.clear();
			// groupFilter.clear();
			this.weighedInFilter.clear();
			this.genderFilter.clear();
		});
		this.lastNameFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	protected void errorNotification() {
		NativeLabel content = new NativeLabel(Translator.translate("Select_group_first"));
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
	 * @return the championship
	 */
	public Championship getChampionship() {
		return this.championship;
	}

	protected AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	/**
	 * @return the ageGroupPrefix
	 */
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	protected Category getCategory() {
		return this.category;
	}

	public Category getCategoryValue() {
		return getCategory();
	}

	protected Gender getGender() {
		return this.gender;
	}

	protected String getLastName() {
		return this.lastName;
	}

	protected Platform getPlatform() {
		return this.platform;
	}

	protected String getTeam() {
		return this.team;
	}

	protected Boolean getWeighedIn() {
		return this.weighedIn;
	}

	protected void init() {
		// cardsXlsWriter = new JXLSCardsDocs();
		// startingXlsWriter = new JXLSStartingListDocs();
		// categoriesXlsWriter = new JXLSCategoriesListDocs();
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
		this.crudGrid = createCrudGrid(crudFormFactory);
		defineFilters(this.crudGrid);
		fillHW(this.crudGrid, this);
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	protected List<Athlete> participationFindAll() {
		List<Athlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(getAgeGroupPrefix(),
		        getChampionship());

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

		List<Athlete> found = stream.sorted(
		        groupCategoryComparator())
		        .collect(Collectors.toList());
		// categoriesXlsWriter.setSortedAthletes(found);
		updateURLLocations();
		return found;
	}

	public void setChampionship(Championship championship) {
		this.championship = championship;
	}

	protected void setAgeGroup(AgeGroup value) {
		this.ageGroup = value;

	}

	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	protected void setCategory(Category category) {
		this.category = category;
	}

	protected void setGender(Gender value) {
		this.gender = value;
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
		this.team = value;
	}

	protected void updateURLLocations() {
		updateURLLocation(UI.getCurrent(), getLocation(), "fop", null);

		String ag = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "ag",
		        ag);
		String ad = getChampionship() != null ? getChampionship().getName() : null;
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
		// when doing tests, the clock may have been started, need to clear
		// otherwise marshal gets confusing message.
		OwlcmsFactory.getFOPs().forEach(f -> f.setWeightAtLastStart(0));
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
		        getCategory(), getAgeGroup(), getChampionship(),
		        getGender(), getWeighedIn(), getTeam(), -1, -1);
		return all;
	}

	private void doSwitchGroup(Group newCurrentGroup) {
		if (newCurrentGroup != null && newCurrentGroup.getName() == "*") {
			setGroup(null);
		} else {
			setGroup(newCurrentGroup);
		}
		// getRouterLayout().updateHeader(true);
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

	private Comparator<? super Athlete> groupCategoryComparator() {
		// for categories listing we want all the participation categories
		Comparator<? super Athlete> groupCategoryComparator = (a1, a2) -> {
			int compare;
			compare = ObjectUtils.compare(a1.getGroup(), a2.getGroup(), true);
			if (compare != 0) {
				return compare;
			}
			Participation mainRankings1 = a1.getMainRankings() != null ? a1.getMainRankings() : null;
			Participation mainRankings2 = a2.getMainRankings() != null ? a2.getMainRankings() : null;
			compare = ObjectUtils.compare(mainRankings1.getCategory(), mainRankings2.getCategory(), true);
			if (compare != 0) {
				return compare;
			}
			compare = ObjectUtils.compare(a1.getEntryTotal(), a2.getEntryTotal());
			return -compare;
		};
		return groupCategoryComparator;
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
				this.crudGrid.setCrudFormFactory(crudFormFactory);
			}
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

}
