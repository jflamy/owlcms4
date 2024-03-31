/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
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
import app.owlcms.spreadsheet.JXLSStartingListDocs;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
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
	private ComboBox<Championship> ageDivisionFilter;
	private ComboBox<String> ageGroupFilter;
	private ComboBox<Platform> platformFilter;
	private String groupName;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in
	 * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public DocsContent() {
	}

	/**
	 * Create the top bar.
	 *
	 * Note: the top bar is created before the content.
	 *
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 *
	 * @param topBar
	 */
	@Override
	public FlexLayout createMenuArea() {
		this.topBar = new FlexLayout();

		Button bwButton = createBWButton();
		Button categoriesListButton = createCategoriesListButton();
		Button teamsListButton = createTeamsListButton();

		Button cardsButton = createCardsButton();
		Button weighInSummaryButton = createWeighInSummaryButton();
		Button sessionsButton = createSessionsButton();
		Button officialSchedule = createOfficalsButton();
		Button checkInButton = createCheckInButton();

		createTopBarGroupSelect();

		Hr hr = new Hr();
		hr.setWidthFull();
		hr.getStyle().set("margin", "0");
		hr.getStyle().set("padding", "0");
		FlexLayout buttons = new FlexLayout(
		        new NativeLabel(Translator.translate("Entries")),
		        bwButton, categoriesListButton, teamsListButton,
		        hr,
		        new NativeLabel(Translator.translate("Preparation_Groups")),
		        sessionsButton, cardsButton, weighInSummaryButton, checkInButton, officialSchedule);
		buttons.getStyle().set("flex-wrap", "wrap");
		buttons.getStyle().set("gap", "1ex");
		buttons.getStyle().set("margin-left", "5em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.add(this.topBarMenu, buttons);
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

		return this.topBar;
	}

	/**
	 * Get the content of the crudGrid. Invoked by refreshGrid.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */

	@Override
	public List<Athlete> findAll() {
		return athletesFindAll();
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
			Championship valueOf = Championship.valueOf(ageDivisionName);
			setAgeDivision(valueOf);
			this.ageDivisionFilter.setValue(valueOf);
		} catch (Exception e) {
			setAgeDivision(null);
			this.ageDivisionFilter.setValue(null);
		}
		// remove if now null
		String value = getAgeDivision() != null ? getAgeDivision().name() : null;
		updateParam(params1, "ad", value);

		List<String> ageGroupParams = params1.get("ag");
		// no age group is the default
		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
		setAgeGroupPrefix(ageGroupPrefix);
		this.ageGroupFilter.setValue(ageGroupPrefix);
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
		this.genderFilter.setValue(genderValue);
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
		this.platformFilter.setValue(this.getPlatform());
		updateParam(params1, "platform", platformParam != null ? platformParam : null);

		// logger.debug("params {}", params1);
		setUrlParameterMap(params1);
		return params1;
	}

	@Override
	public void refresh() {
		this.crudGrid.sort(null);
		this.crudGrid.refreshGrid();
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
		this.groupName = (groups != null && !groups.isEmpty() ? groups.get(0) : null);
		getGroupFilter().setValue(GroupRepository.findByName(this.groupName));

		event.getUI().getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
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

	protected Button createCardsButton() {
		String resourceDirectoryLocation = "/templates/cards";
		String title = Translator.translate("AthleteCards");
		JXLSDownloader cardsButtonFactory = new JXLSDownloader(
		        () -> {
			        // group may have been edited since the page was loaded
			        JXLSCardsDocs cardsXlsWriter = new JXLSCardsDocs();
			        cardsXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        List<Athlete> athletesFindAll = athletesFindAll();
			        cardsXlsWriter.setSortedAthletes(athletesFindAll);
			        return cardsXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedCardsTemplateFileName,
		        Competition::setCardsTemplateFileName,
		        title,
		        Translator.translate("Download"));
		cardsButtonFactory.setProcessingMessage(Translator.translate("LongProcessing"));
		return cardsButtonFactory.createDownloadButton();
	}

	protected Button createCheckInButton() {
		String resourceDirectoryLocation = "/templates/checkin";
		String title = Translator.translate("Preparation.Check-in");
		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        startingXlsWriter.setPostProcessor(null);
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(athletesFindAll()));
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getCheckInTemplateFileName,
		        Competition::setCheckInTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	protected Button createOfficalsButton() {
		String resourceDirectoryLocation = "/templates/officials";
		String title = Translator.translate("StartingList.Officials");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        startingXlsWriter.setSortedAthletes(participationFindAll());
			        startingXlsWriter.setEmptyOk(true);
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedOfficialsListTemplateFileName,
		        Competition::setOfficialsListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	protected Button createSessionsButton() {
		String resourceDirectoryLocation = "/templates/start";
		String title = Translator.translate("StartingList");

		JXLSDownloader startingListFactory = new JXLSDownloader(
		        () -> {
			        JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
			        // group may have been edited since the page was loaded
			        startingXlsWriter.setGroup(
			                getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
			        // get current version of athletes.
			        startingXlsWriter.setPostProcessor(null);
			        // findAll();
			        // List<Athlete> sortedAthletes = startingXlsWriter.getSortedAthletes();
			        startingXlsWriter.setSortedAthletes(AthleteSorter.registrationOrderCopy(athletesFindAll()));
			        return startingXlsWriter;
		        },
		        resourceDirectoryLocation,
		        Competition::getComputedStartListTemplateFileName,
		        Competition::setStartListTemplateFileName,
		        title,
		        Translator.translate("Download"));
		return startingListFactory.createDownloadButton();
	}

	@Override
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

	protected Button createWeighInSummaryButton() {
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

	/**
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	@Override
	protected void defineFilters(OwlcmsCrudGrid<Athlete> crudGrid) {

		this.teamFilter.setPlaceholder(Translator.translate("Team"));
		this.teamFilter.setItems(AthleteRepository.findAllTeams());
		this.teamFilter.getStyle().set("--vaadin-combo-box-overlay-width", "25em");
		this.teamFilter.setClearButtonVisible(true);
		this.teamFilter.addValueChangeListener(e -> {
			setTeam(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.teamFilter);

		getGroupFilter().setPlaceholder(Translator.translate("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());
		getGroupFilter().setItems(groups);
		getGroupFilter().setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		getGroupFilter().getStyle().set("display", "none");
		getGroupFilter().addValueChangeListener(e -> {
			Group value = e.getValue();
			Group currentGroup = value != null ? (value.getName().contentEquals("*") ? null : value) : null;
			setGroup(currentGroup);
			crudGrid.refreshGrid();
			// updateURLLocation(getLocationUI(), getLocation(), getCurrentGroup());
		});
		crudGrid.getCrudLayout().addFilterComponent(getGroupFilter());

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

		if (this.ageDivisionFilter == null) {
			this.ageDivisionFilter = new ComboBox<>();
		}
		this.ageDivisionFilter.setPlaceholder(getTranslation("Championship"));
		List<Championship> adItems = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
		this.ageDivisionFilter.setItems(adItems);
		this.ageDivisionFilter.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
		this.ageDivisionFilter.setClearButtonVisible(true);
		this.ageDivisionFilter.setWidth("8em");
		this.ageDivisionFilter.getStyle().set("margin-left", "1em");
		this.ageDivisionFilter.addValueChangeListener(e -> {
			setAgeDivision(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.ageDivisionFilter);

		if (this.ageGroupFilter == null) {
			this.ageGroupFilter = new ComboBox<>();
		}
		this.ageGroupFilter.setPlaceholder(getTranslation("AgeGroup"));
		List<String> agItems = AgeGroupRepository.findActiveAndUsed(getAgeDivision());
		this.ageGroupFilter.setItems(agItems);
		// ageGroupFilter.setItemLabelGenerator((ad) -> Translator.translate("Division."
		// + ad.name()));
		this.ageGroupFilter.setClearButtonVisible(true);
		this.ageGroupFilter.setWidth("8em");
		this.ageGroupFilter.getStyle().set("margin-left", "1em");
		this.ageGroupFilter.addValueChangeListener(e -> {
			setAgeGroupPrefix(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.ageGroupFilter);
		this.ageGroupFilter.setValue(getAgeGroupPrefix());

		if (this.platformFilter == null) {
			this.platformFilter = new ComboBox<>();
		}
		this.platformFilter.setPlaceholder(getTranslation("Platform"));
		List<Platform> agItems1 = PlatformRepository.findAll();
		this.platformFilter.setItems(agItems1);
		// platformFilter.setItemLabelGenerator((ad) -> Translator.translate("Division."
		// + ad.name()));
		this.platformFilter.setClearButtonVisible(true);
		this.platformFilter.setWidth("8em");
		this.platformFilter.getStyle().set("margin-left", "1em");
		this.platformFilter.addValueChangeListener(e -> {
			setPlatform(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.platformFilter);
		// logger.debug("setting platform filter {}", getPlatform());
		this.platformFilter.setValue(getPlatform());

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			this.lastNameFilter.clear();
			this.ageGroupFilter.clear();
			this.ageDivisionFilter.clear();
			this.platformFilter.clear();
			this.genderFilter.clear();
		});
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

}
