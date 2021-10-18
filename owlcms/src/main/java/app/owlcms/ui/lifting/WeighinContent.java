/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.lifting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
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
import app.owlcms.components.fields.ValidationTextField;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
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
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.crudui.OwlcmsMultiSelectComboBoxProvider;
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
    private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
    private ComboBox<Category> categoryFilter = new ComboBox<>();
    private ComboBox<Group> groupFilter = new ComboBox<>();
    private ComboBox<Gender> genderFilter = new ComboBox<>();

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
        fillHW(crudGrid, this);
    }

    @Override
    public Athlete add(Athlete Athlete) {
        crudFormFactory.add(Athlete);
        return Athlete;
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
                categoryFilter.getValue(), ageGroupFilter.getValue(), ageDivisionFilter.getValue(),
                genderFilter.getValue(), weighedInFilter.getValue(), -1, -1);
        AthleteSorter.registrationOrder(findFiltered);
        return findFiltered;
    }

    /**
     * @return the groupFilter
     */
    public ComboBox<Group> getGroupFilter() {
        return groupFilter;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("WeighIn");
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    public void refresh() {
        crudGrid.refreshGrid();
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public Athlete update(Athlete Athlete) {
        return crudFormFactory.update(Athlete);
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
     * The columns of the crudGrid
     *
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected OwlcmsCrudGrid<Athlete> createGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        Grid<Athlete> grid = new Grid<>(Athlete.class, false);
        grid.addColumn("startNumber").setHeader(getTranslation("Start_"));
        grid.addColumn("lastName").setHeader(getTranslation("LastName"));
        grid.addColumn("firstName").setHeader(getTranslation("FirstName"));
        grid.addColumn("team").setHeader(getTranslation("Team"));
        grid.addColumn("ageGroup").setHeader(getTranslation("AgeGroup"));
        grid.addColumn("category").setHeader(getTranslation("Category"));
        grid.addColumn("group").setHeader(getTranslation("Group"));
        grid.addColumn(new NumberRenderer<>(Athlete::getBodyWeight, "%.2f", this.getLocale()), "bodyWeight")
                .setHeader(getTranslation("BodyWeight"));
        grid.addColumn("snatch1Declaration").setHeader(getTranslation("SnatchDecl_"));
        grid.addColumn("cleanJerk1Declaration").setHeader(getTranslation("C_and_J_decl"));
        grid.addColumn("eligibleForIndividualRanking").setHeader(getTranslation("Eligible"));
        grid.addColumn("eligibleForTeamRanking").setHeader(getTranslation("TeamMember?"));
        OwlcmsCrudGrid<Athlete> crudGrid = new OwlcmsCrudGrid<>(Athlete.class, new OwlcmsGridLayout(Athlete.class) {
            @Override
            public void hideForm() {
                super.hideForm();
                logger.trace("clearing {}", OwlcmsSession.getAttribute("weighIn"));
                OwlcmsSession.setAttribute("weighIn", null);
            }
        },
                crudFormFactory, grid);
        crudGrid.setCrudListener(this);
        crudGrid.setClickRowToUpdate(true);
        return crudGrid;
    }

    /**
     * The filters at the top of the crudGrid
     *
     * @param crudGrid the crudGrid that will be filtered.
     */
    protected void defineFilters(GridCrud<Athlete> crudGrid) {
        lastNameFilter.setPlaceholder(getTranslation("LastName"));
        lastNameFilter.setClearButtonVisible(true);
        lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        lastNameFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        crudGrid.getCrudLayout().addFilterComponent(lastNameFilter);

        ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
        ageDivisionFilter.setItems(AgeDivision.findAll());
        ageDivisionFilter.setItemLabelGenerator((ad) -> getTranslation("Division." + ad.name()));
        ageDivisionFilter.setClearButtonVisible(true);
        ageDivisionFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        crudGrid.getCrudLayout().addFilterComponent(ageDivisionFilter);

        ageGroupFilter.setPlaceholder(getTranslation("AgeGroup"));
        ageGroupFilter.setItems(AgeGroupRepository.findAll());
        // ageGroupFilter.setItemLabelGenerator(AgeDivision::name);
        ageGroupFilter.setClearButtonVisible(true);
        ageGroupFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        ageGroupFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(ageGroupFilter);

        categoryFilter.setPlaceholder(getTranslation("Category"));
        categoryFilter.setItems(CategoryRepository.findActive());
        categoryFilter.setItemLabelGenerator(Category::getName);
        categoryFilter.setClearButtonVisible(true);
        categoryFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        crudGrid.getCrudLayout().addFilterComponent(categoryFilter);

        groupFilter.setPlaceholder(getTranslation("Group"));
        groupFilter.setItems(GroupRepository.findAll());
        groupFilter.setItemLabelGenerator(Group::getName);
        groupFilter.setClearButtonVisible(true);
        groupFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        // hide because the top bar has it
        groupFilter.getStyle().set("display", "none");

        crudGrid.getCrudLayout().addFilterComponent(groupFilter);

        weighedInFilter.setPlaceholder(getTranslation("Weighed_in_p"));
        weighedInFilter.setItems(Boolean.TRUE, Boolean.FALSE);
        weighedInFilter.setItemLabelGenerator((i) -> {
            return i ? getTranslation("Weighed") : getTranslation("Not_weighed");
        });
        weighedInFilter.setClearButtonVisible(true);
        weighedInFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        crudGrid.getCrudLayout().addFilterComponent(weighedInFilter);

        genderFilter.setPlaceholder(getTranslation("Gender"));
        genderFilter.setItems(Gender.M, Gender.F);
        genderFilter.setItemLabelGenerator((i) -> {
            return i == Gender.M ? getTranslation("Gender.M") : getTranslation("Gender.F");
        });
        genderFilter.setClearButtonVisible(true);
        genderFilter.addValueChangeListener(e -> {
            crudGrid.refreshGrid();
        });
        genderFilter.setWidth("10em");
        crudGrid.getCrudLayout().addFilterComponent(genderFilter);

        Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
        clearFilters.addClickListener(event -> {
            lastNameFilter.clear();
            ageDivisionFilter.clear();
            categoryFilter.clear();
            // groupFilter.clear();
            weighedInFilter.clear();
            genderFilter.clear();
        });
        crudGrid.getCrudLayout().addFilterComponent(clearFilters);
    }

    /**
     * The content and ordering of the editing form
     *
     * @param crudFormFactory the factory that will create the form using this information
     */
    private void createFormLayout(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
        List<String> props = new LinkedList<>();
        List<String> captions = new LinkedList<>();

        props.add("lastName");
        captions.add(getTranslation("LastName"));
        props.add("firstName");
        captions.add(getTranslation("FirstName"));

        props.add("bodyWeight");
        captions.add(getTranslation("BodyWeight"));
        props.add("snatch1Declaration");
        captions.add(getTranslation("SnatchDecl_"));
        props.add("cleanJerk1Declaration");
        captions.add(getTranslation("C_and_J_decl"));

        props.add("qualifyingTotal");
        captions.add(getTranslation("EntryTotal"));
        props.add("category");
        captions.add(getTranslation("Category"));
        props.add("eligibleCategories");
        captions.add(getTranslation("Registration.EligibleCategories"));
        props.add("group");
        captions.add(getTranslation("Group"));


        props.add("gender");
        captions.add(getTranslation("Gender"));
        props.add("team");
        captions.add(getTranslation("Team"));

        Competition competition = Competition.getCurrent();
        if (competition.isUseBirthYear()) {
            props.add("yearOfBirth");
            captions.add(getTranslation("YearOfBirth"));
        } else {
            props.add("fullBirthDate");
            captions.add(getTranslation("BirthDate_yyyy"));
        }
        props.add("membership");
        captions.add(getTranslation("Membership"));

        props.add("lotNumber");
        captions.add(getTranslation("Lot"));

        props.add("eligibleForIndividualRanking");
        captions.add(getTranslation("Eligible for Individual Ranking?"));

        props.add("eligibleForTeamRanking");
        captions.add(getTranslation("TeamMember?"));

        crudFormFactory.setVisibleProperties(props.toArray(new String[0]));
        crudFormFactory.setFieldCaptions(captions.toArray(new String[0]));

        crudFormFactory.setFieldProvider("gender", new OwlcmsComboBoxProvider<>(getTranslation("Gender"),
                Arrays.asList(Gender.mfValues()), new TextRenderer<>(Gender::name), Gender::name));
        crudFormFactory.setFieldProvider("group", new OwlcmsComboBoxProvider<>(getTranslation("Group"),
                GroupRepository.findAll(), new TextRenderer<>(Group::getName), Group::getName));
        crudFormFactory.setFieldProvider("category", new OwlcmsComboBoxProvider<>(getTranslation("Category"),
                CategoryRepository.findActive(), new TextRenderer<>(Category::getName), Category::getName));
        crudFormFactory.setFieldProvider("eligibleCategories", new OwlcmsMultiSelectComboBoxProvider<>(getTranslation("Registration.EligibleCategories"),
                new ArrayList<Category>(), new TextRenderer<>(Category::getName), Category::getName));
//        crudFormFactory.setFieldProvider("ageDivision",
//                new OwlcmsComboBoxProvider<>(getTranslation("AgeDivision"), Arrays.asList(AgeDivision.values()),
//                        new TextRenderer<>(ad -> getTranslation("Division." + ad.name())), AgeDivision::name));

        crudFormFactory.setFieldType("bodyWeight", BodyWeightField.class);
        crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class);

        // ValidationTextField (or a wrapper) must be used as workaround for unexplained
        // validation behaviour
        crudFormFactory.setFieldType("snatch1Declaration", ValidationTextField.class);
        crudFormFactory.setFieldType("cleanJerk1Declaration", ValidationTextField.class);
        crudFormFactory.setFieldType("qualifyingTotal", ValidationTextField.class);
        crudFormFactory.setFieldType("yearOfBirth", ValidationTextField.class);

        crudFormFactory.setFieldCreationListener("bodyWeight", (e) -> {
            ((BodyWeightField) e).focus();
        });
    }
}
