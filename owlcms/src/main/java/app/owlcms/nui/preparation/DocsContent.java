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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.components.DownloadDialog;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCardsDocs;
import app.owlcms.spreadsheet.JXLSCategoriesListDocs;
import app.owlcms.spreadsheet.JXLSStartingListDocs;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.NaturalOrderComparator;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ResultsContent.
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@Route(value = "preparation/docs", layout = OwlcmsLayout.class)
public class DocsContent extends RegistrationContent implements HasDynamicTitle {

	public static final String PRECOMP_DOCS_TITLE = "Preparation.PrecompDocsTitle";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(DocsContent.class);
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}

	private ComboBox<AgeDivision> ageDivisionFilter;
	private ComboBox<String> ageGroupFilter;
	private ComboBox<Platform> platformFilter;

	private JXLSStartingListDocs startingXlsWriter;
	private JXLSCategoriesListDocs categoriesXlsWriter;
	private JXLSCardsDocs cardsXlsWriter;
	private String groupName;

	Map<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in
	 * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public DocsContent() {
		cardsXlsWriter = new JXLSCardsDocs();
		startingXlsWriter = new JXLSStartingListDocs();
		categoriesXlsWriter = new JXLSCategoriesListDocs();
	}

	/**
	 * Create the top bar.
	 *
	 * Note: the top bar is created before the content.
	 *
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and
	 *      vice-versa
	 *
	 * @param topBar
	 */
	@Override
	public FlexLayout createMenuArea() {
		topBar = new FlexLayout();

		Button cardsButton = createCardsButton();
		Button weighInSummaryButton = createWeighInSummaryButton();
		Button sessionsButton = createSessionsButton();
		Button categoriesListButton = createCategoriesListButton();
		Button teamsListButton = createTeamsListButton();
		Button officialSchedule = createOfficalsButton();
		Button bwButton = createBWButton();

		createTopBarGroupSelect();

		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		FlexLayout buttons = new FlexLayout(new Label(Translator.translate("Entries")),
		        bwButton, categoriesListButton, teamsListButton, hr,
		        new Label(Translator.translate("Preparation_Groups")),
		        sessionsButton, officialSchedule, cardsButton, weighInSummaryButton);
		buttons.getStyle().set("flex-wrap", "wrap");
		buttons.getStyle().set("gap", "1ex");
//		buttons.setPadding(false);
//		buttons.setMargin(false);
//		buttons.setSpacing(true);
		buttons.getStyle().set("margin-left", "5em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar.getStyle().set("flex", "100 1");
		topBar.add(topBarMenu, buttons);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		return topBar;
	}

	/**
	 * Get the content of the crudGrid. Invoked by refreshGrid.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */

	@Override
	public List<Athlete> findAll() {
		List<Athlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(getAgeGroupPrefix(),
		        getAgeDivision());

		Category catFilterValue = getCategoryValue();
		Stream<Athlete> stream = athletes.stream()
		        .filter(a -> {
			        Platform platformFilterValue = platformFilter != null ? platformFilter.getValue() : null;
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
			        Gender genderFilterValue = genderFilter != null ? genderFilter.getValue() : null;
			        Gender athleteGender = a.getGender();
			        boolean catOk = (catFilterValue == null
			                || catFilterValue.toString().equals(a.getCategory().toString()))
			                && (genderFilterValue == null || genderFilterValue == athleteGender);
			        return catOk;
		        })
		        .filter(a -> getGroup() != null ? getGroup().equals(a.getGroup())
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


	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Preparation.PrecompDocsTitle");
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

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#readParams(com.vaadin.flow.router.Location,
	 *      java.util.Map)
	 */
	@Override
	public HashMap<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		HashMap<String, List<String>> params1 = new HashMap<>(parametersMap);

		List<String> ageDivisionParams = params1.get("ad");
		try {
			String ageDivisionName = (ageDivisionParams != null
			        && !ageDivisionParams.isEmpty() ? ageDivisionParams.get(0) : null);
			AgeDivision valueOf = AgeDivision.valueOf(ageDivisionName);
			setAgeDivision(valueOf);
			ageDivisionFilter.setValue(valueOf);
		} catch (Exception e) {
			setAgeDivision(null);
			ageDivisionFilter.setValue(null);
		}
		// remove if now null
		String value = getAgeDivision() != null ? getAgeDivision().name() : null;
		updateParam(params1, "ad", value);

		List<String> ageGroupParams = params1.get("ag");
		// no age group is the default
		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
		setAgeGroupPrefix(ageGroupPrefix);
		ageGroupFilter.setValue(ageGroupPrefix);
		String value2 = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateParam(params1, "ag", value2);
		
		List<String> groupParams = params1.get("group");
		// no age group is the default
		String groupString = (groupParams != null && !groupParams.isEmpty() ? groupParams.get(0) : null);
		Group groupValue = groupString != null ? GroupRepository.findByName(groupString) : null;
		setGroup(groupValue);
		getGroupFilter().setValue(groupValue);
		updateParam(params1, "group", groupString);

		List<String> genderParams = params1.get("gender");
		// no age group is the default
		String genderString = (genderParams != null && !genderParams.isEmpty() ? genderParams.get(0) : null);
		Gender genderValue = genderString != null ? Gender.valueOf(genderString) : null;
		setGender(genderValue);
		genderFilter.setValue(genderValue);
		updateParam(params1, "gender", genderString);

		List<String> catParams = params1.get("cat");
		String catParam = (catParams != null && !catParams.isEmpty() ? catParams.get(0) : null);
		catParam = catParam != null ? URLDecoder.decode(catParam, StandardCharsets.UTF_8) : null;
		this.setCategory(CategoryRepository.findByCode(catParam));
		String catValue = getCategoryValue() != null ? getCategoryValue().toString() : null;
		updateParam(params1, "cat", catValue);

		List<String> platformParams = params1.get("platform");
		String platformParam = (platformParams != null && !platformParams.isEmpty() ? platformParams.get(0) : null);
		platformParam = platformParam != null ? URLDecoder.decode(platformParam, StandardCharsets.UTF_8) : null;
		this.setPlatform(platformParam != null ? PlatformRepository.findByName(platformParam) : null);
		// logger.debug("reading param platform {}", platformParam);
		platformFilter.setValue(this.getPlatform());
		updateParam(params1, "platform", platformParam != null ? platformParam : null);

		// logger.debug("params {}", params1);
		setUrlParameterMap(params1);
		return params1;
	}

	@Override
	public void refresh() {
		crudGrid.sort(null);
		crudGrid.refreshGrid();
	}

	/**
	 * @see app.owlcms.nui.shared.OwlcmsContent#setHeaderContent()
	 */
	@Override
	public void setHeaderContent() {
		getRouterLayout().setMenuTitle(getPageTitle());
		getRouterLayout().setMenuArea(createMenuArea());
		getRouterLayout().showLocaleDropdown(false);
		getRouterLayout().setDrawerOpened(false);
		getRouterLayout().updateHeader(true);
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
	public void setParameter(BeforeEvent event, @OptionalParameter String unused) {
		Location location = event.getLocation();
		setLocation(location);
		setLocationUI(event.getUI());

		// the OptionalParameter string is the part of the URL path that can be
		// interpreted as REST arguments
		// we use the ? query parameters instead.
		QueryParameters queryParameters = location.getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters();
		HashMap<String, List<String>> params = readParams(location, parametersMap);
		List<String> groups = params.get("group");
		groupName = (groups != null && !groups.isEmpty() ? groups.get(0) : null);
		getGroupFilter().setValue(GroupRepository.findByName(groupName));

		event.getUI().getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		Map<String, List<String>> params = new HashMap<>(
		        location.getQueryParameters().getParameters());
		if (!isIgnoreGroupFromURL() && newGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
		} else {
			params.remove("group");
		}
		params = URLUtils.cleanParams(params);
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	/**
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	@Override
	protected void defineFilters(OwlcmsCrudGrid<Athlete> crudGrid) {
		getGroupFilter().setPlaceholder(Translator.translate("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());
		getGroupFilter().setItems(groups);
		getGroupFilter().setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		getGroupFilter().getStyle().set("display", "none");
		getGroupFilter().addValueChangeListener(e -> {
			Group value = e.getValue();
			Group currentGroup = value != null ? (value.getName().contentEquals("*") ? null : value) : null;
			setGroup(currentGroup);
			crudGrid.refreshGrid();
			//updateURLLocation(getLocationUI(), getLocation(), getCurrentGroup());
		});
		crudGrid.getCrudLayout().addFilterComponent(getGroupFilter());

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

		if (ageDivisionFilter == null) {
			ageDivisionFilter = new ComboBox<>();
		}
		ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
		List<AgeDivision> adItems = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
		ageDivisionFilter.setItems(adItems);
		ageDivisionFilter.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
		ageDivisionFilter.setClearButtonVisible(true);
		ageDivisionFilter.setWidth("8em");
		ageDivisionFilter.getStyle().set("margin-left", "1em");
		ageDivisionFilter.addValueChangeListener(e -> {
			setAgeDivision(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(ageDivisionFilter);

		if (ageGroupFilter == null) {
			ageGroupFilter = new ComboBox<>();
		}
		ageGroupFilter.setPlaceholder(getTranslation("AgeGroup"));
		List<String> agItems = AgeGroupRepository.findActiveAndUsed(getAgeDivision());
		ageGroupFilter.setItems(agItems);
		// ageGroupFilter.setItemLabelGenerator((ad) -> Translator.translate("Division."
		// + ad.name()));
		ageGroupFilter.setClearButtonVisible(true);
		ageGroupFilter.setWidth("8em");
		ageGroupFilter.getStyle().set("margin-left", "1em");
		ageGroupFilter.addValueChangeListener(e -> {
			setAgeGroupPrefix(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(ageGroupFilter);
		ageGroupFilter.setValue(getAgeGroupPrefix());

		if (platformFilter == null) {
			platformFilter = new ComboBox<>();
		}
		platformFilter.setPlaceholder(getTranslation("Platform"));
		List<Platform> agItems1 = PlatformRepository.findAll();
		platformFilter.setItems(agItems1);
		// platformFilter.setItemLabelGenerator((ad) -> Translator.translate("Division."
		// + ad.name()));
		platformFilter.setClearButtonVisible(true);
		platformFilter.setWidth("8em");
		platformFilter.getStyle().set("margin-left", "1em");
		platformFilter.addValueChangeListener(e -> {
			setPlatform(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(platformFilter);
		// logger.debug("setting platform filter {}", getPlatform());
		platformFilter.setValue(getPlatform());

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			lastNameFilter.clear();
			ageGroupFilter.clear();
			ageDivisionFilter.clear();
			platformFilter.clear();
			genderFilter.clear();
		});
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play
	 * (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}



	private Button createBWButton() {
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

	private Button createCardsButton() {
		String resourceDirectoryLocation = "/templates/cards";
		String title = Translator.translate("AthleteCards");
		String downloadedFilePrefix = "cards";
		DownloadDialog cardsButtonFactory = new DownloadDialog(
		        () -> {
			        // group may have been edited since the page was loaded
			        cardsXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        findAll();
			        return cardsXlsWriter;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedCardsTemplateFileName,
		        Competition::setCardsTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return cardsButtonFactory.createTopBarDownloadButton();
	}

	private Button createCategoriesListButton() {
		String resourceDirectoryLocation = "/templates/categories";
		String title = Translator.translate("StartingList.Categories");
		String downloadedFilePrefix = "categories";

		DownloadDialog startingListFactory = new DownloadDialog(
		        () -> {
			        // group may have been edited since the page was loaded
			        categoriesXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        findAll();
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

	private Button createOfficalsButton() {
		String resourceDirectoryLocation = "/templates/officials";
		String title = Translator.translate("StartingList.Officials");
		String downloadedFilePrefix = "officials";

		DownloadDialog startingListFactory = new DownloadDialog(
		        () -> {
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        null,
		        Competition::getComputedOfficialsListTemplateFileName,
		        Competition::setOfficialsListTemplateFileName,
		        title,
		        downloadedFilePrefix, Translator.translate("Download"));
		return startingListFactory.createTopBarDownloadButton();
	}

	private Button createSessionsButton() {
		String resourceDirectoryLocation = "/templates/start";
		String title = Translator.translate("StartingList");
		String downloadedFilePrefix = "startingList";

		DownloadDialog startingListFactory = new DownloadDialog(
		        () -> {
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        findAll();
			        List<Athlete> sortedAthletes = startingXlsWriter.getSortedAthletes();
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(sortedAthletes));
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

	private Button createTeamsListButton() {
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

	private Button createWeighInSummaryButton() {
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

}
