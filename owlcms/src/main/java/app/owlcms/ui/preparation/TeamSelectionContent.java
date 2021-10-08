/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.ui.preparation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.spreadsheet.JXLSCompetitionBook;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.results.Resource;
import app.owlcms.ui.results.ResultsContent;
import app.owlcms.ui.shared.AthleteCrudGrid;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ResultsContent.
 *
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@Route(value = "preparation/teams", layout = AthleteGridLayout.class)
public class TeamSelectionContent extends AthleteGridContent implements HasDynamicTitle {

    static final String TITLE = "TeamMembership";
    final private static Logger logger = (Logger) LoggerFactory.getLogger(TeamSelectionContent.class);
    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
    }

    private Button download;
    private Anchor finalPackage;
    private Group currentGroup;
    private JXLSCompetitionBook xlsWriter;
    private ComboBox<Resource> templateSelect;
    private String ageGroupPrefix;

    private AgeDivision ageDivision;

    private ComboBox<Category> categoryFilter;

    /**
     * Instantiates a new announcer content. Does nothing. Content is created in
     * {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
     */
    public TeamSelectionContent() {
        super();
        defineFilters(crudGrid);
        crudGrid.setClickable(true);
        crudGrid.getGrid().setMultiSort(true);
        setTopBarTitle(getTranslation(TITLE));
    }
    
    /**
     * Define the form used to edit a given athlete's participation
     * The Athlete will in fact be a PAthlete so we only get the specific participation for the
     * age group selected by the page filters.
     *
     * @return the form factory that will create the actual form on demand
     */
    @Override
    protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
        return new TeamParticipationFormFactory(Athlete.class, this);
    }

    /**
     * Get the content of the crudGrid. Invoked by refreshGrid.
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<Athlete> findAll() {
        if (getAgeGroupPrefix() == null && getAgeDivision() == null) {
            return new ArrayList<>();
        }

        List<PAthlete> athletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(ageGroupPrefix, ageDivision);

        Stream<PAthlete> stream = athletes.stream()
                .filter(a -> {
                    Category catFilterValue = categoryFilter.getValue();
                    String catCode = catFilterValue != null ? catFilterValue.getCode() : null;
                    String athleteCode = a.getCategory().getCode();

                    Gender genderFilterValue = genderFilter != null ?  genderFilter.getValue() : null;
                    Gender athleteGender = a.getGender();
                    
                    boolean catOk = (catFilterValue == null || athleteCode.contentEquals(catCode)) 
                            && (genderFilterValue == null || genderFilterValue == athleteGender);
                    //logger.trace("filter {} : {} {} {} | {} {}", catOk, catFilterValue, catCode, athleteCode, genderFilterValue, athleteGender);
                    return catOk;
                });
        return stream.collect(Collectors.toList());
    }

    public Group getGridGroup() {
        return getGroupFilter().getValue();
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation(TITLE);
    }

    @Override
    public boolean isIgnoreGroupFromURL() {
        return false;
    }

    public void refresh() {
        crudGrid.refreshGrid();
    }

    public void setGridGroup(Group group) {
        subscribeIfLifting(group);
        getGroupFilter().setValue(group);
        refresh();
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
     * @see app.owlcms.ui.shared.AthleteGridContent#createCrudGrid(app.owlcms.ui.crudui.OwlcmsCrudFormFactory)
     */
    @Override
    protected AthleteCrudGrid createCrudGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        Grid<Athlete> grid = ResultsContent.createResultGrid();

        OwlcmsGridLayout gridLayout = new OwlcmsGridLayout(Athlete.class);
        AthleteCrudGrid crudGrid = new AthleteCrudGrid(Athlete.class, gridLayout, crudFormFactory, grid) {
            @Override
            protected void initToolbar() {
            }

            @Override
            protected void updateButtonClicked() {
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
     * Create the top bar.
     *
     * Note: the top bar is created before the content.
     *
     * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
     *
     * @param topBar
     */
    @Override
    protected void createTopBar() {
        // logger.trace("createTopBar {}", LoggerUtils.stackTrace());
        // show arrow but close menu
        getAppLayout().setMenuVisible(true);
        getAppLayout().closeDrawer();

        H3 title = new H3();
        title.setText(getTranslation(TITLE));
        title.add();
        title.getStyle().set("margin", "0px 0px 0px 0px").set("font-weight", "normal");

        topBar = getAppLayout().getAppBarElementWrapper();
        xlsWriter = new JXLSCompetitionBook(true, UI.getCurrent());
        StreamResource href = new StreamResource(TITLE+"Report"+".xls", xlsWriter);
        finalPackage = new Anchor(href, "");
        finalPackage.getStyle().set("margin-left", "1em");
        download = new Button(getTranslation(TITLE+".Report"), new Icon(VaadinIcon.DOWNLOAD_ALT));

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
        List<AgeDivision> adItems = AgeGroupRepository.allAgeDivisionsForAllAgeGroups();
        topBarAgeDivisionSelect.setItems(adItems);
        topBarAgeDivisionSelect.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
        topBarAgeDivisionSelect.setClearButtonVisible(true);
        topBarAgeDivisionSelect.setWidth("8em");
        topBarAgeDivisionSelect.getStyle().set("margin-left", "1em");
        setAgeDivisionSelectionListener();
        AgeDivision value = (adItems != null && adItems.size() > 0) ? adItems.get(0) : null;
        setAgeDivision(value);
        topBarAgeDivisionSelect.setValue(value);

//        templateSelect = new ComboBox<>();
//        templateSelect.setPlaceholder(getTranslation("AvailableTemplates"));
//        List<Resource> resourceList = new ResourceWalker().getResourceList("/templates/competitionBook",
//                ResourceWalker::relativeName, null, OwlcmsSession.getLocale());
//        templateSelect.setItems(resourceList);
//        templateSelect.setValue(null);
//        templateSelect.setWidth("15em");
//        templateSelect.getStyle().set("margin-left", "1em");
//        setTemplateSelectionListener(resourceList);

        topBarGroupSelect = new ComboBox<>();
        topBarGroupSelect.setPlaceholder(getTranslation("Group"));
        topBarGroupSelect.setItems(GroupRepository.findAll());
        topBarGroupSelect.setItemLabelGenerator(Group::getName);
        topBarGroupSelect.setClearButtonVisible(true);
        topBarGroupSelect.setValue(null);
        topBarGroupSelect.setWidth("8em");
        setGroupSelectionListener();

        finalPackage.add(download);

        HorizontalLayout buttons = new HorizontalLayout(finalPackage);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

        topBar.getStyle().set("flex", "100 1");
        topBar.removeAll();
        topBar.add(title,
                topBarAgeDivisionSelect, topBarAgeGroupPrefixSelect, 
                /* templateSelect, */ 
                buttons);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        topBar.setFlexGrow(0.2, title);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
    }

    @Override
    protected void defineFilters(GridCrud<Athlete> crud) {

        if (categoryFilter == null) {
            categoryFilter = new ComboBox<>();
            categoryFilter.setClearButtonVisible(true);
            categoryFilter.setPlaceholder(getTranslation("Category"));
            categoryFilter.setClearButtonVisible(true);
            categoryFilter.addValueChangeListener(e -> {
                crud.refreshGrid();
            });
            categoryFilter.setWidth("10em");
        }

        crud.getCrudLayout().addFilterComponent(categoryFilter);

        getGroupFilter().setPlaceholder(getTranslation("Group"));
        getGroupFilter().setItems(GroupRepository.findAll());
        getGroupFilter().setItemLabelGenerator(Group::getName);
        getGroupFilter().addValueChangeListener(e -> {
            logger.debug("updating filters: group={}", e.getValue());
            currentGroup = e.getValue();
            updateURLLocation(getLocationUI(), getLocation(), currentGroup);
            subscribeIfLifting(e.getValue());
        });
        getGroupFilter().setVisible(false);
        crud.getCrudLayout().addFilterComponent(getGroupFilter());

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
    }

    /**
     * We do not connect to the event bus, and we do not track a field of play (non-Javadoc)
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (topBar == null) {
            createTopBar();
        }
    }

    protected void setAgeDivisionSelectionListener() {
        topBarAgeDivisionSelect.addValueChangeListener(e -> {
            // the name of the resulting file is set as an attribute on the <a href tag that
            // surrounds the download button.
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
            topBarAgeGroupPrefixSelect.setEnabled(notEmpty);
            String first = (notEmpty && ageDivisionValue == AgeDivision.IWF) ? ageDivisionAgeGroupPrefixes.get(0) : null;
            logger.debug("ad {} ag {} first {} select {}", ageDivisionValue, ageDivisionAgeGroupPrefixes, first,
                    topBarAgeGroupPrefixSelect);
            topBarAgeGroupPrefixSelect.setValue(notEmpty ? first : null);

            xlsWriter.setAgeDivision(ageDivisionValue);
            finalPackage.getElement().setAttribute("download",
                    "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
                            : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");

            updateFilters(ageDivisionValue, first);

            crudGrid.refreshGrid();
        });
    }

    protected void setAgeGroupPrefixSelectionListener() {
        topBarAgeGroupPrefixSelect.addValueChangeListener(e -> {
            // the name of the resulting file is set as an attribute on the <a href tag that
            // surrounds
            // the download button.
            setAgeGroupPrefix(e.getValue());
            updateFilters(getAgeDivision(), getAgeGroupPrefix());
            xlsWriter.setAgeGroupPrefix(ageGroupPrefix);
            finalPackage.getElement().setAttribute("download",
                    "results" + (getAgeDivision() != null ? "_" + getAgeDivision().name()
                            : (ageGroupPrefix != null ? "_" + ageGroupPrefix : "_all")) + ".xls");
            crudGrid.refreshGrid();
        });
    }

    private void updateFilters(AgeDivision ageDivision2, String ageGroupPrefix2) {
        List<Category> categories = CategoryRepository.findByGenderDivisionAgeBW(genderFilter.getValue(),
                getAgeDivision(), null, null);
        if (getAgeGroupPrefix() != null && !getAgeGroupPrefix().isBlank()) {
            categories = categories.stream().filter((c) -> c.getAgeGroup().getCode().equals(getAgeGroupPrefix()))
                    .collect(Collectors.toList());
        }
        //logger.trace("updateFilters {}, {}, {}", ageDivision2, ageGroupPrefix2, categories);
        categoryFilter.setItems(categories);
    }

    protected void setGroupSelectionListener() {
        topBarGroupSelect.setValue(getGridGroup());
        topBarGroupSelect.addValueChangeListener(e -> {
            setGridGroup(e.getValue());
            currentGroup = e.getValue();
            // the name of the resulting file is set as an attribute on the <a href tag that
            // surrounds the download button.
            xlsWriter.setGroup(currentGroup);
            finalPackage.getElement().setAttribute("download",
                    "results" + (currentGroup != null ? "_" + currentGroup : "_all") + ".xls");
        });
    }

    private AgeDivision getAgeDivision() {
        return ageDivision;
    }

    private String getAgeGroupPrefix() {
        return ageGroupPrefix;
    }

    private Resource searchMatch(List<Resource> resourceList, String curTemplateName) {
        Resource found = null;
        Resource totalTemplate = null;
        for (Resource curResource : resourceList) {
            String fileName = curResource.getFileName();
            if (fileName.equals(curTemplateName)) {
                found = curResource;
                break;
            }
            if (fileName.startsWith("Total")) {
                totalTemplate = curResource;
            }
        }
        if (found != null) {
            return found;
        } else {
            // should be non-null, if not there, null is ok.
            return totalTemplate;
        }
    }

    private void setAgeDivision(AgeDivision ageDivision) {
        this.ageDivision = ageDivision;
    }

    private void setAgeGroupPrefix(String value) {
        this.ageGroupPrefix = value;
    }

    @SuppressWarnings("unused")
    private void setTemplateSelectionListener(List<Resource> resourceList) {
        try {
            String curTemplateName = Competition.getCurrent().getFinalPackageTemplateFileName();
            Resource found = searchMatch(resourceList, curTemplateName);
            templateSelect.addValueChangeListener((e) -> {
                Competition.getCurrent().setFinalPackageTemplateFileName(e.getValue().getFileName());
                CompetitionRepository.save(Competition.getCurrent());
            });
            templateSelect.setValue(found);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void subscribeIfLifting(Group nGroup) {
        logger.debug("subscribeIfLifting {}", nGroup);
        Collection<FieldOfPlay> fops = OwlcmsFactory.getFOPs();
        currentGroup = nGroup;

        // go through all the FOPs
        for (FieldOfPlay fop : fops) {
            // unsubscribe from FOP -- ensures that we clean up if no group is lifting
            try {
                fop.getUiEventBus().unregister(this);
            } catch (Exception ex) {
            }
            try {
                fop.getFopEventBus().unregister(this);
            } catch (Exception ex) {
            }

            // subscribe to fop and start tracking if actually lifting
            if (fop.getGroup() != null && fop.getGroup().equals(nGroup)) {
                logger.debug("subscribing to {} {}", fop, nGroup);
                try {
                    fopEventBusRegister(this, fop);
                } catch (Exception ex) {
                }
                try {
                    uiEventBusRegister(this, fop);
                } catch (Exception ex) {
                }
            }
        }
    }

    public void highlightResetButton() {
        // TODO add button to recompute team points.
    }

}
