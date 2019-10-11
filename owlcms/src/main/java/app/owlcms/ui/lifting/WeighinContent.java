/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.lifting;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import app.owlcms.components.fields.BodyWeightField;
import app.owlcms.components.fields.LocalDateField;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.ui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.shared.AthleteRegistrationFormFactory;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AthleteContent
 * 
 * Defines the toolbar and the table for editing data on athletes.
 * 
 */
@SuppressWarnings("serial")
@Route(value = "preparation/weighin", layout = WeighinLayout.class)
@CssImport(value = "./styles/shared-styles.css")
public class WeighinContent extends VerticalLayout implements CrudListener<Athlete>, OwlcmsContent {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(WeighinContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private TextField lastNameFilter = new TextField();
    private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
    private ComboBox<Category> categoryFilter = new ComboBox<>();
    private ComboBox<Group> groupFilter = new ComboBox<>();

    private ComboBox<Boolean> weighedInFilter = new ComboBox<>();
    private OwlcmsCrudGrid<Athlete> crudGrid;
    private OwlcmsRouterLayout routerLayout;
    private OwlcmsCrudFormFactory<Athlete> crudFormFactory;

    /**
     * Instantiates the athlete crudGrid
     */
    public WeighinContent() {
        crudFormFactory = createFormFactory();
        crudGrid = createGrid(crudFormFactory);
        defineFilters(crudGrid);
//		defineQueries(crudGrid);
        fillHW(crudGrid, this);
    }

    /**
     * The columns of the crudGrid
     * 
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected OwlcmsCrudGrid<Athlete> createGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        Grid<Athlete> grid = new Grid<Athlete>(Athlete.class, false);
        grid.addColumn("startNumber").setHeader(getTranslation("Start_"));
        grid.addColumn("lastName").setHeader(getTranslation("LastName"));
        grid.addColumn("firstName").setHeader(getTranslation("FirstName"));
        grid.addColumn("team").setHeader(getTranslation("Team"));
        grid.addColumn("ageDivision").setHeader(getTranslation("AgeDivision"));
        grid.addColumn("category").setHeader(getTranslation("Category"));
        grid.addColumn("group").setHeader(getTranslation("Group"));
        grid.addColumn(new NumberRenderer<Athlete>(Athlete::getBodyWeight, "%.2f", this.getLocale()), "bodyWeight")
                .setHeader(getTranslation("BodyWeight"));
        grid.addColumn("snatch1Declaration").setHeader(getTranslation("SnatchDecl_"));
        grid.addColumn("cleanJerk1Declaration").setHeader(getTranslation("C_and_J_decl"));

        grid.addColumn("eligibleForIndividualRanking").setHeader(getTranslation("Eligible"));
        OwlcmsCrudGrid<Athlete> crudGrid = new OwlcmsCrudGrid<Athlete>(Athlete.class,
                new OwlcmsGridLayout(Athlete.class), crudFormFactory, grid);
        crudGrid.setCrudListener(this);
        crudGrid.setClickRowToUpdate(true);
        return crudGrid;
    }

    /**
     * Define the form used to edit a given athlete.
     * 
     * @return the form factory that will create the actual form on demand
     */
    protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
        OwlcmsCrudFormFactory<Athlete> athleteEditingFormFactory = new AthleteRegistrationFormFactory(Athlete.class);
        createFormLayout(athleteEditingFormFactory);
        return athleteEditingFormFactory;
    }

    /**
     * The content and ordering of the editing form
     * 
     * @param crudFormFactory the factory that will create the form using this
     *                        information
     */
    private void createFormLayout(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        boolean useBirthYear = Competition.getCurrent().isUseBirthYear();
        crudFormFactory.setVisibleProperties("bodyWeight", "category", "snatch1Declaration", "cleanJerk1Declaration",
                "gender", "group", "lastName", "firstName", "team",
                useBirthYear ? "yearOfBirth" : "fullBirthDate", "ageDivision",
                "qualifyingTotal", "membership", "eligibleForIndividualRanking");
        crudFormFactory.setFieldCaptions(getTranslation("BodyWeight"), getTranslation("Category"),
                getTranslation("Snatch_Declaration"), getTranslation("Clean_and_Jerk_Declaration"),
                getTranslation("Gender"), getTranslation("Group"), getTranslation("LastName"),
                getTranslation("FirstName"), getTranslation("Team"),
                useBirthYear ? getTranslation("YearOfBirth"):getTranslation("BirthDate_yyyy"),
                getTranslation("AgeDivision"), getTranslation("EntryTotal"),
                getTranslation("Membership"),
                getTranslation("Eligible for Individual Ranking?"));
        crudFormFactory.setFieldProvider("gender", new OwlcmsComboBoxProvider<>(getTranslation("Gender"),
                Arrays.asList(Gender.values()), new TextRenderer<>(Gender::name), Gender::name));
        crudFormFactory.setFieldProvider("group", new OwlcmsComboBoxProvider<>(getTranslation("Group"),
                GroupRepository.findAll(), new TextRenderer<>(Group::getName), Group::getName));
        crudFormFactory.setFieldProvider("category", new OwlcmsComboBoxProvider<>(getTranslation("Category"),
                CategoryRepository.findActive(), new TextRenderer<>(Category::getName), Category::getName));
        crudFormFactory.setFieldProvider("ageDivision", new OwlcmsComboBoxProvider<>(getTranslation("AgeDivision"),
                Arrays.asList(AgeDivision.values()), new TextRenderer<>(AgeDivision::name), AgeDivision::name));

        crudFormFactory.setFieldType("bodyWeight", BodyWeightField.class);
        if (useBirthYear) {
            crudFormFactory.setFieldType("yearOfBirth", TextField.class);
        } else {
            crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class);
        }
        crudFormFactory.setFieldCreationListener("bodyWeight", (e) -> {
            ((BodyWeightField) e).focus();
        });
    }

    @Override
    public Athlete add(Athlete Athlete) {
        crudFormFactory.add(Athlete);
        return Athlete;
    }

    @Override
    public Athlete update(Athlete Athlete) {
        return crudFormFactory.update(Athlete);
    }

    @Override
    public void delete(Athlete Athlete) {
        crudFormFactory.delete(Athlete);
        return;
    }

    /**
     * The refresh button on the toolbar calls this.
     * 
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<Athlete> findAll() {
        List<Athlete> findFiltered = AthleteRepository.findFiltered(lastNameFilter.getValue(), groupFilter.getValue(),
                categoryFilter.getValue(), ageDivisionFilter.getValue(), weighedInFilter.getValue(), -1, -1);
        AthleteSorter.registrationOrder(findFiltered);
        return findFiltered;
    }

    /**
     * The filters at the top of the crudGrid
     * 
     * @param crudGrid the crudGrid that will be filtered.
     */
    protected void defineFilters(GridCrud<Athlete> crud) {
        lastNameFilter.setPlaceholder(getTranslation("LastName"));
        lastNameFilter.setClearButtonVisible(true);
        lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        lastNameFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(lastNameFilter);

        ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
        ageDivisionFilter.setItems(AgeDivision.findAll());
        ageDivisionFilter.setItemLabelGenerator(AgeDivision::name);
        ageDivisionFilter.setClearButtonVisible(true);
        ageDivisionFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(ageDivisionFilter);

        categoryFilter.setPlaceholder(getTranslation("Category"));
        categoryFilter.setItems(CategoryRepository.findActive());
        categoryFilter.setItemLabelGenerator(Category::getName);
        categoryFilter.setClearButtonVisible(true);
        categoryFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(categoryFilter);

        groupFilter.setPlaceholder(getTranslation("Group"));
        groupFilter.setItems(GroupRepository.findAll());
        groupFilter.setItemLabelGenerator(Group::getName);
        groupFilter.setClearButtonVisible(true);
        groupFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        // hide because the top bar has it
        groupFilter.getStyle().set("display", "none");

        crud.getCrudLayout().addFilterComponent(groupFilter);

        weighedInFilter.setPlaceholder(getTranslation("Weighed_in_p"));
        weighedInFilter.setItems(Boolean.TRUE, Boolean.FALSE);
        weighedInFilter.setItemLabelGenerator((i) -> {
            return i ? getTranslation("Weighed") : getTranslation("Not_weighed");
        });
        weighedInFilter.setClearButtonVisible(true);
        weighedInFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(weighedInFilter);

        Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
        clearFilters.addClickListener(event -> {
            lastNameFilter.clear();
            ageDivisionFilter.clear();
            categoryFilter.clear();
            // groupFilter.clear();
            weighedInFilter.clear();
        });
        crud.getCrudLayout().addFilterComponent(clearFilters);
    }

    /**
     * @return the groupFilter
     */
    public ComboBox<Group> getGroupFilter() {
        return groupFilter;
    }

    public void refresh() {
        crudGrid.refreshGrid();
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("WeighIn");
    }
}
