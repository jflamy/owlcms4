/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.results;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.components.DownloadDialog;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
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
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.spreadsheet.JXLSResultSheet;
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
public class PackageContent extends AthleteGridContent implements HasDynamicTitle {

    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    final private static Logger logger = (Logger) LoggerFactory.getLogger(PackageContent.class);
    private static final String TITLE = "Results.EndOfCompetition";
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
    }

    protected ComboBox<AgeDivision> topBarAgeDivisionSelect;
    protected ComboBox<String> topBarAgeGroupPrefixSelect;
    private AgeDivision ageDivision;
    private String ageGroupPrefix;

//    private Button catDownloadButton;

    private ComboBox<Category> categoryFilter;
    private Category categoryValue;
//    private Anchor catResultsAnchor;

//    private JXLSCatResults catXlsWriter = new JXLSCatResults(UI.getCurrent());
    private Group currentGroup;
    private DownloadDialog downloadDialog;
//    private JXLSCompetitionBook xlsWriter;
    private List<AgeDivision> adItems;

    /**
     * Instantiates a new announcer content. Does nothing. Content is created in
     * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
     */
    public PackageContent() {
        super();
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

        topBar = new FlexLayout();
//        StreamResource hrefC = new StreamResource("catResults.xls", catXlsWriter);
//        catResultsAnchor = new Anchor(hrefC, "");
//        catResultsAnchor.getStyle().set("margin-left", "1em");
//        catDownloadButton = new Button(getTranslation(TITLE), new Icon(VaadinIcon.DOWNLOAD_ALT));
//        catResultsAnchor.add(catDownloadButton);

        Button finalPackageDownloadButton = createFinalPackageDownloadButton();
        Button categoryResultsDownloadButton = createCategoryResultsDownloadButton();

        HorizontalLayout buttons = new HorizontalLayout(finalPackageDownloadButton, categoryResultsDownloadButton);
        buttons.getStyle().set("margin-left", "5em");
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
        buttons.setPadding(false);
        buttons.setMargin(false);
        buttons.setSpacing(true);

        topBar.getStyle().set("flex", "100 1");
        topBar.removeAll();
        topBar.add(buttons);
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
    public Collection<Athlete> findAll() {
        Competition competition = Competition.getCurrent();
        HashMap<String, Object> beans = competition.computeReportingInfo(ageGroupPrefix, ageDivision);

        // String suffix = (getAgeGroupPrefix() != null) ? getAgeGroupPrefix() : getAgeDivision().name();
        // String key = "mwTot"+suffix;
        // List<Athlete> ranked = AthleteSorter.resultsOrderCopy(athletes, Ranking.TOTAL, false);

        String key = "mwTot";
        @SuppressWarnings("unchecked")
        List<Athlete> ranked = (List<Athlete>) beans.get(key);
        if (ranked == null || ranked.isEmpty()) {
            return new ArrayList<>();
        }
        Category catFilterValue = getCategoryValue();
        Stream<Athlete> stream = ranked.stream()
                .filter(a -> {
                    Gender genderFilterValue = genderFilter != null ? genderFilter.getValue() : null;
                    Gender athleteGender = a.getGender();
                    boolean catOk = (catFilterValue == null
                            || catFilterValue.toString().equals(a.getCategory().toString()))
                            && (genderFilterValue == null || genderFilterValue == athleteGender);
                    // logger.debug("filter {} : {} {} {} | {} {}", catOk, catFilterValue, a.getCategory(),
                    // genderFilterValue, athleteGender);
                    return catOk;
                });
        List<Athlete> found = stream.collect(Collectors.toList());

//        if (topBar != null) {
////            computeAnchors();
//            catXlsWriter.setSortedAthletes(found);
//        }
        updateURLLocations();
        return found;
    }

    public Group getGridGroup() {
        return getGroupFilter().getValue();
    }

    /**
     * @see app.owlcms.apputils.queryparameters.FOPParameters#getLocation()
     */
    @Override
    public Location getLocation() {
        return this.location;
    }

    /**
     * @see app.owlcms.apputils.queryparameters.FOPParameters#getLocationUI()
     */
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
        return getTranslation(TITLE);
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
        } catch (Exception e) {
            setAgeDivision(null);
        }
        // remove if now null
        String value = getAgeDivision() != null ? getAgeDivision().name() : null;
        updateParam(params1, "ad", value);

        List<String> ageGroupParams = params1.get("ag");
        // no age group is the default
        String ageGroupPrefix = (ageGroupParams != null && !ageGroupParams.isEmpty() ? ageGroupParams.get(0) : null);
        setAgeGroupPrefix(ageGroupPrefix);
        String value2 = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
        updateParam(params1, "ag", value2);

        List<String> catParams = params1.get("cat");
        String catParam = (catParams != null && !catParams.isEmpty() ? catParams.get(0) : null);
        catParam = catParam != null ? URLDecoder.decode(catParam, StandardCharsets.UTF_8) : null;
        setCategoryValue(CategoryRepository.findByCode(catParam));
        String catValue = getCategoryValue() != null ? getCategoryValue().toString() : null;
        updateParam(params1, "cat", catValue);

        // logger.debug("params {}", params1);
        return params1;
    }

    public void refresh() {
        crudGrid.refreshGrid();
    }

    public void setCategoryValue(Category category) {
        this.categoryValue = category;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    /*
     * Process query parameters
     *
     * Note: what Vaadin calls a parameter is in the REST style, actually part of the URL path. We use the old-style
     * Query parameters for our purposes.
     *
     * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
     * java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String unused) {
        Location location = event.getLocation();
        setLocation(location);
        setLocationUI(event.getUI());

        // the OptionalParameter string is the part of the URL path that can be interpreted as REST arguments
        // we use the ? query parameters instead.
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> params = readParams(location, parametersMap);

        event.getUI().getPage().getHistory().replaceState(null,
                new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
    }

    @Override
    public void setShowInitialDialog(boolean b) {
        return;
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
        ui.getPage().getHistory().replaceState(null,
                new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
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
        Grid<Athlete> grid = ResultsContent.createResultGrid();

        OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
        AthleteCrudGrid crudGrid = new AthleteCrudGrid(Athlete.class, gridLayout, crudFormFactory, grid) {
            @Override
            protected void initToolbar() {
                Component reset = createReset();
                if (reset != null) {
                    crudLayout.addToolbarComponent(reset);
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
     * @see app.owlcms.nui.shared.AthleteGridContent#createReset()
     */
    @Override
    protected Component createReset() {
        reset = new Button(getTranslation("RecomputeRanks"), IronIcons.REFRESH.create(),
                (e) -> OwlcmsSession.withFop((fop) -> {
                    AthleteRepository.assignCategoryRanks();
                    refresh();
                }));

        reset.getElement().setAttribute("title", getTranslation("RecomputeRanks"));
        reset.getElement().setAttribute("theme", "secondary contrast small icon");
        return reset;
    }

    @Override
    protected void defineFilters(GridCrud<Athlete> crud) {
        //logger.debug("defineFilters from {}",LoggerUtils.whereFrom());
        
        if (topBarAgeDivisionSelect == null) {
            topBarAgeDivisionSelect = new ComboBox<>();
            topBarAgeDivisionSelect.setPlaceholder(getTranslation("AgeDivision"));
            topBarAgeDivisionSelect.setWidth("25ch");
            adItems = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
            topBarAgeDivisionSelect.setItems(adItems);
            topBarAgeDivisionSelect.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
            topBarAgeDivisionSelect.setClearButtonVisible(true);
            topBarAgeDivisionSelect.getStyle().set("margin-left", "1em");
            setAgeDivisionSelectionListener();
            //logger.debug("adItems {}",adItems);
        }
        
        if (topBarAgeGroupPrefixSelect == null) {
            topBarAgeGroupPrefixSelect = new ComboBox<>();
            topBarAgeGroupPrefixSelect.setPlaceholder(getTranslation("AgeGroup"));
            topBarAgeGroupPrefixSelect.setEnabled(false);
            topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
            topBarAgeGroupPrefixSelect.setValue(null);
            topBarAgeGroupPrefixSelect.setWidth("20ch");
            topBarAgeGroupPrefixSelect.setClearButtonVisible(true);
            topBarAgeGroupPrefixSelect.getStyle().set("margin-left", "1em");
            setAgeGroupPrefixSelectionListener(); 
        }

        crud.getCrudLayout().addFilterComponent(topBarAgeDivisionSelect);
        crud.getCrudLayout().addFilterComponent(topBarAgeGroupPrefixSelect);

        if (categoryFilter == null) {
            categoryFilter = new ComboBox<>();
            categoryFilter.setClearButtonVisible(true);
            categoryFilter.setPlaceholder(getTranslation("Category"));
            categoryFilter.setClearButtonVisible(true);
            categoryFilter.setValue(getCategoryValue());
            categoryFilter.addValueChangeListener(e -> {
                // logger.debug("categoryFilter set {}", e.getValue());
                setCategoryValue(e.getValue());
                crud.refreshGrid();
            });
            categoryFilter.setWidth("10em");
        }

        crud.getCrudLayout().addFilterComponent(categoryFilter);

        // hidden group filter
        getGroupFilter().setVisible(false);

        genderFilter.setPlaceholder(getTranslation("Gender"));
        genderFilter.setItems(Gender.M, Gender.F);
        genderFilter.setItemLabelGenerator((i) -> {
            return i == Gender.M ? getTranslation("Gender.M") : getTranslation("Gender.F");
        });
        genderFilter.setClearButtonVisible(true);
        genderFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        genderFilter.setWidth("10em");
        crud.getCrudLayout().addFilterComponent(genderFilter);
        
        if (adItems != null && adItems.size() == 1) {
            setAgeDivision(adItems.get(0));
            topBarAgeDivisionSelect.setValue(adItems.get(0));
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

    protected void setAgeDivisionSelectionListener() {
        topBarAgeDivisionSelect.addValueChangeListener(e -> {
            //logger.debug("topBarAgeDivisionSelect {}",e.getValue());
            // the name of the resulting file is set as an attribute on the <a href tag that
            // surrounds the packageDownloadButton button.
            AgeDivision ageDivisionValue = e.getValue();
            setAgeDivision(ageDivisionValue);
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
            //logger.debug("ageDivisionAgeGroupPrefixes {}",ageDivisionAgeGroupPrefixes);
            topBarAgeGroupPrefixSelect.setEnabled(notEmpty);
            String first = (notEmpty && ageDivisionValue == AgeDivision.IWF) || (ageDivisionAgeGroupPrefixes.size() == 1)? ageDivisionAgeGroupPrefixes.get(0)
                    : null;

            String ageGroupPrefix2 = getAgeGroupPrefix();
            if (ageDivisionAgeGroupPrefixes.contains(ageGroupPrefix2)) {
                // prefix is valid
                topBarAgeGroupPrefixSelect.setValue(ageGroupPrefix2);
            } else {
                // this will trigger other changes and eventually, refresh the grid
                String value = notEmpty ? first : null;
                topBarAgeGroupPrefixSelect.setValue(value);
                if (value == null) {
                    doAgeGroupPrefixRefresh(null);
                }
            }
        });
    }

    protected void setAgeGroupPrefixSelectionListener() {
        topBarAgeGroupPrefixSelect.addValueChangeListener(e -> {
            // the name of the resulting file is set as an attribute on the <a href tag that
            // surrounds
            // the packageDownloadButton button.
            doAgeGroupPrefixRefresh(e.getValue());
        });
    }

    protected void updateURLLocations() {

        updateURLLocation(UI.getCurrent(), getLocation(), "fop", null);
        String ag = getAgeGroupPrefix() != null ? getAgeGroupPrefix() : null;
        updateURLLocation(UI.getCurrent(), getLocation(), "ag",
                ag);
        String ad = getAgeDivision() != null ? getAgeDivision().name() : null;
        updateURLLocation(UI.getCurrent(), getLocation(), "ad",
                ad);
        String cat = getCategoryValue() != null ? getCategoryValue().getComputedCode() : null;
        updateURLLocation(UI.getCurrent(), getLocation(), "cat",
                cat);
        // logger.debug("update URL {} {} {}",ag,ad,cat);
    }

    /**
     * @return true if the current group is safe for editing -- i.e. not lifting currently
     */
    private boolean checkFOP() {
        Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
        FieldOfPlay liftingFop = null;
        search: for (FieldOfPlay fop : fops) {
            if (fop.getGroup() != null && fop.getGroup().equals(currentGroup)) {
                liftingFop = fop;
                break search;
            }
        }
        if (liftingFop != null) {
            Notification.show(
                    getTranslation("Warning_GroupLifting") + liftingFop.getName() + getTranslation("CannotEditResults"),
                    3000, Position.MIDDLE);
            logger.debug(getTranslation("CannotEditResults_logging"), currentGroup, liftingFop);
        } else {
            logger.debug(getTranslation("EditingResults_logging"), currentGroup, liftingFop);
        }
        return liftingFop != null;
    }

    private Button createCategoryResultsDownloadButton() {
        downloadDialog = new DownloadDialog(
                () -> {
                    JXLSResultSheet rs = new JXLSResultSheet();
                    rs.setAgeDivision(ageDivision);
                    rs.setAgeGroupPrefix(ageGroupPrefix);
                    rs.setCategory(categoryValue);
                    // group may have been edited since the page was loaded
                    rs.setGroup(currentGroup != null ? GroupRepository.getById(currentGroup.getId()) : null);
                    rs.setSortedAthletes((List<Athlete>) findAll());
                    return rs;
                },
                "/templates/protocol",
                null,
                Competition::getComputedProtocolTemplateFileName,
                Competition::setProtocolTemplateFileName,
                Translator.translate("CategoryResults"), "results", Translator.translate("Download"));
        Button resultsButton = downloadDialog.createTopBarDownloadButton();
        return resultsButton;
    }

    private Button createFinalPackageDownloadButton() {
        downloadDialog = new DownloadDialog(
                () -> {
                    JXLSCompetitionBook rs = new JXLSCompetitionBook(locationUI);
                    // group may have been edited since the page was loaded
                    rs.setGroup(currentGroup != null ? GroupRepository.getById(currentGroup.getId()) : null);
                    rs.setAgeDivision(ageDivision);
                    rs.setAgeGroupPrefix(ageGroupPrefix);
                    rs.setCategory(categoryValue);
                    return rs;
                },
                "/templates/competitionBook",
                null,
                Competition::getComputedFinalPackageTemplateFileName,
                Competition::setFinalPackageTemplateFileName,
                Translator.translate("FinalResultsPackage"), "finalPackage", Translator.translate("Download"));
        Button resultsButton = downloadDialog.createTopBarDownloadButton();
        return resultsButton;
    }

    private void doAgeGroupPrefixRefresh(String string) {
        setAgeGroupPrefix(string);
        updateFilters(getAgeDivision(), getAgeGroupPrefix());
        //xlsWriter.setAgeGroupPrefix(ageGroupPrefix);
        if (crudGrid != null) {
            crudGrid.refreshGrid();
        }
    }

    private AgeDivision getAgeDivision() {
        return ageDivision;
    }

    private String getAgeGroupPrefix() {
        return ageGroupPrefix;
    }

    private Category getCategoryValue() {
        // logger.trace("categoryValue = {} {}", categoryValue, LoggerUtils.whereFrom());
        return categoryValue;
    }

    private void setAgeDivision(AgeDivision ageDivision) {
        //logger.debug("setAgeDivision to {} from {}",ageDivision, LoggerUtils.whereFrom());
        this.ageDivision = ageDivision;
    }

    private void setAgeGroupPrefix(String value) {
        this.ageGroupPrefix = value;
    }

    private void updateFilters(AgeDivision ageDivision2, String ageGroupPrefix2) {
        // logger.debug("updateFilters {} {} {} {}", ageDivision2, ageGroupPrefix2,
        // getCategoryValue(),LoggerUtils.whereFrom());
        List<Category> categories = CategoryRepository.findByGenderDivisionAgeBW(genderFilter.getValue(),
                getAgeDivision(), null, null);
        if (getAgeGroupPrefix() != null && !getAgeGroupPrefix().isBlank()) {
            categories = categories.stream().filter((c) -> c.getAgeGroup().getCode().equals(getAgeGroupPrefix()))
                    .collect(Collectors.toList());
        }
        if (ageGroupPrefix == null || ageGroupPrefix.isBlank()) {
            categoryFilter.setItems(new ArrayList<>());
        } else {
            Category prevValue = getCategoryValue();
            categoryFilter.setItems(categories);
            // contains is not reliable for Categories, check codes
            if (categories != null && prevValue != null
                    && categories.stream()
                            .anyMatch(c -> c.getComputedCode().contentEquals(prevValue.getComputedCode()))) {
                categoryFilter.setValue(prevValue);
            } else {
                categoryFilter.setValue(null);
            }

        }
    }

}
