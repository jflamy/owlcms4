/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.RequireLogin;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AgeGroupContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/agegroup", layout = AgeGroupLayout.class)
public class AgeGroupContent extends VerticalLayout implements CrudListener<AgeGroup>, OwlcmsContent, RequireLogin {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
//    private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
    private TextField nameFilter = new TextField();
    private Checkbox activeFilter = new Checkbox();
    private OwlcmsRouterLayout routerLayout;
    private OwlcmsCrudFormFactory<AgeGroup> crudFormFactory;

    /**
     * Instantiates the ageGroup crudGrid.
     */
    public AgeGroupContent() {
        crudFormFactory = createFormFactory();
        GridCrud<AgeGroup> crud = createGrid(crudFormFactory);
        defineFilters(crud);
        fillHW(crud, this);
    }

    @Override
    public AgeGroup add(AgeGroup domainObjectToAdd) {
        return crudFormFactory.add(domainObjectToAdd);
    }

    /**
     * Define the form used to edit a given ageGroup.
     *
     * @return the form factory that will create the actual form on demand
     */
    private OwlcmsCrudFormFactory<AgeGroup> createFormFactory() {
        OwlcmsCrudFormFactory<AgeGroup> editingFormFactory = new AgeGroupEditingFormFactory(AgeGroup.class);
        createFormLayout(editingFormFactory);
        return editingFormFactory;
    }

    /**
     * The content and ordering of the editing form
     *
     * @param crudFormFactory the factory that will create the form using this
     *                        information
     */
    protected void createFormLayout(OwlcmsCrudFormFactory<AgeGroup> crudFormFactory) {
        List<String> props = new LinkedList<>();
        List<String> captions = new LinkedList<>();

        props.add("name");
        captions.add(getTranslation("Name"));
        props.add("gender");
        captions.add(getTranslation("Gender"));
        props.add("minAge");
        captions.add(getTranslation("MinimumAge"));
        props.add("maxAge");
        captions.add(getTranslation("MaximumAge"));
        props.add("active");
        captions.add(getTranslation("Active"));
        props.add("categoriesAsString");
        captions.add(getTranslation("BodyWeightCategories"));

        crudFormFactory.setVisibleProperties(props.toArray(new String[0]));
        crudFormFactory.setFieldCaptions(captions.toArray(new String[0]));
//        crudFormFactory.setFieldProvider("ageGroup", new OwlcmsComboBoxProvider<>(getTranslation("AgeGroup"),
//                AgeGroupRepository.findAll(), new TextRenderer<>(AgeGroup::getName), AgeGroup::getName));
    }

    /**
     * The columns of the crudGrid
     *
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected GridCrud<AgeGroup> createGrid(OwlcmsCrudFormFactory<AgeGroup> crudFormFactory) {
        Grid<AgeGroup> grid = new Grid<>(AgeGroup.class, false);
        grid.addColumn(new ComponentRenderer<>(cat -> {
            // checkbox to avoid entering in the form
            Checkbox activeBox = new Checkbox("Name");
            activeBox.setLabel(null);
            activeBox.getElement().getThemeList().set("secondary", true);
            activeBox.setValue(cat.isActive());
            activeBox.addValueChangeListener(click -> {
                activeBox.setValue(click.getValue());
                cat.setActive(click.getValue());
                AgeGroupRepository.save(cat);
                grid.getDataProvider().refreshItem(cat);
            });
            return activeBox;
        })).setHeader(getTranslation("Active")).setWidth("0");
        grid.addColumn(AgeGroup::getName).setHeader(getTranslation("Name"));
        grid.addColumn(new TextRenderer<AgeGroup>(
                item -> getTranslation("Division." + item.getAgeDivision().name())))
                .setHeader(getTranslation("AgeDivision"));
        grid.addColumn(AgeGroup::getGender).setHeader(getTranslation("Gender"));
        grid.addColumn(AgeGroup::getMinAge).setHeader(getTranslation("MinimumAge"));
        grid.addColumn(AgeGroup::getMaxAge).setHeader(getTranslation("MaximumAge"));
        grid.addColumn(AgeGroup::getCategoriesAsString).setAutoWidth(true).setHeader(getTranslation("BodyWeightCategories"));

        GridCrud<AgeGroup> crud = new OwlcmsCrudGrid<>(AgeGroup.class, new OwlcmsGridLayout(AgeGroup.class),
                crudFormFactory, grid);
        crud.setCrudListener(this);
        crud.setClickRowToUpdate(true);
        return crud;
    }

    /**
     * The filters at the top of the crudGrid
     *
     * @param crudGrid the crudGrid that will be filtered.
     */
    protected void defineFilters(GridCrud<AgeGroup> crud) {
        nameFilter.setPlaceholder(getTranslation("Name"));
        nameFilter.setClearButtonVisible(true);
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(nameFilter);

        ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
        ageDivisionFilter.setItems(AgeDivision.findAll());
        ageDivisionFilter.setItemLabelGenerator((ad) -> getTranslation("Division."+ad.name()));
        ageDivisionFilter.setClearButtonVisible(true);
        ageDivisionFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(ageDivisionFilter);
        crud.getCrudLayout().addToolbarComponent(new Label(""));

        activeFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        activeFilter.setLabel(getTranslation("Active"));
        activeFilter.setAriaLabel(getTranslation("ActiveCategoriesOnly"));
        crud.getCrudLayout().addFilterComponent(activeFilter);

        Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
        clearFilters.addClickListener(event -> {
            ageDivisionFilter.clear();
        });
        crud.getCrudLayout().addFilterComponent(clearFilters);
    }

    @Override
    public void delete(AgeGroup domainObjectToDelete) {
        crudFormFactory.delete(domainObjectToDelete);
    }

    /**
     * The refresh button on the toolbar
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<AgeGroup> findAll() {
        return AgeGroupRepository.findFiltered(nameFilter.getValue(), (Gender) null, ageDivisionFilter.getValue(),
                (Integer) null, activeFilter.getValue(), -1, -1);
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Preparation_AgeGroups");
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public AgeGroup update(AgeGroup domainObjectToUpdate) {
        return crudFormFactory.update(domainObjectToUpdate);
    }
}
