/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.AttachEvent;
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
import app.owlcms.data.category.MastersAgeGroup;
import app.owlcms.data.competition.Competition;
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
@Route(value = "preparation/athletes", layout = RegistrationLayout.class)
@HtmlImport("frontend://styles/shared-styles.html")
public class RegistrationContent extends VerticalLayout
		implements CrudListener<Athlete>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	private TextField lastNameFilter = new TextField();
	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();
	private ComboBox<Boolean> weighedInFilter = new ComboBox<>();
	private ComboBox<String> ageGroupFilter = new ComboBox<>();
	private OwlcmsRouterLayout routerLayout;
	private OwlcmsCrudGrid<Athlete> crudGrid;
	private OwlcmsCrudFormFactory<Athlete> crudFormFactory;
	

	/**
	 * Instantiates the athlete crudGrid
	 */
	public RegistrationContent() {
		crudFormFactory = createFormFactory();
		crudGrid = createCrudGrid(crudFormFactory);
		defineFilters(crudGrid);
		fillHW(crudGrid, this);
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected OwlcmsCrudGrid<Athlete> createCrudGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		Grid<Athlete> grid = new Grid<>(Athlete.class, false);
		grid.addColumn("lotNumber").setHeader(getTranslation("Lot")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("lastName").setHeader(getTranslation("LastName")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("firstName").setHeader(getTranslation("FirstName")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("team").setHeader(getTranslation("Team")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("yearOfBirth").setHeader(getTranslation("BirthDate")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("gender").setHeader(getTranslation("Gender")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("ageDivision").setHeader(getTranslation("AgeDivision")); //$NON-NLS-1$ //$NON-NLS-2$
		if (Competition.getCurrent().isMasters()) {
			grid.addColumn("mastersAgeGroup").setHeader(getTranslation("AgeGroup")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		grid.addColumn("category").setHeader(getTranslation("Category")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn(
			new NumberRenderer<>(Athlete::getBodyWeight, "%.2f", this.getLocale()),"bodyWeight") //$NON-NLS-1$ //$NON-NLS-2$
			.setHeader(getTranslation("BodyWeight")); //$NON-NLS-1$
		grid.addColumn("group").setHeader(getTranslation("Group")); //$NON-NLS-1$ //$NON-NLS-2$
		grid.addColumn("eligibleForIndividualRanking").setHeader(getTranslation("Eligible")); //$NON-NLS-1$ //$NON-NLS-2$
		OwlcmsCrudGrid<Athlete> crud = new OwlcmsCrudGrid<>(
				Athlete.class,
				new OwlcmsGridLayout(Athlete.class),
				crudFormFactory,
				grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		return crud;
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
		List<String> props = new LinkedList<>();
		List<String> captions = new LinkedList<>();
		
		props.add("lastName"); captions.add(getTranslation("LastName")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("firstName"); captions.add(getTranslation("FirstName")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("gender"); captions.add(getTranslation("Gender")); //$NON-NLS-1$ //$NON-NLS-2$

		props.add("team"); captions.add(getTranslation("Team")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("fullBirthDate"); captions.add(getTranslation("BirthDate_yyyy")); //$NON-NLS-1$ //$NON-NLS-2$
		if (Competition.getCurrent().isMasters()) {
			props.add("mastersAgeGroup"); captions.add(getTranslation("AgeGroup")); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			props.add("ageDivision"); captions.add(getTranslation("AgeDivision")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		props.add("category"); captions.add(getTranslation("Category")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("group"); captions.add(getTranslation("Group")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("qualifyingTotal"); captions.add(getTranslation("EntryTotal")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("bodyWeight"); captions.add(getTranslation("BodyWeight")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("snatch1Declaration"); captions.add(getTranslation("SnatchDecl_")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("cleanJerk1Declaration"); captions.add(getTranslation("C_and_J_decl")); //$NON-NLS-1$ //$NON-NLS-2$
		props.add("eligibleForIndividualRanking"); captions.add(getTranslation("Eligible for Individual Ranking?"));  //$NON-NLS-1$ //$NON-NLS-2$
		props.add("lotNumber"); captions.add(getTranslation("Lot")); //$NON-NLS-1$ //$NON-NLS-2$
		crudFormFactory.setVisibleProperties((String[]) props.toArray(new String[0]));
		crudFormFactory.setFieldCaptions((String[]) captions.toArray(new String[0]));
		
		crudFormFactory.setFieldProvider("gender", //$NON-NLS-1$
			new ComboBoxProvider<>(
					getTranslation("Gender"), Arrays.asList(Gender.values()), new TextRenderer<>(Gender::name), Gender::name)); //$NON-NLS-1$
		crudFormFactory.setFieldProvider("group", //$NON-NLS-1$
			new ComboBoxProvider<>(
					getTranslation("Group"), GroupRepository.findAll(), new TextRenderer<>(Group::getName), Group::getName)); //$NON-NLS-1$
		crudFormFactory.setFieldProvider("category", //$NON-NLS-1$
			new ComboBoxProvider<>(
					getTranslation("Category"), CategoryRepository.findActive(), new TextRenderer<>(Category::getName), //$NON-NLS-1$
					Category::getName));
		crudFormFactory.setFieldProvider("ageDivision", //$NON-NLS-1$
			new ComboBoxProvider<>(
					getTranslation("AgeDivision"), Arrays.asList(AgeDivision.values()), new TextRenderer<>(AgeDivision::name), //$NON-NLS-1$
					AgeDivision::name));

		crudFormFactory.setFieldType("bodyWeight", BodyWeightField.class); //$NON-NLS-1$
		crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class); //$NON-NLS-1$
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
	 * The refresh button on the toolbar; also called by refreshGrid when the group is changed.
	 *
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		List<Athlete> all = AthleteRepository
			.findFiltered(lastNameFilter.getValue(), groupFilter.getValue(), categoryFilter.getValue(),
				ageDivisionFilter.getValue(), weighedInFilter.getValue(), -1, -1);
		return doExtraFiltering(all);
	}

	public Collection<Athlete> doFindAll(EntityManager em) {
		List<Athlete> all = AthleteRepository.doFindFiltered(em, lastNameFilter.getValue(), groupFilter.getValue(),
				categoryFilter.getValue(), ageDivisionFilter.getValue(), weighedInFilter.getValue(), -1, -1);
		return doExtraFiltering(all);
	}

	private Collection<Athlete> doExtraFiltering(List<Athlete> all) {
		String filterValue = ageGroupFilter != null ? ageGroupFilter.getValue() : null;
		if (filterValue == null) {
			return all;
		} else {
			List<Athlete> some = all.stream().filter(a -> a.getMastersAgeGroup().startsWith(filterValue))
					.collect(Collectors.toList());
			return some;
		}
	}

	/**
	 * The filters at the top of the crudGrid
	 *
	 * @param crudGrid the crudGrid that will be filtered.
	 */
	protected void defineFilters(OwlcmsCrudGrid<Athlete> crudGrid) {
		lastNameFilter.setPlaceholder(getTranslation("LastName")); //$NON-NLS-1$
		lastNameFilter.setClearButtonVisible(true);
		lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		lastNameFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		lastNameFilter.setWidth("10em"); //$NON-NLS-1$
		crudGrid.getCrudLayout().addFilterComponent(lastNameFilter);

		ageDivisionFilter.setPlaceholder(getTranslation("AgeDivision")); //$NON-NLS-1$
		ageDivisionFilter.setItems(AgeDivision.findAll());
		ageDivisionFilter.setItemLabelGenerator(AgeDivision::name);
		ageDivisionFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		lastNameFilter.setWidth("10em"); //$NON-NLS-1$
		crudGrid.getCrudLayout().addFilterComponent(ageDivisionFilter);
		
		if (Competition.getCurrent().isMasters()) {
			ageGroupFilter.setPlaceholder(getTranslation("AgeGroup")); //$NON-NLS-1$
			ageGroupFilter.setItems(MastersAgeGroup.findAllStrings());
//		ageGroupFilter.setItemLabelGenerator(AgeDivision::name);
			ageGroupFilter.addValueChangeListener(e -> {
				crudGrid.refreshGrid();
			});
			ageGroupFilter.setWidth("10em"); //$NON-NLS-1$
			crudGrid.getCrudLayout().addFilterComponent(ageGroupFilter);
		}

		categoryFilter.setPlaceholder(getTranslation("Category")); //$NON-NLS-1$
		categoryFilter.setItems(CategoryRepository.findActive());
		categoryFilter.setItemLabelGenerator(Category::getName);
		categoryFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		categoryFilter.setWidth("10em"); //$NON-NLS-1$
		crudGrid.getCrudLayout().addFilterComponent(categoryFilter);

		groupFilter.setPlaceholder(getTranslation("Group")); //$NON-NLS-1$
		groupFilter.setItems(GroupRepository.findAll());
		groupFilter.setItemLabelGenerator(Group::getName);
		groupFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		groupFilter.setWidth("10em"); //$NON-NLS-1$
		crudGrid.getCrudLayout().addFilterComponent(groupFilter);

		weighedInFilter.setPlaceholder(getTranslation("Weighed_in_p")); //$NON-NLS-1$
		weighedInFilter.setItems(Boolean.TRUE,Boolean.FALSE);
		weighedInFilter.setItemLabelGenerator((i) -> {return i ? getTranslation("Weighed") : getTranslation("Not_weighed");}); //$NON-NLS-1$ //$NON-NLS-2$
		weighedInFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		weighedInFilter.setWidth("10em"); //$NON-NLS-1$
		crudGrid.getCrudLayout().addFilterComponent(weighedInFilter);

		Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
		clearFilters.addClickListener(event -> {
			lastNameFilter.clear();
			ageDivisionFilter.clear();
			categoryFilter.clear();
			groupFilter.clear();
			weighedInFilter.clear();
		});
		lastNameFilter.setWidth("10em"); //$NON-NLS-1$
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
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
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		getRouterLayout().closeDrawer();
	}

	public void refreshCrudGrid() {
		crudGrid.refreshGrid();
	}
	
	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Preparation_Registration"); //$NON-NLS-1$
	}
}
