/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

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
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireLogin;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "npreparation/categories", layout = CategoryLayout.class)
public class CategoryContent extends VerticalLayout implements CrudListener<Category>, OwlcmsContent, RequireLogin {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(CategoryContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private Checkbox activeFilter = new Checkbox();
    private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
    private ComboBox<AgeGroup> ageGroupFilter = new ComboBox<>();
    private OwlcmsCrudFormFactory<Category> crudFormFactory;
    private TextField nameFilter = new TextField();
    private OwlcmsLayout routerLayout;

    /**
     * Instantiates the category crudGrid.
     */
    public CategoryContent() {
        OwlcmsFactory.waitDBInitialized();
        crudFormFactory = createFormFactory();
        GridCrud<Category> crud = createGrid(crudFormFactory);
        defineFilters(crud);
        fillHW(crud, this);
    }

    @Override
    public Category add(Category domainObjectToAdd) {
        return crudFormFactory.add(domainObjectToAdd);
    }

    @Override
    public void delete(Category domainObjectToDelete) {
        crudFormFactory.delete(domainObjectToDelete);
    }

    /**
     * The refresh button on the toolbar
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<Category> findAll() {
        return CategoryRepository.findFiltered(nameFilter.getValue(), (Gender) null, ageDivisionFilter.getValue(),
                ageGroupFilter.getValue(), (Integer) null, (Double) null, activeFilter.getValue(), -1, -1);
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Preparation_Categories");
    }

    @Override
    public OwlcmsLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public Category update(Category domainObjectToUpdate) {
        return crudFormFactory.update(domainObjectToUpdate);
    }

    /**
     * The content and ordering of the editing form
     *
     * @param crudFormFactory the factory that will create the form using this information
     */
    protected void createFormLayout(OwlcmsCrudFormFactory<Category> crudFormFactory) {
        List<String> props = new LinkedList<>();
        List<String> captions = new LinkedList<>();

        props.add("name");
        captions.add(getTranslation("Name"));
        props.add("ageGroup");
        captions.add(getTranslation("AgeGroup"));
        props.add("gender");
        captions.add(getTranslation("Gender"));
        props.add("minimumWeight");
        captions.add(getTranslation("MinimumWeight"));
        props.add("maximumWeight");
        captions.add(getTranslation("MaximumWeight"));
        props.add("wr");
        captions.add(getTranslation("WorldRecord"));
//        props.add("active");
//        captions.add(getTranslation("Active"));

        crudFormFactory.setVisibleProperties(props.toArray(new String[0]));
        crudFormFactory.setFieldCaptions(captions.toArray(new String[0]));
        crudFormFactory.setFieldProvider("ageGroup", new OwlcmsComboBoxProvider<>(getTranslation("AgeGroup"),
                AgeGroupRepository.findAll(), new TextRenderer<>(AgeGroup::getName), AgeGroup::getName));
    }

    /**
     * The columns of the crudGrid
     *
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected GridCrud<Category> createGrid(OwlcmsCrudFormFactory<Category> crudFormFactory) {
        Grid<Category> grid = new Grid<>(Category.class, false);
//        grid.addColumn(new ComponentRenderer<>(cat -> {
//            // checkbox to avoid entering in the form
//            Checkbox activeBox = new Checkbox("Name");
//            activeBox.setLabel(null);
//            activeBox.getElement().getThemeList().set("secondary", true);
//            activeBox.setValue(cat.isActive());
//            activeBox.addValueChangeListener(click -> {
//                activeBox.setValue(click.getValue());
//                cat.setActive(click.getValue());
//                CategoryRepository.save(cat);
//                grid.getDataProvider().refreshItem(cat);
//            });
//            return activeBox;
//        })).setHeader(getTranslation("Active")).setWidth("0");
        grid.addColumn(Category::getName).setHeader(getTranslation("Name"));
        grid.addColumn(new TextRenderer<Category>(
                item -> getTranslation("Division." + item.getAgeGroup().getAgeDivision().name())))
                .setHeader(getTranslation("AgeDivision"));
        grid.addColumn(Category::getGender).setHeader(getTranslation("Gender"));
        grid.addColumn(Category::getMinimumWeight).setHeader(getTranslation("MinimumWeight"));
        grid.addColumn(Category::getMaximumWeight).setHeader(getTranslation("MaximumWeight"));

        GridCrud<Category> crud = new OwlcmsCrudGrid<>(Category.class, new OwlcmsGridLayout(Category.class),
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
    protected void defineFilters(GridCrud<Category> crud) {
        nameFilter.setPlaceholder(getTranslation("Name"));
        nameFilter.setClearButtonVisible(true);
        nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        nameFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(nameFilter);

        ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision"));
        ageDivisionFilter.setItems(AgeDivision.findAll());
        ageDivisionFilter.setItemLabelGenerator((ad) -> getTranslation("Division." + ad.name()));
        ageDivisionFilter.setClearButtonVisible(true);
        ageDivisionFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(ageDivisionFilter);
        crud.getCrudLayout().addToolbarComponent(new Label(""));

        ageGroupFilter.setPlaceholder(getTranslation("AgeGroup"));
        ageGroupFilter.setItems(AgeGroupRepository.findAll());
        ageGroupFilter.setItemLabelGenerator(AgeGroup::getName);
        ageGroupFilter.setClearButtonVisible(true);
        ageGroupFilter.addValueChangeListener(e -> {
            crud.refreshGrid();
        });
        crud.getCrudLayout().addFilterComponent(ageGroupFilter);
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

    /**
     * Define the form used to edit a given category.
     *
     * @return the form factory that will create the actual form on demand
     */
    private OwlcmsCrudFormFactory<Category> createFormFactory() {
        OwlcmsCrudFormFactory<Category> editingFormFactory = new CategoryEditingFormFactory(Category.class);
        createFormLayout(editingFormFactory);
        return editingFormFactory;
    }
}
