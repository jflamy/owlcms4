/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.lifting;

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

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
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
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
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
import app.owlcms.nui.results.IFilterCascade;
import app.owlcms.nui.shared.NAthleteRegistrationFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCardsWeighIn;
import app.owlcms.spreadsheet.JXLSJurySheet;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class WeighinContent
 *
 * Defines the toolbar and the table for editing data on athletes during weigh-in
 *
 */
@SuppressWarnings("serial")
@Route(value = "preparation/weighin", layout = OwlcmsLayout.class)
@CssImport(value = "./styles/shared-styles.css")
public class WeighinContent extends BaseContent
        implements CrudListener<Athlete>, OwlcmsContent, NextAthleteAble, IFilterCascade {
	/**
	 * Variation to do Next instead of closing.
	 *
	 */
	private final class NextCrudGrid extends OwlcmsCrudGrid<Athlete> {
		Button batchButton;

		private NextCrudGrid(Class<Athlete> domainType, OwlcmsGridLayout crudLayout,
		        OwlcmsCrudFormFactory<Athlete> owlcmsCrudFormFactory, Grid<Athlete> grid) {
			super(domainType, crudLayout, owlcmsCrudFormFactory, grid);
		}

		/**
		 * Inits the toolbar.
		 */
		@Override
		protected void initToolbar() {
			this.findAllButton = new Button(getTranslation("RefreshList"), VaadinIcon.REFRESH.create(),
			        e -> findAllButtonClicked());
			this.findAllButton.getElement().setAttribute("title", getTranslation("RefreshList"));
			this.crudLayout.addToolbarComponent(this.findAllButton);

			this.addButton = new Button(VaadinIcon.PLUS.create(), e -> addButtonClicked());
			getAddButton().setText(getTranslation("Add"));
			// getAddButton().addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
			this.addButton.getElement().setAttribute("title", getTranslation("Add"));
			this.crudLayout.addToolbarComponent(this.addButton);

			this.updateButton = new Button(VaadinIcon.PENCIL.create(), e -> batchButtonClicked());
			this.deleteButton = new Button(VaadinIcon.TRASH.create(), e -> deleteButtonClicked());

			this.batchButton = new Button(VaadinIcon.FILE_TEXT.create(), e -> batchButtonClicked());
			this.batchButton.setText(getTranslation("WeighIn.Batch"));
			this.crudLayout.addToolbarComponent(this.batchButton);

			updateButtons();
		}

		@Override
		protected void saveCallBack(OwlcmsCrudGrid<Athlete> owlcmsCrudGrid, String successMessage,
		        CrudOperation operation, Athlete a) {
			try {
				Athlete nextAthlete = getNextAthlete(owlcmsCrudGrid.getGrid().asSingleSelect().getValue());
				if (isNextMode() && nextAthlete != null && operation == CrudOperation.UPDATE) {
					owlcmsCrudGrid.getGrid().asSingleSelect().clear();
					owlcmsCrudGrid.getOwlcmsGridLayout().hideForm();
					refreshGrid();
					owlcmsCrudGrid.getGrid().asSingleSelect().setValue(nextAthlete);
					updateButtonClicked();
				} else {
					owlcmsCrudGrid.getGrid().asSingleSelect().clear();
					owlcmsCrudGrid.getOwlcmsGridLayout().hideForm();
					refreshGrid();
					Notification.show(successMessage);
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
		}

		private void batchButtonClicked() {
			setNextMode(!isNextMode());
			if (isNextMode()) {
				this.batchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			} else {
				this.batchButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
			}

		}
	}

	final private static Logger logger = (Logger) LoggerFactory.getLogger(WeighinContent.class);

	static {
		logger.setLevel(Level.INFO);
	}
	private boolean nextMode;
	private ComboBox<String> ageGroupFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	private OwlcmsCrudGrid<Athlete> crudGrid;
	private Group currentGroup;
	private ComboBox<Gender> genderFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();
	private TextField lastNameFilter = new TextField();
	private OwlcmsLayout routerLayout;
	private ComboBox<Boolean> weighedInFilter = new ComboBox<>();
	private Button cardsButton;
	private GroupSelectionMenu topBarMenu;
	private Button juryButton;
	private Button startingWeightsButton;
	private Button weighInButton;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private ComboBox<String> teamFilter = new ComboBox<>();
	private Boolean weighedIn;
	private Gender gender;
	private List<Championship> championshipItems;
	private ComboBox<Championship> championshipFilter;
	private List<String> championshipAgeGroupPrefixes;
	private Championship championship;
	private String ageGroupPrefix;
	private Category category;
	private String lastName;
	private String team;
	private Platform platform;
	private NAthleteRegistrationFormFactory athleteEditingFormFactory;

	/**
	 * Instantiates the athlete crudGrid
	 */
	public WeighinContent() {
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
		this.crudGrid = createGrid(crudFormFactory);
		defineFilters(this.crudGrid);
		fillHW(this.crudGrid, this);
	}

	@Override
	public Athlete add(Athlete athlete) {
		if (athlete.getGroup() == null && this.getCurrentGroup() != null) {
			athlete.setGroup(this.getCurrentGroup());
		}
		((OwlcmsCrudFormFactory<Athlete>) this.crudGrid.getCrudFormFactory()).add(athlete);
		return athlete;
	}

	@Override
	public FlexLayout createMenuArea() {
		createTopBarGroupSelect();

		this.cardsButton = createCardsButton();
		this.startingWeightsButton = createStartingWeightsButton();
		this.weighInButton = createWeighInButton();
		this.juryButton = createJuryButton();

		Button start = new Button(getTranslation("GenerateStartNumbers"), (e) -> {
			generateStartNumbers();
		});
		Button clear = new Button(getTranslation("ClearStartNumbers"), (e) -> {
			clearStartNumbers();
		});

		Hr hr = new Hr();
		hr.setWidth("100%");
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("WeighIn_StartNumbers")),
		        start, clear,
		        hr,
		        new NativeLabel(Translator.translate("WeighIn_SessionDocuments")),
		        this.weighInButton, this.cardsButton, this.startingWeightsButton, this.juryButton);
		buttons.getStyle().set("flex-wrap", "wrap");
		buttons.getStyle().set("gap", "1ex");
		buttons.getStyle().set("margin-left", "3em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		FlexLayout topBar = new FlexLayout();
		topBar.getStyle().set("flex", "100 1");
		topBar.removeAll();
		topBar.add(this.topBarMenu, buttons);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		return topBar;
	}

	@Override
	public void delete(Athlete athlete) {
		((OwlcmsCrudFormFactory<Athlete>) this.crudGrid.getCrudFormFactory()).delete(athlete);
	}

	/**
	 * The refresh button on the toolbar calls this.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		// List<Athlete> findFiltered = AthleteRepository.findFiltered(this.lastNameFilter.getValue(),
		// getGroupFilter().getValue(),
		// this.categoryFilter.getValue(), this.ageGroupFilter.getValue(), this.ageDivisionFilter.getValue(),
		// this.genderFilter.getValue(), this.weighedInFilter.getValue(), (String) null, -1, -1);
		// AthleteSorter.registrationOrder(findFiltered);
		List<Athlete> findFiltered = athletesFindAll();
		updateURLLocations();
		return findFiltered;
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
	public List<Athlete> getAthletes() {
		return (List<Athlete>) findAll();
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

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	// @Override
	// public Athlete getNextAthlete(Athlete current) {
	// ArrayList<Athlete> all = new ArrayList<>(findAll());
	// if (current == null && all.size() > 0) {
	// current = all.get(0);
	// }
	// for (int i = 0; i < all.size(); i++) {
	// //TODO: get first empty bw, careful about looping back to self
	// if (all.get(i).getId().equals(current.getId())) {
	// return (i + 1 < all.size() ? all.get(i + 1) : null);
	// }
	// }
	// return null;
	// }

	@Override
	public Athlete getNextAthlete(Athlete current) {
		ArrayList<Athlete> all = new ArrayList<>(findAll());

		// Make sure we have updated data for the current athlete
		if (current != null) {
			for (int i = 0; i < all.size(); i++) {
				if (all.get(i).getId().equals(current.getId())) {
					current = all.get(i);
					break;
				}
			}
			if (current == null) {
				current = all.get(0);
			}
		} else if (all.size() > 0) {
			current = all.get(0);
		}

		// If the current athlete has a body weight entered, then find the
		// first athlete that does not have a body weight entered. This ensures
		// that the order will match the weigh-in form even if the current
		// athlete has moved up a weight class. Otherwise just find the next
		// athlete based on current order.
		
		// quick workaround: a "no show" is indicated by removing the session.
		Double curWeight = current.getBodyWeight();
		if (curWeight != null || current.getGroup() == null) {
			for (int i = 0; i < all.size(); i++) {
				Athlete next = all.get(i);
				if (next.getBodyWeight() == null) {
					return next;
				}
			}
		} else {
			for (int i = 0; i < all.size() - 1; i++) {
				if (all.get(i).getId().equals(current.getId())) {
					return all.get(i + 1);
				}
			}
			// start from the top again instead of exiting
			for (int i = 0; i < all.size(); i++) {
				Athlete next = all.get(i);
				if (next.getBodyWeight() == null) {
					return next;
				}
			}
		}

		return null;
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("WeighIn");
	}

	@Override
	public Athlete getPreviousAthlete(Athlete current) {
		ArrayList<Athlete> all = new ArrayList<>(findAll());
		for (int i = 0; i < all.size(); i++) {
			if (all.get(i).getId().equals(current.getId())) {
				return (i - 1 > 0 ? all.get(i - 1) : null);
			}
		}
		return null;
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

	public boolean isNextMode() {
		return this.nextMode;
	}

	public void refresh() {
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

	public void setCategory(Category category) {
		this.category = category;
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

	public void setCrudGrid(OwlcmsCrudGrid<Athlete> crudGrid) {
		this.crudGrid = crudGrid;
	}

	@Override
	public void setGender(Gender gender) {
		this.gender = gender;
	}

	@Override
	public void setGenderFilter(ComboBox<Gender> genderFilter) {
		this.genderFilter = genderFilter;
	}

	public void setGroupFilter(ComboBox<Group> groupFilter) {
		this.groupFilter = groupFilter;
	}

	@Override
	public void setHeaderContent() {
		this.routerLayout.setMenuTitle(getPageTitle());
		this.routerLayout.setMenuArea(createMenuArea());
		this.routerLayout.showLocaleDropdown(false);
		this.routerLayout.setDrawerOpened(false);
		this.routerLayout.updateHeader(true);
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public void setLastNameFilter(TextField lastNameFilter) {
		this.lastNameFilter = lastNameFilter;
	}

	public void setNextMode(boolean b) {
		this.nextMode = b;
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
			setCurrentGroup(GroupRepository.findByName(groupName));
		} else {
			this.setCurrentGroup(null);
		}
		if (getCurrentGroup() != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(getCurrentGroup().getName())));
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

	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

	public void setTeam(String team) {
		this.team = team;
	}

	public void setTeamFilter(ComboBox<String> teamFilter) {
		this.teamFilter = teamFilter;
	}

	public void setWeighedIn(Boolean weighedIn) {
		this.weighedIn = weighedIn;
	}

	public void setWeighedInFilter(ComboBox<Boolean> weighedInFilter) {
		this.weighedInFilter = weighedInFilter;
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

		// we also need athletes with no participations (implies no category)
		List<Athlete> noCat = AthleteRepository.findAthletesNoCategory();
		List<Athlete> found2 = filterAthletes(noCat);
		regCatAthletes.addAll(found2);

		// sort
		List<Athlete> regCatAthletesList = new ArrayList<>(regCatAthletes);
		AthleteSorter.registrationOrder(regCatAthletesList);

		// regCatAthletesList.sort(groupCategoryComparator());

		updateURLLocations();
		return regCatAthletesList;
	}

	/**
	 * Define the form used to edit a given athlete.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
		athleteEditingFormFactory = new NAthleteRegistrationFormFactory(Athlete.class,
		        this.getCurrentGroup(), this);
		createFormLayout(athleteEditingFormFactory);
		return athleteEditingFormFactory;
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected OwlcmsCrudGrid<Athlete> createGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		Grid<Athlete> grid = new Grid<>(Athlete.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn("startNumber").setHeader(getTranslation("Start_")).setAutoWidth(true);
		grid.addColumn("lastName").setHeader(getTranslation("LastName"));
		grid.addColumn("firstName").setHeader(getTranslation("FirstName"));
		grid.addColumn("team").setHeader(getTranslation("Team")).setAutoWidth(true);
		grid.addColumn("gender").setHeader(getTranslation("Gender")).setAutoWidth(true);
		grid.addColumn(new TextRenderer<>(a -> a.getAgeGroupDisplayName())).setHeader(getTranslation("AgeGroup"))
		        .setAutoWidth(true);
		grid.addColumn("category").setHeader(getTranslation("Category")).setAutoWidth(true);
		grid.addColumn(new NumberRenderer<>(Athlete::getBodyWeight, "%.2f", this.getLocale()))
		        .setSortProperty("bodyWeight")
		        .setHeader(getTranslation("BodyWeight")).setAutoWidth(true);
		grid.addColumn("snatch1Declaration").setHeader(getTranslation("SnatchDecl_"));
		grid.addColumn("cleanJerk1Declaration").setHeader(getTranslation("C_and_J_decl"));
		grid.addColumn("eligibleCategories").setHeader(getTranslation("Weighin.EligibleCategories")).setAutoWidth(true);
		grid.addColumn("entryTotal").setHeader(getTranslation("EntryTotal")).setAutoWidth(true);
		grid.addColumn("federationCodes").setHeader(getTranslation("Registration.FederationCodesShort"))
		        .setAutoWidth(true);
		NextCrudGrid crudGrid = new NextCrudGrid(Athlete.class, new OwlcmsGridLayout(Athlete.class) {
			@Override
			public void hideForm() {
				super.hideForm();
				OwlcmsSession.setAttribute("weighIn", null);
			}
		}, crudFormFactory, grid);
		crudGrid.setCrudListener(this);
		crudGrid.setClickRowToUpdate(true);
		return crudGrid;
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
			getGroupFilter().setValue(getCurrentGroup());
			// switching to group "*" is understood to mean all groups
			this.topBarMenu = new GroupSelectionMenu(groups, getCurrentGroup(),
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
		this.defineRegistrationFilters(crudGrid);

		this.defineSelectionListeners();
	}

	protected void defineRegistrationFilters(OwlcmsCrudGrid<Athlete> crudGrid) {
		this.groupFilter.setPlaceholder(Translator.translate("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());
		this.groupFilter.setItems(groups);
		this.groupFilter.setItemLabelGenerator(Group::getName);
		this.groupFilter.addValueChangeListener(e -> {
			setGroup(e.getValue());
			crudGrid.refreshGrid();
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

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			clearFilters();
		});
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	protected void errorNotification() {
		NativeLabel content = new NativeLabel(getTranslation("Select_group_first"));
		content.getElement().setAttribute("theme", "error");
		Button buttonInside = new Button(getTranslation("GotIt"));
		buttonInside.getElement().setAttribute("theme", "error primary");
		VerticalLayout verticalLayout = new VerticalLayout(content, buttonInside);
		verticalLayout.setAlignItems(Alignment.CENTER);
		Notification notification = new Notification(verticalLayout);
		notification.setDuration(3000);
		buttonInside.addClickListener(event -> notification.close());
		notification.setPosition(Position.MIDDLE);
		notification.open();
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

	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	protected List<Athlete> participationFindAll() {
		List<Athlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(getAgeGroupPrefix(),
		        getChampionship());

		List<Athlete> found = filterAthletes(athletes);
		// categoriesXlsWriter.setSortedAthletes(found);
		updateURLLocations();
		
		// enable quick batch mode only when doing a session
		boolean sessionSelected = this.getGroup() != null && !this.getGroup().getName().equals("*");
		((NextCrudGrid) crudGrid).batchButton.setEnabled(sessionSelected);
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
			        if (aLastName == null || aLastName.isBlank()) {
				        return false;
			        }
			        aLastName = aLastName.toLowerCase();
			        fLastName = fLastName.toLowerCase();
			        return aLastName.startsWith(fLastName);
		        })
		        .filter(a -> getWeighedIn() == null
		                || (getWeighedIn() ? (a.getBodyWeight() != null && a.getBodyWeight() > 0)
		                        : (a.getBodyWeight() == null || a.getBodyWeight() < 0.1)))
		        .filter(a -> catFilterValue != null ? a.getCategory() != null : true)
		        .filter(a -> {
			        Gender genderFilterValue = getGender();
			        Gender athleteGender = a.getGender();
			        boolean catOk = (catFilterValue == null
			                || catFilterValue.toString().equals(a.getCategory().toString()))
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

	protected void setContentGroup(ComponentValueChangeEvent<ComboBox<Group>, Group> e) {
		this.groupFilter.setValue(e.getValue());
	}

	private void clearStartNumbers() {
		Group group = getCurrentGroup();
		// logger.debug("group {}",getCurrentGroup());
		if (group == null) {
			errorNotification();
			return;
		}
		JPAService.runInTransaction((em) -> {
			List<Athlete> currentGroupAthletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, group, null,
			        (Gender) null);
			for (Athlete a : currentGroupAthletes) {
				// logger.debug(a.getShortName());
				a.setStartNumber(0);
			}
			return currentGroupAthletes;
		});
		refresh();
	}

	private Button createCardsButton() {
		String resourceDirectoryLocation = "/templates/cards";
		String title = Translator.translate("AthleteCards");
		JXLSDownloader cardsButtonFactory = new JXLSDownloader(
		        () -> {
			        JXLSCardsWeighIn rs = new JXLSCardsWeighIn();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedCardsTemplateFileName,
		        Competition::setCardsTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return cardsButtonFactory.createDownloadButton();
	}

	/**
	 * The content and ordering of the editing form
	 *
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	private void createFormLayout(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		// if (!Config.getCurrent().featureSwitch("oldAthleteForm")) {
		// return;
		// }
		// List<String> props = new LinkedList<>();
		// List<String> captions = new LinkedList<>();
		//
		// props.add("lastName");
		// captions.add(getTranslation("LastName"));
		// props.add("firstName");
		// captions.add(getTranslation("FirstName"));
		//
		// props.add("bodyWeight");
		// captions.add(getTranslation("BodyWeight"));
		// props.add("snatch1Declaration");
		// captions.add(getTranslation("SnatchDecl_"));
		// props.add("cleanJerk1Declaration");
		// captions.add(getTranslation("C_and_J_decl"));
		//
		// props.add("qualifyingTotal");
		// captions.add(getTranslation("EntryTotal"));
		// props.add("category");
		// captions.add(getTranslation("Weighin.Category"));
		// props.add("eligibleCategories");
		// captions.add(getTranslation("Weighin.EligibleCategories"));
		// props.add("group");
		// captions.add(getTranslation("Group"));
		//
		// props.add("gender");
		// captions.add(getTranslation("Gender"));
		// props.add("team");
		// captions.add(getTranslation("Team"));
		//
		// Competition competition = Competition.getCurrent();
		// if (competition.isUseBirthYear()) {
		// props.add("yearOfBirth");
		// captions.add(getTranslation("YearOfBirth"));
		// } else {
		// props.add("fullBirthDate");
		// captions.add(getTranslation("BirthDate_yyyy"));
		// }
		// props.add("membership");
		// captions.add(getTranslation("Membership"));
		//
		// props.add("coach");
		// captions.add(getTranslation("Coach"));
		// props.add("custom1");
		// captions.add(getTranslation("Custom1.Title"));
		// props.add("custom2");
		// captions.add(getTranslation("Custom2.Title"));
		//
		// props.add("lotNumber");
		// captions.add(getTranslation("Lot"));
		//
		// props.add("federationCodes");
		// captions.add(getTranslation("Registration.FederationCodes"));
		//
		// props.add("eligibleForIndividualRanking");
		// captions.add(getTranslation("Eligible for Individual Ranking?"));
		//
		// crudFormFactory.setVisibleProperties(props.toArray(new String[0]));
		// crudFormFactory.setFieldCaptions(captions.toArray(new String[0]));
		//
		// crudFormFactory.setFieldProvider("gender", new OwlcmsComboBoxProvider<>(getTranslation("Gender"),
		// Arrays.asList(Gender.mfValues()), new TextRenderer<>(Gender::name), Gender::name));
		// List<Group> groups = GroupRepository.findAll();
		// groups.sort(new NaturalOrderComparator<>());
		// crudFormFactory.setFieldProvider("group", new OwlcmsComboBoxProvider<>(getTranslation("Group"),
		// groups, new TextRenderer<>(Group::getName), Group::getName));
		// crudFormFactory.setFieldProvider("category", new OwlcmsComboBoxProvider<>(getTranslation("Category"),
		// CategoryRepository.findActive(), new TextRenderer<>(Category::getNameWithAgeGroup),
		// Category::getNameWithAgeGroup));
		// crudFormFactory.setFieldProvider("eligibleCategories",
		// new CheckBoxGroupProvider<>(getTranslation("Weighin.EligibleCategories"),
		// new ArrayList<Category>(), (c) -> (c.getNameWithAgeGroup())));
		// // crudFormFactory.setFieldProvider("ageDivision",
		// // new OwlcmsComboBoxProvider<>(getTranslation("Championship"), Arrays.asList(Championship.values()),
		// // new TextRenderer<>(ad -> getTranslation("Division." + ad.name())), Championship::name));
		//
		// crudFormFactory.setFieldType("bodyWeight", LocalizedDecimalField.class);
		// crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class);
		//
		// // ValidationTextField (or a wrapper) must be used as workaround for unexplained
		// // validation behaviour
		// crudFormFactory.setFieldType("snatch1Declaration", ValidationTextField.class);
		// crudFormFactory.setFieldType("cleanJerk1Declaration", ValidationTextField.class);
		// crudFormFactory.setFieldType("qualifyingTotal", ValidationTextField.class);
		// crudFormFactory.setFieldType("yearOfBirth", ValidationTextField.class);
		//
		// crudFormFactory.setFieldCreationListener("bodyWeight", (e) -> {
		// ((LocalizedDecimalField) e).focus();
		// });
	}

	private Button createJuryButton() {
		String resourceDirectoryLocation = "/templates/jury";
		String title = Translator.translate("Jury");

		JXLSDownloader juryButton = new JXLSDownloader(
		        () -> {
			        JXLSJurySheet rs = new JXLSJurySheet();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedJuryTemplateFileName,
		        Competition::setJuryTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return juryButton.createDownloadButton();
	}

	private Button createStartingWeightsButton() {
		String resourceDirectoryLocation = "/templates/emptyProtocol";
		String title = Translator.translate("EmptyProtocolSheet");

		JXLSDownloader startingWeightsButton = new JXLSDownloader(
		        () -> {
			        JXLSWeighInSheet rs = new JXLSWeighInSheet();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedStartingWeightsSheetTemplateFileName,
		        Competition::setStartingWeightsSheetTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingWeightsButton.createDownloadButton();
	}

	private Button createWeighInButton() {
		String resourceDirectoryLocation = "/templates/weighin";
		String title = Translator.translate("WeighinForm");

		JXLSDownloader startingWeightsButton = new JXLSDownloader(
		        () -> {
			        JXLSWeighInSheet rs = new JXLSWeighInSheet();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedStartingWeightsSheetTemplateFileName,
		        Competition::setStartingWeightsSheetTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingWeightsButton.createDownloadButton();
	}

	private void doSwitchGroup(Group newCurrentGroup) {
		if (newCurrentGroup != null && newCurrentGroup.getName() == "*") {
			setCurrentGroup(null);
			athleteEditingFormFactory.setCurrentGroup(null);
		} else {
			setCurrentGroup(newCurrentGroup);
			athleteEditingFormFactory.setCurrentGroup(newCurrentGroup);
		}
		// getRouterLayout().updateHeader(true);
		getGroupFilter().setValue(newCurrentGroup);
	}

	private void generateStartNumbers() {
		Group group = getCurrentGroup();
		if (group == null) {
			errorNotification();
			return;
		}
		AthleteRepository.assignStartNumbers(group);
		refresh();
	}

	private Group getCurrentGroup() {
		return this.currentGroup;
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
			Category c1 = mainRankings1 != null ? mainRankings1.getCategory() : null;
			Category c2 = mainRankings2 != null ? mainRankings2.getCategory() : null;
			compare = ObjectUtils.compare(
			        c1,
			        c2,
			        true);
			if (compare != 0) {
				return compare;
			}
			compare = ObjectUtils.compare(a1.getEntryTotal(), a2.getEntryTotal());
			return -compare;
		};
		return groupCategoryComparator;
	}

	private void setCurrentGroup(Group currentGroup) {
		this.currentGroup = currentGroup;
	}

	private void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(
		        location.getQueryParameters().getParameters());
		if (!isIgnoreGroupFromURL() && newGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
			if (newGroup != null) {
				params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
				this.setCurrentGroup(newGroup);
				OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
				this.crudGrid.setCrudFormFactory(crudFormFactory);
			}
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	private void updateURLLocations() {
	}
}