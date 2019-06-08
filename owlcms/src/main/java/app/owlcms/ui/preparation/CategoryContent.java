/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Collection;

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
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.shared.AppLayoutAware;
import app.owlcms.ui.shared.ContentWrapping;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/categories", layout = CategoryLayout.class)
public class CategoryContent extends VerticalLayout
implements CrudListener<Category>, ContentWrapping, AppLayoutAware, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(CategoryContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
	private TextField nameFilter = new TextField();
	private Checkbox activeFilter = new Checkbox();
	private OwlcmsRouterLayout routerLayout;
	private OwlcmsCrudFormFactory<Category> crudFormFactory;

	/**
	 * Instantiates the category crudGrid.
	 */
	public CategoryContent() {
		crudFormFactory = createFormFactory();
		GridCrud<Category> crud = createGrid(crudFormFactory);
		defineFilters(crud);
		fillHW(crud, this);
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<Category> createGrid(OwlcmsCrudFormFactory<Category> crudFormFactory) {
		Grid<Category> grid = new Grid<>(Category.class, false);
		grid.setColumns("name", "ageDivision", "gender", "minimumWeight", "maximumWeight", "active");
		grid.getColumnByKey("name").setHeader("Name");
		grid.getColumnByKey("ageDivision").setHeader("Age Division");
		grid.getColumnByKey("gender").setHeader("Gender");

		GridCrud<Category> crud = new OwlcmsCrudGrid<>(
				Category.class,
				new OwlcmsGridLayout(Category.class),
				crudFormFactory,
				grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		return crud;
	}

	/**
	 * Define the form used to edit a given category.
	 *
	 * @return the form factory that will create the actual form on demand
	 */
	private OwlcmsCrudFormFactory<Category> createFormFactory() {
		OwlcmsCrudFormFactory<Category> editingFormFactory = createCategoryEditingFormFactory();
		createFormLayout(editingFormFactory);
		return editingFormFactory;
	}

	/**
	 * The content and ordering of the editing form
	 *
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	protected void createFormLayout(OwlcmsCrudFormFactory<Category> crudFormFactory) {
		crudFormFactory.setVisibleProperties("name",
				"ageDivision",
				"gender",
				"minimumWeight",
				"maximumWeight",
				"wr",
				"active");
		crudFormFactory.setFieldCaptions("Name",
				"Age Division",
				"Gender",
				"Minimum Weight",
				"Maximum Weight",
				"World Record",
				"Active");
	}

	/**
	 * Create the actual form generator with all the conversions and validations required
	 *
	 * {@link RegistrationContent#createAthleteEditingFormFactory} for example of redefinition of bindField
	 *
	 * @return the actual factory, with the additional mechanisms to do validation
	 */
	private OwlcmsCrudFormFactory<Category> createCategoryEditingFormFactory() {
		return new CategoryEditingFormFactory(Category.class);
	}



	public Category add(Category domainObjectToAdd) {
		return crudFormFactory.add(domainObjectToAdd);
	}

	public Category update(Category domainObjectToUpdate) {
		return crudFormFactory.update(domainObjectToUpdate);
	}

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
		return CategoryRepository
				.findFiltered(nameFilter.getValue(), ageDivisionFilter.getValue(), null, activeFilter.getValue(), -1, -1);
	}

	/**
	 * The filters at the top of the crudGrid
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	protected void defineFilters(GridCrud<Category> crud) {
		nameFilter.setPlaceholder("Name");
		nameFilter.setClearButtonVisible(true);
		nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		nameFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
		.addFilterComponent(nameFilter);

		ageDivisionFilter.setPlaceholder("Age Division");
		ageDivisionFilter.setItems(AgeDivision.findAll());
		ageDivisionFilter.setItemLabelGenerator(AgeDivision::name);
		ageDivisionFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
		.addFilterComponent(ageDivisionFilter);
		crud.getCrudLayout()
		.addToolbarComponent(new Label(""));

		activeFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		activeFilter.setLabel("Active");
		activeFilter.setAriaLabel("Active Categories Only");
		crud.getCrudLayout()
		.addFilterComponent(activeFilter);

		Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
		clearFilters.addClickListener(event -> {
			ageDivisionFilter.clear();
		});
		crud.getCrudLayout()
		.addFilterComponent(clearFilters);
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
		return "Preparation - Categories";
	}
}
