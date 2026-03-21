/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.LazyCrudListener;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.components.JXLSDownloader;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.team.Team;
import app.owlcms.data.team.TeamSelectionTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.IAthleteEditing;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.spreadsheet.JXLSStartingListDocs;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ResultsContent.
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@Route(value = "preparation/teams", layout = OwlcmsLayout.class)
public class TeamSelectionContent extends BaseContent
		implements OwlcmsContent, IAthleteEditing {

	// Copied from DocumentsContent to support team list report
	protected java.util.List<app.owlcms.data.athlete.Athlete> participationFindAll() {
		java.util.LinkedHashMap<String, app.owlcms.data.athlete.Athlete> athletesByKey = new java.util.LinkedHashMap<>();
		app.owlcms.data.agegroup.AgeGroupRepository.allParticipationsForAgeGroupAgeDivision(getAgeGroupPrefix(), getChampionship())
		        .forEach(participation -> {
		        	app.owlcms.data.athlete.Athlete athlete = participation.getAthlete();
		        	Long athleteId = athlete.getId();
		        	String athleteKey = athleteId != null
		        	        ? "id:" + athleteId
		        	        : String.join("|",
		        	                athlete.getLastName() != null ? athlete.getLastName() : "",
		        	                athlete.getFirstName() != null ? athlete.getFirstName() : "",
		        	                athlete.getTeam() != null ? athlete.getTeam() : "",
		        	                athlete.getFullBirthDate() != null ? athlete.getFullBirthDate().toString() : "");
		        	athletesByKey.putIfAbsent(athleteKey, athlete);
		        });
		return new java.util.ArrayList<>(athletesByKey.values());
	}

	public static final String TITLE = "TeamMembership.Title";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamSelectionContent.class);

	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}
	protected FlexLayout topBar;
	protected ComboBox<Group> topBarGroupSelect;
	// private boolean teamFilterRecusion;
	private List<Championship> adItems;
	private Championship championship;
	private String ageGroupPrefix;
	private OwlcmsCrudGrid<TeamTreeItem> crudGrid;
	private Group currentGroup;
	private Anchor finalPackage;

	// private DecimalFormat floatFormat;
	// private ComboBox<Category> categoryFilter;
	private ComboBox<Gender> genderFilter;
	private Column<TeamTreeItem> membershipColumn;
	private Column<TeamTreeItem> mixedMembershipColumn;
	private OwlcmsLayout routerLayout;
	private ComboBox<Championship> topBarChampionshipSelect;
	// private ComboBox<String> teamFilter;
	private ComboBox<String> topBarAgeGroupPrefixSelect;
	private JXLSCompetitionBook xlsWriter;

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public TeamSelectionContent() {
		OwlcmsFactory.waitDBInitialized();
	}

	@Override
	public void closeDialog() {
		this.crudGrid.getCrudLayout().hideForm();
		this.crudGrid.getGrid().asSingleSelect().clear();
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
		// Ensure an xlsWriter exists for listeners that reference it (placeholder)
		this.xlsWriter = new JXLSCompetitionBook(true, UI.getCurrent());
		// Ensure finalPackage Anchor exists for listeners that update the download attribute
		this.finalPackage = new Anchor("", "");
		this.finalPackage.getStyle().set("margin-left", "1em");

		// Use the same logic as DocumentsContent for the team list report
		String resourceDirectoryLocation = "/templates/teams";
		String title = Translator.translate("StartingList.Teams");
		JXLSDownloader teamListDownloader = new JXLSDownloader(
			() -> {
				JXLSStartingListDocs startingXlsWriter = new JXLSStartingListDocs();
				startingXlsWriter.setGroup(getGroup() != null ? GroupRepository.getById(getGroup().getId()) : null);
				startingXlsWriter.setSortedAthletes(app.owlcms.data.athleteSort.AthleteSorter.registrationOrderCopy(participationFindAll()));
				startingXlsWriter.createTeamColumns(9, 6);
				return startingXlsWriter;
			},
			resourceDirectoryLocation,
			app.owlcms.data.competition.Competition::getComputedTeamsListTemplateFileName,
			app.owlcms.data.competition.Competition::setTeamsListTemplateFileName,
			title,
			Translator.translate("Download")
		);
		Button teamListButton = teamListDownloader.createDownloadButton();
		HorizontalLayout buttons = new HorizontalLayout(teamListButton);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		this.topBar.getStyle().set("flex", "100 1");
		this.topBar.removeAll();
		// this.topBar.add(this.topBarChampionshipSelect, this.topBarAgeGroupPrefixSelect);
		this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
	this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);
	this.topBar.add(buttons);
	return this.topBar;
	}

	/**
	 * Get the content of the crudGrid. Invoked by refreshGrid. Not currently used because we are using instead a TreeGrid and a
	 * LazyCrudListener<TeamTreeItem>()
	 *
	 * @see TreeDataProvider
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	public Collection<TeamTreeItem> findAll() {
		List<TeamTreeItem> allTeams = new ArrayList<>();

		TeamSelectionTreeData teamTreeData = new TeamSelectionTreeData(getAgeGroupPrefix(), getChampionship(),
		        getGenderFilter().getValue(), Ranking.SNATCH_CJ_TOTAL, false);
		Map<Gender, List<TeamTreeItem>> teamsByGender = teamTreeData.getTeamItemsByGender();

		List<TeamTreeItem> mensTeams = teamsByGender.get(Gender.M);
		if (mensTeams != null) {
			allTeams.addAll(mensTeams);
		}
		List<TeamTreeItem> womensTeams = teamsByGender.get(Gender.F);
		if (womensTeams != null) {
			allTeams.addAll(womensTeams);
		}

		return allTeams;
	}

	public Championship getChampionship() {
		return this.championship;
	}

	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	@Override
	public OwlcmsCrudGrid<?> getEditingGrid() {
		return this.crudGrid;
	}

	public ComboBox<Gender> getGenderFilter() {
		return this.genderFilter;
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

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	public void refresh() {
		this.crudGrid.refreshGrid();
	}

	public void setChampionship(Championship championship) {
		this.championship = championship;
	}

	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
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

		logger.debug("parsing query parameters");
		List<String> groupNames = params.get("group");
		if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
			String groupName = groupNames.get(0);
			this.currentGroup = GroupRepository.findByName(groupName);
		} else {
			this.currentGroup = null;
		}
		if (this.currentGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(this.currentGroup.getName())));
		} else {
			params.remove("group");
		}
		params.remove("fop");

		// change the URL to reflect group
		Location newLocation = new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params)));
		URLUtils.replaceState(event.getUI().getPage().getHistory(),null, newLocation, getLocation());
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}

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
	protected OwlcmsCrudGrid<TeamTreeItem> createCrudGrid(OwlcmsCrudFormFactory<TeamTreeItem> crudFormFactory) {
		TreeGrid<TeamTreeItem> grid = new TreeGrid<>();
		boolean teamFlags = URLUtils.checkFlags();

		grid.addComponentHierarchyColumn((p -> {
			// null indicates that the entry is for a team, not a person
			if (p.isTeamMember() != null) {
				return new Div(p.formatName());
			}

			String team = p.getTeam().getName();
			String tag = null;
			if (teamFlags && !team.isBlank()) {
				tag = Team.getImgTag(team, "style='width:3em'");
			}
			HorizontalLayout hl = new HorizontalLayout();
			if (tag != null) {
				hl.add(new Html(tag));
			}
			hl.add(new Text(p.formatName()));
			return hl;
		})).setHeader(Translator.translate("Name")).setWidth("32ch");
		grid.addColumn(TeamTreeItem::getCategory).setHeader(Translator.translate("Category"))
		        .setTextAlign(ColumnTextAlign.CENTER);

		ComponentRenderer<Component, TeamTreeItem> warningRenderer = new ComponentRenderer<>(p -> {
			if (p.isWarning()) {
				NativeLabel label = new NativeLabel("\u26a0");
				return label;
			} else {
				return new NativeLabel();
			}
		});
		grid.addColumn(warningRenderer).setHeader(Translator.translate("Competition.TooManyPerCat"))
		        .setTextAlign(ColumnTextAlign.CENTER);

		ComponentRenderer<Component, TeamTreeItem> membershipRenderer = new ComponentRenderer<>(p -> {
			if (p.getAthlete() == null) {
				long nb = p.getTeamMembers().stream().filter(pa -> pa.isTeamMember()).count();
				Championship champ = getChampionship();
				int maxSize = champ != null ? champ.getMaxTeamSize() : Competition.getCurrent().getMaxTeamSize();
				NativeLabel label = new NativeLabel(
				        nb > maxSize ? nb + "\u26a0" : nb + "");
				p.setMembershipLabel(label);
				return label;
			} else {
				// checkbox to avoid entering in the form
				Checkbox activeBox = new Checkbox("Name");
				activeBox.setLabel(null);
				activeBox.getElement().getThemeList().set("secondary", true);
				activeBox.setValue(p.isTeamMember() != null ? p.isTeamMember() : false);
				activeBox.addValueChangeListener(click -> {
					Boolean value = click.getValue();
					activeBox.setValue(value);
					JPAService.runInTransaction(em -> toggleTeamMember(p, value, em));
				});
				// prevent grid row selection from triggering
				activeBox.getElement().addEventListener("click", ignore -> {
				}).addEventData("event.stopPropagation()");
				return activeBox;
			}
		});
		this.membershipColumn = grid.addColumn(membershipRenderer).setHeader(Translator.translate("TeamMembership.TeamMember"))
		        .setSortable(true).setTextAlign(ColumnTextAlign.CENTER);

		ComponentRenderer<Component, TeamTreeItem> mixedMembershipRenderer = new ComponentRenderer<>(p -> {
			if (!canInspectMixedMembership()) {
				return new NativeLabel();
			}
			if (p.getAthlete() == null) {
				long nb = p.getTeamMembers().stream().filter(this::isEffectiveMixedTeamMember).count();
				Championship champ = getChampionship();
				int maxSize = champ != null
				        ? (champ.getMixedBestN() != null ? champ.getMixedBestN() : champ.getMaxTeamSize())
				        : Competition.getCurrent().getMixedBestNElseDefault();
				NativeLabel label = new NativeLabel(
				        nb > maxSize ? nb + "\u26a0" : Long.toString(nb));
				p.setMixedMembershipLabel(label);
				return label;
			}

			Checkbox mixedBox = new Checkbox("Name");
			mixedBox.setLabel(null);
			mixedBox.getElement().getThemeList().set("secondary", true);
			mixedBox.setValue(isEffectiveMixedTeamMember(p));
			if (isExplicitMixedChampionshipSelected()) {
				mixedBox.addValueChangeListener(click -> {
					Boolean value = click.getValue();
					mixedBox.setValue(value);
					JPAService.runInTransaction(em -> toggleMixedTeamMember(p, value, em));
				});
			} else {
				mixedBox.setReadOnly(true);
			}
			mixedBox.getElement().addEventListener("click", ignore -> {
			}).addEventData("event.stopPropagation()");
			return mixedBox;
		});
		this.mixedMembershipColumn = grid.addColumn(mixedMembershipRenderer)
		        .setHeader(Translator.translate("TeamMembership.MixedTeamMember"))
		        .setSortable(true).setTextAlign(ColumnTextAlign.CENTER);
		updateMixedTeamUi(getChampionship(), this.genderFilter != null ? this.genderFilter.getValue() : null);

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(TeamTreeItem.class);
		OwlcmsCrudGrid<TeamTreeItem> crudGrid = new OwlcmsCrudGrid<>(TeamTreeItem.class, gridLayout,
		        crudFormFactory, grid) {
			@SuppressWarnings("deprecation")
			@Override
			public void refreshGrid() {
				if (TeamSelectionContent.this.topBar == null) {
					return;
				}
				TeamSelectionTreeData teamTreeData = new TeamSelectionTreeData(getAgeGroupPrefix(), getChampionship(),
				        TeamSelectionContent.this.genderFilter.getValue(), Ranking.SNATCH_CJ_TOTAL, false);
				this.grid.setDataProvider(new TreeDataProvider<>(teamTreeData));
			}

			@Override
			protected void initToolbar() {
			}

			@Override
			protected void updateButtonClicked() {
				TeamTreeItem item = this.grid.asSingleSelect().getValue();
				if (item.getAthlete() == null) {
					return;
				}

				TeamTreeItem domainObject = this.grid.asSingleSelect().getValue();
				showForm(CrudOperation.UPDATE, domainObject, false, this.savedMessage, event -> {
					try {
						TeamTreeItem updatedObject = this.updateOperation.perform(domainObject);
						this.grid.asSingleSelect().clear();
						refreshGrid();
						this.grid.asSingleSelect().setValue(updatedObject);
					} catch (IllegalArgumentException ignore) {
					} catch (CrudOperationException e1) {
						refreshGrid();
					} catch (Exception e2) {
						refreshGrid();
						throw e2;
					}
				});
			}

			@Override
			protected void updateButtons() {
			}
		};

		defineFilters(crudGrid);
		defineContent(crudGrid);
		crudGrid.setClickRowToUpdate(true);
		crudGrid.setWidth("100ch");
		return crudGrid;
	}

	/**
	 * We do not control the groups on other screens/displays
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	protected void defineFilters(OwlcmsCrudGrid<TeamTreeItem> crudGrid2) {

		this.topBarAgeGroupPrefixSelect = new ComboBox<>();
		this.topBarAgeGroupPrefixSelect.setPlaceholder(Translator.translate("AgeGroup"));
		this.topBarAgeGroupPrefixSelect.setEnabled(false);
		this.topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		this.topBarAgeGroupPrefixSelect.setValue(null);
		this.topBarAgeGroupPrefixSelect.setWidth("15em");
		this.topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		this.topBarAgeGroupPrefixSelect.getStyle().set("margin-left", "1em");
		setAgeGroupPrefixSelectionListener();

		this.topBarChampionshipSelect = new ComboBox<>();
		this.topBarChampionshipSelect.setPlaceholder(Translator.translate("Championship"));
		this.adItems = Championship.findAllUsed(true);
		this.topBarChampionshipSelect.setItems(this.adItems);
		this.topBarChampionshipSelect.setItemLabelGenerator((ad) -> ad.getName());
		this.topBarChampionshipSelect.setClearButtonVisible(true);
		this.topBarChampionshipSelect.setWidth("15em");
		this.topBarChampionshipSelect.getStyle().set("margin-left", "1em");
		setChampionshipSelectionListener();

		if (this.genderFilter == null) {
			this.genderFilter = new ComboBox<>();
			this.genderFilter.setPlaceholder(Translator.translate("Gender"));
			updateGenderFilterOptions(getChampionship());
			this.genderFilter.setItemLabelGenerator((i) -> {
				return i.asGenderName();
			});
			this.genderFilter.setClearButtonVisible(true);
			this.genderFilter.addValueChangeListener(e -> {
				updateMembershipColumnVisibility(e.getValue());
				updateMixedTeamUi(getChampionship(), e.getValue());
				crudGrid2.refreshGrid();
			});
			this.genderFilter.setWidth("10em");
		}
		updateMembershipColumnVisibility(this.genderFilter.getValue());
		updateMixedTeamUi(getChampionship(), this.genderFilter.getValue());

		crudGrid2.getCrudLayout().addFilterComponent(this.topBarChampionshipSelect);
		crudGrid2.getCrudLayout().addFilterComponent(this.topBarAgeGroupPrefixSelect);
		crudGrid2.getCrudLayout().addFilterComponent(this.genderFilter);
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsCrudFormFactory<TeamTreeItem> crudFormFactory = new TeamItemSelectionFormFactory(TeamTreeItem.class,
		        this);
		this.crudGrid = createCrudGrid(crudFormFactory);
		fillHW(this.crudGrid, this);
		Championship value = (this.adItems != null && this.adItems.size() > 0) ? this.adItems.get(0) : null;
		setChampionship(value);
		this.topBarChampionshipSelect.setValue(value);
	}

	private int countTeamMembers(List<TeamTreeItem> teamMembers) {
		return (int) teamMembers.stream().filter(m -> m.isTeamMember()).count();
	}

	private int countMixedTeamMembers(List<TeamTreeItem> teamMembers) {
		return (int) teamMembers.stream().filter(this::isEffectiveMixedTeamMember).count();
	}

	private void defineContent(OwlcmsCrudGrid<TeamTreeItem> crudGrid) {
		crudGrid.setCrudListener(new LazyCrudListener<TeamTreeItem>() {
			@Override
			public TeamTreeItem add(TeamTreeItem user) {
				AthleteRepository.save(user.getAthlete());
				return user;
			}

			@Override
			public void delete(TeamTreeItem user) {
				AthleteRepository.delete(user.getAthlete());
			}

			@Override
			public DataProvider<TeamTreeItem, ?> getDataProvider() {
				return new TreeDataProvider<>(
				        new TeamSelectionTreeData(getAgeGroupPrefix(), getChampionship(), getGenderFilter().getValue(),
				                Ranking.SNATCH_CJ_TOTAL, false));
			}

			@Override
			public TeamTreeItem update(TeamTreeItem user) {
				AthleteRepository.save(user.getAthlete());
				return user;
			}
		});
	}

	private void setChampionshipSelectionListener() {
		this.topBarChampionshipSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the download button.
			Championship championshipValue = e.getValue();
			setChampionship(championshipValue);
			updateGenderFilterOptions(championshipValue);
			updateMixedTeamUi(championshipValue, this.genderFilter != null ? this.genderFilter.getValue() : null);
			updateMembershipColumnVisibility(this.genderFilter != null ? this.genderFilter.getValue() : null);
			// logger.debug("championshipSelectionListener {}", championshipValue);
			if (championshipValue == null) {
				this.topBarAgeGroupPrefixSelect.setValue(null);
				this.topBarAgeGroupPrefixSelect.setItems(new ArrayList<>());
				this.topBarAgeGroupPrefixSelect.setEnabled(false);
				this.topBarAgeGroupPrefixSelect.setValue(null);
				this.crudGrid.refreshGrid();
				return;
			}

			List<String> championshipAgeGroupPrefixes;
			championshipAgeGroupPrefixes = AgeGroupRepository.findActiveAndUsedAgeGroupNames(championshipValue);

			this.topBarAgeGroupPrefixSelect.setItems(championshipAgeGroupPrefixes);
			boolean notEmpty = championshipAgeGroupPrefixes.size() > 0;
			this.topBarAgeGroupPrefixSelect.setEnabled(notEmpty);
			String first = (notEmpty && championshipValue.getType().isIWF()) ? championshipAgeGroupPrefixes.get(0)
			        : null;
			// logger.debug("championship {} ag {} first {} select {}", championshipValue,
			// championshipAgeGroupPrefixes, first,
			// topBarAgeGroupPrefixSelect);

			this.xlsWriter.setChampionship(championshipValue);
			this.finalPackage.getElement().setAttribute("download",
			        "results" + (getChampionship() != null ? "_" + getChampionship().getName()
			                : (this.ageGroupPrefix != null ? "_" + this.ageGroupPrefix : "_all")) + ".xls");

			String value = notEmpty ? first : null;
			// logger.debug("setting prefix to {}", value);
			this.topBarAgeGroupPrefixSelect.setValue(value);
			updateFilters();

			if (this.crudGrid != null && value == null) {
				// if prefix is already null, does not refresh. Force it.
				this.crudGrid.refreshGrid();
			}

		});
	}

	private void setAgeGroupPrefixSelectionListener() {
		this.topBarAgeGroupPrefixSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the download button.
			String prefix = e.getValue();
			setAgeGroupPrefix(prefix);

			// logger.debug("ageGroupPrefixSelectionListener {}",prefix);
			// updateFilters(getChampionship(), getAgeGroupPrefix());
			this.xlsWriter.setAgeGroupPrefix(this.ageGroupPrefix);
			this.finalPackage.getElement().setAttribute("download",
			        "results" + (getChampionship() != null ? "_" + getChampionship().getName()
			                : (this.ageGroupPrefix != null ? "_" + this.ageGroupPrefix : "_all")) + ".xls");

			if (this.crudGrid != null) {
				this.crudGrid.refreshGrid();
			}

		});
	}

	private Object toggleTeamMember(TeamTreeItem tti, Boolean value, EntityManager em) {
		logger.info("{} {} as team member for category {}", value ? "setting" : "removing",
		        tti.getAthlete().getShortName(), tti.getAthlete().getCategory().getNameWithAgeGroup());
		Participation _getOriginalParticipation = ((PAthlete) tti.getAthlete())._getOriginalParticipation();
		boolean member = Boolean.TRUE.equals(value);
		_getOriginalParticipation.setTeamMember(member);
		tti.setTeamMember(member);
		em.merge(_getOriginalParticipation);
		TeamTreeItem parent = tti.getParent();
		List<TeamTreeItem> teamMembers = tti.getParent().getTeamMembers();
		if (this.crudGrid != null) {
			this.crudGrid.refreshGrid();
			return null;
		}
		parent.getMembershipLabel().setText("" + (teamMembers != null ? countTeamMembers(teamMembers) : 0));
		return null;
	}

	private Object toggleMixedTeamMember(TeamTreeItem tti, Boolean value, EntityManager em) {
		logger.info("{} {} as mixed team member for category {}", value ? "setting" : "removing",
		        tti.getAthlete().getShortName(), tti.getAthlete().getCategory().getNameWithAgeGroup());
		Participation originalParticipation = ((PAthlete) tti.getAthlete())._getOriginalParticipation();
		boolean member = Boolean.TRUE.equals(value);
		originalParticipation.setMixedTeamMember(member);
		tti.setMixedTeamMember(member);
		em.merge(originalParticipation);
		TeamTreeItem parent = tti.getParent();
		List<TeamTreeItem> teamMembers = tti.getParent().getTeamMembers();
		if (this.crudGrid != null) {
			this.crudGrid.refreshGrid();
			return null;
		}
		if (parent.getMixedMembershipLabel() != null) {
			parent.getMixedMembershipLabel().setText("" + (teamMembers != null ? countMixedTeamMembers(teamMembers) : 0));
		}
		return null;
	}

	private boolean canInspectMixedMembership() {
		return getChampionship() != null;
	}

	private boolean isExplicitMixedChampionshipSelected() {
		Championship championship = getChampionship();
		return championship != null && championship.isExplicitMixedTeamMembers();
	}

	private boolean isEffectiveMixedTeamMember(TeamTreeItem item) {
		if (item == null || item.getAthlete() == null) {
			return false;
		}
		return isExplicitMixedChampionshipSelected()
		        ? Boolean.TRUE.equals(item.isMixedTeamMember())
		        : Boolean.TRUE.equals(item.isTeamMember());
	}

	private void updateGenderFilterOptions(Championship championship) {
		if (this.genderFilter == null) {
			return;
		}
		if (championship != null) {
			this.genderFilter.setItems(Gender.M, Gender.F, Gender.MF);
		} else {
			this.genderFilter.setItems(Gender.M, Gender.F);
			if (this.genderFilter.getValue() == Gender.MF) {
				this.genderFilter.setValue(null);
			}
		}
	}

	private void updateMixedTeamUi(Championship championship, Gender selectedGender) {
		if (this.mixedMembershipColumn != null) {
			this.mixedMembershipColumn.setVisible(championship != null && selectedGender == Gender.MF);
		}
	}

	private void updateMembershipColumnVisibility(Gender selectedGender) {
		if (this.membershipColumn != null) {
			this.membershipColumn.setVisible(selectedGender != Gender.MF);
		}
	}

	private void updateFilters() {
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

}

