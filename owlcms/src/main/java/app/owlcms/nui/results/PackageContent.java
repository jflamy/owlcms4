/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.results;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.layout.CrudLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ResultsParametersReader;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.AthleteCrudGrid;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.spreadsheet.JXLSWinningSheet;
import app.owlcms.spreadsheet.JXLSWorkbookStreamSource;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class PackageContent.
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@Route(value = "results/finalpackage", layout = OwlcmsLayout.class)
public class PackageContent extends AthleteGridContent implements HasDynamicTitle, ResultsParametersReader, IFilterCascade {

	static final String TITLE = "Results.EndOfCompetition";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(PackageContent.class);
	static {
		jexlLogger.setLevel(Level.ERROR);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private AgeGroup ageGroup;
	private ComboBox<String> ageGroupFilter;
	private String ageGroupPrefix;
	private Category category;
	private ComboBox<Category> categoryFilter;
	private Category categoryValue;
	private Championship championship;
	private List<String> championshipAgeGroupPrefixes;
	private ComboBox<Championship> championshipFilter;
	private List<Championship> championshipItems;
	private Group currentGroup;
	private JXLSDownloader downloadDialog;
	private Gender gender;
	private Checkbox includeUnfinishedCategories;
	private ComboBox<Ranking> rankingSelector;
	private Ranking scoringSystem;
	private boolean winnersOnly;

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public PackageContent() {
	}

	@Override
	public Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		// First call parent to handle FOP, group, sound parameters
		Map<String, List<String>> params = super.readParams(location, parametersMap);
		
		// Parse results-specific parameters: championship (ad), age group prefix (agp), category (cat), gender
		// Championship
		List<String> ageDivisionParams = params.get("ad");
		String ageDivisionName = (ageDivisionParams != null && !ageDivisionParams.isEmpty() ? ageDivisionParams.get(0) : null);
		if (ageDivisionName != null && !ageDivisionName.isEmpty()) {
			Championship valueOf = Championship.of(ageDivisionName);
			setChampionship(valueOf);
		}
		
		// Age group prefix
		List<String> ageGroupParams = params.get("agp");
		String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
		setAgeGroupPrefix(ageGroupPrefix);
		
		// Category
		List<String> catParams = params.get("cat");
		String catParam = (catParams != null && !catParams.isEmpty() ? catParams.get(0) : null);
		if (catParam != null) {
			catParam = java.net.URLDecoder.decode(catParam, java.nio.charset.StandardCharsets.UTF_8);
			setCategory(app.owlcms.data.category.CategoryRepository.findByCode(catParam));
		}
		
		// Gender
		List<String> genderParams = params.get("gender");
		String genderParam = (genderParams != null && !genderParams.isEmpty() ? genderParams.get(0) : null);
		if (genderParam != null) {
			try {
				Gender g = Gender.valueOf(genderParam.toUpperCase());
				setGender(g);
			} catch (IllegalArgumentException e) {
				// Invalid gender value, ignore
			}
		}
		
		return params;
	}
	
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		// Call parent to parse URL parameters
		super.setParameter(event, parameter);
		
		// Now apply the parsed URL parameters to the filter UI components
		// This runs AFTER readParams() has set our field values
		applyUrlFiltersToUI();
	}
	
	/**
	 * Apply URL parameters to filter dropdowns.
	 * Called after readParams() has populated the field values.
	 */
	private void applyUrlFiltersToUI() {
		Championship urlAD = getChampionship();
		String urlAG = getAgeGroupPrefix();
		Category urlCat = getCategoryValue();
		Gender urlGender = getGender();

		// Apply championship filter
		if (urlAD != null && getChampionshipFilter() != null) {
			List<Championship> items = getChampionshipItems();
			if (items != null && items.contains(urlAD)) {
				getChampionshipFilter().setValue(urlAD);
			}
		}
		
		// Apply age group prefix filter
		if (urlAG != null && getAgeGroupFilter() != null) {
			List<String> prefixes = getChampionshipAgeGroupPrefixes();
			if (prefixes != null && prefixes.contains(urlAG)) {
				getAgeGroupFilter().setValue(urlAG);
			}
		}
		
		// Apply category filter
		if (urlCat != null && getCategoryFilter() != null) {
			getCategoryFilter().setValue(urlCat);
		}
		
		// Apply gender filter
		if (urlGender != null && getGenderFilter() != null) {
			getGenderFilter().setValue(urlGender);
		}
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
		// show arrow but close menu
		getAppLayout().setMenuVisible(true);
		getAppLayout().closeDrawer();

		this.topBar = new FlexLayout();

		Button finalPackageDownloadButton = createFinalPackageDownloadButton();
		Button registrationResultsButton = createRegistrationResultsDownloadButton();
		Button categoryResultsDownloadButton = createCategoryResultsDownloadButton();

		HorizontalLayout buttons = new HorizontalLayout(registrationResultsButton, categoryResultsDownloadButton,
		        finalPackageDownloadButton);
		buttons.getStyle().set("margin-left", "5em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		buttons.setPadding(false);
		buttons.setMargin(false);
		buttons.setSpacing(true);

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.removeAll();
		this.topBar.add(buttons);
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
	public Collection<Athlete> findAll() {
		Competition competition = Competition.getCurrent();
		HashMap<String, Object> beans = competition.computeReportingInfo(this.ageGroupPrefix, this.championship);

		String key = "mwTot";
		@SuppressWarnings("unchecked")
		List<Athlete> ranked = (List<Athlete>) beans.get(key);

		boolean allCategories = Boolean.TRUE.equals(this.includeUnfinishedCategories.getValue());
		// unfinished categories need to be computed using all relevant athletes, including not weighed-in yet
		@SuppressWarnings("unchecked")
		String unfinishedCategories = AthleteRepository.allUnfinishedCategories().toString();
		logger.info("unfinished categories {} {}", unfinishedCategories, LoggerUtils.whereFrom());

		if (ranked == null || ranked.isEmpty()) {
			return new ArrayList<>();
		}

		Category catFilterValue = getCategoryValue();
		Stream<Athlete> stream = ranked.stream()
		        // .peek(r -> logger.debug("looking at {} {} *** {}", r.getAbbreviatedName(), r.getCategory().getCode(), r.getParticipations().get(0)))
		        .filter(a -> {
			        Gender genderFilterValue = this.getGender();
			        Gender athleteGender = a.getGender();
			        boolean catOk = (catFilterValue == null
			                || (a.getCategory() != null && catFilterValue.getCode().equals(a.getCategory().getCode())))
			                && (genderFilterValue == null || genderFilterValue == athleteGender)
			                && (allCategories || !unfinishedCategories.contains(a.getCategory().getCode()));
			        return catOk;
		        })
		        .filter(a -> !this.winnersOnly || a.getTotalRank() == 1)
		// /* logger.debug( */.peek(r -> logger./**/warn("including {} {} *** {}", r.getAbbreviatedName(), r.getCategory().getCode(),
		// r.getParticipations().get(0)))
		;
		List<Athlete> found = stream.collect(Collectors.toList());
		logger.debug("{} PackageContent findAll", found.size());
		updateURLLocations();
		return found;
	}

	@Override
	public AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	@Override
	public ComboBox<String> getAgeGroupFilter() {
		return this.ageGroupFilter;
	}

	@Override
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	@Override
	public Category getCategory() {
		return this.category;
	}

	@Override
	public ComboBox<Category> getCategoryFilter() {
		return this.categoryFilter;
	}

	@Override
	public Category getCategoryValue() {
		return this.categoryValue;
	}

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
		return super.getCrudGrid();
	}

	/**
	 * @see app.owlcms.nui.results.IFilterCascade#getCrudLayout(org.vaadin.crudui.crud.impl.GridCrud)
	 */
	@Override
	public CrudLayout getCrudLayout(GridCrud<Athlete> crud) {
		return crud.getCrudLayout();
	}

	@Override
	public Gender getGender() {
		return this.gender;
	}

	@Override
	public ComboBox<Gender> getGenderFilter() {
		return this.genderFilter;
	}

	public Group getGridGroup() {
		return getGroupFilter().getValue();
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
		return Translator.translate(TITLE);
	}

	public Ranking getScoringSystem() {
		// Return global default if not yet set
		return this.scoringSystem != null ? this.scoringSystem : Competition.getCurrent().getScoringSystem();
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	@Override
	public boolean isShowInitialDialog() {
		return false;
	}

	public void refresh() {
		getCrudGrid().refreshGrid();
	}

	@Override
	public void setAgeGroup(AgeGroup ag) {
		this.ageGroup = ag;
	}

	@Override
	public void setAgeGroupFilter(ComboBox<String> topBarAgeGroupPrefixSelect) {
		this.ageGroupFilter = topBarAgeGroupPrefixSelect;
	}

	@Override
	public void setAgeGroupPrefix(String value) {
		this.ageGroupPrefix = value;
	}

	@Override
	public void setCategory(Category cat) {
		this.category = cat;
	}

	@Override
	public void setCategoryFilter(ComboBox<Category> categoryFilter) {
		this.categoryFilter = categoryFilter;
	}

	@Override
	public void setCategoryValue(Category category) {
		this.categoryValue = category;
	}

	@Override
	public void setChampionship(Championship ageDivision) {
		// logger.debug("setAgeDivision to {} from {}",championship, LoggerUtils.whereFrom());
		this.championship = ageDivision;
	}

	@Override
	public void setChampionshipAgeGroupPrefixes(List<String> championshipAgeGroupPrefixes) {
		this.championshipAgeGroupPrefixes = championshipAgeGroupPrefixes;
	}

	@Override
	public void setChampionshipFilter(ComboBox<Championship> topBarAgeDivisionSelect) {
		this.championshipFilter = topBarAgeDivisionSelect;
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

	public void setRankingSelector(ComboBox<Ranking> rankingSelector) {
		this.rankingSelector = rankingSelector;
	}

	@Override
	public void setShowInitialDialog(boolean b) {
	}

	@Override
	public boolean showGenderFilter() {
		return true;
	}

	@Override
	public Athlete update(Athlete a) {
		Athlete a1 = super.update(a);
		return a1;
	}

	@Override
	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(
		        location.getQueryParameters().getParameters());
		if (!isIgnoreGroupFromURL() && newGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
		} else {
			params.remove("group");
		}
		Location newLocation = new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params)));
		URLUtils.replaceState(ui.getPage().getHistory(),null, newLocation, location);
	}

	@Override
	protected HorizontalLayout announcerButtons(FlexLayout topBar2) {
		return null;
	}

	/**
	 * Gets the crudGrid.
	 *
	 * @return the crudGrid crudGrid
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createCrudGrid(app.owlcms.nui.crudui.OwlcmsCrudFormFactory)
	 */
	@Override
	protected AthleteCrudGrid createCrudGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		Grid<Athlete> grid = SessionResultsContent.createResultGrid(this.getScoringSystem());

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
		AthleteCrudGrid crudGrid = new AthleteCrudGrid(Athlete.class, gridLayout, crudFormFactory, grid) {
			@Override
			protected void initToolbar() {
				Component reset = createReset();
				if (reset != null) {
					this.crudLayout.addToolbarComponent(reset);
					Element toolbar = reset.getParent().get().getElement();
					toolbar.getStyle().set("flex-wrap", "wrap").set("align-content", "center");
				}
			}

			@Override
			protected void updateButtonClicked() {
				// only edit non-lifting groups
				if (!checkFOP()) {
					super.updateButtonClicked();
				}
			}

			@Override
			protected void updateButtons() {
			}
		};

		crudGrid.setCrudListener(this);
		crudGrid.setClickRowToUpdate(true);
		crudGrid.getCrudLayout().addToolbarComponent(getGroupFilter());

		return crudGrid;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createReset()
	 */
	@Override
	protected Component createReset() {
		this.reset = new Button(Translator.translate("RecomputeRanks"), new Icon(VaadinIcon.REFRESH),
		        (e) -> {
			        // resetRanks();
			        Competition.recomputeAllAthleteRanks();
			        refresh();
		        });

		this.reset.getElement().setAttribute("title", Translator.translate("RecomputeRanks"));
		this.reset.getElement().setAttribute("theme", "secondary contrast small icon");
		return this.reset;
	}

	@Override
	protected void defineFilters(GridCrud<Athlete> crud) {
		defineFilterCascade(crud);
		this.includeUnfinishedCategories = new Checkbox(Translator.translate("Video.includeNotCompleted"));
		getCrudLayout(crud).addFilterComponent(this.includeUnfinishedCategories);
		defineSelectionListeners();

		this.includeUnfinishedCategories.addValueChangeListener(e -> crud.refreshGrid());

		Checkbox winnersOnlyCheckbox = new Checkbox(Translator.translate("Results.WinnersOnly"));

		if (this.getRankingSelector() == null) {
			ComboBox<Ranking> scoringCombo = new ComboBox<>(Translator.translate("Ranking.BestAthlete"));
			scoringCombo.setItems(Ranking.scoringSystems());
			scoringCombo.setItemLabelGenerator(r -> Ranking.getScoringExplanation(r));
			scoringCombo.getElement().getStyle().set("--vaadin-combo-box-overlay-width", "30ch");
			scoringCombo.setWidth("30ch");
			this.setRankingSelector(scoringCombo);
			scoringCombo.setClearButtonVisible(true);
			getCrudLayout(crud).addFilterComponent(scoringCombo);
			scoringCombo.addValueChangeListener(event -> {
				if (!event.isFromClient()) {
					return;
				}
				bestAthleteScoringSelected(event.getValue());
			});
			// Initialize to current value or global default
			Ranking initialValue = this.scoringSystem != null ? this.scoringSystem : Competition.getCurrent().getScoringSystem();
			scoringCombo.setValue(initialValue);
			if (this.scoringSystem == null) {
				setScoringSystem(initialValue); // Set field on first initialization
			}

			winnersOnlyCheckbox.setValue(this.winnersOnly);
			winnersOnlyCheckbox.addValueChangeListener(event -> {
				if (!event.isFromClient()) {
					return;
				}
				setWinnersOnly(event.getValue());
				resetGrid();
			});
			getCrudLayout(crud).addFilterComponent(new VerticalLayout(scoringCombo, winnersOnlyCheckbox));
		}
		
		Button clearFilters = new Button(null, VaadinIcon.CLOSE.create());
		clearFilters.addClickListener(event -> {
			clearFilters();
			this.includeUnfinishedCategories.setValue(false);
			winnersOnlyCheckbox.setValue(false);
		});
		
		getCrudLayout(crud).addFilterComponent(clearFilters);

		this.getCategoryFilter().setClearButtonVisible(true);
		this.getCategoryFilter().setPlaceholder(Translator.translate("Category"));
		this.getCategoryFilter().setClearButtonVisible(true);
		this.getCategoryFilter().setWidth("10em");
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {

	}

	protected void updateURLLocations() {
		if (getLocation() == null) {
			// sometimes called from routines outside of normal event flow.
			return;
		}

		updateURLLocation(UI.getCurrent(), getLocation(), "fop", null);
		String agp = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "agp",
		        agp);
		String ad = getChampionship() != null ? getChampionship().getName() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "ad",
		        ad);
		String cat = getCategoryValue() != null ? getCategoryValue().getComputedCode() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "cat",
		        cat);
		String gender = getGender() != null ? getGender().name() : null;
		updateURLLocation(UI.getCurrent(), getLocation(), "gender",
		        gender);
		// logger.debug("update URL {} {} {}",agp,ad,cat);
	}

	/**
	 * @return true if the current group is safe for editing -- i.e. not lifting currently
	 */
	private boolean checkFOP() {
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		FieldOfPlay liftingFop = null;
		search: for (FieldOfPlay fop : fops) {
			if (fop.getGroup() != null && fop.getGroup().equals(this.currentGroup)) {
				liftingFop = fop;
				break search;
			}
		}
		if (liftingFop != null) {
			Notification.show(
			        Translator.translate("Warning_GroupLifting") + liftingFop.getName() + Translator.translate("CannotEditResults"),
			        3000, Position.MIDDLE);
			logger.debug(Translator.translate("CannotEditResults_logging"), this.currentGroup, liftingFop);
		} else {
			logger.debug(Translator.translate("EditingResults_logging"), this.currentGroup, liftingFop);
		}
		return liftingFop != null;
	}

	private Ranking computeScoringSystem() {
		Ranking ranking;
		if (getRankingSelector() != null && getRankingSelector().getValue() != null) {
			ranking = getRankingSelector().getValue();
		} else {
			ranking = Competition.getCurrent().getScoringSystem();
		}
		return ranking;
	}

	private Ranking computeScoringSystemForBook() {
		Ranking ranking;
		if (getRankingSelector() != null && getRankingSelector().getValue() != null) {
			ranking = getRankingSelector().getValue();
		} else {
			ranking = Competition.getCurrent().getScoringSystem();
		}
		return ranking;
	}

	private Button createCategoryResultsDownloadButton() {
		this.downloadDialog = new JXLSDownloader(
		        () -> {
			        JXLSWinningSheet rs = new JXLSWinningSheet(true);
			        rs.setChampionship(this.championship);
			        rs.setAgeGroupPrefix(this.ageGroupPrefix);
			        rs.setCategory(getCategoryValue());
			        // group may have been edited since the page was loaded
			        rs.setGroup(this.currentGroup != null ? GroupRepository.getById(this.currentGroup.getId()) : null);

			        // For eligibility category results, only use scoring system when a championship is selected.
			        // Without a championship, pass null to ignore the global dropdown value.
			        Ranking computeScoringSystem = (this.championship != null) ? computeScoringSystem() : null;
			        logger.debug("setBestLifterScoringSystem {} {}", computeScoringSystem);
			        rs.setBestLifterScoringSystem(computeScoringSystem);

			        List<Athlete> all = (List<Athlete>) findAll();
			        rs.setSortedAthletes(all);
			        return rs;
		        },
		        "/templates/competitionResults",
		        Competition::getComputedResultsTemplateFileName,
		        Competition::setResultsTemplateFileName,
		        Translator.translate("EligibilityCategoryResults"),
		        Translator.translate("Download"));
		this.downloadDialog.setProcessingMessage(Translator.translate("LongProcessing"));
		Button resultsButton = this.downloadDialog.createDownloadButton();
		return resultsButton;
	}

	private Button createFinalPackageDownloadButton() {
		this.downloadDialog = new JXLSDownloader(
				() -> {
					JXLSCompetitionBook rs = new JXLSCompetitionBook(this.locationUI);
					rs.setChampionship(this.championship);
					rs.setAgeGroupPrefix(this.ageGroupPrefix);
					rs.setCategory(this.categoryValue);
					rs.setIncludeUnfinished(Boolean.TRUE.equals(this.includeUnfinishedCategories.getValue()));
					rs.setWinnersOnly(this.winnersOnly);
					Ranking computeScoringSystem = computeScoringSystemForBook();
					logger.debug("setBestLifterScoringSystem {} {}", computeScoringSystem);
					rs.setBestLifterScoringSystem(computeScoringSystem);
					return rs;
				},
				"/templates/competitionBook",
				Competition::getComputedFinalPackageTemplateFileName,
				Competition::setFinalPackageTemplateFileName,
				Translator.translate("FinalResultsPackage"),
				Translator.translate("Download"));
		this.downloadDialog.setProcessingMessage(Translator.translate("LongProcessing"));
		Button resultsButton = this.downloadDialog.createDownloadButton();
		highlight(resultsButton);
		return resultsButton;
	}

	private Button createRegistrationResultsDownloadButton() {
		this.downloadDialog = new JXLSDownloader(
		        () -> {
			        JXLSWinningSheet rs = new JXLSWinningSheet(false);
			        rs.setChampionship(this.championship);
			        rs.setAgeGroupPrefix(this.ageGroupPrefix);
			        rs.setCategory(getCategoryValue());
			        rs.setGroup(null);
			        rs.setSortedAthletes((List<Athlete>) findAll());

			        Ranking computeScoringSystem = computeScoringSystem();
			        rs.setBestLifterScoringSystem(computeScoringSystem);
			        return rs;
		        },
		        "/templates/competitionResults",
		        Competition::getComputedResultsTemplateFileName,
		        Competition::setResultsTemplateFileName,
		        Translator.translate("RegistrationCategoryResults"),
		        Translator.translate("Download"));
		this.downloadDialog.setProcessingMessage(Translator.translate("LongProcessing"));
		Button resultsButton = this.downloadDialog.createDownloadButton();
		return resultsButton;
	}

	private ComboBox<Ranking> getRankingSelector() {
		return this.rankingSelector;
	}

	private void highlight(Button button) {
		button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
	}

	private void resetGrid() {
		// Create new grid with updated columns for the new scoring system
		Grid<Athlete> newGrid = SessionResultsContent.createResultGrid(this.getScoringSystem());
		
		// Replace just the grid component, preserving all filters and layout
		this.getCrudGrid().replaceGrid(newGrid);
		
		// Refresh data
		this.getCrudGrid().refreshGrid();
	}

	@SuppressWarnings("unused")
	private void resetRanks() {
		// clear ranks, for debugging purposes
		JPAService.runInTransaction(em -> {
			List<Athlete> l = AthleteRepository.findAllByGroupAndWeighIn(null, true);
			for (Athlete a : l) {
				for (Participation p : a.getParticipations()) {
					p.setSnatchRank(-2);
					p.setCleanJerkRank(-2);
					p.setTotalRank(-2);
					p.setCategoryScoreRank(-2);
					p.setCustomRank(-2);
				}
				em.merge(a);
			}
			return null;
		});
	}

	private void setScoringSystem(Ranking value) {
		this.scoringSystem = value;
	}
	
	private void bestAthleteScoringSelected(Ranking ranking) {
		setScoringSystem(ranking);
		resetGrid();
	}
	
	private void setWinnersOnly(boolean value) {
		this.winnersOnly = value;
	}
	
	@Override
	public void onChampionshipChanged(Championship championship) {
		// Update scoring system dropdown when championship changes
		// Get best athlete scoring system from championship's age groups
		// Use null age group to get scoring from all age groups in championship
		if (this.getRankingSelector() != null) {
			Ranking newRanking;
			if (championship != null) {
				// Get scoring system for the championship (across all age groups)
				// The age group prefix will be recomputed by the cascade
				newRanking = championship.getBestAthleteScoringSystem(null);
			} else {
				newRanking = null;
			}
			if (newRanking == null) {
				newRanking = Competition.getCurrent().getScoringSystem();
			}
			// Update dropdown and field, then refresh grid via helper
			this.getRankingSelector().setValue(newRanking);
			setScoringSystem(newRanking);
			resetGrid();
		}
	}

}
