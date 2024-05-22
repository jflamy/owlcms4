/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

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
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.results.IFilterCascade;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCardsDocs;
import app.owlcms.spreadsheet.JXLSStartingListDocs;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
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
public class DocsContent extends RegistrationContent implements HasDynamicTitle, IFilterCascade {

	public static final String PRECOMP_DOCS_TITLE = "Preparation.PrecompDocsTitle";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(DocsContent.class);
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private ComboBox<String> ageGroupFilter;
	private ComboBox<Category> categoryFilter;
	private List<String> championshipAgeGroupPrefixes;
	private ComboBox<Championship> championshipFilter;
	private List<Championship> championshipItems;
	private ComboBox<Gender> genderFilter;
	private String groupName;
	private ComboBox<Platform> platformFilter;

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
		return athletesFindAll(false);
	}

	@Override
	public ComboBox<String> getAgeGroupFilter() {
		return this.ageGroupFilter;
	}

	@Override
	public ComboBox<Category> getCategoryFilter() {
		return this.categoryFilter;
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
	public ComboBox<Gender> getGenderFilter() {
		return this.genderFilter;
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
//	@Override
//	public HashMap<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
//		HashMap<String, List<String>> params1 = new HashMap<>(parametersMap);
//
//		List<String> ageDivisionParams = params1.get("ad");
//		try {
//			String ageDivisionName = (ageDivisionParams != null
//			        && !ageDivisionParams.isEmpty() ? ageDivisionParams.get(0) : null);
//			Championship valueOf = Championship.of(ageDivisionName);
//			if (valueOf != null) {
//				setChampionship(valueOf);
//				this.getChampionshipFilter().setValue(valueOf);
//			}
//		} catch (Exception e) {
//			setChampionship(null);
//			this.getChampionshipFilter().setValue(null);
//		}
//		// remove if now null
//		String value = getChampionship() != null ? getChampionship().getName() : null;
//		updateParam(params1, "ad", value);
//
//		List<String> ageGroupParams = params1.get("ag");
//		// no age group is the default
//		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
//		setAgeGroupPrefix(ageGroupPrefix);
//		this.getAgeGroupFilter().setValue(ageGroupPrefix);
//		String value2 = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
//		updateParam(params1, "ag", value2);
//
//		List<String> groupParams = params1.get("group");
//		// no age group is the default
//		String groupString = (groupParams != null && !groupParams.isEmpty() ? groupParams.get(0) : null);
//		Group groupValue = groupString != null ? GroupRepository.findByName(groupString) : null;
//		setGroup(groupValue);
//		getGroupFilter().setValue(groupValue);
//		updateParam(params1, "group", groupString);
//
//		List<String> genderParams = params1.get("gender");
//		// no age group is the default
//		String genderString = (genderParams != null && !genderParams.isEmpty() ? genderParams.get(0) : null);
//		Gender genderValue = genderString != null ? Gender.valueOf(genderString) : null;
//		setGender(genderValue);
//		this.getGenderFilter().setValue(genderValue);
//		updateParam(params1, "gender", genderString);
//
//		List<String> catParams = params1.get("cat");
//		String catParam = (catParams != null && !catParams.isEmpty() ? catParams.get(0) : null);
//		catParam = catParam != null ? URLDecoder.decode(catParam, StandardCharsets.UTF_8) : null;
//		this.setCategory(CategoryRepository.findByCode(catParam));
//		String catValue = getCategoryValue() != null ? getCategoryValue().toString() : null;
//		updateParam(params1, "cat", catValue);
//
//		List<String> platformParams = params1.get("platform");
//		String platformParam = (platformParams != null && !platformParams.isEmpty() ? platformParams.get(0) : null);
//		platformParam = platformParam != null ? URLDecoder.decode(platformParam, StandardCharsets.UTF_8) : null;
//		this.setPlatform(platformParam != null ? PlatformRepository.findByName(platformParam) : null);
//		// logger.debug("reading param platform {}", platformParam);
//		this.getPlatformFilter().setValue(this.getPlatform());
//		updateParam(params1, "platform", platformParam != null ? platformParam : null);
//
//		// logger.debug("params {}", params1);
//		setUrlParameterMap(params1);
//		return params1;
//	}

	@Override
	public Category getCategoryValue() {
		return super.getCategoryValue();
	}

	@Override
	public void refresh() {
		this.crudGrid.sort(null);
		this.crudGrid.refreshGrid();
	}

	@Override
	public void setAgeGroupFilter(ComboBox<String> ageGroupFilter) {
		this.ageGroupFilter = ageGroupFilter;
	}

	@Override
	public void setCategoryFilter(ComboBox<Category> categoryFilter) {
		this.categoryFilter = categoryFilter;
	}

	@Override
	public void setCategoryValue(Category category) {
		super.setCategory(category);
	}

	@Override
	public void setChampionshipAgeGroupPrefixes(List<String> championshipAgeGroupPrefixes) {
		this.championshipAgeGroupPrefixes = championshipAgeGroupPrefixes;
	}

	@Override
	public void setChampionshipFilter(ComboBox<Championship> championshipSelect) {
		this.championshipFilter = championshipSelect;
	}

	@Override
	public void setChampionshipItems(List<Championship> championshipItems) {
		this.championshipItems = championshipItems;
	}

	@Override
	public void setGenderFilter(ComboBox<Gender> genderFilter) {
		this.genderFilter = genderFilter;
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
		Map<String, List<String>> params = readParams(location, parametersMap);
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
			        List<Athlete> athletesFindAll = athletesFindAll(true);
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
			        List<Athlete> athletesFindAll = athletesFindAll(true);
			        startingXlsWriter.setSortedAthletes(athletesFindAll);
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
			        List<Athlete> athletesFindAll = athletesFindAll(true);
			        startingXlsWriter.setSortedAthletes(athletesFindAll);
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
		this.defineFilterCascade(crudGrid);
		this.defineRegistrationFilters(crudGrid, false);
		this.defineSelectionListeners();

		this.getTeamFilter().setPlaceholder(Translator.translate("Team"));
		this.getTeamFilter().setItems(AthleteRepository.findAllTeams());
		this.getTeamFilter().getStyle().set("--vaadin-combo-box-overlay-width", "25em");
		this.getTeamFilter().setClearButtonVisible(true);
		this.getTeamFilter().addValueChangeListener(e -> {
			setTeam(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.getTeamFilter());

		if (this.getPlatformFilter() == null) {
			this.setPlatformFilter(new ComboBox<>());
		}
		this.getPlatformFilter().setPlaceholder(getTranslation("Platform"));
		List<Platform> agItems1 = PlatformRepository.findAll();
		this.getPlatformFilter().setItems(agItems1);
		// platformFilter.setItemLabelGenerator((ad) -> Translator.translate("Division."
		// + ad.name()));
		this.getPlatformFilter().setClearButtonVisible(true);
		this.getPlatformFilter().setWidth("8em");
		this.getPlatformFilter().getStyle().set("margin-left", "1em");
		this.getPlatformFilter().addValueChangeListener(e -> {
			setPlatform(e.getValue());
			crudGrid.refreshGrid();
		});
		crudGrid.getCrudLayout().addFilterComponent(this.getPlatformFilter());
		// logger.debug("setting platform filter {}", getPlatform());
		this.getPlatformFilter().setValue(getPlatform());

		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			clearFilters();
		});
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	@Override
	public void clearFilters() {
		this.getAgeGroupFilter().clear();
		this.getChampionshipFilter().clear();
		this.getCategoryFilter().clear();
		this.getGenderFilter().clear();
		this.getPlatformFilter().clear();
		this.getLastNameFilter().clear();
		this.getWeighedInFilter().clear();
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	public ComboBox<Platform> getPlatformFilter() {
		return platformFilter;
	}

	public void setPlatformFilter(ComboBox<Platform> platformFilter) {
		this.platformFilter = platformFilter;
	}

}
