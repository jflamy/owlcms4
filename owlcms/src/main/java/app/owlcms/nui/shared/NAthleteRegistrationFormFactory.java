/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Binder.BindingBuilder;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.Setter;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;

import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.LocalizedDecimalField;
import app.owlcms.components.fields.LocalizedIntegerField;
import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.RuleViolationException;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.displays.athletecard.AthleteCard;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public final class NAthleteRegistrationFormFactory extends OwlcmsCrudFormFactory<Athlete> implements NavigationPage {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(NAthleteRegistrationFormFactory.class);
	final private static int NB_COLUMNS = 3;
	HasValue<?, ?> dateField = null;
	private List<Category> allEligible;
	private LocalizedDecimalField bodyWeightField;
	private ComboBox<Category> categoryField;
	private boolean catGenderOk;
	private boolean changeListenersEnabled;
	private boolean checkOther20kgFields;
	private LocalizedIntegerField cleanJerk1DeclarationField;
	private TextField coachField;
	private Group currentGroup;
	private TextField custom1Field;
	private TextField custom2Field;
	private Athlete editedAthlete = null;
	private CheckboxGroup<Category> eligibleField;
	private CheckboxGroup<String> ageGroupTeamField;
	private TextField federationCodesField;
	private Map<HasValue<?, ?>, Binding<Athlete, ?>> fieldToBinding = new HashMap<>();
	private TextField firstNameField;
	private LocalDateField fullBirthDateField;
	private boolean genderCatOk;
	private ComboBox<Gender> genderField;
	private ComboBox<Group> groupField;
	private Button hiddenButton;
	private Checkbox ignoreErrorsCheckbox;
	private Category initialCategory;
	private TextField lastNameField;
	private LocalizedIntegerField lotNumberField;
	private TextField membershipField;
	private Button printButton;
	private LocalizedIntegerField qualifyingTotalField;
	private LocalizedIntegerField snatch1DeclarationField;
	private LocalizedIntegerField startNumberField;
	private TextField teamField;
	private TextField wrappedBWTextField;
	private LocalizedIntegerField yobField;
	private Checkbox invitedCheckbox;

	public NAthleteRegistrationFormFactory(Class<Athlete> domainType, Group group) {
		super(domainType);
		// logger.trace("constructor {} {}", System.identityHashCode(this), group);
		this.setCurrentGroup(group);
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		Athlete nAthlete = JPAService.runInTransaction((em) -> {
			return em.merge(athlete);
		});
		enablePrint(nAthlete);
		return nAthlete;
	}

	/**
	 * Create a binder with our special validation status handling
	 *
	 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildBinder(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object)
	 */
	@Override
	public Binder<Athlete> buildBinder(CrudOperation operation, Athlete ignored) {
		this.binder = super.buildBinder(operation, getEditedAthlete());
		this.initialCategory = getEditedAthlete().getCategory();
		setValidationStatusHandler(true);
		return this.binder;
	}

	/**
	 * Change the caption to show the current athlete name and group
	 *
	 * @see org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory#buildCaption(org.vaadin.crudui.
	 *      crudGrid.CrudOperation, java.lang.Object)
	 */
	@Override
	public String buildCaption(CrudOperation operation, Athlete a) {
		if (a.getLastName() == null && a.getFirstName() == null) {
			return null;
		}
		// If null, CrudLayout.showForm will build its own, for backward compatibility
		return a.getFullId();
	}

	/**
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildFooter(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object, com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener)
	 */
	@Override
	public Component buildFooter(CrudOperation operation,
			Athlete athlete,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener,
			boolean shortcutEnter,
			Button... buttons) {

		Button operationButton = null;

		if (operation == CrudOperation.UPDATE) {
			operationButton = buildOperationButton(CrudOperation.UPDATE, athlete, postOperationCallBack);
		} else if (operation == CrudOperation.ADD) {
			operationButton = buildOperationButton(CrudOperation.ADD, athlete, postOperationCallBack);
		}
		operationButton.addClickShortcut(Key.ENTER);

		Button deleteButton = buildDeleteButton(CrudOperation.DELETE, athlete, deleteButtonClickListener);
		Checkbox validateEntries = buildIgnoreErrorsCheckbox();
		Button cancelButton = buildCancelButton(cancelButtonClickListener);

		HorizontalLayout footerLayout = new HorizontalLayout();
		footerLayout.setWidth("100%");
		footerLayout.setSpacing(true);
		footerLayout.setPadding(false);

		if (deleteButton != null && operation != CrudOperation.ADD) {
			footerLayout.add(deleteButton);
		}

		NativeLabel spacer = new NativeLabel();
		footerLayout.add(spacer);// , operationTrigger);
		VerticalLayout vl = new VerticalLayout();
		vl.setSizeUndefined();
		vl.setPadding(false);
		vl.setMargin(false);
		vl.add(validateEntries);
		footerLayout.add(vl);
		footerLayout.setAlignItems(Alignment.CENTER);

		if (cancelButton != null) {
			footerLayout.add(cancelButton);
		}

		if (operationButton != null) {
			footerLayout.add(operationButton);
			if (operation == CrudOperation.UPDATE && shortcutEnter) {
				ShortcutRegistration reg = operationButton.addClickShortcut(Key.ENTER);
				reg.allowBrowserDefault();
			}
		}
		footerLayout.setFlexGrow(1.0, spacer);
		return footerLayout;
	}

	/**
	 * @see app.owlcms.nui.crudui.OwlcmsCrudFormFactory#buildNewForm(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object, boolean,
	 *      com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.ComponentEventListener,
	 *      com.vaadin.flow.component.button.Button[])
	 */
	@Override
	public Component buildNewForm(CrudOperation operation, Athlete aFromList, boolean readOnly,
			ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
			ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

		// logger.trace("buildNewForm {} {} {}", System.identityHashCode(this), getCurrentGroup(),
		// LoggerUtils.stackTrace());
		setupAthlete(operation, aFromList);
		this.binder = buildBinder(operation, getEditedAthlete());

		// when in weigh-in, the validations need to ignore the weight that was last
		// loaded.
		OwlcmsSession.setAttribute("weighIn", getEditedAthlete());

		// workaround for Return shortcut not working. Check that this is still needed
		// in v23+
		this.hiddenButton = new Button("doit");
		this.hiddenButton.getStyle().set("visibility", "hidden");

		createPrintButton();
		setChangeListenersEnabled(false);

		Component footer = this.buildFooter(operation, getEditedAthlete(), cancelButtonClickListener,
				operationButtonClickListener, deleteButtonClickListener, false, this.hiddenButton, this.printButton);

		// dumpCategories(aFromList, null, new
		// ArrayList<Category>(aFromList.getEligibleCategories()));

		Component form = createTabSheets(footer);
		this.binder.readBean(aFromList);

		// binder has read bean.
		filterCategories(getEditedAthlete().getCategory(), operation != CrudOperation.ADD);

		setFocus(form);
		return form;
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete athlete) {
		AthleteRepository.delete(athlete);
	}

	public void enablePrint(Athlete domainObject) {
		if (domainObject == null || domainObject.getId() == null) {
			this.printButton.setEnabled(false);
		} else {
			this.printButton.setEnabled(true);
			this.hiddenButton.getElement().setAttribute("onClick",
					getWindowOpenerFromClass(AthleteCard.class, domainObject.getId().toString()));
		}
	}

	@Override
	public Collection<Athlete> findAll() {
		throw new UnsupportedOperationException(); // should be called on the grid, not on the form
	}

	@Override
	public void setValidationStatusHandler(boolean showErrorsOnFields) {
		this.binder.setValidationStatusHandler((s) -> {
			List<BindingValidationStatus<?>> fieldValidationErrors = s.getFieldValidationErrors();
			for (BindingValidationStatus<?> error : fieldValidationErrors) {
				HasValue<?, ?> field = error.getField();
				logger.debug("error message: {} field: {}", error.getMessage(), field);
				if (field instanceof HasValidation) {
					logger.debug("has validation");
					HasValidation vf = (HasValidation) field;
					vf.setInvalid(true);
					vf.setErrorMessage(error.getMessage().get());

				}
			}
			setValid(!s.hasErrors());
			s.notifyBindingValidationStatusHandlers();
		});
	}

	/**
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		logger.trace("updating athlete {}", athlete.toString());
		athlete.enforceCategoryIsEligible();
		AthleteRepository.save(athlete);
		return athlete;
	}

	/**
	 * Add the field-level validations
	 *
	 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#bindField(com.vaadin.flow.component.HasValue,
	 *      java.lang.String, java.lang.Class)
	 */
	@Override
	protected void bindField(@SuppressWarnings("rawtypes") HasValue field, String property, Class<?> propertyType,
			CrudFormConfiguration c) {
		throw new UnsupportedOperationException("should not be calling this method");
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateBodyWeight(Binder.BindingBuilder<Athlete, Double> bindingBuilder, boolean isRequired) {
		Validator<Double> v1 = new DoubleRangeValidator(Translator.translate("Weight_under_350"), 0.1D, 350.0D);
		bindingBuilder.withValidator(v1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateCategory(Binder.BindingBuilder<Athlete, Category> bindingBuilder) {

		// check that there are eligibility categories
		// check that category is consistent with body weight
		Validator<Category> v0 = Validator.from((category) -> {
			// logger.debug("v0");
			try {
				Double bw = this.bodyWeightField.getValue();
				if (category == null && bw != null) {
					// logger.debug("0 category {} {} bw {}", category, cat, bw);
					return false;
				} else {
					return true;
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_noEligiblity_check_age_entry"));
		bindingBuilder.withValidator(v0);

		// check that category is consistent with body weight
		Validator<Category> v1 = Validator.from((category) -> {
			// logger.debug("v1");
			try {
				Double bw = this.bodyWeightField.getValue();
				if (category == null && bw == null) {
					// logger.debug("1 category {} {} bw {}", category, cat, bw);
					return true;
				} else if (bw == null) {
					// logger.debug("2 category {} {} bw {}", category != null ?
					// category.getComputedName() : null, cat, bw);
					// no body weight - no contradiction
					return true;
				} else if (bw != null && category == null) {
					// logger.debug("3 category {} {} bw {}", category, cat, bw);
					return false;
				}
				Double min = category.getMinimumWeight();
				Double max = category.getMaximumWeight();
				// logger.debug("comparing {} ]{},{}] with body weight {}",
				// category.getComputedName(), min, max, bw);
				return (bw > min && bw <= max);
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_body_weight"));
		bindingBuilder.withValidator(v1);

		// check that category is consistent with age
		Validator<Category> v2 = Validator.from((category) -> {
			// logger.debug("v2");
			try {
				Category cat = this.categoryField.getValue();
				Integer age = getAgeFromFields();
				if (category == null && age == null) {
					logger.debug("1 category {} {} age {}", category, cat, age);
					return true;
				} else if (age == null) {
					logger.debug("2 category {} {} age {}", category != null ? category.getCode() : null, cat, age);
					// no body weight - no contradiction
					return true;
				}
				if (category != null && age != null) {
					int min = category.getAgeGroup().getMinAge();
					int max = category.getAgeGroup().getMaxAge();
					logger.debug("comparing {} [{},{}] with age {}", category.getCode(), min, max, age);
					return (age >= min && age <= max);
				} else {
					return true;
				}
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_age"));
		bindingBuilder.withValidator(v2);

		// check that category is consistent with gender
		Validator<Category> v3 = Validator.from((category) -> {
			// logger.debug("v3");
			try {
				if (category == null) {
					return true;
				}
				Binding<Athlete, ?> genderBinding = this.fieldToBinding.get(this.genderField);
				Gender g = this.genderField.getValue();
				Gender catGender = category != null ? category.getGender() : null;
				logger.debug("categoryValidation: validating gender {} vs category {}: {} {}", g, catGender,
						catGender == g);
				if (g == null) {
					// no gender - no contradiction
					return true;
				}
				this.catGenderOk = catGender == g;
				if (this.catGenderOk && !this.genderCatOk) {
					// validate() does not validate if no change, ugly workaround
					logger.debug("checking gender");
					genderBinding.validate();
				}
				return catGender == g;
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_gender"));
		bindingBuilder.withValidator(v3);

		// a category change requires explicit ok.
		Validator<Category> v4 = Validator.from((category) -> {
			// logger.debug("v4");
			try {
				if (isIgnoreErrors() || this.initialCategory == null
						|| (getEditedAthlete().getBodyWeight() == null && category == null)) {
					return true;
				}
				logger.debug("initialCategory = {}  new category = {}", this.initialCategory,
						getEditedAthlete().getCategory());
				return category != null && category.sameAs(this.initialCategory);
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("CategoryChange_MustForce", this.initialCategory));
		bindingBuilder.withValidator(v4);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateFullBirthDate(Binder.BindingBuilder bindingBuilder) {
		Validator<LocalDate> fv = this.fullBirthDateField.formatValidation(OwlcmsSession.getLocale());
		bindingBuilder.withValidator(fv);

		Validator<LocalDate> v = Validator.from(ld -> {
			if (ld == null) {
				return true;
			}
			return ld.compareTo(LocalDate.now()) <= 0;
		}, Translator.translate("BirthDate_cannot_future"));
		bindingBuilder.withValidator(v);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateGender(Binder.BindingBuilder<Athlete, Gender> bindingBuilder) {
		// check that category is consistent with gender
		Validator<Gender> v2 = Validator.from((g) -> {
			try {
				if (g == null) {
					return true;
				}
				Binding<Athlete, ?> catBinding = this.fieldToBinding.get(this.categoryField);
				Category category = this.categoryField.getValue();
				Gender catGender = category != null ? category.getGender() : null;
				logger.trace("genderValidation: validating gender {} vs category {}: {}", g, catGender, catGender == g);
				this.genderCatOk = catGender == null || catGender == g;
				if (this.genderCatOk && !this.catGenderOk) {
					// turn off message if present.
					logger.debug("checking category");
					catBinding.validate();
				}
				return this.genderCatOk;
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_gender"));
		bindingBuilder.withValidator(v2);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateYearOfBirth(Binder.BindingBuilder bindingBuilder) {
		// String message = Translator.translate("InvalidYearFormat");
		// RegexpValidator re = new RegexpValidator(message, "|((19|20)[0-9][0-9])");
		// bindingBuilder.withNullRepresentation("");
		// bindingBuilder.withValidator(re);
		// yobConverter = new StringToIntegerConverter(message) {
		// @Override
		// protected NumberFormat getFormat(java.util.Locale locale) {
		// NumberFormat format = NumberFormat.getIntegerInstance();
		// format.setGroupingUsed(false);
		// return format;
		// }
		// };
		// bindingBuilder.withConverter(yobConverter);
		Validator<Integer> v1 = new IntegerRangeValidator(Translator.translate("InvalidYearFormat"), 1900, 2050);
		bindingBuilder.withValidator(v1);
	}

	private Category bestMatch(List<Category> allEligible2) {
		return allEligible2 != null ? (allEligible2.size() > 0 ? allEligible2.get(0) : null) : null;
	}

	private <T> BindingBuilder<Athlete, T> bindField(BindingBuilder<Athlete, T> bindingBuilder, HasValue<?, T> field,
			ValueProvider<Athlete, T> getter, Setter<Athlete, T> setter) {
		Binding<Athlete, T> binding;
		binding = bindingBuilder.bind(getter, setter);
		this.fieldToBinding.put(field, binding);
		return bindingBuilder;
	}

	private Checkbox buildIgnoreErrorsCheckbox() {
		this.ignoreErrorsCheckbox = new Checkbox(Translator.translate("RuleViolation.ignoreErrors"), e -> {
			if (BooleanUtils.isTrue(isIgnoreErrors())) {
				logger./**/warn/**/("{}!Errors ignored - checkbox override for athlete {}",
						OwlcmsSession.getFop().getLoggingName(), this.getEditedAthlete().getShortName());
				// binder.validate();
				this.binder.writeBeanAsDraft(this.editedAthlete, true);
			}

		});
		this.ignoreErrorsCheckbox.getStyle().set("margin-left", "3em");
		return this.ignoreErrorsCheckbox;
	}

	private boolean categoryIsEligible(Category category, List<Category> eligibles) {
		return eligibles.stream().anyMatch(c -> c.sameAs(category));
	}

	private void checkOther20kgFields(LocalizedIntegerField fieldA,
			LocalizedIntegerField fieldB) {
		logger.debug/* edit */("entering checkOther20kgFields {} {}", isCheckOther20kgFields(),
				LoggerUtils.whereFrom());
		if (isCheckOther20kgFields()) {
			setCheckOther20kgFields(false); // prevent recursion
			this.fieldToBinding.get(fieldA).validate();
			this.fieldToBinding.get(fieldB).validate();
		}
	}

	private void clearErrors(LocalizedIntegerField cleanJerk1DeclarationField2,
			LocalizedIntegerField qualifyingTotalField2) {
		cleanJerk1DeclarationField2.setInvalid(false);
		qualifyingTotalField2.setInvalid(false);
	}

	private String computeDesc(Group g) {
		String desc = g.getDescription();
		if (desc != null && !desc.isBlank()) {
			return (g.getName() + " - " + desc);
		} else {
			return g.getName();
		}
	}

	private void configure20kgWeightField(LocalizedIntegerField field) {
		TextField textField = field.getWrappedTextField();
		textField.setAutoselect(true);
		textField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		textField.setPattern("^(-?\\d+)|()$"); // optional minus and at least one digit, or empty.
		textField.setAllowedCharPattern("[0-9-]");
		textField.addValueChangeListener((e) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			setCheckOther20kgFields(true);
		});
	}

	private FormLayout createDrawForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.DrawTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.lotNumberField = new LocalizedIntegerField();
		bindField(this.binder.forField(this.lotNumberField), this.lotNumberField, Athlete::getLotNumber, Athlete::setLotNumber);
		layoutAddFormItem(layout, this.lotNumberField, Translator.translate("Lot"));

		this.startNumberField = new LocalizedIntegerField();
		bindField(this.binder.forField(this.startNumberField), this.startNumberField, Athlete::getStartNumber,
				Athlete::setStartNumber);
		layoutAddFormItem(layout, this.startNumberField, Translator.translate("StartNumber"));
		return layout;
	}

	private FormLayout createGroupCategoryForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.GroupCatTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.categoryField = new ComboBox<>();
		BindingBuilder<Athlete, Category> bb = this.binder.forField(this.categoryField);
		validateCategory(bb);
		bindField(bb, this.categoryField, Athlete::getCategory, Athlete::setCategory);
		safeCategorySetItems(CategoryRepository.findActive());
		this.categoryField.setRenderer(new TextRenderer<>(Category::getTranslatedName));
		FormItem fi = layoutAddFormItem(layout, this.categoryField, Translator.translate("Category"));
		layout.setColspan(fi, 1);

		this.eligibleField = new CheckboxGroup<>();
		bindField(this.binder.forField(this.eligibleField), this.eligibleField, Athlete::getEligibleCategories,
				Athlete::setEligibleCategories);
		this.eligibleField.setRenderer(new TextRenderer<>(Category::getTranslatedName));
		FormItem fi1 = layoutAddFormItem(layout, this.eligibleField, Translator.translate("Weighin.EligibleCategories"));
		layout.setColspan(fi1, NB_COLUMNS - 1);

		layoutAddFormItem(layout, new NativeLabel(), "");

		this.ageGroupTeamField = new CheckboxGroup<>(null, getEditedAthlete().getPossibleAgeGroupTeams());
		bindField(this.binder.forField(this.ageGroupTeamField), this.ageGroupTeamField, Athlete::getAgeGroupTeams,
				Athlete::setAgeGroupTeams);
		this.ageGroupTeamField.setValue(getEditedAthlete().getPossibleAgeGroupTeams());
		FormItem fi4 = layoutAddFormItem(layout, this.ageGroupTeamField, Translator.translate("TeamMembership.Title"));
		layout.setColspan(fi4, NB_COLUMNS - 1);

		this.groupField = new ComboBox<>();
		bindField(this.binder.forField(this.groupField), this.groupField, Athlete::getGroup, Athlete::setGroup);
		List<Group> allGroups = GroupRepository.findAll();
		allGroups.sort(new NaturalOrderComparator<>());
		this.groupField.setItems(allGroups);
		this.groupField.setWidth("25em");
		this.groupField.setRenderer(new TextRenderer<>(g -> computeDesc(g)));
		this.groupField.setItemLabelGenerator(g -> computeDesc(g));
		this.groupField.setClearButtonVisible(true);
		FormItem fi2 = layoutAddFormItem(layout, this.groupField, Translator.translate("Group"));
		layout.setColspan(fi2, 2);

		this.invitedCheckbox = new Checkbox();
		bindField(this.binder.forField(this.invitedCheckbox), this.invitedCheckbox,
				(a) -> !a.isEligibleForIndividualRanking(),
				(a, b) -> a.setEligibleForIndividualRanking(!b));
		FormItem fi3 = layoutAddFormItem(layout, this.invitedCheckbox, Translator.translate("Invited/Extra"));
		layout.setColspan(fi3, NB_COLUMNS - 2);

		return layout;
	}

	private FormLayout createIdForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.IdentificationTitle");
		title.getElement().getStyle().set("margin-top", "-1.5ex");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.lastNameField = new TextField();
		bindField(this.binder.forField(this.lastNameField), this.lastNameField, Athlete::getLastName, Athlete::setLastName);
		this.lastNameField.setSizeFull();
		layoutAddFormItem(layout, this.lastNameField, Translator.translate("LastName"));

		this.firstNameField = new TextField();
		this.firstNameField.setSizeFull();
		bindField(this.binder.forField(this.firstNameField), this.firstNameField, Athlete::getFirstName, Athlete::setFirstName);
		layoutAddFormItem(layout, this.firstNameField, Translator.translate("FirstName"));

		Competition competition = Competition.getCurrent();
		if (competition.isUseBirthYear()) {
			this.yobField = new LocalizedIntegerField();
			this.yobField.getWrappedTextField().setPlaceholder("yyyy");
			BindingBuilder<Athlete, Integer> bb = this.binder.forField(this.yobField);
			validateYearOfBirth(bb);
			bindField(bb, this.yobField, Athlete::getYearOfBirth, Athlete::setYearOfBirth);
			layoutAddFormItem(layout, this.yobField, Translator.translate("YearOfBirth"));
		} else {
			this.fullBirthDateField = new LocalDateField();
			this.fullBirthDateField.getWrappedTextField().setPlaceholder("yyyy-mm-dd");
			this.fullBirthDateField.getWrappedTextField().setValueChangeMode(ValueChangeMode.ON_CHANGE);
			BindingBuilder<Athlete, LocalDate> bb = this.binder.forField(this.fullBirthDateField);
			validateFullBirthDate(bb);
			bindField(bb, this.fullBirthDateField, Athlete::getFullBirthDate, Athlete::setFullBirthDate);
			layoutAddFormItem(layout, this.fullBirthDateField, Translator.translate("BirthDate"));
		}

		this.genderField = new ComboBox<>();
		BindingBuilder<Athlete, Gender> bb = this.binder.forField(this.genderField);
		validateGender(bb);
		bindField(bb, this.genderField, Athlete::getGender, Athlete::setGender);
		this.genderField.setItems(Arrays.asList(Gender.mfValues()));
		layoutAddFormItem(layout, this.genderField, Translator.translate("Gender"));

		this.teamField = new TextField();
		this.teamField.setSizeFull();
		bindField(this.binder.forField(this.teamField), this.teamField, Athlete::getTeam, Athlete::setTeam);
		layoutAddFormItem(layout, this.teamField, Translator.translate("Team"));

		this.membershipField = new TextField();
		bindField(this.binder.forField(this.membershipField), this.membershipField, Athlete::getMembership, Athlete::setMembership);
		layoutAddFormItem(layout, this.membershipField, Translator.translate("Membership"));

		return layout;
	}

	private FormLayout createInfoForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.InfoTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.coachField = new TextField();
		bindField(this.binder.forField(this.coachField), this.coachField, Athlete::getCoach, Athlete::setCoach);
		FormItem formItem = layoutAddFormItem(layout, this.coachField, Translator.translate("Coach"));
		this.coachField.setSizeFull();
		layout.add(new Div());
		layout.setColspan(formItem, 2);

		this.custom1Field = new TextField();
		bindField(this.binder.forField(this.custom1Field), this.custom1Field, Athlete::getCustom1, Athlete::setCustom1);
		this.custom1Field.setSizeFull();
		layoutAddFormItem(layout, this.custom1Field, Translator.translate("Custom1.Title"));

		this.custom2Field = new TextField();
		bindField(this.binder.forField(this.custom2Field), this.custom2Field, Athlete::getCustom2, Athlete::setCustom2);
		this.custom2Field.setSizeFull();
		layoutAddFormItem(layout, this.custom2Field, Translator.translate("Custom2.Title"));

		return layout;
	}

	private FormLayout createLayout() {
		FormLayout layout = new FormLayout();
		layout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.TOP),
				new ResponsiveStep("800px", NB_COLUMNS, LabelsPosition.ASIDE));
		return layout;
	}

	private void createPrintButton() {
		this.printButton = new Button(Translator.translate("AthleteCard"), new Icon(VaadinIcon.PRINT));
		enablePrint(getEditedAthlete());
		this.printButton.setThemeName("secondary success");

		// ensure that writeBean() is called; this horror is due to the fact that we
		// must open a new window from the client side, and cannot save on click.
		this.printButton.addClickListener(click -> {
			try {
				Athlete editedAthlete2 = getEditedAthlete();
				if (isIgnoreErrors()) {
					getEditedAthlete().setValidation(false);
				}
				this.binder.writeBean(editedAthlete2);
				this.update(editedAthlete2);
				this.hiddenButton.clickInClient();
			} catch (ValidationException e) {
				getEditedAthlete().setValidation(true);
				this.binder.validate();
			}

		});
	}

	private FormLayout createRecordForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.RecordsTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.federationCodesField = new TextField();
		this.federationCodesField.setSizeFull();
		this.federationCodesField.setHelperText(Translator.translate("Registration.FederationCodes"));
		bindField(this.binder.forField(this.federationCodesField), this.federationCodesField, Athlete::getFederationCodes,
				Athlete::setFederationCodes);
		FormItem formItem2 = layoutAddFormItem(layout, this.federationCodesField,
				Translator.translate("Registration.Federations"));
		layout.setColspan(formItem2, 2);
		layout.add(new Div());

		LocalizedIntegerField bestSnatchField = new LocalizedIntegerField();
		bindField(this.binder.forField(bestSnatchField), bestSnatchField, Athlete::getPersonalBestSnatch,
				Athlete::setPersonalBestSnatch);
		layoutAddFormItem(layout, bestSnatchField, Translator.translate("PersonalBestSnatch"));

		LocalizedIntegerField bestCleanJerkField = new LocalizedIntegerField();
		bindField(this.binder.forField(bestCleanJerkField), bestCleanJerkField, Athlete::getPersonalBestCleanJerk,
				Athlete::setPersonalBestCleanJerk);
		layoutAddFormItem(layout, bestCleanJerkField, Translator.translate("PersonalBestCleanJerk"));

		LocalizedIntegerField bestTotalField = new LocalizedIntegerField();
		bindField(this.binder.forField(bestTotalField), bestTotalField, Athlete::getPersonalBestTotal,
				Athlete::setPersonalBestTotal);
		layoutAddFormItem(layout, bestTotalField, Translator.translate("PersonalBestTotal"));

		return layout;
	}

	private FlexLayout createTabSheets(Component footer) {
		TabSheet ts = new TabSheet();

		FormLayout idLayout = createIdForm();
		FormLayout groupCatLayout = createGroupCategoryForm();
		FormLayout weighInLayout = createWeighInForm();
		VerticalLayout content = new VerticalLayout(new Div(),
				idLayout,
				separator(),
				weighInLayout,
				separator(),
				groupCatLayout);
		ts.add(Translator.translate("Athlete.IdTab"),
				content);

		FormLayout infoLayout = createInfoForm();
		FormLayout personalBestLayout = createRecordForm();
		FormLayout drawLayout = createDrawForm();
		VerticalLayout content2 = new VerticalLayout(new Div(),
				infoLayout,
				separator(),
				personalBestLayout,
				separator(),
				drawLayout);
		ts.add(Translator.translate("Athlete.InfoTab"),
				content2);
		FlexLayout mainLayout = new FlexLayout(
				ts, footer);

		mainLayout.setFlexDirection(FlexDirection.COLUMN);
		mainLayout.setFlexGrow(1.0D, ts);
		// 37rem is the dialog height for the first tab - fits on a 1366x768 laptop with
		// bookmark bar open. We don't need bigger than 37rem
		mainLayout.setHeight("calc(min(\"100%\",\"37rem\"))");
		mainLayout.setWidth("60rem");
		return mainLayout;
	}

	private Component createTitle(String string) {
		H4 title = new H4(Translator.translate(string));
		title.getStyle().set("margin-top", "0");
		title.getStyle().set("margin-bottom", "0.5ex");
		return title;
	}

	private FormLayout createWeighInForm() {
		FormLayout layout = createLayout();
		Component title = createTitle("Athlete.WeighInTitle");
		layout.add(title);
		layout.setColspan(title, NB_COLUMNS);

		this.bodyWeightField = new LocalizedDecimalField();
		this.bodyWeightField.getWrappedTextField().setAllowedCharPattern("[0-9.,]");
		BindingBuilder<Athlete, Double> bb = this.binder.forField(this.bodyWeightField);
		validateBodyWeight(bb, false);
		bindField(bb, this.bodyWeightField, Athlete::getBodyWeight, Athlete::setBodyWeight);
		layoutAddFormItem(layout, this.bodyWeightField, Translator.translate("BodyWeight"));

		this.snatch1DeclarationField = new LocalizedIntegerField();
		BindingBuilder<Athlete, Integer> bb1 = this.binder.forField(this.snatch1DeclarationField);
		configure20kgWeightField(this.snatch1DeclarationField);
		Validator<? super Integer> v1 = ValidationUtils.<Integer>checkUsingException((unused) -> {
			return validateStartingTotals(this.snatch1DeclarationField, this.cleanJerk1DeclarationField, this.qualifyingTotalField);
		});
		bb1.withValidator(v1);
		bindField(bb1, this.snatch1DeclarationField,
				a -> {
					String snatch1Declaration = a.getSnatch1Declaration();
					return snatch1Declaration != null && !snatch1Declaration.isBlank()
							? Integer.valueOf(snatch1Declaration)
									: null;
				},
				(a, v) -> {
					a.setSnatch1Declaration(v != null ? v.toString() : "");
				});
		layoutAddFormItem(layout, this.snatch1DeclarationField, Translator.translate("SnatchDecl_"));

		this.cleanJerk1DeclarationField = new LocalizedIntegerField();
		BindingBuilder<Athlete, Integer> bb2 = this.binder.forField(this.cleanJerk1DeclarationField);
		configure20kgWeightField(this.cleanJerk1DeclarationField);
		Validator<? super Integer> v2 = ValidationUtils.<Integer>checkUsingException((unused) -> {
			return validateStartingTotals(this.cleanJerk1DeclarationField, this.snatch1DeclarationField, this.qualifyingTotalField);
		});
		bb2.withValidator(v2);

		bindField(bb2, this.cleanJerk1DeclarationField,
				a -> {
					String cleanJerk1Declaration = a.getCleanJerk1Declaration();
					return cleanJerk1Declaration != null && !cleanJerk1Declaration.isBlank()
							? Integer.valueOf(cleanJerk1Declaration)
									: null;
				},
				(a, v) -> {
					a.setCleanJerk1Declaration(v != null ? v.toString() : "");
				});
		layoutAddFormItem(layout, this.cleanJerk1DeclarationField, Translator.translate("C_and_J_decl"));

		this.qualifyingTotalField = new LocalizedIntegerField();
		BindingBuilder<Athlete, Integer> bb3 = this.binder.forField(this.qualifyingTotalField);
		configure20kgWeightField(this.qualifyingTotalField);
		Validator<? super Integer> v3 = ValidationUtils.<Integer>checkUsingException((unused) -> {
			return validateStartingTotals(this.qualifyingTotalField, this.cleanJerk1DeclarationField, this.snatch1DeclarationField);
		});
		bb3.withValidator(v3);
		bindField(bb3, this.qualifyingTotalField, Athlete::getQualifyingTotal, Athlete::setQualifyingTotal);
		layoutAddFormItem(layout, this.qualifyingTotalField, Translator.translate("EntryTotal"));

		return layout;
	}

	@SuppressWarnings("unused")
	private void dumpCategories(Athlete editedAthlete2, Double bw, List<Category> allEligible2) {
		logger.debug("{} bw={} {}", editedAthlete2, bw, allEligible2);
	}

	@SuppressWarnings({ "unchecked" })
	private void filterCategories(Category category, boolean initCategories) {
		setChangeListenersEnabled(false);

		Group curGroup = this.groupField.getValue();
		// force reading all groups
		this.groupField.setValue(curGroup);

		this.categoryField.setClearButtonVisible(true);

		if (initCategories) {
			this.allEligible = findEligibleCategories(this.genderField, getAgeFromFields(), this.bodyWeightField,
					this.categoryField, this.qualifyingTotalField);
			logger.debug/* edit */("*** allEligibles init {}", this.allEligible);
			updateCategoryFields(category, this.categoryField, this.eligibleField, this.qualifyingTotalField, this.allEligible, false);
		}

		this.genderField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
					getAgeFromFields(), null);
			safeCategorySetItems(pertinentCategories);
			recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField, this.eligibleField, this.dateField,
					this.qualifyingTotalField);
		});

		if (Competition.getCurrent().isUseBirthYear()) {
			this.dateField = this.yobField;
			// Workaround for bug
			// https://stackoverflow.com/questions/55532055/java-casting-java-11-throws-lambdaconversionexception-while-1-8-does-not
			ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
				if (!isChangeListenersEnabled()) {
					return;
				}
				List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
						getAgeFromFields(), null);
				safeCategorySetItems(pertinentCategories);
				recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField, this.eligibleField, this.dateField,
						this.qualifyingTotalField);
			};
			this.yobField.addValueChangeListener(listener);
		} else {
			this.dateField = this.fullBirthDateField;
			ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
				if (!isChangeListenersEnabled()) {
					return;
				}
				List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
						getAgeFromFields(), null);
				safeCategorySetItems(pertinentCategories);
				recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField, this.eligibleField, this.dateField,
						this.qualifyingTotalField);
			};
			this.fullBirthDateField.addValueChangeListener(listener);
		}
		this.wrappedBWTextField = this.bodyWeightField.getWrappedTextField();
		this.wrappedBWTextField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
		this.wrappedBWTextField.setAutoselect(true);
		this.wrappedBWTextField.addValueChangeListener((vc) -> {
			// logger.debug("wrappedBWTextField listenersEnabled={} invalid={}",
			// isChangeListenersEnabled(), bodyWeightField.isInvalid());
			if (!isChangeListenersEnabled() || this.bodyWeightField.isInvalid()) {
				return;
			}
			recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField, this.eligibleField, this.dateField,
					this.qualifyingTotalField);
		});

		this.categoryField.addValueChangeListener((vc) -> {
			Category value = vc.getValue();
			logger.debug/* edit */("category new value {} listenersEnabled {} {}", value, isChangeListenersEnabled(),
					(value == null ? LoggerUtils.stackTrace() : LoggerUtils.whereFrom()));
			if (!isChangeListenersEnabled()) {
				return;
			}
			setChangeListenersEnabled(false); // prevent recursion.
			if (this.genderField.getValue() == null) {
				this.genderField.setValue(value.getGender());
			}
			recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField, this.eligibleField, this.dateField,
					this.qualifyingTotalField);
			setChangeListenersEnabled(true);
		});

		this.eligibleField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			setChangeListenersEnabled(false); // prevent recursion.
			// false as last argument: do not reset to all eligible categories
			Category value = this.categoryField.getValue();
			logger.debug/* edit */("eligible new value {} listenersEnabled {} {}", value, isChangeListenersEnabled(),
					(value == null ? LoggerUtils.stackTrace() : LoggerUtils.whereFrom()));
			Set<Category> selectedCategories = this.eligibleField.getSelectedItems();
			this.allEligible = findEligibleCategories(this.genderField, getAgeFromFields(), this.bodyWeightField, this.categoryField,
					this.qualifyingTotalField);
			Stream<Category> filter = this.allEligible.stream().filter(c -> c.sameAsAny(selectedCategories));
			Category category2 = filter.findFirst().orElse(null);
			this.categoryField.setValue(category2);
			setChangeListenersEnabled(true);
		});

		this.qualifyingTotalField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled() || this.qualifyingTotalField.isInvalid()) {
				return;
			}
			recomputeCategories(this.genderField, this.bodyWeightField, this.categoryField, this.eligibleField, this.dateField,
					this.qualifyingTotalField);
		});
		this.qualifyingTotalField.setAutoselect(true);

		setChangeListenersEnabled(true);
	}

	private List<Category> findEligibleCategories(ComboBox<Gender> genderField, Integer ageFromFields,
			LocalizedDecimalField bodyWeightField, ComboBox<Category> categoryField,
			LocalizedIntegerField qualifyingTotalField2) {
		logger.debug/* edit */("findEligibleCategories");
		// best match is first
		Double bw = bodyWeightField.getValue();
		Double catW = categoryField.getValue() != null ? categoryField.getValue().getMaximumWeight() : null;
		if (bw == null) {
			bw = catW;
		}
		return CategoryRepository.doFindEligibleCategories(this.getEditedAthlete(), genderField.getValue(),
				ageFromFields, bw, zeroIfNull(qualifyingTotalField2));
	}

	@SuppressWarnings("unchecked")
	private Integer getAgeFromFields() {
		Integer age = null;
		LocalDate now;
		if (Config.getCurrent().isUseCompetitionDate()) {
			now = Competition.getCurrent().getCompetitionDate();
		} else {
			now = LocalDate.now();
		}
		if (Competition.getCurrent().isUseBirthYear()) {
			try {
				Integer yob = this.yobField.getValue();

				age = (now.getYear()) - yob;
			} catch (Exception e) {
				age = null;
			}
		} else {
			LocalDate date = this.fullBirthDateField.getValue();
			if (date != null) {
				age = (now.getYear() - date.getYear());
			}
		}
		return age;
	}

	private Group getCurrentGroup() {
		return this.currentGroup;
	}

	private Athlete getEditedAthlete() {
		return this.editedAthlete;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private Gender getGenderFieldValue() {
		// HasValue<?, Gender> genderField = (HasValue<?, Gender>)
		// binder.getBinding("gender").get().getField();
		Gender gender = this.genderField.getValue();
		return gender;
	}

	private boolean isChangeListenersEnabled() {
		return this.changeListenersEnabled;
	}

	private boolean isCheckOther20kgFields() {
		return this.checkOther20kgFields;
	}

	private boolean isIgnoreErrors() {
		return BooleanUtils.isTrue(this.ignoreErrorsCheckbox == null ? null : this.ignoreErrorsCheckbox.getValue());
	}

	private FormItem layoutAddFormItem(FormLayout layout, Component field,
			String translate) {
		Div label = new Div();
		label.setText(translate);
		label.getStyle().set("text-align", "right");
		FormItem fi = layout.addFormItem(field, label);
		fi.getStyle().set("align-items", "center");
		fi.getStyle().set("align-self", "center");
		fi.getStyle().set("margin-bottom", "-0.5em");
		fi.getStyle().set("padding-bottom", "0");
		return fi;
	}

	private void recomputeCategories(
			ComboBox<Gender> genderField, LocalizedDecimalField bodyWeightField,
			ComboBox<Category> categoryField, CheckboxGroup<Category> eligibleField,
			HasValue<?, ?> dateField, LocalizedIntegerField qualifyingTotalField2) {

		Category cat = categoryField.getValue();
		Integer age = getAgeFromFields();
		logger.debug/* edit */("cat={} age={}", cat, age);
		if (bodyWeightField.getValue() != null) {
			if (genderField.getValue() != null && age != null) {
				// body weight, gender, date
				this.allEligible = findEligibleCategories(genderField, getAgeFromFields(), bodyWeightField,
						categoryField, qualifyingTotalField2);
				logger.debug/* edit */("cat {} eli {}", cat, this.allEligible);
				if (cat != null && categoryIsEligible(cat, this.allEligible)) {
					// current category is amongst eligibles. Don't recompute anything.
					logger.debug/* edit */("leave alone");
					Category bestMatchCategory = bestMatch(this.allEligible);
					updateCategoryFields(bestMatchCategory, categoryField, eligibleField, qualifyingTotalField2,
							this.allEligible, false);
				} else {
					logger.debug/* edit */("recompute, cat={} allEligible = {}", cat, this.allEligible);
					// category is null or not within eligibles, recompute
					Category bestMatchCategory = bestMatch(this.allEligible);
					updateCategoryFields(bestMatchCategory, categoryField, eligibleField, qualifyingTotalField2,
							this.allEligible, true);
				}
			} else {
				logger.debug/* edit */("bw, but need age and gender");
				// cannot compute eligibility and category
			}
		} else {
			// no body weight, but category available.
			if (age != null && cat != null) {
				Double bw = cat.getMaximumWeight();
				Gender gender = cat.getGender();
				int qualifyingTotal = qualifyingTotalField2.getValue();
				Integer ageFromFields = getAgeFromFields();
				if (ageFromFields != null && ageFromFields > 5 && ageFromFields < 120) {
					this.allEligible = CategoryRepository.doFindEligibleCategories(this.getEditedAthlete(), gender,
							ageFromFields, bw, qualifyingTotal);
					logger.debug/* edit */("cat-based allEligible {} {}", cat, this.allEligible);
					Category bestMatchCategory = bestMatch(this.allEligible);
					updateCategoryFields(bestMatchCategory, categoryField, eligibleField, qualifyingTotalField2,
							this.allEligible,
							true);
				}
			} else if (genderField.getValue() != null && age != null) {
				// use age and qualifying total
				logger.debug/* edit */("using age and total");
				this.allEligible = findEligibleCategories(genderField, getAgeFromFields(), bodyWeightField,
						categoryField, qualifyingTotalField2);
				updateCategoryFields(null, categoryField, eligibleField, qualifyingTotalField2,
						this.allEligible, true);

			} else {
				// cannot compute eligibility and category
				logger.debug/* edit */("cat, but need age");
			}
		}

	}

	private void safeCategorySetItems(List<Category> categories) {
		Category curCat = this.categoryField.getValue();
		logger.debug/* edit */("safeCategorySetItems curCat={}", curCat);
		boolean listenersEnabled = isChangeListenersEnabled();
		try {
			setChangeListenersEnabled(false);
			this.categoryField.setItems(categories);
		} finally {
			setChangeListenersEnabled(listenersEnabled);
		}
		this.categoryField.setValue(curCat);
	}

	private Hr separator() {
		Hr hr = new Hr();
		hr.getStyle().set("margin-top", "0.4em");
		hr.getStyle().set("margin-bottom", "0.4em");
		hr.getStyle().set("background-color", "var(--lumo-contrast-30pct)");
		hr.getStyle().set("height", "2px");
		return hr;
	}

	private void setChangeListenersEnabled(boolean changeListenersEnabled) {
		this.changeListenersEnabled = changeListenersEnabled;
	}

	private void setCheckOther20kgFields(boolean checkOther20kgFields) {
		logger.debug("checkOther20kgFields={} {}", checkOther20kgFields, LoggerUtils.whereFrom());
		this.checkOther20kgFields = checkOther20kgFields;
	}

	private void setCurrentGroup(Group currentGroup) {
		this.currentGroup = currentGroup;
	}

	private void setEditedAthlete(Athlete editedAthlete) {
		this.editedAthlete = editedAthlete;
	}

	private void setFocus(Component form) {
		form.addAttachListener((e) -> {
			String lastNameValue;
			Double bwValue;
			if ((lastNameValue = this.lastNameField.getValue()) == null || lastNameValue.isBlank()) {
				this.lastNameField.focus();
			} else if ((bwValue = this.bodyWeightField.getValue()) == null || bwValue < 0.01D) {
				this.bodyWeightField.focus();
			} else {
				this.groupField.focus();
			}
		});
	}

	private void setupAthlete(CrudOperation operation, Athlete aFromList) {
		if (operation == CrudOperation.ADD) {
			Athlete editedAthlete2 = new Athlete();
			if (getCurrentGroup() != null) {
				editedAthlete2.setGroup(getCurrentGroup());
			}
			logger.debug("created new Athlete {}", System.identityHashCode(editedAthlete2));
			setEditedAthlete(editedAthlete2);
		} else if (aFromList != null) {
			setEditedAthlete(AthleteRepository.findById(aFromList.getId()));
		}
	}

	private void updateCategoryFields(Category bestMatch, ComboBox<Category> categoryField,
			CheckboxGroup<Category> eligibleField, LocalizedIntegerField qualifyingTotalField2,
			List<Category> allEligible,
			boolean recomputeEligibles) {

		LinkedHashSet<Category> newEligibles = new LinkedHashSet<>();
		Set<Category> prevEligibles;
		if (recomputeEligibles) {
			prevEligibles = new LinkedHashSet<>();
			prevEligibles.addAll(allEligible);
		} else {
			prevEligibles = eligibleField.getValue();
		}
		logger.debug/* edit */("updateCategoryFields {} {} - {} {} {}",
				categoryField.getValue(), bestMatch, prevEligibles.size(), allEligible.size(),
				LoggerUtils.whereFrom());

		if (prevEligibles != null) {
			// update the list of eligible categories. Must use the matching items in
			// allEligibles so that database updates work.
			for (Category oldEligible : prevEligibles) {
				for (Category newEligible : allEligible) {
					if (newEligible.getCode().contentEquals(oldEligible.getCode())) {
						logger.debug("substituting eligibles {} {}", newEligible.longDump(),
								System.identityHashCode(newEligible));
						newEligibles.add(newEligible);
						break;
					}
				}
			}
			logger.debug/* edit */("updateCategoryFields all eligibles {}",
					allEligible.stream().map(v -> v.shortDump()).collect(Collectors.toList()));

			List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
					getAgeFromFields(), null);

			boolean listenerStatus = isChangeListenersEnabled();
			try {
				setChangeListenersEnabled(false);
				safeCategorySetItems(pertinentCategories);
				eligibleField.setItems(allEligible);
			} finally {
				setChangeListenersEnabled(listenerStatus);
			}
			eligibleField.setValue(newEligibles);
		}

		Category matchingEligible = null;
		if (bestMatch != null) {
			// find the matching eligible with the same code.
			matchingEligible = null;
			for (Category eligible : newEligibles) {
				if (eligible.getCode().contentEquals(bestMatch.getCode())) {
					matchingEligible = eligible;
					break;
				}
			}
			logger.debug/* edit */("category {} {} matching eligible {}", categoryField, bestMatch, matchingEligible);
			categoryField.setValue(matchingEligible);
		} else {
			logger.debug("category is null");
		}
	}

	private boolean validateStartingTotals(
			LocalizedIntegerField snatch1DeclarationField2, LocalizedIntegerField cleanJerk1DeclarationField2,
			LocalizedIntegerField qualifyingTotalField2) throws RuleViolationException.Rule15_20Violated {
		if (isIgnoreErrors()) {
			return true;
		}
		try {
			// logger.debug("before {} validation", snatch1DeclarationField2);
			getEditedAthlete().validateStartingTotalsRule(
					zeroIfNull(this.snatch1DeclarationField),
					zeroIfNull(this.cleanJerk1DeclarationField),
					zeroIfNull(this.qualifyingTotalField));
			// clear errors on other fields.
			// logger.debug("clearing errors on {} {}", cleanJerk1DeclarationField2,
			// qualifyingTotalField2);
			clearErrors(cleanJerk1DeclarationField2, qualifyingTotalField2);
			// logger.debug("checking again on {} {}", cleanJerk1DeclarationField2,
			// qualifyingTotalField2);
			checkOther20kgFields(cleanJerk1DeclarationField2, qualifyingTotalField2);
		} catch (Exception e1) {
			// logger.debug("{} validation failed", snatch1DeclarationField2);
			// signal exception on all fields that must be mutually-consistent
			checkOther20kgFields(cleanJerk1DeclarationField2, qualifyingTotalField2);
			throw e1;
		}
		return true;
	}

	// private String zeroIfEmpty(String snatch1Declaration) {
	// if (snatch1Declaration == null || snatch1Declaration.isBlank()) {
	// return "0";
	// }
	// return snatch1Declaration;
	// }

	private int zeroIfNull(LocalizedIntegerField intField) {
		Integer value = intField.getValue();
		return value != null ? value : 0;
	}
}