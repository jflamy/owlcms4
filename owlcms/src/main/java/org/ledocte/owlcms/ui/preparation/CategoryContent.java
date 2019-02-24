/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.preparation;

import java.util.Collection;

import org.ledocte.owlcms.data.category.AgeDivision;
import org.ledocte.owlcms.data.category.Category;
import org.ledocte.owlcms.data.category.CategoryRepository;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudLayout;
import org.ledocte.owlcms.ui.crudui.OwlcmsGridCrud;
import org.ledocte.owlcms.ui.home.ContentWrapping;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Route;

/**
 * The Class CategoryContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/categories", layout = CategoryLayout.class)
public class CategoryContent extends VerticalLayout
		implements CrudListener<Category>, ContentWrapping {

	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();

	/**
	 * Instantiates a new category content.
	 */
	public CategoryContent() {
		GridCrud<Category> crud = getFilteringGridCrud();
		fillHW(crud, this);
	}

	private GridCrud<Category> getFilteringGridCrud() {
		OwlcmsCrudFormFactory<Category> crudFormFactory = new OwlcmsCrudFormFactory<Category>(Category.class);
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

		Grid<Category> grid = new Grid<Category>(Category.class, false);
		grid.setColumns("name", "ageDivision", "gender", "minimumWeight", "maximumWeight", "active");
		grid.getColumnByKey("name")
			.setHeader("Name");
		grid.getColumnByKey("ageDivision")
			.setHeader("Age Division");
		grid.getColumnByKey("gender")
			.setHeader("Gender");

		GridCrud<Category> crud = new OwlcmsGridCrud<Category>(Category.class,
				new OwlcmsCrudLayout(Category.class),
				crudFormFactory,
				grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);

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

		Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
		clearFilters.addClickListener(event -> {
			ageDivisionFilter.clear();
		});
		crud.getCrudLayout()
			.addFilterComponent(clearFilters);

		crud.setFindAllOperation(
			DataProvider.fromCallbacks(
				query -> CategoryRepository
					.findByAgeDivision(ageDivisionFilter.getValue(), query.getOffset(), query.getLimit())
					.stream(),
				query -> CategoryRepository.countByAgeDivision(ageDivisionFilter.getValue())));
		return crud;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Category add(Category Category) {
		CategoryRepository.save(Category);
		return Category;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Category update(Category Category) {
		if (Category.getId()
			.equals(5l)) {
			throw new RuntimeException("A simulated error has occurred");
		}
		return CategoryRepository.save(Category);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Category Category) {
		CategoryRepository.delete(Category);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Category> findAll() {
		return CategoryRepository.findAll();
	}

}
