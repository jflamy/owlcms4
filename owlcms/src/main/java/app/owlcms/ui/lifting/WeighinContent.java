/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.lifting;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.HtmlImport;
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
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
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
@HtmlImport("frontend://styles/shared-styles.html")
public class WeighinContent extends VerticalLayout 
		implements CrudListener<Athlete>, OwlcmsContent {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(WeighinContent.class);
	static {logger.setLevel(Level.INFO);}

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
		grid.addColumn("startNumber").setHeader(getTranslation("WeighinContent.37")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("lastName").setHeader(getTranslation("WeighinContent.36")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("firstName").setHeader(getTranslation("WeighinContent.35")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("team").setHeader(getTranslation("WeighinContent.34")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("ageDivision").setHeader(getTranslation("WeighinContent.33")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("category").setHeader(getTranslation("WeighinContent.32")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("group").setHeader(getTranslation("WeighinContent.31")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn(new NumberRenderer<Athlete>(Athlete::getBodyWeight, "%.2f", this.getLocale()),"bodyWeight").setHeader(getTranslation("WeighinContent.30")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		grid.addColumn("snatch1Declaration").setHeader(getTranslation("WeighinContent.29")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("cleanJerk1Declaration").setHeader(getTranslation("WeighinContent.27")); //$NON-NLS-1$ //$NON-NLS-2$

		grid.addColumn("eligibleForIndividualRanking").setHeader(getTranslation("WeighinContent.28"));	 //$NON-NLS-1$ //$NON-NLS-2$
		OwlcmsCrudGrid<Athlete> crudGrid = new OwlcmsCrudGrid<Athlete>(Athlete.class,
				new OwlcmsGridLayout(Athlete.class),
				crudFormFactory,
				grid);
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
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	private void createFormLayout(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		crudFormFactory.setVisibleProperties(
			"bodyWeight", //$NON-NLS-1$
			"category", //$NON-NLS-1$
			"snatch1Declaration", //$NON-NLS-1$
			"cleanJerk1Declaration", //$NON-NLS-1$
			"gender", //$NON-NLS-1$
			"group", //$NON-NLS-1$
			"lastName", //$NON-NLS-1$
			"firstName", //$NON-NLS-1$
			"team", //$NON-NLS-1$
			"fullBirthDate", //$NON-NLS-1$
			"ageDivision", //$NON-NLS-1$
			"qualifyingTotal", //$NON-NLS-1$
			"eligibleForIndividualRanking"); //$NON-NLS-1$
		crudFormFactory.setFieldCaptions(
			getTranslation("WeighinContent.26"), //$NON-NLS-1$
			getTranslation("WeighinContent.25"), //$NON-NLS-1$
			getTranslation("WeighinContent.24"), //$NON-NLS-1$
			getTranslation("WeighinContent.23"), //$NON-NLS-1$
			getTranslation("WeighinContent.22"), //$NON-NLS-1$
			getTranslation("WeighinContent.21"), //$NON-NLS-1$
			getTranslation("WeighinContent.20"), //$NON-NLS-1$
			getTranslation("WeighinContent.19"), //$NON-NLS-1$
			getTranslation("WeighinContent.18"), //$NON-NLS-1$
			getTranslation("WeighinContent.17"), //$NON-NLS-1$
			getTranslation("WeighinContent.16"), //$NON-NLS-1$
			getTranslation("WeighinContent.15"), //$NON-NLS-1$

			getTranslation("WeighinContent.14")); //$NON-NLS-1$
		crudFormFactory.setFieldProvider("gender", //$NON-NLS-1$
            new ComboBoxProvider<>(getTranslation("WeighinContent.13"), Arrays.asList(Gender.values()), new TextRenderer<>(Gender::name), Gender::name)); //$NON-NLS-1$
		crudFormFactory.setFieldProvider("group", //$NON-NLS-1$
            new ComboBoxProvider<>(getTranslation("WeighinContent.12"), GroupRepository.findAll(), new TextRenderer<>(Group::getName), Group::getName)); //$NON-NLS-1$
		crudFormFactory.setFieldProvider("category", //$NON-NLS-1$
            new ComboBoxProvider<>(getTranslation("WeighinContent.11"), CategoryRepository.findActive(), new TextRenderer<>(Category::getName), Category::getName)); //$NON-NLS-1$
		crudFormFactory.setFieldProvider("ageDivision", //$NON-NLS-1$
            new ComboBoxProvider<>(getTranslation("WeighinContent.10"), Arrays.asList(AgeDivision.values()), new TextRenderer<>(AgeDivision::name), AgeDivision::name)); //$NON-NLS-1$
		
		crudFormFactory.setFieldType("bodyWeight", BodyWeightField.class); //$NON-NLS-1$
		crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class); //$NON-NLS-1$
		
		crudFormFactory.setFieldCreationListener("bodyWeight", (e) -> {((BodyWeightField) e).focus();}); //$NON-NLS-1$
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
		return AthleteRepository
				.findFiltered(lastNameFilter.getValue(), groupFilter.getValue(), categoryFilter.getValue(),
					ageDivisionFilter.getValue(), weighedInFilter.getValue(), -1, -1);
	}
	
	/**
	 * The filters at the top of the crudGrid
	 * 
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	protected void defineFilters(GridCrud<Athlete> crud) {
		lastNameFilter.setPlaceholder(getTranslation("WeighinContent.0")); //$NON-NLS-1$
		lastNameFilter.setClearButtonVisible(true);
		lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		lastNameFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(lastNameFilter);

		ageDivisionFilter.setPlaceholder(getTranslation("WeighinContent.1")); //$NON-NLS-1$
		ageDivisionFilter.setItems(AgeDivision.findAll());
		ageDivisionFilter.setItemLabelGenerator(AgeDivision::name);
		ageDivisionFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(ageDivisionFilter);

		categoryFilter.setPlaceholder(getTranslation("WeighinContent.2")); //$NON-NLS-1$
		categoryFilter.setItems(CategoryRepository.findActive());
		categoryFilter.setItemLabelGenerator(Category::getName);
		categoryFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(categoryFilter);
		
		groupFilter.setPlaceholder(getTranslation("WeighinContent.3")); //$NON-NLS-1$
		groupFilter.setItems(GroupRepository.findAll());
		groupFilter.setItemLabelGenerator(Group::getName);
		groupFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		// hide because the top bar has it
		groupFilter.getStyle().set(getTranslation("WeighinContent.4"), getTranslation("WeighinContent.5")); //$NON-NLS-1$ //$NON-NLS-2$
		
		crud.getCrudLayout()
			.addFilterComponent(groupFilter);
		
		weighedInFilter.setPlaceholder(getTranslation("WeighinContent.6")); //$NON-NLS-1$
		weighedInFilter.setItems(Boolean.TRUE,Boolean.FALSE);
		weighedInFilter.setItemLabelGenerator((i) -> {return i ? getTranslation("WeighinContent.7") : getTranslation("WeighinContent.8");}); //$NON-NLS-1$ //$NON-NLS-2$
		weighedInFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(weighedInFilter);
		
		Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
		clearFilters.addClickListener(event -> {
			lastNameFilter.clear();
			ageDivisionFilter.clear();
			categoryFilter.clear();
			//groupFilter.clear();
			weighedInFilter.clear();
		});
		crud.getCrudLayout()
			.addFilterComponent(clearFilters);
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
		return getTranslation("WeighinContent.9"); //$NON-NLS-1$
	}
}
