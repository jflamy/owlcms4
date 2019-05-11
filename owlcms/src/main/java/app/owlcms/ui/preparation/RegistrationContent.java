/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

import app.owlcms.components.fields.BodyWeightField;
import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.MastersAgeGroup;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.shared.AppLayoutAware;
import app.owlcms.ui.shared.ContentWrapping;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.LoggerUtils;
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
		implements CrudListener<Athlete>, ContentWrapping, AppLayoutAware {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationContent.class);
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
	

	/**
	 * Instantiates the athlete crudGrid
	 */
	public RegistrationContent() {
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = createFormFactory();
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
		grid.addColumn("lotNumber").setHeader("Lot");
		grid.addColumn("lastName").setHeader("Last Name");
		grid.addColumn("firstName").setHeader("First Name");
		grid.addColumn("team").setHeader("Team");
		grid.addColumn("yearOfBirth").setHeader("Birth");
		grid.addColumn("gender").setHeader("Gender");
		grid.addColumn("ageDivision").setHeader("Age Division");
		if (Competition.getCurrent().isMasters()) {
			grid.addColumn("mastersAgeGroup").setHeader("Age Group");
		}
		grid.addColumn("category").setHeader("Category");
		grid.addColumn(
			new NumberRenderer<>(Athlete::getBodyWeight, "%.2f", this.getLocale()))
			.setHeader("Body Weight");
		grid.addColumn("group").setHeader("Group");
		grid.addColumn("invited").setHeader("Invited");
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
		List<String> props = new LinkedList<>();
		List<String> captions = new LinkedList<>();
		
		props.add("firstName"); captions.add("First Name");
		props.add("gender"); captions.add("Gender");

		props.add("team"); captions.add("Team");
		props.add("fullBirthDate"); captions.add("Birth Date (yyyy-mm-dd)");
		if (Competition.getCurrent().isMasters()) {
			props.add("mastersAgeGroup"); captions.add("Age Group");
		} else {
			props.add("ageDivision"); captions.add("Age Division");
		}
		props.add("category"); captions.add("Category");
		props.add("group"); captions.add("Group");
		props.add("qualifyingTotal"); captions.add("Entry Total");
		props.add("bodyWeight"); captions.add("Body Weight");
		props.add("snatch1Declaration"); captions.add("Snatch Decl.");
		props.add("cleanJerk1Declaration"); captions.add("C&J Decl.");
		props.add("invited"); captions.add("Invited?"); 
		props.add("lotNumber"); captions.add("Lot");
		crudFormFactory.setVisibleProperties((String[]) props.toArray(new String[0]));
		crudFormFactory.setFieldCaptions((String[]) captions.toArray(new String[0]));
		
		crudFormFactory.setFieldProvider("gender",
			new ComboBoxProvider<>(
					"Gender", Arrays.asList(Gender.values()), new TextRenderer<>(Gender::name), Gender::name));
		crudFormFactory.setFieldProvider("group",
			new ComboBoxProvider<>(
					"Group", GroupRepository.findAll(), new TextRenderer<>(Group::getName), Group::getName));
		crudFormFactory.setFieldProvider("category",
			new ComboBoxProvider<>(
					"Category", CategoryRepository.findActive(), new TextRenderer<>(Category::getName),
					Category::getName));
		crudFormFactory.setFieldProvider("ageDivision",
			new ComboBoxProvider<>(
					"AgeDivision", Arrays.asList(AgeDivision.values()), new TextRenderer<>(AgeDivision::name),
					AgeDivision::name));

		crudFormFactory.setFieldType("bodyWeight", BodyWeightField.class);
		crudFormFactory.setFieldType("fullBirthDate", LocalDateField.class);
	}

	/**
	 * Create the conversions and validations required
	 *
	 * @return the factory to create a form with field binding and validations
	 */
	private OwlcmsCrudFormFactory<Athlete> createAthleteEditingFormFactory() {
		return new OwlcmsCrudFormFactory<Athlete>(Athlete.class) {
			
			private Athlete editedAthlete = null;
			private boolean genderCatOk = false;
			private boolean catGenderOk = false;

			/**
			 * Add bean-level validations
			 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildBinder(org.vaadin.crudui.crud.CrudOperation, java.lang.Object)
			 */
			@Override
			protected Binder<Athlete> buildBinder(CrudOperation operation, Athlete domainObject) {
				editedAthlete  = domainObject;
				binder = super.buildBinder(operation, domainObject);
				binder.withValidator(ValidationUtils.checkUsing((a) -> a.validateStartingTotalsRule(), ""));
				updateErrorLabelFromBeanValidationErrors();
				return binder;
			}
			
			/** 
			 * Change the caption to show the current athlete name and group
			 * 
			 * @see
			 * org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory#buildCaption(org.vaadin.crudui.
			 * crudGrid.CrudOperation, java.lang.Object) 
			 */
			@Override
			public String buildCaption(CrudOperation operation, Athlete a) {
				if (a.getLastName() == null && a.getFirstName() == null)
					return null;
				// If null, CrudLayout.showForm will build its own, for backward compatibility
				return a.getFullId();
			}

			/**
			 * Add the field-level validations
			 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#bindField(com.vaadin.flow.component.HasValue, java.lang.String, java.lang.Class)
			 */
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			protected void bindField(HasValue field, String property, Class<?> propertyType) {
				Binder.BindingBuilder bindingBuilder = binder.forField(field);
				
				if ("bodyWeight".equals(property)) {
					bodyWeightValidation(bindingBuilder, ((BodyWeightField) field).isRequired());
					bindingBuilder.bind(property);
				} else if ("fullBirthDate".equals(property)) {
					fullBirthDateValidation(bindingBuilder);
					HasValue<?, ?> bdateField = bindingBuilder.getField();
					bdateField.addValueChangeListener((e) -> {
						LocalDate date = (LocalDate) e.getValue();
						HasValue<?, ?> genderField = binder.getBinding("gender").get().getField();
						Optional<Binding<Athlete, ?>> magBinding = binder.getBinding("mastersAgeGroup");
						if (magBinding.isPresent()) {
							HasValue<?, String> ageGroupField = (HasValue<?, String>) magBinding.get().getField();
							Gender gender = (Gender) genderField.getValue();
							if (gender != null && date != null) {
								int year = date.getYear();
								ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(),year));
							} else {
								ageGroupField.setValue(null);
							}
						}
					});
					bindingBuilder.bind(property);
				} else if ("category".equals(property)) {
					categoryValidation(bindingBuilder);
					//filterCategories(bindingBuilder);
					bindingBuilder.bind(property);
				} else if ("gender".equals(property)) {
					genderValidation(bindingBuilder);
					HasValue<?, ?> genderField = bindingBuilder.getField();
					genderField.addValueChangeListener((e) -> {
						Gender gender = (Gender) e.getValue();
						Optional<Binding<Athlete, ?>> fbdBinding = binder.getBinding("fullBirthDate");
						HasValue<?, LocalDate> dateField = (HasValue<?, LocalDate>) fbdBinding.get().getField();
						Optional<Binding<Athlete, ?>> agBinding = binder.getBinding("mastersAgeGroup");
						if (agBinding.isPresent()) {
							HasValue<?, String> ageGroupField = (HasValue<?, String>) agBinding.get().getField();
							LocalDate date = dateField.getValue();
							if (gender != null && date != null) {
								int year = date.getYear();
								ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(),year));
							} else {
								ageGroupField.setValue("");
							}
						}
					});
					bindingBuilder.bind(property);
				} else {
					super.bindField(field, property, propertyType);
				}
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			protected void fullBirthDateValidation(Binder.BindingBuilder bindingBuilder) {
				LocalDateField ldtf = (LocalDateField) bindingBuilder.getField();
				Validator<LocalDate> fv = ldtf.formatValidation(OwlcmsSession.getLocale());
				bindingBuilder.withValidator(fv);

				Validator<LocalDate> v = Validator.from(
					ld -> {
						if (ld == null)
							return true;
						return ld.compareTo(LocalDate.now()) <= 0;
					},
					"Birth date cannot be in the future");
				bindingBuilder.withValidator(v);
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			protected void bodyWeightValidation(Binder.BindingBuilder bindingBuilder, boolean isRequired) {
				Validator<Double> v1 = new DoubleRangeValidator(
						"Weight should be between 0 and 350kg", 0.0D, 350.0D);
				// check wrt body category
				Validator<Double> v2 = Validator.from((weight) -> {
						if (!isRequired && weight == null)
							return true;
						// inconsistent selection is signaled on the category dropdown since the weight is a factual
						// measure
						Binding<Athlete, ?> categoryBinding = binder.getBinding("category").get();
						categoryBinding.validate(true).isError();
						return true;
					}, "Body Weight is outside of selected category");
				bindingBuilder.withValidator(v1);
				bindingBuilder.withValidator(v2);
			}

			@SuppressWarnings({ "rawtypes", "unchecked" })
			protected void categoryValidation(Binder.BindingBuilder bindingBuilder) {
				
				// check that category is consistent with body weight
				Validator<Category> v1 = Validator.from((category) -> {
					if (category == null) return true;
					try {
						Binding<Athlete, ?> bwBinding = binder.getBinding("bodyWeight").get();
						Double bw = (Double) bwBinding.getField().getValue();
						if (bw == null)
							// no body weight - no contradiction
							return true;
						Double min = category.getMinimumWeight();
						Double max = category.getMaximumWeight();
						logger.trace(
							"comparing {} ]{},{}] with body weight {}", category.getName(), min, max, bw);
						return (bw > min && bw <= max);
					} catch (Exception e) {
						logger.error(LoggerUtils.stackTrace(e));
					}
					return true;
				},
					"Category does not match body weight");
				bindingBuilder.withValidator(v1);

				// check that category is consistent with gender
				Validator<Category> v2 = Validator.from((category) -> {
					try {
						if (category == null) return true;
						Binding<Athlete, ?> genderBinding = binder.getBinding("gender").get();
						ComboBox<Gender> genderCombo = (ComboBox<Gender>) genderBinding.getField();
						Gender g = (Gender) genderCombo.getValue();
						Gender catGender = category != null ? category.getGender() : null;
						logger.debug("categoryValidation: validating gender {} vs category {}: {}",g,catGender,catGender == g);
						if (g == null) {
							// no gender - no contradiction
							return true;
						}
						catGenderOk = catGender == g;
						if (catGenderOk && !genderCatOk) {
							// validate() does not validate if no change, ugly workaround
							logger.debug("resetting gender");
							genderCombo.setValue(null);
							genderCombo.setValue(g); // turn off message if present.
						}
						return catGender == g;
					} catch (Exception e) {
						logger.error(LoggerUtils.stackTrace(e));
					}
					return true;
				},
					"Category does not match gender.");
				bindingBuilder.withValidator(v2);
			}
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			protected void genderValidation(Binder.BindingBuilder bindingBuilder) {
				// check that category is consistent with gender
				Validator<Gender> v2 = Validator.from((g) -> {
					try {
						if (g == null) return true;
						Binding<Athlete, ?> catBinding = binder.getBinding("category").get();
						ComboBox<Category> categoryCombo = (ComboBox<Category>) catBinding.getField();
						Category category = (Category) categoryCombo.getValue();
						logger.debug("genderValidation: validating gender {} vs category {}: {}",g,category.getGender(),category.getGender() == g);
						genderCatOk = category.getGender() == g;
						if (genderCatOk && !catGenderOk) {
							 // turn off message if present.
							logger.debug("resetting category");
							categoryCombo.setValue(null);
							categoryCombo.setValue(category);
						}
						return genderCatOk;
					} catch (Exception e) {
						logger.error(LoggerUtils.stackTrace(e));
					}
					return true;
				},
					"Category does not match gender.");
				bindingBuilder.withValidator(v2);
			}

			@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
			public void filterCategories(BindingBuilder categoryBindingBuilder) {
				ComboBox<Category> categoryField = (ComboBox<Category>) categoryBindingBuilder.getField();
				Binding<Athlete, ?> genderBinding = binder.getBinding("gender").get();
				ComboBox<Gender> genderField =  (ComboBox<Gender>) genderBinding.getField();
				ListDataProvider<Category> listDataProvider = new ListDataProvider<Category>(CategoryRepository.findActive(genderField.getValue()));
				genderField.addValueChangeListener((vc) -> {categoryField.setDataProvider(listDataProvider);});
			}
		};
	}

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
		lastNameFilter.setPlaceholder("Last name");
		lastNameFilter.setClearButtonVisible(true);
		lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
		lastNameFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		lastNameFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(lastNameFilter);

		ageDivisionFilter.setPlaceholder("Age Division");
		ageDivisionFilter.setItems(AgeDivision.findAll());
		ageDivisionFilter.setItemLabelGenerator(AgeDivision::name);
		ageDivisionFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		lastNameFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(ageDivisionFilter);
		
		if (Competition.getCurrent().isMasters()) {
			ageGroupFilter.setPlaceholder("Age Group");
			ageGroupFilter.setItems(MastersAgeGroup.findAllStrings());
//		ageGroupFilter.setItemLabelGenerator(AgeDivision::name);
			ageGroupFilter.addValueChangeListener(e -> {
				crudGrid.refreshGrid();
			});
			ageGroupFilter.setWidth("10em");
			crudGrid.getCrudLayout().addFilterComponent(ageGroupFilter);
		}

		categoryFilter.setPlaceholder("Category");
		categoryFilter.setItems(CategoryRepository.findActive());
		categoryFilter.setItemLabelGenerator(Category::getName);
		categoryFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		categoryFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(categoryFilter);

		groupFilter.setPlaceholder("Group");
		groupFilter.setItems(GroupRepository.findAll());
		groupFilter.setItemLabelGenerator(Group::getName);
		groupFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		groupFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(groupFilter);

		weighedInFilter.setPlaceholder("Weighed-In?");
		weighedInFilter.setItems(Boolean.TRUE,Boolean.FALSE);
		weighedInFilter.setItemLabelGenerator((i) -> {return i ? "Weighed" : "Not weighed";});
		weighedInFilter.addValueChangeListener(e -> {
			crudGrid.refreshGrid();
		});
		weighedInFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(weighedInFilter);

		Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
		clearFilters.addClickListener(event -> {
			lastNameFilter.clear();
			ageDivisionFilter.clear();
			categoryFilter.clear();
			groupFilter.clear();
			weighedInFilter.clear();
		});
		lastNameFilter.setWidth("10em");
		crudGrid.getCrudLayout().addFilterComponent(clearFilters);
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.shared.AppLayoutAware#getRouterLayout() */
	@Override
	public OwlcmsRouterLayout getRouterLayout() {
		return routerLayout;
	}

	/* (non-Javadoc)
	 * @see
	 * app.owlcms.ui.shared.AppLayoutAware#setRouterLayout(app.owlcms.ui.shared.OwlcmsRouterLayout) */
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
}
