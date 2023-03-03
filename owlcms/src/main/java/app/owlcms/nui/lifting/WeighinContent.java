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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.form.impl.field.provider.CheckBoxGroupProvider;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.components.DownloadDialog;
import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.LocalizedDecimalField;
import app.owlcms.components.fields.ValidationTextField;
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
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.AthleteRegistrationFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCards;
import app.owlcms.spreadsheet.JXLSJurySheet;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.utils.NaturalOrderComparator;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class WeighinContent
 *
 * Defines the toolbar and the table for editing data on athletes during
 * weigh-in
 *
 */
@SuppressWarnings("serial")
@Route(value = "preparation/weighin", layout = OwlcmsLayout.class)
@CssImport(value = "./styles/shared-styles.css")
public class WeighinContent extends VerticalLayout implements CrudListener<Athlete>, OwlcmsContent, FOPParameters {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(WeighinContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
	private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	private OwlcmsCrudGrid<Athlete> crudGrid;
	private Group currentGroup;
	private ComboBox<Gender> genderFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();
	private TextField lastNameFilter = new TextField();
	private Location location;
	private UI locationUI;
	private OwlcmsLayout routerLayout;
	private ComboBox<Boolean> weighedInFilter = new ComboBox<>();

	private Button cardsButton;

	// private Group group;

//    private ComboBox<Group> groupSelect;
	private GroupSelectionMenu topBarMenu;
//    private FlexLayout topBar;

	private Button juryButton;

	private Button startingWeightsButton;
	private Button weighInButton;

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	/**
	 * Instantiates the athlete crudGrid
	 */
	public WeighinContent() {
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
		crudGrid = createGrid(crudFormFactory);
		defineFilters(crudGrid);
		fillHW(crudGrid, this);
	}

	@Override
	public Athlete add(Athlete athlete) {
		if (athlete.getGroup() == null && currentGroup != null) {
			athlete.setGroup(currentGroup);
		}
		((OwlcmsCrudFormFactory<Athlete>) crudGrid.getCrudFormFactory()).add(athlete);
		return athlete;
	}

	@Override
	public FlexLayout createMenuArea() {
		createTopBarGroupSelect();

		JXLSCards cardsWriter = new JXLSCards();
		JXLSJurySheet juryWriter = new JXLSJurySheet();
		cardsButton = createCardsButton(cardsWriter);
		startingWeightsButton = createStartingWeightsButton();
		weighInButton = createWeighInButton();
		juryButton = createJuryButton(juryWriter);

		Button start = new Button(getTranslation("GenerateStartNumbers"), (e) -> {
			generateStartNumbers();
		});
		Button clear = new Button(getTranslation("ClearStartNumbers"), (e) -> {
			clearStartNumbers();
		});

		HorizontalLayout buttons = new HorizontalLayout(start, clear, weighInButton, cardsButton, startingWeightsButton,
		        juryButton);
		buttons.setPadding(false);
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		FlexLayout topBar = new FlexLayout();
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
	 * The refresh button on the toolbar calls this.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		List<Athlete> findFiltered = AthleteRepository.findFiltered(lastNameFilter.getValue(),
		        getGroupFilter().getValue(),
		        categoryFilter.getValue(), ageGroupFilter.getValue(), ageDivisionFilter.getValue(),
		        genderFilter.getValue(), weighedInFilter.getValue(), -1, -1);
		AthleteSorter.registrationOrder(findFiltered);
		return findFiltered;
	}

//    public ComboBox<Group> getGroupSelect() {
//        return groupSelect;
//    }

	/**
	 * @return the groupFilter
	 */
	public ComboBox<Group> getGroupFilter() {
		return groupFilter;
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
		return getTranslation("WeighIn");
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

//    /**
//     * @param groupSelect the groupSelect to set
//     */
//    public void setGroupSelect(ComboBox<Group> groupSelect) {
//        this.groupSelect = groupSelect;
//    }

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	public void refresh() {
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
			setCurrentGroup(GroupRepository.findByName(groupName));
		} else {
			currentGroup = null;
		}
		if (getCurrentGroup() != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(getCurrentGroup().getName())));
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

	private Button createCardsButton(JXLSCards cardsWriter) {
		String resourceDirectoryLocation = "/templates/cards";
		String title = Translator.translate("AthleteCards");
		String downloadedFilePrefix = "cards";
		DownloadDialog cardsButtonFactory = new DownloadDialog(
		        () -> {
			        JXLSCards rs = new JXLSCards();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedCardsTemplateFileName,
		        Competition::setCardsTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return cardsButtonFactory.createTopBarDownloadButton();
	}

	/**
	 * The content and ordering of the editing form
	 *
	 * @param crudFormFactory the factory that will create the form using this
	 *                        information
	 */
	private void createFormLayout(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		List<String> props = new LinkedList<>();
		List<String> captions = new LinkedList<>();

		props.add("lastName");
		captions.add(getTranslation("LastName"));
		props.add("firstName");
		captions.add(getTranslation("FirstName"));

		props.add("bodyWeight");
		captions.add(getTranslation("BodyWeight"));
		props.add("snatch1Declaration");
		captions.add(getTranslation("SnatchDecl_"));
		props.add("cleanJerk1Declaration");
		captions.add(getTranslation("C_and_J_decl"));

		props.add("qualifyingTotal");
		captions.add(getTranslation("EntryTotal"));
		props.add("category");
		captions.add(getTranslation("Weighin.Category"));
		props.add("eligibleCategories");
		captions.add(getTranslation("Weighin.EligibleCategories"));
		props.add("group");
		captions.add(getTranslation("Group"));

		props.add("gender");
		captions.add(getTranslation("Gender"));
		props.add("team");
		captions.add(getTranslation("Team"));

		Competition competition = Competition.getCurrent();
		if (competition.isUseBirthYear()) {
			props.add("yearOfBirth");
			captions.add(getTranslation("YearOfBirth"));
		} else {
			props.add("fullBirthDate");
			captions.add(getTranslation("BirthDate_yyyy"));
		}
		props.add("membership");
		captions.add(getTranslation("Membership"));

		props.add("coach");
		captions.add(getTranslation("Coach"));
		props.add("custom1");
		captions.add(getTranslation("Custom1.Title"));
		props.add("custom2");
		captions.add(getTranslation("Custom2.Title"));

		props.add("lotNumber");
		captions.add(getTranslation("Lot"));

		props.add("federationCodes");
		captions.add(getTranslation("Registration.FederationCodes"));

		props.add("eligibleForIndividualRanking");
		captions.add(getTranslation("Eligible for Individual Ranking?"));

		crudFormFactory.setVisibleProperties(props.toArray(new String[0]));
		crudFormFactory.setFieldCaptions(captions.toArray(new String[0]));

		crudFormFactory.setFieldProvider("gender", new OwlcmsComboBoxProvider<>(getTranslation("Gender"),
		        Arrays.asList(Gender.mfValues()), new TextRenderer<>(Gender::name), Gender::name));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());
		crudFormFactory.setFieldProvider("group", new OwlcmsComboBoxProvider<>(getTranslation("Group"),
		        groups, new TextRenderer<>(Group::getName), Group::getName));
		crudFormFactory.setFieldProvider("category", new OwlcmsComboBoxProvider<>(getTranslation("Category"),
		        CategoryRepository.findActive(), new TextRenderer<>(Category::getName), Category::getName));
		crudFormFactory.setFieldProvider("eligibleCategories",
		        new CheckBoxGroupProvider<>(getTranslation("Weighin.EligibleCategories"),
		                new ArrayList<Category>(), (c) -> (c.getName())));
//        crudFormFactory.setFieldProvider("ageDivision",
//                new OwlcmsComboBoxProvider<>(getTranslation("AgeDivision"), Arrays.asList(AgeDivision.values()),
//                        new TextRenderer<>(ad -> getTranslation("Division." + ad.name())), AgeDivision::name));

		crudFormFactory.setFieldType("bodyWeight", LocalizedDecimalField.class);
		crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class);

		// ValidationTextField (or a wrapper) must be used as workaround for unexplained
		// validation behaviour
		crudFormFactory.setFieldType("snatch1Declaration", ValidationTextField.class);
		crudFormFactory.setFieldType("cleanJerk1Declaration", ValidationTextField.class);
		crudFormFactory.setFieldType("qualifyingTotal", ValidationTextField.class);
		crudFormFactory.setFieldType("yearOfBirth", ValidationTextField.class);

		crudFormFactory.setFieldCreationListener("bodyWeight", (e) -> {
			((LocalizedDecimalField) e).focus();
		});
	}

	private Button createJuryButton(JXLSJurySheet juryWriter) {
		String resourceDirectoryLocation = "/templates/jury";
		String title = Translator.translate("Jury");
		String downloadedFilePrefix = "jury";

		DownloadDialog juryButton = new DownloadDialog(
		        () -> {
			        JXLSJurySheet rs = new JXLSJurySheet();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedJuryTemplateFileName,
		        Competition::setJuryTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return juryButton.createTopBarDownloadButton();
	}

	private Button createStartingWeightsButton() {
		String resourceDirectoryLocation = "/templates/emptyProtocol";
		String title = Translator.translate("EmptyProtocolSheet");
		String downloadedFilePrefix = "startingWeights";

		DownloadDialog startingWeightsButton = new DownloadDialog(
		        () -> {
			        JXLSWeighInSheet rs = new JXLSWeighInSheet();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedStartingWeightsSheetTemplateFileName,
		        Competition::setStartingWeightsSheetTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return startingWeightsButton.createTopBarDownloadButton();
	}

	private Button createWeighInButton() {
		String resourceDirectoryLocation = "/templates/weighin";
		String title = Translator.translate("WeighinForm");
		String downloadedFilePrefix = "weighIn";

		DownloadDialog startingWeightsButton = new DownloadDialog(
		        () -> {
			        JXLSWeighInSheet rs = new JXLSWeighInSheet();
			        // group may have been edited since the page was loaded
			        Group curGroup = getGroupFilter().getValue();
			        rs.setGroup(curGroup != null ? GroupRepository.getById(curGroup.getId()) : null);
			        return rs;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedStartingWeightsSheetTemplateFileName,
		        Competition::setStartingWeightsSheetTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return startingWeightsButton.createTopBarDownloadButton();
	}

	private void doSwitchGroup(Group newCurrentGroup) {
		if (newCurrentGroup != null && newCurrentGroup.getName() == "*") {
			setCurrentGroup(null);
		} else {
			setCurrentGroup(newCurrentGroup);
		}
		getRouterLayout().updateHeader(true);
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
		return currentGroup;
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
				currentGroup = newGroup;
				OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
				crudGrid.setCrudFormFactory(crudFormFactory);
			}
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	/**
	 * Define the form used to edit a given athlete.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
		OwlcmsCrudFormFactory<Athlete> athleteEditingFormFactory = new AthleteRegistrationFormFactory(Athlete.class,
		        currentGroup);
		// logger.trace("created form factory {} {}",
		// System.identityHashCode(athleteEditingFormFactory), currentGroup);
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
		grid.addColumn("ageGroup").setHeader(getTranslation("AgeGroup")).setAutoWidth(true);
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
		OwlcmsCrudGrid<Athlete> crudGrid = new OwlcmsCrudGrid<>(Athlete.class, new OwlcmsGridLayout(Athlete.class) {
			@Override
			public void hideForm() {
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

	protected void createTopBarGroupSelect() {
		// there is already all the SQL filtering logic for the group attached
		// hidden field in the crudGrid part of the page so we just set that
		// filter.

		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());

		OwlcmsSession.withFop(fop -> {
			// logger.debug("initial setting group to {} {}", getCurrentGroup(),
			// LoggerUtils.whereFrom());
			getGroupFilter().setValue(getCurrentGroup());
			// switching to group "*" is understood to mean all groups
			topBarMenu = new GroupSelectionMenu(groups, getCurrentGroup(),
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

		lastNameFilter.setPlaceholder(getTranslation("LastName"));
		lastNameFilter.setClearButtonVisible(true);
		lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		lastNameFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(lastNameFilter);

		ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
		ageDivisionFilter.setItems(AgeDivision.findAll());
		ageDivisionFilter.setItemLabelGenerator((ad) -> getTranslation("Division." + ad.name()));
		ageDivisionFilter.setClearButtonVisible(true);
		ageDivisionFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(ageDivisionFilter);

		ageGroupFilter.setPlaceholder(getTranslation("AgeGroup"));
		ageGroupFilter.setItems(AgeGroupRepository.findAll());
		// ageGroupFilter.setItemLabelGenerator(AgeDivision::name);
		ageGroupFilter.setClearButtonVisible(true);
		ageGroupFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		ageGroupFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(ageGroupFilter);

		categoryFilter.setPlaceholder(getTranslation("Category"));
		categoryFilter.setItems(CategoryRepository.findActive());
		categoryFilter.setItemLabelGenerator(Category::getName);
		categoryFilter.setClearButtonVisible(true);
		categoryFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(categoryFilter);

		groupFilter.setPlaceholder(getTranslation("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());
		groupFilter.setItems(groups);
		groupFilter.setItemLabelGenerator(Group::getName);
		groupFilter.setClearButtonVisible(true);
		groupFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
			setCurrentGroup(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), e.getValue());
		});
		crudGrid.getCrudLayout().addFilterComponent(groupFilter);
		// hide because the top bar has it
		groupFilter.getStyle().set("display", "none");

		weighedInFilter.setPlaceholder(getTranslation("Weighed_in_p"));
		weighedInFilter.setItems(Boolean.TRUE, Boolean.FALSE);
		weighedInFilter.setItemLabelGenerator((i) -> {
			return i ? getTranslation("Weighed") : getTranslation("Not_weighed");
		});
		weighedInFilter.setClearButtonVisible(true);
		weighedInFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(weighedInFilter);

		genderFilter.setPlaceholder(getTranslation("Gender"));
		if (Competition.getCurrent().isGenderInclusive()) {
			genderFilter.setItems(Gender.M, Gender.F);
			genderFilter.setItemLabelGenerator((i) -> {
				switch (i) {
				case M:
					return getTranslation("Gender.M");
				case F:
					return getTranslation("Gender.F");
				default:
					throw new IllegalStateException("can't happen");
				}
			});
		} else {
			genderFilter.setItems(Gender.M, Gender.F, Gender.I);
			genderFilter.setItemLabelGenerator((i) -> {
				switch (i) {
				case M:
					return getTranslation("Gender.M");
				case F:
					return getTranslation("Gender.F");
				case I:
					return getTranslation("Gender.I");
				default:
					throw new IllegalStateException("can't happen");
				}
			});
		}
		genderFilter.setClearButtonVisible(true);
		genderFilter.addValueChangeListener(e -> {
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
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	protected void errorNotification() {
		Label content = new Label(getTranslation("Select_group_first"));
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

	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	protected void setContentGroup(ComponentValueChangeEvent<ComboBox<Group>, Group> e) {
		groupFilter.setValue(e.getValue());
	}
}
