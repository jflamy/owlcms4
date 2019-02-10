package org.ledocte.owlcms.ui.preparation;

import java.util.Collection;

import org.ledocte.owlcms.data.AgeDivision;
import org.ledocte.owlcms.data.category.Category;
import org.ledocte.owlcms.data.category.CategoryRepository;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudLayout;
import org.ledocte.owlcms.ui.crudui.OwlcmsGridCrud;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.CrudFormFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Route;

/**
 * @author Alejandro Duarte
 */
@SuppressWarnings("serial")
@Route(value = "preparation/categories", layout = CategoryLayout.class)
public class CategoryContent extends VerticalLayout implements CrudListener<Category> { // or implements
																						// LazyCrudListener<Category>

	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();

	public CategoryContent() {
		setSizeFull();
		GridCrud<Category> crud = getFilteringGrid();
		add(crud);
	}

	private GridCrud<Category> getFilteringGrid() {
		GridCrud<Category> crud = new OwlcmsGridCrud<Category>(Category.class, new OwlcmsCrudLayout(Category.class));
		crud.setCrudListener(this);

		CrudFormFactory<Category> crudFormFactory = crud.getCrudFormFactory();
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

		Grid<Category> grid = crud.getGrid();
		grid.setColumns("name", "ageDivision", "gender", "minimumWeight", "maximumWeight", "active");
		grid.getColumnByKey("name")
			.setHeader("Name");
		grid.getColumnByKey("ageDivision")
			.setHeader("Age Division");
		grid.getColumnByKey("gender")
			.setHeader("Gender");
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

	@Override
	public Category add(Category Category) {
		CategoryRepository.save(Category);
		return Category;
	}

	@Override
	public Category update(Category Category) {
		if (Category.getId()
			.equals(5l)) {
			throw new RuntimeException("A simulated error has occurred");
		}
		return CategoryRepository.save(Category);
	}

	@Override
	public void delete(Category Category) {
		CategoryRepository.delete(Category);
	}

	@Override
	public Collection<Category> findAll() {
		return CategoryRepository.findAll();
	}

	/*
	 * if this implements LazyCrudListener<Category>:
	 * 
	 * @Override public DataProvider<Category, Void> getDataProvider() { return
	 * DataProvider.fromCallbacks( query ->
	 * CategoryRepository.findByNameLike(nameFilter.getValue(),
	 * groupFilter.getValue(), query.getOffset(), query.getLimit()).stream(), query
	 * -> CategoryRepository.countByNameLike(nameFilter.getValue(),
	 * groupFilter.getValue())); }
	 */

}
