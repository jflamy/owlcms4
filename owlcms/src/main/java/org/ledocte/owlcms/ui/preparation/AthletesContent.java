/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.preparation;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.AthleteRepository;
import org.ledocte.owlcms.data.athlete.Gender;
import org.ledocte.owlcms.data.category.AgeDivision;
import org.ledocte.owlcms.data.category.Category;
import org.ledocte.owlcms.data.category.CategoryRepository;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.group.GroupRepository;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudLayout;
import org.ledocte.owlcms.ui.crudui.OwlcmsGridCrud;
import org.ledocte.owlcms.ui.home.ContentWrapping;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

/**
 * Class AthleteContent
 * 
 * Defines the toolbar and the table for editing data for the athletes.
 * 
 */
@SuppressWarnings("serial")
@Route(value = "preparation/athletes", layout = AthletesLayout.class)
public class AthletesContent extends VerticalLayout
		implements CrudListener<Athlete>, ContentWrapping {

	private TextField lastNameFilter = new TextField();
	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();
	private Checkbox weighedInFilter = new Checkbox();

	/**
	 * Instantiates a new athlete editing table.
	 */
	public AthletesContent() {
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
		GridCrud<Athlete> crud = createGrid(crudFormFactory);		
		defineFilters(crud);
		defineQueries(crud);
		fillHW(crud, this);
	}

	protected void defineQueries(GridCrud<Athlete> crud) {
		crud.setFindAllOperation(
			DataProvider.fromCallbacks(
				query -> AthleteRepository
					.findFiltered(lastNameFilter.getValue(), groupFilter.getValue(), categoryFilter.getValue(),
						ageDivisionFilter.getValue(), null, query.getOffset(), query.getLimit())
					.stream(),
				query -> AthleteRepository.countFiltered(lastNameFilter.getValue(), groupFilter.getValue(),
					categoryFilter.getValue(), ageDivisionFilter.getValue(), null)));
	}

	protected GridCrud<Athlete> createGrid(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		Grid<Athlete> grid = new Grid<Athlete>(Athlete.class, false);
		grid.addColumn("lastName").setHeader("Last Name");
		grid.addColumn("firstName").setHeader("First Name");
		grid.addColumn("team").setHeader("Team");
		grid.addColumn("ageDivision").setHeader("Age Division");
		grid.addColumn("category").setHeader("Category");
		grid.addColumn(new NumberRenderer<Athlete>(Athlete::getBodyWeight, "%.2f", this.getLocale())).setHeader("Body Weight");
		grid.addColumn("group").setHeader("Group");
		grid.addColumn("invited").setHeader("Invited");	
		GridCrud<Athlete> crud = new OwlcmsGridCrud<Athlete>(Athlete.class,
				new OwlcmsCrudLayout(Athlete.class),
				crudFormFactory,
				grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		return crud;
	}

	/**
	 * Define the form used to edit a given athlete
	 * 
	 * @return the form factory that will create the actual form on demand
	 */
	protected OwlcmsCrudFormFactory<Athlete> createFormFactory() {
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = new OwlcmsCrudFormFactory<Athlete>(Athlete.class);
		crudFormFactory.setVisibleProperties("lastName",
			"firstName",
			"gender",
			"fullBirthDate",
			"ageDivision",
			"category",
			"group",
			"bodyWeight",
			"snatch1Declaration",
			"cleanJerk1Declaration");
		crudFormFactory.setFieldCaptions("Last Name",
			"First Name",
			"Gender",
			"Birth Date",
			"Age Division",
			"Category",
			"Group",
			"Body Weight",
			"Snatch Declaration",
			"Clean&Jerk Declaration");
		crudFormFactory.setFieldProvider("gender",
            new ComboBoxProvider<>("Gender", Arrays.asList(Gender.values()), new TextRenderer<>(Gender::name), Gender::name));
		crudFormFactory.setFieldProvider("group",
            new ComboBoxProvider<>("Group", GroupRepository.findAll(), new TextRenderer<>(Group::getName), Group::getName));
		crudFormFactory.setFieldProvider("category",
            new ComboBoxProvider<>("Category", CategoryRepository.findAll(), new TextRenderer<>(Category::getName), Category::getName));
		crudFormFactory.setFieldProvider("ageDivision",
            new ComboBoxProvider<>("AgeDivision", Arrays.asList(AgeDivision.values()), new TextRenderer<>(AgeDivision::name), AgeDivision::name));
		
		crudFormFactory.setFieldType("bodyWeight", BodyWeightField.class);
		
		crudFormFactory.setFieldProvider("fullBirthDate", () -> {
            DatePicker datePicker = new DatePicker();
            //datePicker.addValueChangeListener((e) -> {System.err.println(e.getValue());});
            datePicker.setMax(LocalDate.now());
            return datePicker;
        });
		return crudFormFactory;
	}

	protected void defineFilters(GridCrud<Athlete> crud) {
		lastNameFilter.setPlaceholder("Last name");
		lastNameFilter.setClearButtonVisible(true);
		lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		lastNameFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(lastNameFilter);

		ageDivisionFilter.setPlaceholder("Age Division");
		ageDivisionFilter.setItems(AgeDivision.findAll());
		ageDivisionFilter.setItemLabelGenerator(AgeDivision::name);
		ageDivisionFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(ageDivisionFilter);

		categoryFilter.setPlaceholder("Category");
		categoryFilter.setItems(CategoryRepository.findAll());
		categoryFilter.setItemLabelGenerator(Category::getName);
		categoryFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(categoryFilter);
		
		groupFilter.setPlaceholder("Group");
		groupFilter.setItems(GroupRepository.findAll());
		groupFilter.setItemLabelGenerator(Group::getName);
		groupFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		crud.getCrudLayout()
			.addFilterComponent(groupFilter);
		
		weighedInFilter.addValueChangeListener(e -> {
			crud.refreshGrid();
		});
		weighedInFilter.setLabel("Weighed-in");
		crud.getCrudLayout()
			.addFilterComponent(weighedInFilter);
		
		Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
		clearFilters.addClickListener(event -> {
			lastNameFilter.clear();
			ageDivisionFilter.clear();
			categoryFilter.clear();
			groupFilter.clear();
			weighedInFilter.clear();
		});
		crud.getCrudLayout()
			.addFilterComponent(clearFilters);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete Athlete) {
		AthleteRepository.save(Athlete);
		return Athlete;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete Athlete) {
		if (Athlete.getId()
			.equals(5l)) {
			throw new RuntimeException("A simulated error has occurred");
		}
		return AthleteRepository.save(Athlete);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {
		AthleteRepository.delete(Athlete);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		return AthleteRepository.findAll();
	}

}
