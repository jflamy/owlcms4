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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
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
import app.owlcms.data.athleteSort.RegistrationOrderComparator;
import app.owlcms.data.category.Category;
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
import app.owlcms.nui.results.IFilterCascade;
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
public class RegistrationContent extends BaseContent implements CrudListener<Athlete>, OwlcmsContent, IFilterCascade {

	final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private ComboBox<Championship> championshipFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	protected OwlcmsCrudGrid<Athlete> crudGrid;
	private ComboBox<Gender> genderFilter = new ComboBox<>();
	private ComboBox<String> teamFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();
	private TextField lastNameFilter = new TextField();
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
	private List<Championship> championshipItems;
	private ComboBox<String> ageGroupFilter;
	private List<String> championshipAgeGroupPrefixes;
	private NAthleteRegistrationFormFactory athleteEditingFormFactory;

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
	public void clearFilters() {
		this.getLastNameFilter().clear();
		this.getAgeGroupFilter().clear();
		this.getChampionshipFilter().clear();
		this.getCategoryFilter().clear();
		this.getGenderFilter().clear();
		this.getTeamFilter().clear();
		this.getWeighedInFilter().clear();
	}

	@Override
	public FlexLayout createMenuArea() {
		createTopBarGroupSelect();

//		Button bwButton = createBWButton();
//		Button categoriesListButton = createCategoriesListButton();
//		Button teamsListButton = createTeamsListButton();

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
		clearLifts.getElement().setAttribute("title", Translator.translate("ClearLifts_forListed"));

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
//		        new NativeLabel(Translator.translate("Preparation")),
		        drawLots, deleteAthletes, clearLifts,
		        resetCats
//		        , hr,
//		        new NativeLabel(Translator.translate("Entries")),
//		        bwButton, categoriesListButton, teamsListButton
		        );
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
		this.topBar.setJustifyContentMode(JustifyContentMode.START);

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
		List<Athlete> findFiltered = athletesFindAll(false);
		updateURLLocations();
		return findFiltered;
		// return athletesFindAll();
	}

	@Override
	public ComboBox<String> getAgeGroupFilter() {
		return this.ageGroupFilter;
	}

	/**
	 * @return the ageGroupPrefix
	 */
	@Override
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	@Override
	public ComboBox<Category> getCategoryFilter() {
		return this.categoryFilter;
	}

	@Override
	public Category getCategoryValue() {
		return getCategory();
	}

	/**
	 * @return the championship
	 */
	@Override
	public Championship getChampionship() {
		return this.championship;
	}

	@Override
	public List<String> getChampionshipAgeGroupPrefixes() {
		return this.championshipAgeGroupPrefixes;
	}

	@Override
	public ComboBox<Championship> getChampionshipFilter() {
		return this.championshipFilter;
	}

	@Override
	public List<Championship> getChampionshipItems() {
		return this.championshipItems;
	}

	@Override
	public OwlcmsCrudGrid<Athlete> getCrudGrid() {
		return this.crudGrid;
	}

	@Override
	public Gender getGender() {
		return this.gender;
	}

	@Override
	public ComboBox<Gender> getGenderFilter() {
		return this.genderFilter;
	}

	/**
	 * @return the groupFilter
	 */
	@Override
	public ComboBox<Group> getGroupFilter() {
		return this.groupFilter;
	}

	public ComboBox<Group> getGroupSelect() {
		return this.groupSelect;
	}

	@Override
	public Logger getLogger() {
		return logger;
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
	public void setAgeGroupFilter(ComboBox<String> ageGroupFilter) {
		this.ageGroupFilter = ageGroupFilter;
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	@Override
	public void setCategoryFilter(ComboBox<Category> categoryFilter) {
		this.categoryFilter = categoryFilter;
	}

	@Override
	public void setCategoryValue(Category category) {
		this.setCategory(category);
	}

	@Override
	public void setChampionship(Championship championship) {
		this.championship = championship;
	}

	@Override
	public void setChampionshipAgeGroupPrefixes(List<String> championshipAgeGroupPrefixes) {
		this.championshipAgeGroupPrefixes = championshipAgeGroupPrefixes;
	}

	@Override
	public void setChampionshipFilter(ComboBox<Championship> championshipFilter) {
		this.championshipFilter = championshipFilter;
	}

	@Override
	public void setChampionshipItems(List<Championship> championshipItems) {
		this.championshipItems = championshipItems;
	}

	@Override
	public void setGender(Gender value) {
		this.gender = value;
	}

	@Override
	public void setGenderFilter(ComboBox<Gender> genderFilter) {
		this.genderFilter = genderFilter;
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

	@SuppressWarnings("unchecked")
	protected List<Athlete> athletesFindAll(boolean sessionOrder) {
		List<Athlete> found = participationFindAll();
		// for cards and starting lists we only want the actual athlete, without duplicates
		Set<Athlete> regCatAthletes = found.stream().map(pa -> ((PAthlete) pa)._getAthlete())
		        .collect(Collectors.toSet());

		// we also need athletes with no participations (implies no category)
		List<Athlete> noCat = AthleteRepository.findAthletesNoCategory();
		List<Athlete> found2 = filterAthletes(noCat);
		regCatAthletes.addAll(found2);

		// sort
		List<Athlete> regCatAthletesList = new ArrayList<>(regCatAthletes);
		if (sessionOrder) {
			Collections.sort(regCatAthletesList, RegistrationOrderComparator.athleteSessionRegistrationOrderComparator);
		} else {
			AthleteSorter.registrationOrder(regCatAthletesList);
		}

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
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationBWCopy(athletesFindAll(false)));
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
			        var athletes = participationFindAll();
			        AthleteSorter.registrationOrder(athletes);
			        categoriesXlsWriter.setSortedAthletes(athletes);
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
		        .setTextAlign(ColumnTextAlign.CENTER)
		        .setRenderer(
		                new TextRenderer<Athlete>(a -> a.getCategory() != null ? a.getCategory().toString() : "-"));
		grid.addColumn(new NumberRenderer<>(Athlete::getBodyWeight, "%.2f", this.getLocale()))
		        .setSortProperty("bodyWeight")
		        .setHeader(Translator.translate("BodyWeight")).setAutoWidth(true).setTextAlign(ColumnTextAlign.CENTER);
		Column<Athlete> groupCol = grid.addColumn("group").setHeader(Translator.translate("Group")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("eligibleCategories").setHeader(Translator.translate("Registration.EligibleCategories"))
		        .setAutoWidth(true);
		grid.addColumn("subCategory").setHeader(Translator.translate("SubCategory")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("entryTotal").setHeader(Translator.translate("EntryTotal")).setAutoWidth(true)
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn("federationCodes").setHeader(Translator.translate("Registration.FederationCodesShort"))
		        .setAutoWidth(true);

		List<GridSortOrder<Athlete>> sortOrder = new ArrayList<>();
		// groupWeighinTimeComparator implements traditional platform name comparisons e.g. USAW.
		groupCol.setComparator((a,b) -> Group.groupWeighinTimeComparator.compare(a.getGroup(), b.getGroup()));
		sortOrder.add(new GridSortOrder<Athlete>(groupCol, SortDirection.ASCENDING));
		grid.sort(sortOrder);

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
		athleteEditingFormFactory = new NAthleteRegistrationFormFactory(Athlete.class,
		        getGroup(), null);
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
		groups.sort(Group.groupWeighinTimeComparator);

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
			        Translator.translate("AllGroups"), false);
		});
	}

	/**
	 * The filters at the top of the crudGrid
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */

	protected void defineFilters(OwlcmsCrudGrid<Athlete> crudGrid) {
		this.getLastNameFilter().setPlaceholder(Translator.translate("LastName"));
		this.getLastNameFilter().setClearButtonVisible(true);
		this.getLastNameFilter().setValueChangeMode(ValueChangeMode.EAGER);
		this.getLastNameFilter().addValueChangeListener(e -> {
			setLastName(e.getValue());
			crudGrid.refreshGrid();
		});
		this.getLastNameFilter().setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.getLastNameFilter());

		this.defineFilterCascade(crudGrid);
		this.defineRegistrationFilters(crudGrid, true);

		this.defineSelectionListeners();
	}

	protected void defineRegistrationFilters(OwlcmsCrudGrid<Athlete> crudGrid, boolean clearFilter) {
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

		this.getWeighedInFilter().setPlaceholder(Translator.translate("Weighed_in_p"));
		this.getWeighedInFilter().setItems(Boolean.TRUE, Boolean.FALSE);
		this.getWeighedInFilter().setItemLabelGenerator((i) -> {
			return i ? Translator.translate("Weighed") : Translator.translate("Not_weighed");
		});
		this.getWeighedInFilter().setClearButtonVisible(true);
		this.getWeighedInFilter().addValueChangeListener(e -> {
			setWeighedIn(e.getValue());
			crudGrid.refreshGrid();
		});
		this.getWeighedInFilter().setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(this.getWeighedInFilter());

		this.getTeamFilter().setPlaceholder(Translator.translate("Team"));
		this.getTeamFilter().setItems(AthleteRepository.findAllTeams());
		this.getTeamFilter().getStyle().set("--vaadin-combo-box-overlay-width", "25em");
		this.getTeamFilter().setClearButtonVisible(true);
		this.getTeamFilter().addValueChangeListener(e -> {
			setTeam(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.getTeamFilter());

		if (clearFilter) {
			Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
			clearFilters.addClickListener(event -> {
				clearFilters();
			});
			crudGrid.getCrudLayout().addFilterComponent(clearFilters);
		}
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

	protected AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	protected Category getCategory() {
		return this.category;
	}

	protected String getLastName() {
		return this.lastName;
	}

	protected TextField getLastNameFilter() {
		return this.lastNameFilter;
	}

	protected Platform getPlatform() {
		return this.platform;
	}

	protected String getTeam() {
		return this.team;
	}

	protected ComboBox<String> getTeamFilter() {
		return this.teamFilter;
	}

	protected Boolean getWeighedIn() {
		return this.weighedIn;
	}

	protected ComboBox<Boolean> getWeighedInFilter() {
		return this.weighedInFilter;
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

		List<Athlete> found = filterAthletes(athletes);
		updateURLLocations();
		return found;
	}

	private List<Athlete> filterAthletes(List<Athlete> athletes) {
		Category catFilterValue = getCategoryValue();
		Group group2 = getGroup() == null
		        ? null
		        : (getGroup().getName() == "*" ? null : getGroup());
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
		        .filter(a -> {
			        return group2 != null ? group2.equals(a.getGroup())
			                : true;
		        })
		        .filter(a -> {
			        String fLastName = getLastName();
			        if (fLastName == null) {
				        return true;
			        }
			        String aLastName = a.getLastName();
			        if (aLastName == null || aLastName.isBlank())
				        return false;
			        aLastName = aLastName.toLowerCase();
			        fLastName = fLastName.toLowerCase();
			        return aLastName.startsWith(fLastName);
		        })
		        .filter(a -> {
					if (getWeighedIn() == null) {
						return true;
					}

					if (getWeighedIn()) {
						return a.getBodyWeight() != null && a.getBodyWeight() > 0;
					}

					return a.getBodyWeight() == null || a.getBodyWeight() == 0;
				})
		        // .filter(a -> a.getCategory() != null)
		        .filter(a -> {
			        Gender genderFilterValue = getGender();
			        Gender athleteGender = a.getGender();
			        boolean catOk = (catFilterValue == null
			                || (a.getCategory() != null
			                        && catFilterValue.toString().equals(a.getCategory().toString())))
			                && (genderFilterValue == null || genderFilterValue == athleteGender);
			        return catOk;
		        })
		        .filter(a -> getTeam() != null ? getTeam().contentEquals(a.getTeam())
		                : true)
		        .map(a -> {
			        if (a.getTeam() == null) {
				        a.setTeam("");
			        }
			        return a;
		        });

		List<Athlete> found = stream.sorted(
		        groupCategoryComparator())
		        .collect(Collectors.toList());
		return found;
	}

	protected void setAgeGroup(AgeGroup value) {
		this.ageGroup = value;

	}

	protected void setCategory(Category category) {
		this.category = category;
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

	protected void setLastNameFilter(TextField lastNameFilter) {
		this.lastNameFilter = lastNameFilter;
	}

	protected void setPlatform(Platform platformValue) {
		this.platform = platformValue;
	}

	protected void setTeam(String value) {
		this.team = value;
	}

	protected void setTeamFilter(ComboBox<String> teamFilter) {
		this.teamFilter = teamFilter;
	}

	protected void setWeighedInFilter(ComboBox<Boolean> weighedInFilter) {
		this.weighedInFilter = weighedInFilter;
	}

	protected void updateURLLocations() {
		if (getLocation() == null) {
			// sometimes called from routines outside of normal event flow.
			return;
		}
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
			List<Athlete> athletes = athletesFindAll(false);
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
			List<Athlete> athletes = athletesFindAll(false);
			for (Athlete a : athletes) {
				Athlete ath = em.find(Athlete.class, a.getId());
				em.remove(ath);
			}
			em.flush();
			return null;
		});
		refreshCrudGrid();
	}
	//
	// private Collection<Athlete> doFindAll(EntityManager em) {
	// List<Athlete> all = AthleteRepository.doFindFiltered(em, getLastName(), getGroup(),
	// getCategory(), getAgeGroup(), getChampionship(),
	// getGender(), getWeighedIn(), getTeam(), -1, -1);
	// return all;
	// }

	private void doSwitchGroup(Group newCurrentGroup) {
		logger.debug("newCurrentGroup.getName() {}", newCurrentGroup.getName());
		if (newCurrentGroup != null && newCurrentGroup.getName() == "*") {
			setGroup(null);
			athleteEditingFormFactory.setCurrentGroup(null);
		} else {
			setGroup(newCurrentGroup);
			athleteEditingFormFactory.setCurrentGroup(newCurrentGroup);
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
		Comparator<? super Athlete> groupCategoryComparator = (a1, a2) -> {
			int compare;
			compare = ObjectUtils.compare(a1.getGroup(), a2.getGroup(), true);
			if (compare != 0) {
				logComparison(compare, a1, a2, "group");
				return compare;
			}

			// deal with athletes not fully registered or not eligible to any category.
			Participation mainRankings1 = a1.getMainRankings() != null ? a1.getMainRankings() : null;
			Participation mainRankings2 = a2.getMainRankings() != null ? a2.getMainRankings() : null;
			Category category1 = mainRankings1 != null ? mainRankings1.getCategory() : null;
			Category category2 = mainRankings2 != null ? mainRankings2.getCategory() : null;
			compare = ObjectUtils.compare(category1, category2, true);
			if (compare != 0) {
				logComparison(compare, a1, a2, "mainCategory");
				return compare;
			}

			compare = ObjectUtils.compare(a1.getEntryTotal(), a2.getEntryTotal());
			logComparison(compare, a1, a2, "entryTotal");
			return -compare;
		};
		return groupCategoryComparator;
	}

	private void logComparison(int compare, Athlete a1, Athlete a2, String string) {
		if (compare == 0) {
			// logger.trace("({}) {} = {}", string, athleteLog(a1), athleteLog(a2));
		} else if (compare < 0) {
			// logger.trace("({}) {} < {}", string, athleteLog(a1), athleteLog(a2));
		} else if (compare > 0) {
			// logger.trace("({}) {} > {}", string, athleteLog(a1), athleteLog(a2));
		}
	}

	@SuppressWarnings("unused")
	private String athleteLog(Athlete a1) {
		Participation mainRankings1 = a1.getMainRankings() != null ? a1.getMainRankings() : null;
		Category category1 = mainRankings1 != null ? mainRankings1.getCategory() : null;
		return "[" + a1.getShortName() + " " + a1.getGroup() + " " + category1 + " " + a1.getEntryTotal() + "]";
	}

	private void resetCategories() {
		AthleteRepository.resetParticipations();
		this.setChampionshipItems(Championship.findAllUsed(true));
		this.getChampionshipFilter().setItems(this.getChampionshipItems());
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
