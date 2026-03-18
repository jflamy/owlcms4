/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.results;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.components.GroupSelectionMenu;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.WinningOrderComparator;
import app.owlcms.data.category.UnfinishedCategories;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.AthleteCrudGrid;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSMedalsSheet;
import app.owlcms.spreadsheet.JXLSResultSheet;
import app.owlcms.spreadsheet.JXLSWinningSheet;
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
@Route(value = "results/results", layout = OwlcmsLayout.class)
public class SessionResultsContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(SessionResultsContent.class);
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}

	public static Grid<Athlete> createResultGrid(Ranking scoringSystem) {
		Grid<Athlete> grid = new Grid<>(Athlete.class, false);
		grid.getThemeNames().add("row-stripes");
		ThemeList themes = grid.getThemeNames();
		themes.add("compact");
		themes.add("row-stripes");

		grid.addColumn("category").setHeader(Translator.translate("Category"));

		grid.addColumn("total").setHeader(Translator.translate("Total"))
		        .setComparator(new WinningOrderComparator(Ranking.TOTAL, true));
		grid.addColumn("totalRank").setHeader(Translator.translate("TotalRank"))
		        .setComparator(new WinningOrderComparator(Ranking.TOTAL, false))
		        .setRenderer(new TextRenderer<>((a) -> Ranking.formatScoreboardRank(a.getTotalRank())));

		grid.addColumn("lastName").setHeader(Translator.translate("LastName"));
		grid.addColumn("firstName").setHeader(Translator.translate("FirstName"));
		grid.addColumn("team").setHeader(Translator.translate("Team"));
		grid.addColumn("group").setHeader(Translator.translate("Group"));
		grid.addColumn("bestSnatch").setHeader(Translator.translate("Snatch"));
		grid.addColumn("snatchRank").setHeader(Translator.translate("SnatchRank"))
		        .setComparator(new WinningOrderComparator(Ranking.SNATCH, false))
		        .setRenderer(new TextRenderer<>((a) -> Ranking.formatScoreboardRank(a.getSnatchRank())));
		grid.addColumn("bestCleanJerk").setHeader(Translator.translate("Clean_and_Jerk"));
		grid.addColumn("cleanJerkRank").setHeader(Translator.translate("Clean_and_Jerk_Rank"))
		        .setComparator(new WinningOrderComparator(Ranking.CLEANJERK, false))
		        .setRenderer(new TextRenderer<>((a) -> Ranking.formatScoreboardRank(a.getCleanJerkRank())));

		// NumberRenderer<Athlete> renderer;
		// renderer = new NumberRenderer<>(a -> Ranking.getRankingValue(a, scoringSystem), "%.2f", OwlcmsSession.getLocale(), "0000.00");
		grid.addColumn(
		        a -> computeScore(scoringSystem, a))
		        //.setSortProperty("bestAthleteScore")
		        .setHeader(
		                // Translator.translate("Ranking." + scoringSystem
		                Translator.translate("Score"))
		        //.setComparator(new WinningOrderComparator(scoringSystem, true))
//		        .setComparator( (d,e) -> Comparator
//		        		.comparing((Athlete x) -> computeScore(scoringSystem, df, x))
//		        		.compare(d,e));
		        .setAutoWidth(true)
		        .setSortable(true);
		return grid;
	}

	public static String computeScore(Ranking scoringSystem, Athlete a) {
		var compSS = Competition.getCurrent().getScoringSystem();
		var ageGroup = a.getAgeGroup();

		Ranking ss;
		if (scoringSystem != null) {
		    // use the dropdown selection if it is present.
		    ss = scoringSystem;
		} else if (ageGroup != null) {
			ss = ageGroup.getBestAthleteScoringSystem() != null ?  ageGroup.getBestAthleteScoringSystem() : compSS;
		} else {
			// defensive
			ss = Competition.getCurrent().getScoringSystem();
		}
		return Ranking.getScoringTitle(ss) + " " + String.format(OwlcmsSession.getLocale(), "%7.2f", Ranking.getRankingValue(a, ss));
	}

	public static String formatBlankRank(Integer total) {
		if (total == null || total == 0) {
			return "&nbsp;";
		} else if (total == -1) {
			// invited lifter, not eligible.
			return Translator.translate("Results.Extra/Invited");
		} else {
			return total.toString();
		}
	}

	public static String formatScoreboardRank(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			// invited lifter, not eligible.
			return Translator.translate("Results.Extra/Invited");
		} else {
			return total.toString();
		}
	}

	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Group currentGroup;
	private JXLSDownloader downloadDialog;
	private Checkbox medalsOnly;
	private ComboBox<Ranking> rankingSelector;
	private Ranking scoringSystem;

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public SessionResultsContent() {
	}

	/**
	 * Gets the crudGrid.
	 *
	 * @return the crudGrid crudGrid
	 *
	 * @see app.owlcms.nui.shared.AthleteGridContent#createCrudGrid(app.owlcms.nui.crudui.OwlcmsCrudFormFactory)
	 */
	@Override
	public AthleteCrudGrid createCrudGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		Grid<Athlete> grid = SessionResultsContent.createResultGrid(null);

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
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 */
	@Override
	public FlexLayout createMenuArea() {
		logger.debug("createMenuArea");
		// show back arrow but close menu
		getAppLayout().setMenuVisible(true);
		getAppLayout().closeDrawer();

		this.topBar = new FlexLayout();

		Button resultsButton = createEligibilityResultsDownloadButton();
		Button registrationResultsButton = createRegistrationResultsDownloadButton();
		highlight(registrationResultsButton);
		Button medalsButtons = createGroupMedalsDownloadButton();

		createTopBarGroupSelect();

		HorizontalLayout buttons = new HorizontalLayout(registrationResultsButton, resultsButton, medalsButtons);
		buttons.getStyle().set("margin-left", "5em");
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		buttons.setMargin(false);
		buttons.setPadding(false);
		buttons.setSpacing(true);

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.removeAll();
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
	public Collection<Athlete> findAll() {
		Gender currentGender = this.getGenderFilter().getValue();

		List<Athlete> rankedAthletes = AthleteSorter.assignCategoryRanks(getCurrentGroup());
		logger.debug("ResultsContent ranked athletes {}", rankedAthletes.size());

		// unfinished categories need to be computed using all relevant athletes, including not weighed-in yet
		UnfinishedCategories unfinishedCategories = AthleteRepository.allUnfinishedCategories();
		// logger.debug("ResultsContent unfinished categories {}", unfinishedCategories);

		if (getCurrentGroup() != null) {
			rankedAthletes = AthleteSorter.displayOrderCopy(rankedAthletes).stream()
			        .filter(a -> a.getGroup() != null ? a.getGroup().equals(getCurrentGroup()) : false)
			        .filter(a -> a.getGender() != null
			                ? (currentGender != null ? currentGender.equals(a.getGender()) : true)
			                : false)
			        .map(a -> {
				        if (a.getCategory() != null && unfinishedCategories.contains(a.getCategory())) {
					        a.setCategoryFinished(false);
				        } else {
					        a.setCategoryFinished(true);
				        }
				        return a;
			        })
			        .collect(Collectors.toList());
		} else {
			rankedAthletes = AthleteSorter.displayOrderCopy(rankedAthletes).stream()
			        .filter(a -> a.getGender() != null
			                ? (currentGender != null ? currentGender.equals(a.getGender()) : true)
			                : false)
			        .map(a -> {
				        if (a.getCategory() != null && unfinishedCategories.contains(a.getCategory())) {
					        a.setCategoryFinished(false);
				        } else {
					        a.setCategoryFinished(true);
				        }
				        return a;
			        })
			        .peek(a -> {
				        logger.debug("{}, {}", a.isCategoryFinished(), a.getFullName());
			        })
			        .collect(Collectors.toList());
		}

		Boolean medals = this.medalsOnly.getValue();
		if (medals != null && medals) {
			return rankedAthletes.stream()
			        .filter(a -> a.getMainRankings().getTotalRank() >= 1 && a.getMainRankings().getTotalRank() <= 3)
			        .collect(Collectors.toList());
		} else {
			return rankedAthletes;
		}
	}

	public Group getGridGroup() {
		return getGroupFilter().getValue();
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
		return Translator.translate("GroupResults");
	}

	public Ranking getScoringSystem() {
		return this.scoringSystem; // not reliable.
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	public void refresh() {
		this.getCrudGrid().sort(null);
		this.getCrudGrid().refreshGrid();
	}

	public void setGridGroup(Group group) {
		// subscribeIfLifting(group);
		getGroupFilter().setValue(group);
		refresh();
	}

	/**
	 * Parse the http query parameters
	 *
	 * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
	 *
	 * @param event     Vaadin navigation event
	 * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query parameters instead.
	 *
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		setLocation(event.getLocation());
		setLocationUI(event.getUI());
		QueryParameters queryParameters = getLocation().getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters(); // immutable
		HashMap<String, List<String>> params = new HashMap<>(parametersMap);

		logger.debug("parsing query parameters ResultContent");
		List<String> groupNames = params.get("group");
		if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
			String groupName = groupNames.get(0);
			if (groupName == "*") {
				// special group to show all athletes
				this.setCurrentGroup(null);
			} else {
				if (groupName.contains("%") || groupName.contains("+")) {
					groupName = URLDecoder.decode(groupName, StandardCharsets.UTF_8);
				}
				this.setCurrentGroup(GroupRepository.findByName(groupName));
			}
		} else {
			// if no group, we pick the first alphabetical group as a filter
			// to avoid showing hundreds of athlete at the end of each of the groups
			// (which has a noticeable impact on slower machines)
			List<Group> groups = GroupRepository.findAll();
			groups.sort(new NaturalOrderComparator<>());
			this.setCurrentGroup((groups.size() > 0 ? groups.get(0) : null));
		}
		if (this.getCurrentGroup() != null) {
			params.put("group", Arrays.asList(this.getCurrentGroup().getName()));
		} else {
			// params.remove("group");
			params.put("group", Arrays.asList("*"));
		}
		doSwitchGroup(this.getCurrentGroup());
		params.remove("fop");
		logger.debug("params {}", params);

		// change the URL to reflect group
		Location newLocation = new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params)));
		URLUtils.replaceState(event.getUI().getPage().getHistory(), null, newLocation, getLocation());
	}

	public void setRankingSelector(ComboBox<Ranking> rankingSelector) {
		this.rankingSelector = rankingSelector;
	}

	@Override
	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(
		        location.getQueryParameters().getParameters());
		if (!isIgnoreGroupFromURL() && newGroup != null) {
			params.put("group", Arrays.asList(newGroup.getName()));
		} else {
			params.remove("group");
		}
		Location newLocation = new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params)));
		URLUtils.replaceState(ui.getPage().getHistory(), null, newLocation, location);
	}

	@Override
	protected HorizontalLayout announcerButtons(FlexLayout topBar2) {
		return null;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createReset()
	 */
	@Override
	protected Component createReset() {
		this.reset = new Button(Translator.translate("RecomputeRanks"), new Icon(VaadinIcon.REFRESH),
		        (e) -> OwlcmsSession.withFop((fop) -> {
			        AthleteRepository.assignCategoryRanks();
			        refresh();
		        }));

		this.reset.getElement().setAttribute("title", Translator.translate("RecomputeRanks"));
		this.reset.getElement().setAttribute("theme", "secondary contrast small icon");
		return this.reset;
	}

	@Override
	protected void createTopBarGroupSelect() {
		// there is already all the SQL filtering logic for the group attached
		// hidden field in the crudGrid part of the page so we just set that
		// filter.

		List<Group> groups = GroupRepository.findAll();
		groups.sort(Group.groupSelectionComparator.reversed());

		OwlcmsSession.withFop(fop -> {
			// logger.debug("top bar setting group to {} {}", this.getCurrentGroup(), LoggerUtils.whereFrom());
			getGroupFilter().setValue(this.getCurrentGroup());
			// switching to group "*" is understood to mean all groups
			this.topBarMenu = new GroupSelectionMenu(groups, this.getCurrentGroup(),
			        fop,
			        (g1) -> doSwitchGroup(g1),
			        (g1) -> doSwitchGroup(new Group("*")),
			        null,
			        Translator.translate("AllGroups"), true);
		});
	}

	/**
	 * We do not control the groups on other screens/displays
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	@Override
	protected void defineFilters(GridCrud<Athlete> crud) {
		// logger.debug("defineFilters {} - {}\n{}", getGroup(), currentGroup,LoggerUtils.stackTrace());

		getGroupFilter().setPlaceholder(Translator.translate("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());
		getGroupFilter().setItems(groups);
		getGroupFilter().setValue(this.currentGroup);
		createTopBarGroupSelect();
		getGroupFilter().setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		getGroupFilter().getStyle().set("display", "none");
		getGroupFilter().addValueChangeListener(e -> {
			updateURLLocation(getLocationUI(), getLocation(), this.getCurrentGroup());
		});
		crud.getCrudLayout().addFilterComponent(getGroupFilter());

		if (this.medalsOnly == null) {
			this.medalsOnly = new Checkbox();
		}
		this.medalsOnly.setLabel(Translator.translate("MedalsOnly"));
		this.medalsOnly.setValue(false);
		this.medalsOnly.addValueChangeListener(e -> {
			this.getCrudGrid().getGrid().getElement().getClassList().set("medals", Boolean.TRUE.equals(e.getValue()));
			crud.refreshGrid();
		});
		crud.getCrudLayout().addFilterComponent(this.medalsOnly);

		if (this.getGenderFilter() == null) {
			this.setGenderFilter(new ComboBox<>());
		}
		this.getGenderFilter().setPlaceholder(Translator.translate("Gender"));
		this.getGenderFilter().setItems(Gender.M, Gender.F);
		this.getGenderFilter().setItemLabelGenerator((i) -> {
			return i.asGenderName();
		});
		this.getGenderFilter().setClearButtonVisible(true);
		this.getGenderFilter().addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		this.getGenderFilter().setWidth("10em");
		crud.getCrudLayout().addFilterComponent(this.getGenderFilter());

		if (this.getRankingSelector() == null) {
			ComboBox<Ranking> scoringCombo = new ComboBox<>(Translator.translate("Ranking.BestAthlete"));
			scoringCombo.setItems(Ranking.scoringSystems());
			scoringCombo.setItemLabelGenerator(r -> Ranking.getScoringExplanation(r));
			scoringCombo.getElement().getStyle().set("--vaadin-combo-box-overlay-width", "30ch");
			scoringCombo.setWidth("30ch");
			this.setRankingSelector(scoringCombo);
			getCrudLayout(crud).addFilterComponent(scoringCombo);
			scoringCombo.setValue(computeScoringSystem());
			scoringCombo.addValueChangeListener(event -> {
				if (!event.isFromClient()) {
					return;
				}
				setScoringSystem(event.getValue());
				resetGrid();
			});
		}
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
	}

	/**
	 * @return true if the current group is safe for editing -- i.e. not lifting currently
	 */
	private boolean checkFOP() {
		Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
		FieldOfPlay liftingFop = null;
		search: for (FieldOfPlay fop : fops) {
			if (fop.getGroup() != null && fop.getGroup().equals(this.getCurrentGroup())) {
				liftingFop = fop;
				break search;
			}
		}
		if (liftingFop != null) {
			Notification.show(
			        Translator.translate("Warning_GroupLifting") + liftingFop.getName()
			                + Translator.translate("CannotEditResults"),
			        3000, Position.MIDDLE);
			logger.debug(Translator.translate("CannotEditResults_logging"), this.getCurrentGroup(), liftingFop);
			// subscribeIfLifting(currentGroup);
		} else {
			logger.debug(Translator.translate("EditingResults_logging"), this.getCurrentGroup(), liftingFop);
		}
		return liftingFop != null;
	}

	private Ranking computeScoringSystem() {
		Ranking ranking;
		if (getRankingSelector() != null && getRankingSelector().getValue() != null) {
			ranking = getRankingSelector().getValue();
		} else {
			ranking = getScoringSystem() != null ? getScoringSystem() : Competition.getCurrent().getScoringSystem();
		}
		logger.debug("computeScoringSystem {}", ranking);
		return ranking;
	}

	private Button createEligibilityResultsDownloadButton() {
		this.downloadDialog = new JXLSDownloader(
		        () -> {
			        JXLSWinningSheet rs = new JXLSWinningSheet();
			        Ranking computeScoringSystem = computeScoringSystem();
			        rs.setBestLifterScoringSystem(computeScoringSystem);
			        // group may have been edited since the page was loaded
			        rs.setGroup(this.getCurrentGroup() != null ? GroupRepository.getById(this.getCurrentGroup().getId()) : null);
			        return rs;
		        },
		        "/templates/competitionResults",
		        Competition::getComputedResultsTemplateFileName,
		        Competition::setResultsTemplateFileName,
		        Translator.translate("EligibilityCategoryResults"),
		        Translator.translate("Download"));
		Button resultsButton = this.downloadDialog.createDownloadButton();
		return resultsButton;
	}

	private Button createGroupMedalsDownloadButton() {
		this.downloadDialog = new JXLSDownloader(
		        () -> {
			        JXLSMedalsSheet rs = new JXLSMedalsSheet();
			        Ranking computeScoringSystem = computeScoringSystem();
			        rs.setBestLifterScoringSystem(computeScoringSystem);
			        // group may have been edited since the page was loaded
			        rs.setGroup(this.getCurrentGroup() != null ? GroupRepository.getById(this.getCurrentGroup().getId()) : null);
			        return rs;
		        },
		        "/templates/medals",
		        Competition::getComputedMedalsTemplateFileName,
		        Competition::setMedalsTemplateFileName,
		        Translator.translate("Results.Medals"),
		        Translator.translate("Download"));
		Button resultsButton = this.downloadDialog.createDownloadButton();
		return resultsButton;
	}

	private Button createRegistrationResultsDownloadButton() {
		this.downloadDialog = new JXLSDownloader(
		        () -> {
			        JXLSResultSheet rs = new JXLSResultSheet(false);
			        Ranking computeScoringSystem = computeScoringSystem();
			        rs.setBestLifterScoringSystem(computeScoringSystem);
			        // group may have been edited since the page was loaded
			        rs.setGroup(this.getCurrentGroup() != null ? GroupRepository.getById(this.getCurrentGroup().getId()) : null);
			        return rs;
		        },
		        "/templates/protocol",
		        Competition::getComputedProtocolTemplateFileName,
		        Competition::setProtocolTemplateFileName,
		        Translator.translate("RegistrationCategoryResults"),
		        Translator.translate("Download"));
		Button resultsButton = this.downloadDialog.createDownloadButton();
		return resultsButton;
	}

	private void doSwitchGroup(Group newCurrentGroup) {
		if (newCurrentGroup != null && newCurrentGroup.getName() == "*") {
			this.setCurrentGroup(null);
		} else {
			this.setCurrentGroup(newCurrentGroup);
		}
		setGridGroup(this.getCurrentGroup());
		if (this.downloadDialog != null) {
			this.downloadDialog.createDownloadButton();
		}
		MenuBar oldMenu = this.topBarMenu;
		createTopBarGroupSelect();
		if (this.topBar != null) {
			this.topBar.replace(oldMenu, this.topBarMenu);
		}
	}

	private CrudLayout getCrudLayout(GridCrud<Athlete> crud) {
		return crud.getCrudLayout();
	}

	private Group getCurrentGroup() {
		return this.currentGroup;
	}

	private ComboBox<Ranking> getRankingSelector() {
		return this.rankingSelector;
	}

	private void highlight(Button button) {
		button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
	}

	private void resetGrid() {
		// we cannot just reset the data provider because we are changing columns.
		// brute-force way to recompute the grid layout without reloading the page.
		var g = this.getCrudGrid().getCrudLayout();
		var parent = ((Component) g).getParent().get();
		parent.getChildren().forEach(c -> c.removeFromParent());
		parent.removeFromParent();
		this.setRankingSelector(null);
		this.setGenderFilter(null);
		init();
	}

	private void setCurrentGroup(Group currentGroup) {
		// logger.debug("setCurrentGroup {} {}",currentGroup, LoggerUtils.whereFrom());
		this.currentGroup = currentGroup;
	}

	private void setScoringSystem(Ranking value) {
		this.scoringSystem = value;
	}

}
