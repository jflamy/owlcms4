/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.results;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.crud.CrudOperationException;
import org.vaadin.crudui.crud.LazyCrudListener;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.team.TeamResultsTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.IAthleteEditing;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireLogin;
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ResultsContent.
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@Route(value = "results/teamresults", layout = OwlcmsLayout.class)
public class TeamResultsContent extends VerticalLayout
        implements OwlcmsContent, RequireLogin, IAthleteEditing {

	static final String TITLE = "TeamResults.Title";
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamResultsContent.class);

	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
	}

	protected FlexLayout topBar;

	protected ComboBox<Group> topBarGroupSelect;
	// private boolean teamFilterRecusion;
	private List<AgeDivision> adItems;

	private AgeDivision ageDivision;

	private String ageGroupPrefix;
	private OwlcmsCrudGrid<TeamTreeItem> crudGrid;
	private Group currentGroup;
	private Button download;

	private Anchor finalPackage;

	private DecimalFormat floatFormat;
	// private ComboBox<Category> categoryFilter;
	private ComboBox<Gender> genderFilter;
	private Location location;

	private UI locationUI;
	private OwlcmsLayout routerLayout;
	private ComboBox<AgeDivision> topBarAgeDivisionSelect;
	// private ComboBox<String> teamFilter;
	private ComboBox<String> topBarAgeGroupPrefixSelect;
	private JXLSCompetitionBook xlsWriter;

	/**
	 * Instantiates a new announcer content. Does nothing. Content is created in
	 * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public TeamResultsContent() {
		super();
		OwlcmsFactory.waitDBInitialized();
	}

	@Override
	public void closeDialog() {
		crudGrid.getCrudLayout().hideForm();
		crudGrid.getGrid().asSingleSelect().clear();
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
		xlsWriter = new JXLSCompetitionBook(true, UI.getCurrent());
		StreamResource href = new StreamResource(TITLE + "Report" + ".xls", xlsWriter);
		finalPackage = new Anchor(href, "");
		finalPackage.getStyle().set("margin-left", "1em");
		download = new Button(getTranslation(TITLE + ".Report"), new Icon(VaadinIcon.DOWNLOAD_ALT));

		topBarAgeGroupPrefixSelect = new ComboBox<>();
		topBarAgeGroupPrefixSelect.setPlaceholder(getTranslation("AgeGroup"));

		topBarAgeGroupPrefixSelect.setEnabled(false);
		topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		topBarAgeGroupPrefixSelect.setValue(null);
		topBarAgeGroupPrefixSelect.setWidth("8em");
		topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
		topBarAgeGroupPrefixSelect.getStyle().set("margin-left", "1em");
		setAgeGroupPrefixSelectionListener();

		topBarAgeDivisionSelect = new ComboBox<>();
		topBarAgeDivisionSelect.setPlaceholder(getTranslation("AgeDivision"));
		adItems = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
		topBarAgeDivisionSelect.setItems(adItems);
		topBarAgeDivisionSelect.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
		topBarAgeDivisionSelect.setClearButtonVisible(true);
		topBarAgeDivisionSelect.setWidth("8em");
		topBarAgeDivisionSelect.getStyle().set("margin-left", "1em");
		setAgeDivisionSelectionListener();

		finalPackage.add(download);
		HorizontalLayout buttons = new HorizontalLayout(finalPackage);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar.getStyle().set("flex", "100 1");
		topBar.removeAll();
		topBar.add(topBarAgeDivisionSelect, topBarAgeGroupPrefixSelect);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
		return topBar;
	}

	/**
	 * Get the content of the crudGrid. Invoked by refreshGrid. Not currently used
	 * because we are using instead a TreeGrid and a
	 * LazyCrudListener<TeamTreeItem>()
	 *
	 * @see TreeDataProvider
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	public Collection<TeamTreeItem> findAll() {
		List<TeamTreeItem> allTeams = new ArrayList<>();

		TeamResultsTreeData teamResultsTreeData = new TeamResultsTreeData(getAgeGroupPrefix(), getAgeDivision(),
		        getGenderFilter().getValue(), Ranking.SNATCH_CJ_TOTAL, false);
		Map<Gender, List<TeamTreeItem>> teamsByGender = teamResultsTreeData.getTeamItemsByGender();

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

	public AgeDivision getAgeDivision() {
		return ageDivision;
	}

	public String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	@Override
	public OwlcmsCrudGrid<?> getEditingGrid() {
		return crudGrid;
	}

	public ComboBox<Gender> getGenderFilter() {
		return genderFilter;
	}

	public Location getLocation() {
		return location;
	}

	public UI getLocationUI() {
		return locationUI;
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
		return getTranslation(TITLE);
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return routerLayout;
	}

	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	public void refresh() {
		crudGrid.refreshGrid();
	}

	public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

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
			currentGroup = GroupRepository.findByName(groupName);
		} else {
			currentGroup = null;
		}
		if (currentGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(currentGroup.getName())));
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
				        new TeamResultsTreeData(getAgeGroupPrefix(), getAgeDivision(), getGenderFilter().getValue(),
				                Ranking.SNATCH_CJ_TOTAL, false));
			}

			@Override
			public TeamTreeItem update(TeamTreeItem user) {
				AthleteRepository.save(user.getAthlete());
				return user;
			}
		});
	}

	private String formatDouble(double d, int decimals) {
		if (floatFormat == null) {
			floatFormat = new DecimalFormat();
			floatFormat.setMinimumIntegerDigits(1);
			floatFormat.setMaximumFractionDigits(decimals);
			floatFormat.setMinimumFractionDigits(decimals);
			floatFormat.setGroupingUsed(false);
		}
		return floatFormat.format(d);
	}

	private void setAgeDivisionSelectionListener() {
		topBarAgeDivisionSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the download button.
			AgeDivision ageDivisionValue = e.getValue();
			setAgeDivision(ageDivisionValue);
			// logger.debug("ageDivisionSelectionListener {}",ageDivisionValue);
			if (ageDivisionValue == null) {
				topBarAgeGroupPrefixSelect.setValue(null);
				topBarAgeGroupPrefixSelect.setItems(new ArrayList<String>());
				topBarAgeGroupPrefixSelect.setEnabled(false);
				topBarAgeGroupPrefixSelect.setValue(null);
				crudGrid.refreshGrid();
				return;
			}

			List<String> ageDivisionAgeGroupPrefixes;
			ageDivisionAgeGroupPrefixes = AgeGroupRepository.findActiveAndUsed(ageDivisionValue);

			topBarAgeGroupPrefixSelect.setItems(ageDivisionAgeGroupPrefixes);
			boolean notEmpty = ageDivisionAgeGroupPrefixes.size() > 0;
			topBarAgeGroupPrefixSelect.setEnabled(notEmpty);
			String first = (notEmpty && ageDivisionValue == AgeDivision.IWF) ? ageDivisionAgeGroupPrefixes.get(0)
			        : null;
			// logger.debug("ad {} ag {} first {} select {}", ageDivisionValue,
			// ageDivisionAgeGroupPrefixes, first,
			// topBarAgeGroupPrefixSelect);

			xlsWriter.setAgeDivision(ageDivisionValue);
			finalPackage.getElement().setAttribute("download",
			        "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
			                : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");

			String value = notEmpty ? first : null;
			// logger.debug("setting prefix to {}", value);
			topBarAgeGroupPrefixSelect.setValue(value);
			updateFilters(ageDivisionValue, first);

			if (crudGrid != null && value == null) {
				// if prefix is already null, does not refresh. Force it.
				crudGrid.refreshGrid();
			}

		});
	}

	private void setAgeGroupPrefixSelectionListener() {
		topBarAgeGroupPrefixSelect.addValueChangeListener(e -> {
			// the name of the resulting file is set as an attribute on the <a href tag that
			// surrounds the download button.
			String prefix = e.getValue();
			setAgeGroupPrefix(prefix);

			// logger.debug("ageGroupPrefixSelectionListener {}",prefix);
			// updateFilters(getAgeDivision(), getAgeGroupPrefix());
			xlsWriter.setAgeGroupPrefix(ageGroupPrefix);
			finalPackage.getElement().setAttribute("download",
			        "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
			                : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");

			if (crudGrid != null) {
				crudGrid.refreshGrid();
			}

		});
	}

	private void updateFilters(AgeDivision ageDivision2, String ageGroupPrefix2) {
//        List<Category> categories = CategoryRepository.findByGenderDivisionAgeBW(genderFilter.getValue(),
//                getAgeDivision(), null, null);
//        if (getAgeGroupPrefix() != null && !getAgeGroupPrefix().isBlank()) {
//            categories = categories.stream().filter((c) -> c.getAgeGroup().getCode().equals(getAgeGroupPrefix()))
//                    .collect(Collectors.toList());
//        }
//         logger.trace("updateFilters {}, {}, {}", ageDivision2, ageGroupPrefix2, categories);
//        categoryFilter.setItems(categories);
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
		grid.addHierarchyColumn(TeamTreeItem::formatName).setHeader(Translator.translate("Name"));
		grid.addColumn(TeamTreeItem::getGender).setHeader(Translator.translate("Gender"))
		        .setTextAlign(ColumnTextAlign.END);
		grid.addColumn(TeamTreeItem::getCategory).setHeader(Translator.translate("Category"))
		        .setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(TeamTreeItem::getPoints, "points").setHeader(Translator.translate("TeamResults.Points"))
		        .setTextAlign(ColumnTextAlign.END);
		grid.addColumn(t -> formatDouble(t.getSinclairScore(), 3), "sinclairScore")
		        .setHeader(Translator.translate("Scoreboard.Sinclair"))
		        .setTextAlign(ColumnTextAlign.END);
		grid.addColumn(t -> formatDouble(t.getSmfScore(), 3), "smfScore")
		        .setHeader(Translator.translate("smm"))
		        .setTextAlign(ColumnTextAlign.END);
		grid.addColumn(TeamTreeItem::formatProgress).setHeader(Translator.translate("TeamResults.Status"))
		        .setTextAlign(ColumnTextAlign.END);

		OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(TeamTreeItem.class);
		OwlcmsCrudGrid<TeamTreeItem> crudGrid = new OwlcmsCrudGrid<>(TeamTreeItem.class, gridLayout,
		        crudFormFactory, grid) {
			@SuppressWarnings("deprecation")
			@Override
			public void refreshGrid() {
				if (topBar == null) {
					return;
				}
				// logger.debug("refreshing grid {} {} {}",getAgeGroupPrefix(),
				// getAgeDivision(),
				// genderFilter.getValue());
				TeamResultsTreeData teamResultsTreeData = new TeamResultsTreeData(getAgeGroupPrefix(), getAgeDivision(),
				        genderFilter.getValue(), Ranking.SNATCH_CJ_TOTAL, false);
				grid.setDataProvider(new TreeDataProvider<>(teamResultsTreeData));
			}

			@Override
			protected void initToolbar() {
			}

			@Override
			protected void updateButtonClicked() {
				TeamTreeItem item = grid.asSingleSelect().getValue();
				if (item.getAthlete() == null) {
					return;
				}

				TeamTreeItem domainObject = grid.asSingleSelect().getValue();
				showForm(CrudOperation.UPDATE, domainObject, false, savedMessage, event -> {
					try {
						TeamTreeItem updatedObject = updateOperation.perform(domainObject);
						grid.asSingleSelect().clear();
						refreshGrid();
						grid.asSingleSelect().setValue(updatedObject);
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
		return crudGrid;
	}

	/**
	 * We do not control the groups on other screens/displays
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	protected void defineFilters(OwlcmsCrudGrid<TeamTreeItem> crudGrid2) {
//        if (teamFilter == null) {
//            teamFilter = new ComboBox<>();
//            teamFilter.setPlaceholder(getTranslation("Team"));
//            teamFilter.setClearButtonVisible(true);
//            teamFilter.addValueChangeListener(e -> {
//                if (!teamFilterRecusion) return;
//                crudGrid2.refreshGrid();
//            });
//            teamFilter.setWidth("10em");
//        }
//        crudGrid2.getCrudLayout().addFilterComponent(teamFilter);

		if (genderFilter == null) {
			genderFilter = new ComboBox<>();
			genderFilter.setPlaceholder(getTranslation("Gender"));
			genderFilter.setItems(Gender.M, Gender.F);
			genderFilter.setItemLabelGenerator((i) -> {
				return i == Gender.M ? getTranslation("Gender.Men") : getTranslation("Gender.Women");
			});
			genderFilter.setClearButtonVisible(true);
			genderFilter.addValueChangeListener(e -> {
				crudGrid2.refreshGrid();
			});
			genderFilter.setWidth("10em");
		}
		crudGrid2.getCrudLayout().addFilterComponent(genderFilter);

//        if (categoryFilter == null) {
//            categoryFilter = new ComboBox<>();
//            categoryFilter.setClearButtonVisible(true);
//            categoryFilter.setPlaceholder(getTranslation("Category"));
//            categoryFilter.setClearButtonVisible(true);
//            categoryFilter.addValueChangeListener(e -> {
//                crudGrid2.refreshGrid();
//            });
//            categoryFilter.setWidth("10em");
//        }
//        crudGrid2.getCrudLayout().addFilterComponent(categoryFilter);
	}

	/**
	 * We do not connect to the event bus, and we do not track a field of play
	 * (non-Javadoc)
	 *
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsCrudFormFactory<TeamTreeItem> crudFormFactory = new TeamItemResultsFormFactory(TeamTreeItem.class, this);
		crudGrid = createCrudGrid(crudFormFactory);
		fillHW(crudGrid, this);
		AgeDivision value = (adItems != null && adItems.size() > 0) ? adItems.get(0) : null;
		setAgeDivision(value);
		topBarAgeDivisionSelect.setValue(value);
	}

}
