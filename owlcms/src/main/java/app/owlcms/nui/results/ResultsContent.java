/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.NumberRenderer;
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
import app.owlcms.utils.LoggerUtils;
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
public class ResultsContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(ResultsContent.class);
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}

	public static Grid<Athlete> createResultGrid() {
		Grid<Athlete> grid = new Grid<>(Athlete.class, false);
		grid.getThemeNames().add("row-stripes");
		ThemeList themes = grid.getThemeNames();
		themes.add("compact");
		themes.add("row-stripes");

		grid.addColumn("category").setHeader(Translator.translate("Category"));

		grid.addColumn("total").setHeader(Translator.translate("Total"))
		        .setComparator(new WinningOrderComparator(Ranking.TOTAL, true));
		grid.addColumn("totalRank").setHeader(Translator.translate("TotalRank"))
		        .setComparator(new WinningOrderComparator(Ranking.TOTAL, false));

		grid.addColumn("lastName").setHeader(Translator.translate("LastName"));
		grid.addColumn("firstName").setHeader(Translator.translate("FirstName"));
		grid.addColumn("team").setHeader(Translator.translate("Team"));
		grid.addColumn("group").setHeader(Translator.translate("Group"));
		grid.addColumn("bestSnatch").setHeader(Translator.translate("Snatch"));
		grid.addColumn("snatchRank").setHeader(Translator.translate("SnatchRank"))
		        .setComparator(new WinningOrderComparator(Ranking.SNATCH, false));
		grid.addColumn("bestCleanJerk").setHeader(Translator.translate("Clean_and_Jerk"));
		grid.addColumn("cleanJerkRank").setHeader(Translator.translate("Clean_and_Jerk_Rank"))
		        .setComparator(new WinningOrderComparator(Ranking.CLEANJERK, false));

		String protocolFileName = Competition.getCurrent().getProtocolTemplateFileName();
		if (protocolFileName != null && (protocolFileName.toLowerCase().contains("fhq"))) {
			// historical
			grid.addColumn(new NumberRenderer<>(Athlete::getCategorySinclair, "%.3f", OwlcmsSession.getLocale(), "-"))
			        .setSortProperty("categorySinclair")
			        .setHeader("Cat. Sinclair")
			        .setComparator(new WinningOrderComparator(Ranking.CAT_SINCLAIR, true));
		}

		grid.addColumn(new NumberRenderer<>(Athlete::getSinclairForDelta, "%.3f", OwlcmsSession.getLocale(), "0.000"))
		        .setSortProperty("sinclair").setHeader(Translator.translate("sinclair"))
		        .setComparator(new WinningOrderComparator(Ranking.BW_SINCLAIR, true));
		// FIXME Use the selected scoring system
		grid.addColumn(new NumberRenderer<>(Athlete::getqPoints, "%.2f", OwlcmsSession.getLocale(), "0.00"))
		        .setSortProperty("qPoints").setHeader(Translator.translate("Qpoints"))
		        .setComparator(new WinningOrderComparator(Ranking.QPOINTS, true));
		grid.addColumn(new NumberRenderer<>(Athlete::getSmfForDelta, "%.3f", OwlcmsSession.getLocale(), "-"))
		        .setHeader(Translator.translate("smm"))
		        .setSortProperty("smm")
		        .setComparator(new WinningOrderComparator(Ranking.SMM, true));

		grid.addColumn(new NumberRenderer<>(Athlete::getRobi, "%.3f", OwlcmsSession.getLocale(), "-"))
		        .setSortProperty("robi")
		        .setHeader(Translator.translate("robi")).setComparator(new WinningOrderComparator(Ranking.ROBI, true));
		return grid;
	}

	private Group currentGroup;
	private JXLSDownloader downloadDialog;
	private Checkbox medalsOnly;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in
	 * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public ResultsContent() {
		defineFilters(this.getCrudGrid());
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
		Grid<Athlete> grid = createResultGrid();

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
		AthleteCrudGrid crudGrid = new AthleteCrudGrid(Athlete.class, gridLayout, crudFormFactory, grid) {

			@Override
			protected void initToolbar() {
				Component reset = createReset();
				if (reset != null) {
					this.crudLayout.addToolbarComponent(reset);
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

		defineFilters(crudGrid);

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
		Group currentGroup = getGroupFilter().getValue();
		Gender currentGender = this.getGenderFilter().getValue();

		List<Athlete> rankedAthletes = AthleteSorter.assignCategoryRanks(currentGroup);

		if (currentGroup != null) {
			rankedAthletes = AthleteSorter.displayOrderCopy(rankedAthletes).stream()
			        .filter(a -> a.getGroup() != null ? a.getGroup().equals(currentGroup) : false)
			        .filter(a -> a.getGender() != null
			                ? (currentGender != null ? currentGender.equals(a.getGender()) : true)
			                : false)
			        .collect(Collectors.toList());
		} else {
			rankedAthletes = AthleteSorter.displayOrderCopy(rankedAthletes).stream()
			        .filter(a -> a.getGender() != null
			                ? (currentGender != null ? currentGender.equals(a.getGender()) : true)
			                : false)
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

		logger.debug("parsing query parameters ResultContent");
		List<String> groupNames = params.get("group");
		if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
			String groupName = groupNames.get(0);
			if (groupName == "*") {
				// special group to show all athletes
				this.currentGroup = null;
			} else {
				if (groupName.contains("%") || groupName.contains("+")) {
					groupName = URLDecoder.decode(groupName, StandardCharsets.UTF_8);
				}
				this.currentGroup = GroupRepository.findByName(groupName);
			}
		} else {
			// if no group, we pick the first alphabetical group as a filter
			// to avoid showing hundreds of athlete at the end of each of the groups
			// (which has a noticeable impact on slower machines)
			List<Group> groups = GroupRepository.findAll();
			groups.sort(new NaturalOrderComparator<>());
			this.currentGroup = (groups.size() > 0 ? groups.get(0) : null);
		}
		if (this.currentGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(this.currentGroup.getName())));
		} else {
			// params.remove("group");
			params.put("group", Arrays.asList(URLUtils.urlEncode("*")));
		}
		doSwitchGroup(this.currentGroup);
		params.remove("fop");
		logger.debug("params {}", params);

		// change the URL to reflect group
		event.getUI().getPage().getHistory().replaceState(null,
		        new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params))));
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
		ui.getPage().getHistory().replaceState(null,
		        new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
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
			logger.trace("initial setting group to {} {}", this.currentGroup, LoggerUtils.whereFrom());
			getGroupFilter().setValue(this.currentGroup);
			// switching to group "*" is understood to mean all groups
			this.topBarMenu = new GroupSelectionMenu(groups, this.currentGroup,
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
		if (this.medalsOnly != null) {
			return;
		}

		getGroupFilter().setPlaceholder(Translator.translate("Group"));
		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<>());
		getGroupFilter().setItems(groups);
		getGroupFilter().setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		getGroupFilter().getStyle().set("display", "none");
		getGroupFilter().addValueChangeListener(e -> {
			logger.debug("updating filters: group={}", e.getValue());
			this.currentGroup = e.getValue();
			updateURLLocation(getLocationUI(), getLocation(), this.currentGroup);
			// subscribeIfLifting(e.getValue());
		});
		crud.getCrudLayout().addFilterComponent(getGroupFilter());

		this.medalsOnly = new Checkbox();
		this.medalsOnly.setLabel(Translator.translate("MedalsOnly"));
		this.medalsOnly.setValue(false);
		this.medalsOnly.addValueChangeListener(e -> {
			this.getCrudGrid().getGrid().getElement().getClassList().set("medals", Boolean.TRUE.equals(e.getValue()));
			crud.refreshGrid();
		});
		crud.getCrudLayout().addFilterComponent(this.medalsOnly);

		this.getGenderFilter().setPlaceholder(Translator.translate("Gender"));
		this.getGenderFilter().setItems(Gender.M, Gender.F);
		this.getGenderFilter().setItemLabelGenerator((i) -> {
			return i == Gender.M ? Translator.translate("Gender.Men") : Translator.translate("Gender.Women");
		});
		this.getGenderFilter().setClearButtonVisible(true);
		this.getGenderFilter().addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		this.getGenderFilter().setWidth("10em");
		crud.getCrudLayout().addFilterComponent(this.getGenderFilter());
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
			// subscribeIfLifting(currentGroup);
		} else {
			logger.debug(Translator.translate("EditingResults_logging"), this.currentGroup, liftingFop);
		}
		return liftingFop != null;
	}

	private Button createEligibilityResultsDownloadButton() {
		this.downloadDialog = new JXLSDownloader(
		        () -> {
			        JXLSResultSheet rs = new JXLSResultSheet();
			        // group may have been edited since the page was loaded
			        rs.setGroup(this.currentGroup != null ? GroupRepository.getById(this.currentGroup.getId()) : null);
			        return rs;
		        },
		        "/templates/protocol",
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
			        // group may have been edited since the page was loaded
			        rs.setGroup(this.currentGroup != null ? GroupRepository.getById(this.currentGroup.getId()) : null);
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
			        // group may have been edited since the page was loaded
			        rs.setGroup(this.currentGroup != null ? GroupRepository.getById(this.currentGroup.getId()) : null);
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
			this.currentGroup = null;
		} else {
			this.currentGroup = newCurrentGroup;
		}
		setGridGroup(this.currentGroup);
		if (this.downloadDialog != null) {
			this.downloadDialog.createDownloadButton();
		}
		MenuBar oldMenu = this.topBarMenu;
		createTopBarGroupSelect();
		if (this.topBar != null) {
			this.topBar.replace(oldMenu, this.topBarMenu);
		}
	}

	private void highlight(Button button) {
		button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
	}
}
