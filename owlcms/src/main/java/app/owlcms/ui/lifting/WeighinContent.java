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
import com.vaadin.flow.router.HasDynamicTitle;
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
import app.owlcms.ui.shared.AppLayoutAware;
import app.owlcms.ui.shared.AthleteRegistrationFormFactory;
import app.owlcms.ui.shared.ContentWrapping;
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
		implements CrudListener<Athlete>, ContentWrapping, AppLayoutAware, HasDynamicTitle {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(WeighinContent.class);
	static {logger.setLevel(Level.INFO);}

	private TextField lastNameFilter = new TextField();
	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
	private ComboBox<Category> categoryFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();

	private ComboBox<Boolean> weighedInFilter = new ComboBox<>();
	private OwlcmsCrudGrid<Athlete> crudGrid;
	private OwlcmsRouterLayout routerLayout;

	/**
	 * Instantiates the athlete crudGrid
	 */
	public WeighinContent() {
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
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
		grid.addColumn("startNumber").setHeader("Start#");
		grid.addColumn("lastName").setHeader("Last Name");
		grid.addColumn("firstName").setHeader("First Name");
		grid.addColumn("team").setHeader("Team");
		grid.addColumn("ageDivision").setHeader("Age Division");
		grid.addColumn("category").setHeader("Category");
		grid.addColumn(new NumberRenderer<Athlete>(Athlete::getBodyWeight, "%.2f", this.getLocale()),"bodyWeight").setHeader("Body Weight");
		grid.addColumn("group").setHeader("Group");
		grid.addColumn("eligibleForIndividualRanking").setHeader("Eligible");	
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
		OwlcmsCrudFormFactory<Athlete> athleteEditingFormFactory = createAthleteEditingFormFactory();
		createFormLayout(athleteEditingFormFactory);
		return athleteEditingFormFactory;
	}

	/**
	 * The content and ordering of the editing form
	 * 
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	private void createFormLayout(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		crudFormFactory.setVisibleProperties("lastName",
			"firstName",
			"gender",
			"team",
			"fullBirthDate",
			"ageDivision",
			"category",
			"group",
			"qualifyingTotal",
			"bodyWeight",
			"snatch1Declaration",
			"cleanJerk1Declaration",
			"eligibleForIndividualRanking");
		crudFormFactory.setFieldCaptions("Last Name",
			"First Name",
			"Gender",
			"Team",
			"Birth Date",
			"Age Division",
			"Category",
			"Group",
			"Entry Total",
			"Body Weight",
			"Snatch Declaration",
			"Clean&Jerk Declaration",
			"Eligible for individual ranking?");
		crudFormFactory.setFieldProvider("gender",
            new ComboBoxProvider<>("Gender", Arrays.asList(Gender.values()), new TextRenderer<>(Gender::name), Gender::name));
		crudFormFactory.setFieldProvider("group",
            new ComboBoxProvider<>("Group", GroupRepository.findAll(), new TextRenderer<>(Group::getName), Group::getName));
		crudFormFactory.setFieldProvider("category",
            new ComboBoxProvider<>("Category", CategoryRepository.findActive(), new TextRenderer<>(Category::getName), Category::getName));
		crudFormFactory.setFieldProvider("ageDivision",
            new ComboBoxProvider<>("AgeDivision", Arrays.asList(AgeDivision.values()), new TextRenderer<>(AgeDivision::name), AgeDivision::name));
		
		crudFormFactory.setFieldType("bodyWeight", BodyWeightField.class);
		crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class);
	}

	/**
	 * Create the actual form generator with all the conversions and validations required
	 * 
	 * @return the actual factory with field binding and validations
	 */
	private OwlcmsCrudFormFactory<Athlete> createAthleteEditingFormFactory() {
		return new AthleteRegistrationFormFactory(Athlete.class);
	}
//	private OwlcmsCrudFormFactory<Athlete> createAthleteEditingFormFactory() {
//		return new OwlcmsCrudFormFactory<Athlete>(Athlete.class) {
//			@Override
//			public String buildCaption(CrudOperation operation, Athlete a) {
//				if (a.getLastName() == null && a.getFirstName() == null) return null;
//				// If null, CrudLayout.showForm will build its own, for backward compatibility
//				return a.getFullId();
//			}
//			
//			@SuppressWarnings({ "unchecked", "rawtypes" })
//			@Override
//			protected void bindField(HasValue field, String property, Class<?> propertyType) {
//				Binder.BindingBuilder bindingBuilder = binder.forField(field);
//				if ("fullBirthDate".equals(property)) {
//					fullBirthDateValidation(bindingBuilder);
//					bindingBuilder.bind(property);
//				} else if ("bodyWeight".equals(property)) {
//					bodyWeightValidation(bindingBuilder);
//					bindingBuilder.bind(property);
//				} else if ("category".equals(property)) {
//					categoryValidation(bindingBuilder);
//					bindingBuilder.bind(property);
//				} else if (property.endsWith("Declaration")) {
//					declarationValidation(bindingBuilder);
//					bindingBuilder.bind(property);
//				} else {
//					super.bindField(field, property, propertyType);
//				}
//			}
//
//			@SuppressWarnings({ "rawtypes"})
//			private void declarationValidation(BindingBuilder bindingBuilder) {
//				TextField declField = (TextField)bindingBuilder.getField();
//				declField.setPattern("^[-]{0,1}\\d*$");
//				declField.setPreventInvalidInput(true);
//			}
//
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			protected void fullBirthDateValidation(Binder.BindingBuilder bindingBuilder) {
//				Validator<LocalDate> fv = ((LocalDateField)bindingBuilder.getField()).formatValidation(OwlcmsSession.getLocale());
//				bindingBuilder.withValidator(fv);
//				
//				Validator<LocalDate> v = Validator.from(
//					ld -> {
//						if (ld == null)
//							return true;
//						return (ld.compareTo(LocalDate.now()) <= 0);
//					},
//					"Birth date cannot be in the future");
//				bindingBuilder.withValidator(v);
//			}
//
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			protected void bodyWeightValidation(Binder.BindingBuilder bindingBuilder) {
//				Validator<Double> fv = ((BodyWeightField)bindingBuilder.getField()).formatValidation(OwlcmsSession.getLocale());
//				bindingBuilder.withValidator(fv);
//				
//				Validator<Double> v1 = new DoubleRangeValidator(
//						"Weight should be between 0 and 350kg", 0.0D, 350.0D);
//				// check wrt body category
//				Validator<Double> v2 = Validator
//					.from((weight) -> {
//						if (weight == null) return true;
//						// tell the category drop down to signal inconsistent selection
//						Binding<Athlete, ?> categoryBinding = binder.getBinding("category").get();
//						categoryBinding.validate(true).isError();
//						return true;
//					}, "Body Weight is outside of selected category");
//				bindingBuilder.withValidator(v1);
//				bindingBuilder.withValidator(v2);
//			}
//
//			@SuppressWarnings({ "rawtypes", "unchecked" })
//			protected void categoryValidation(Binder.BindingBuilder bindingBuilder) {
//				// check that category is consistent with body weight
//				Validator<Category> v = Validator
//					.from((category) -> {
//						try {
//							Binding<Athlete, ?> bwBinding = binder.getBinding("bodyWeight").get();
//							Double bw = (Double) bwBinding.getField().getValue();
//							if (bw == null) {
//								// no body weight - no contradiction
//								return true;
//							}
//							Double min = category.getMinimumWeight();
//							Double max = category.getMaximumWeight();
//							logger.debug(
//								"comparing {} ]{},{}] with body weight {}", category.getName(), min, max, bw);
//							return (bw > min && bw <= max);
//						} catch (Exception e) {
//							e.printStackTrace();
//						}
//						return true;
//					},
//						"Category does not match body weight");
//				bindingBuilder.withValidator(v);
//			}
//		};
//	}


	/**
	 * The plus button on the toolbar triggers an add
	 * 
	 * This method is called when the pop-up is closed.
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete Athlete) {
		AthleteRepository.save(Athlete);
		return Athlete;
	}

	/**
	 * The pencil button on the toolbar triggers an edit.
	 * 
	 * This method is called when the pop-up is closed with Update
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete Athlete) {
		return AthleteRepository.save(Athlete);
	}

	/**
	 * The delete button on the toolbar triggers this method
	 * 
	 * (or the one in the form)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {
		AthleteRepository.delete(Athlete);
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
		categoryFilter.setItems(CategoryRepository.findActive());
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
		// hide because the top bar has it
		groupFilter.getStyle().set("display", "none");
		
		crud.getCrudLayout()
			.addFilterComponent(groupFilter);
		
		weighedInFilter.setPlaceholder("Weighed-In?");
		weighedInFilter.setItems(Boolean.TRUE,Boolean.FALSE);
		weighedInFilter.setItemLabelGenerator((i) -> {return i ? "Weighed" : "Not weighed";});
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
		return "Weigh-in";
	}
}
