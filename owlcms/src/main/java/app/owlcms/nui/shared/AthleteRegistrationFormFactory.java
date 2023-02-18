/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.value.ValueChangeMode;

import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.LocalizedDecimalField;
import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
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
public final class AthleteRegistrationFormFactory extends OwlcmsCrudFormFactory<Athlete> implements NavigationPage {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteRegistrationFormFactory.class);

	HasValue<?, ?> dateField = null;

	private List<Category> allEligible;

	private LocalizedDecimalField bodyWeightField;

	private boolean catGenderOk;

	private boolean changeListenersEnabled;

	private boolean checkOther20kgFields;

	private Athlete editedAthlete = null;

	private boolean genderCatOk;

	private Button hiddenButton;

	private TextField lastNameField;

	private Object lastNameValue;

	private Button printButton;

	private StringToIntegerConverter yobConverter;

	private Checkbox ignoreErrorsCheckbox;

	private Group currentGroup;

	private Category initialCategory;

	public AthleteRegistrationFormFactory(Class<Athlete> domainType, Group group) {
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

		Label spacer = new Label();
		footerLayout.add(spacer, operationTrigger);
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
		printButton = new Button(Translator.translate("AthleteCard"), IronIcons.PRINT.create());

		// logger.trace("buildNewForm {} {} {}", System.identityHashCode(this),
		// getCurrentGroup(),
		// LoggerUtils.stackTrace());
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

		// when in weigh-in, the validations need to ignore the weight that was last
		// loaded.
		OwlcmsSession.setAttribute("weighIn", getEditedAthlete());

		hiddenButton = new Button("doit");
		hiddenButton.getStyle().set("visibility", "hidden");
		enablePrint(getEditedAthlete());
		printButton.setThemeName("secondary success");

		// ensure that writeBean() is called; this horror is due to the fact that we
		// must open a new window from the client side, and cannot save on click.
		printButton.addClickListener(click -> {
			try {
				Athlete editedAthlete2 = getEditedAthlete();
				if (isIgnoreErrors()) {
					getEditedAthlete().setValidation(false);
				}
				binder.writeBean(editedAthlete2);
				this.update(editedAthlete2);
				hiddenButton.clickInClient();
			} catch (ValidationException e) {
				getEditedAthlete().setValidation(true);
				binder.validate();
			}

		});

		setChangeListenersEnabled(false);
		Component form = super.buildNewForm(operation, getEditedAthlete(), readOnly, cancelButtonClickListener,
		        operationButtonClickListener, deleteButtonClickListener, hiddenButton, printButton);

		// binder has read bean.
		filterCategories(getEditedAthlete().getCategory(), operation != CrudOperation.ADD);

		lastNameField = null;
		lastNameValue = null;
		try {
			Binding<Athlete, ?> lastNameBinding = binder.getBinding("lastName").get();
			lastNameField = (TextField) lastNameBinding.getField();
			lastNameValue = lastNameField.getValue();
		} catch (Exception e1) {
		}
		form.addAttachListener((e) -> {
			if (lastNameField != null && lastNameValue == null) {
				lastNameField.focus();
			} else {
				bodyWeightField.focus();
			}
		});

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
			printButton.setEnabled(false);
		} else {
			printButton.setEnabled(true);
			hiddenButton.getElement().setAttribute("onClick",
			        getWindowOpenerFromClass(AthleteCard.class, domainObject.getId().toString()));
		}
	}

	@Override
	public Collection<Athlete> findAll() {
		throw new UnsupportedOperationException(); // should be called on the grid, not on the form
	}

	@Override
	public void setValidationStatusHandler(boolean showErrorsOnFields) {
		binder.setValidationStatusHandler((s) -> {
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

	private Category bestMatch(List<Category> allEligible2) {
		return allEligible2 != null ? (allEligible2.size() > 0 ? allEligible2.get(0) : null) : null;
	}

	private Checkbox buildIgnoreErrorsCheckbox() {
		ignoreErrorsCheckbox = new Checkbox(Translator.translate("RuleViolation.ignoreErrors"), e -> {
			if (BooleanUtils.isTrue(isIgnoreErrors())) {
				logger./**/warn/**/("{}!Errors ignored - checkbox override for athlete {}",
				        OwlcmsSession.getFop().getLoggingName(), this.getEditedAthlete().getShortName());
				// binder.validate();
				binder.writeBeanAsDraft(editedAthlete, true);
			}

		});
		ignoreErrorsCheckbox.getStyle().set("margin-left", "3em");
		return ignoreErrorsCheckbox;
	}

	private boolean categoryIsEligible(Category category, List<Category> eligibles) {
		return eligibles.stream().anyMatch(c -> c.sameAs(category));
	}

	private void checkOther20kgFields(String prop1, String prop2) {
		logger.debug("entering checkOther20kgFields {} {}", isCheckOther20kgFields(), LoggerUtils.whereFrom());
		if (isCheckOther20kgFields()) {
			setCheckOther20kgFields(false); // prevent recursion
			Binding<Athlete, ?> prop1Binding = binder.getBinding(prop1).get();
			logger.debug("before {} explicit validate", prop1);
			prop1Binding.validate();
			Binding<Athlete, ?> prop2Binding = binder.getBinding(prop2).get();
			logger.debug("before {} explicit validate", prop2);
			prop2Binding.validate();
		}
	}

	private void clearErrors(String otherProp1, String otherProp2) {
		Binding<Athlete, ?> prop1Binding = binder.getBinding(otherProp1).get();
		((TextField) prop1Binding.getField()).setInvalid(false);
		Binding<Athlete, ?> prop2Binding = binder.getBinding(otherProp2).get();
		((TextField) prop2Binding.getField()).setInvalid(false);
	}

	private String computeDesc(Group g) {
		String desc = g.getDescription();
		if (desc != null && !desc.isBlank()) {
			return (g.getName() + " - " + desc);
		} else {
			return g.getName();
		}
	}

	private void configure20kgWeightField(HasValue<?, ?> field) {
		TextField textField = (TextField) field;
		textField.setAutoselect(true);
		textField.setValueChangeMode(ValueChangeMode.ON_BLUR);
		textField.setPattern("^(-?\\d+)|()$"); // optional minus and at least one digit, or empty.
		textField.setAllowedCharPattern("[0-9-]");
		textField.addValueChangeListener((e) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			setCheckOther20kgFields(true);
		});
	}

	private List<Category> doFindEligibleCategories(Gender gender, Integer ageFromFields, Double bw,
	        int qualifyingTotal) {
		allEligible = CategoryRepository.findByGenderAgeBW(gender, ageFromFields, bw);
		allEligible = allEligible.stream().filter(c -> qualifyingTotal >= c.getQualifyingTotal())
		        .collect(Collectors.toList());
		return allEligible;
	}

	@SuppressWarnings({ "unchecked" })
	private void filterCategories(Category category, boolean initCategories) {
		setChangeListenersEnabled(false);

		Binding<Athlete, ?> groupBinding = binder.getBinding("group").get();
		ComboBox<Group> groupField = (ComboBox<Group>) groupBinding.getField();
		Group curGroup = groupField.getValue();
		// force reading all groups
		List<Group> allGroups = GroupRepository.findAll();
		allGroups.sort(new NaturalOrderComparator<Group>());
		groupField.setItems(allGroups);
		groupField.setValue(curGroup);
		groupField.setRenderer(new TextRenderer<Group>(g -> computeDesc(g)));
		groupField.setItemLabelGenerator(g -> computeDesc(g));

		Binding<Athlete, ?> genderBinding = binder.getBinding("gender").get();
		ComboBox<Gender> genderField = (ComboBox<Gender>) genderBinding.getField();

		Binding<Athlete, ?> bodyWeightBinding = binder.getBinding("bodyWeight").get();
		bodyWeightField = (LocalizedDecimalField) bodyWeightBinding.getField();

		Binding<Athlete, ?> categoryBinding = binder.getBinding("category").get();
		ComboBox<Category> categoryField = (ComboBox<Category>) categoryBinding.getField();
		categoryField.setClearButtonVisible(true);

		Binding<Athlete, ?> eligibleBinding = binder.getBinding("eligibleCategories").get();
		CheckboxGroup<Category> eligibleField = (CheckboxGroup<Category>) eligibleBinding.getField();

		Binding<Athlete, ?> qualifyingTotalBinding = binder.getBinding("qualifyingTotal").get();
		TextField qualifyingTotalField = (TextField) qualifyingTotalBinding.getField();

		if (initCategories) {
			allEligible = findEligibleCategories(genderField, getAgeFromFields(), bodyWeightField,
			        categoryField, qualifyingTotalField);
			logger.trace("**gender = {}, eligible = {}", genderField.getValue(), allEligible);
			// ListDataProvider<Category> listDataProvider = new
			// ListDataProvider<>(allEligible);
			updateCategoryFields(category, categoryField, eligibleField, qualifyingTotalField, allEligible, false);
		}

		genderField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
			        getAgeFromFields(), null);
			categoryField.setItems(pertinentCategories);
			recomputeCategories(genderField, bodyWeightField, categoryField, eligibleField, dateField,
			        qualifyingTotalField);
		});

		if (Competition.getCurrent().isUseBirthYear()) {
			Optional<Binding<Athlete, ?>> yobBinding = binder.getBinding("yearOfBirth");
			HasValue<?, String> yobField = (HasValue<?, String>) yobBinding.get().getField();
			dateField = yobField;
			// Workaround for bug
			// https://stackoverflow.com/questions/55532055/java-casting-java-11-throws-lambdaconversionexception-while-1-8-does-not
			ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
				if (!isChangeListenersEnabled()) {
					return;
				}
				List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
				        getAgeFromFields(), null);
				categoryField.setItems(pertinentCategories);
				recomputeCategories(genderField, bodyWeightField, categoryField, eligibleField, dateField,
				        qualifyingTotalField);
			};
			yobField.addValueChangeListener(listener);
		} else {
			Optional<Binding<Athlete, ?>> fbdBinding = binder.getBinding("fullBirthDate");
			HasValue<?, LocalDate> birthDateField = (HasValue<?, LocalDate>) fbdBinding.get().getField();
			dateField = birthDateField;
			ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
				if (!isChangeListenersEnabled()) {
					return;
				}
				List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
				        getAgeFromFields(), null);
				categoryField.setItems(pertinentCategories);
				recomputeCategories(genderField, bodyWeightField, categoryField, eligibleField, dateField,
				        qualifyingTotalField);
			};
			birthDateField.addValueChangeListener(listener);
		}

		TextField wrappedBWTextField = bodyWeightField.getWrappedTextField();
		wrappedBWTextField.setValueChangeMode(ValueChangeMode.ON_BLUR);
		wrappedBWTextField.setAutoselect(true);
		bodyWeightField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled() || bodyWeightField.isInvalid()) {
				return;
			}
			recomputeCategories(genderField, bodyWeightField, categoryField, eligibleField, dateField,
			        qualifyingTotalField);
		});

		categoryField.addValueChangeListener((vc) -> {
			Category value = vc.getValue();
			// logger.debug("category new value {}",value);
			if (!isChangeListenersEnabled()) {
				return;
			}
			setChangeListenersEnabled(false); // prevent recursion.
			if (genderField.getValue() == null) {
				genderField.setValue(value.getGender());
			}
			if (value == null) {
				eligibleField.setItems(new ArrayList<>());
			} else {
				recomputeCategories(genderField, bodyWeightField, categoryField, eligibleField, dateField,
				        qualifyingTotalField);
			}
			setChangeListenersEnabled(true);
		});

		eligibleField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled()) {
				return;
			}
			setChangeListenersEnabled(false); // prevent recursion.
			// false as last argument: do not reset to all eligible categories
			// logger.debug("eligibleField update");
			Set<Category> selectedCategories = eligibleField.getSelectedItems();
			allEligible = findEligibleCategories(genderField, getAgeFromFields(), bodyWeightField, categoryField,
			        qualifyingTotalField);
			Stream<Category> filter = allEligible.stream().filter(c -> c.sameAsAny(selectedCategories));
//            //logger.debug("selected categories in order {}", filter.collect(Collectors.toList()));
//            filter = allEligible.stream().filter(c -> c.sameAsAny(selectedCategories));
			Category category2 = filter.findFirst().orElse(null);
			categoryField.setValue(category2);
//          updateCategoryFields(category2, categoryField, eligibleField, qualifyingTotalField, allEligible, true);
//            updateCategoryFields(categoryField.getValue(), categoryField, eligibleField, qualifyingTotalField,
//                    allEligible, false);
			setChangeListenersEnabled(true);
		});

		qualifyingTotalField.addValueChangeListener((vc) -> {
			if (!isChangeListenersEnabled() || qualifyingTotalField.isInvalid()) {
				return;
			}
			recomputeCategories(genderField, bodyWeightField, categoryField, eligibleField, dateField,
			        qualifyingTotalField);
		});
		qualifyingTotalField.setAutoselect(true);

		setChangeListenersEnabled(true);
	}

	private List<Category> findEligibleCategories(ComboBox<Gender> genderField, Integer ageFromFields,
	        LocalizedDecimalField bodyWeightField, ComboBox<Category> categoryField, TextField qualifyingTotalField) {
		// best match is first
		Double bw = bodyWeightField.getValue();
		Double catW = categoryField.getValue() != null ? categoryField.getValue().getMaximumWeight() : null;
		if (bw == null) {
			bw = catW;
		}
		return doFindEligibleCategories(genderField.getValue(),
		        ageFromFields, bw, Athlete.zeroIfInvalid(qualifyingTotalField.getValue()));
	}

	@SuppressWarnings("unchecked")
	private Integer getAgeFromFields() {
		Integer age = null;
		if (Competition.getCurrent().isUseBirthYear()) {
			Optional<Binding<Athlete, ?>> yobBinding = binder.getBinding("yearOfBirth");
			HasValue<?, String> yobField = (HasValue<?, String>) yobBinding.get().getField();
			Result<Integer> yR = yobConverter.convertToModel(yobField.getValue(),
			        new ValueContext(OwlcmsSession.getLocale()));
			try {
				Integer yob = yR.getOrThrow((message) -> {
					return new Exception(message);
				});
				age = (LocalDate.now().getYear()) - yob;
			} catch (Exception e) {
				age = null;
			}
		} else {
			Optional<Binding<Athlete, ?>> fbdBinding = binder.getBinding("fullBirthDate");
			HasValue<?, LocalDate> dateField = (HasValue<?, LocalDate>) fbdBinding.get().getField();
			LocalDate date = dateField.getValue();
			if (date != null) {
				age = (LocalDate.now().getYear() - date.getYear());
			}
		}
		return age;
	}

	private Group getCurrentGroup() {
		return currentGroup;
	}

	private Athlete getEditedAthlete() {
		return editedAthlete;
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private Gender getGenderFieldValue() {
		HasValue<?, Gender> genderField = (HasValue<?, Gender>) binder.getBinding("gender").get().getField();
		Gender gender = genderField.getValue();
		return gender;
	}

	@SuppressWarnings("unchecked")
	private Integer getIntegerFieldValue(String property) {
		Optional<Binding<Athlete, ?>> binding = binder.getBinding(property);
		HasValue<?, String> field = (HasValue<?, String>) binding.get().getField();
		return Athlete.zeroIfInvalid(field.getValue());
	}

	private boolean isChangeListenersEnabled() {
		return changeListenersEnabled;
	}

	private boolean isCheckOther20kgFields() {
		return checkOther20kgFields;
	}

	private boolean isIgnoreErrors() {
		return BooleanUtils.isTrue(ignoreErrorsCheckbox == null ? null : ignoreErrorsCheckbox.getValue());
	}

	private void recomputeCategories(
	        ComboBox<Gender> genderField, LocalizedDecimalField bodyWeightField,
	        ComboBox<Category> categoryField, CheckboxGroup<Category> eligibleField,
	        HasValue<?, ?> dateField, TextField qualifyingTotalField) {

		Category value = categoryField.getValue();
		Integer age = getAgeFromFields();
		if (bodyWeightField.getValue() != null) {
			if (genderField.getValue() != null && age != null) {
				// body weight, gender, date
				allEligible = findEligibleCategories(genderField, getAgeFromFields(), bodyWeightField,
				        categoryField, qualifyingTotalField);
				// logger.debug("cat {} eli {}", value, allEligible);
				if (value != null && categoryIsEligible(value, allEligible)) {
					// current category is amongst eligibles. Don't recompute anything.
					// logger.debug("leave alone");
				} else {
					// logger.debug("recompute");
					// category is null or not within eligibles, recompute
					Category bestMatchCategory = bestMatch(allEligible);
					updateCategoryFields(bestMatchCategory, categoryField, eligibleField, qualifyingTotalField,
					        allEligible, true);
				}
			} else {
				// cannot compute eligibility and category
			}
		} else {
			// no body weight, but category available.
			if (genderField.getValue() != null && age != null && value != null) {
				Double bw = value.getMaximumWeight();
				int qualifyingTotal = Athlete.zeroIfInvalid(qualifyingTotalField.getValue());
				Integer ageFromFields = getAgeFromFields();
				if (ageFromFields != null && ageFromFields > 5 && ageFromFields < 120) {
					doFindEligibleCategories(genderField.getValue(), ageFromFields, bw, qualifyingTotal);
					Category bestMatchCategory = bestMatch(allEligible);
					updateCategoryFields(bestMatchCategory, categoryField, eligibleField, qualifyingTotalField,
					        allEligible,
					        true);
				}
			} else {
				// cannot compute eligibility and category
			}
		}

//        if (bodyWeightField.getValue() != null && genderField.getValue() != null && dateField.getValue() != null) {
//            allEligible = findEligibleCategories(genderField, bodyWeightField, qualifyingTotalField);
//            Category category2 = bestMatch(allEligible);
//            updateCategoryFields(category2, categoryField, eligibleField, qualifyingTotalField, allEligible, true);
//        } else if (bodyWeightField.getValue() == null && genderField.getValue() != null && dateField.getValue() != null && categoryField.getValue() != null) {
//            Double bw = categoryField.getValue().getMaximumWeight();
//            int qualifyingTotal = Athlete.zeroIfInvalid(qualifyingTotalField.getValue());
//            Integer ageFromFields = getAgeFromFields();
//            if (ageFromFields != null && ageFromFields > 5 && ageFromFields < 120) {
//                doFindEligibleCategories(genderField.getValue(), ageFromFields, bw, qualifyingTotal);
//                Category category2 = bestMatch(allEligible);
//                updateCategoryFields(category2, categoryField, eligibleField, qualifyingTotalField, allEligible, true);
//            }
//        }
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

	private void updateCategoryFields(Category bestMatch, ComboBox<Category> categoryField,
	        CheckboxGroup<Category> eligibleField, TextField qualifyingTotalField, List<Category> allEligible,
	        boolean recomputeEligibles) {

		LinkedHashSet<Category> newEligibles = new LinkedHashSet<>();
		Set<Category> prevEligibles;
		if (recomputeEligibles) {
			prevEligibles = new LinkedHashSet<>();
			prevEligibles.addAll(allEligible);
		} else {
			prevEligibles = eligibleField.getValue();
		}
		// logger.debug("updateCategoryFields {} {} - {} {} {}",
		// categoryField.getValue(), bestMatch, prevEligibles, allEligible,
		// LoggerUtils.whereFrom());

		if (prevEligibles != null) {
			// update the list of eligible categories. Must use the matching items in
			// allEligibles so that
			// database updates work.

			for (Category oldEligible : prevEligibles) {
				for (Category newEligible : allEligible) {
					if (ObjectUtils.compare(newEligible.getName(), oldEligible.getName()) == 0) {
						logger.debug("substituting eligibles {} {}", newEligible.longDump(),
						        System.identityHashCode(newEligible));
						newEligibles.add(newEligible);
						break;
					}
				}
			}
			logger.debug("all eligibles {}", allEligible.stream().map(v -> v.longDump()).collect(Collectors.toList()));

			List<Category> pertinentCategories = CategoryRepository.findByGenderAgeBW(getGenderFieldValue(),
			        getAgeFromFields(), null);
			categoryField.setItems(pertinentCategories);
			eligibleField.setItems(allEligible);
			eligibleField.setValue(newEligibles);
		}

		Category matchingEligible = null;
		if (bestMatch != null) {
			// we can't have category without eligibility relationship and one with same id
			// that has it in the
			// eligibility list
			// so we find the one in the eligibility list and use it.
			matchingEligible = null;
			for (Category eligible : newEligibles) {
				if (ObjectUtils.compare(eligible.getName(), bestMatch.getName()) == 0) {
					matchingEligible = eligible;
					break;
				}
			}
			categoryField.setValue(matchingEligible);
			// logger.debug("category {} {} matching eligible {} {}", bestMatch,
			// System.identityHashCode(bestMatch), matchingEligible,
			// System.identityHashCode(matchingEligible));
		} else {
			logger.debug("category is null");
		}
	}

	private boolean validateStartingTotals(String mainProp, String otherProp1, String otherProp2) {
		if (isIgnoreErrors()) {
			return true;
		}
		try {
			logger.debug("before {} validation", mainProp);
			getEditedAthlete().validateStartingTotalsRule(getIntegerFieldValue("snatch1Declaration"),
			        getIntegerFieldValue("cleanJerk1Declaration"), getIntegerFieldValue("qualifyingTotal"));
			// clear errors on other fields.
			logger.debug("clearing errors on {} {}", otherProp1, otherProp2);
			clearErrors(otherProp1, otherProp2);
			logger.debug("checking again on {} {}", otherProp1, otherProp2);
			checkOther20kgFields(otherProp1, otherProp2);
		} catch (Exception e1) {
			logger.debug("{} validation failed", mainProp);
			// signal exception on all fields that must be mutually-consistent
			checkOther20kgFields(otherProp1, otherProp2);
			throw e1;
		}
		return true;
	}

	/**
	 * Add the field-level validations
	 *
	 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#bindField(com.vaadin.flow.component.HasValue,
	 *      java.lang.String, java.lang.Class)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
		Binder.BindingBuilder bindingBuilder = binder.forField(field);

		if ("bodyWeight".equals(property)) {
			validateBodyWeight(bindingBuilder, ((LocalizedDecimalField) field).isRequired());
			bindingBuilder.bind(property);
		} else if ("fullBirthDate".equals(property)) {
			validateFullBirthDate(bindingBuilder);
			bindingBuilder.bind(property);
		} else if ("yearOfBirth".equals(property)) {
			validateYearOfBirth(bindingBuilder);
			((TextField) field).setValueChangeMode(ValueChangeMode.EAGER);
			bindingBuilder.bind(property);
		} else if ("category".equals(property)) {
			validateCategory(bindingBuilder);
			bindingBuilder.bind(property);
		} else if ("gender".equals(property)) {
			validateGender(bindingBuilder);
			bindingBuilder.bind(property);
		} else if (property.endsWith("snatch1Declaration")) {
			configure20kgWeightField(field);
			Validator<String> v2 = ValidationUtils.<String>checkUsingException((unused) -> {
				return validateStartingTotals("snatch1Declaration", "cleanJerk1Declaration", "qualifyingTotal");
			});
			bindingBuilder.withValidator(v2);
			bindingBuilder.bind(property);
		} else if (property.endsWith("cleanJerk1Declaration")) {
			configure20kgWeightField(field);
			Validator<String> v2 = ValidationUtils.<String>checkUsingException((unused) -> {
				return validateStartingTotals("cleanJerk1Declaration", "snatch1Declaration", "qualifyingTotal");
			});
			bindingBuilder.withValidator(v2);
			bindingBuilder.bind(property);
		} else if ("qualifyingTotal".equals(property)) {
			configure20kgWeightField(field);
			Validator<String> v2 = ValidationUtils.<String>checkUsingException((unused) -> {
				return validateStartingTotals("qualifyingTotal", "snatch1Declaration", "cleanJerk1Declaration");
			});

			bindingBuilder.withValidator(v2);
			bindingBuilder.withConverter(new StringToIntegerConverter(Translator.translate("InvalidIntegerValue")));
			bindingBuilder.bind(property);
		} else if ("eligibleCategories".equals(property)) {
			bindingBuilder.bind(property);
		} else {
			super.bindField(field, property, propertyType, c);
		}
	}

	/**
	 * Create a binder with our special validation status handling
	 *
	 * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildBinder(org.vaadin.crudui.crud.CrudOperation,
	 *      java.lang.Object)
	 */
	@Override
	protected Binder<Athlete> buildBinder(CrudOperation operation, Athlete ignored) {
		binder = super.buildBinder(operation, getEditedAthlete());
		initialCategory = getEditedAthlete().getCategory();
		setValidationStatusHandler(true);
		return binder;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateBodyWeight(Binder.BindingBuilder bindingBuilder, boolean isRequired) {
		Validator<Double> v1 = new DoubleRangeValidator(Translator.translate("Weight_under_350"), 0.1D, 350.0D);
		bindingBuilder.withValidator(v1);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateCategory(Binder.BindingBuilder bindingBuilder) {

		// check that category is consistent with body weight
		Validator<Category> v1 = Validator.from((category) -> {
			try {
				Binding<Athlete, ?> catBinding = binder.getBinding("category").get();
				Category cat = (Category) catBinding.getField().getValue();
				Binding<Athlete, ?> bwBinding = binder.getBinding("bodyWeight").get();
				Double bw = (Double) bwBinding.getField().getValue();
				if (category == null && bw == null) {
					logger.debug("1 category {} {} bw {}", category, cat, bw);
					return true;
				} else if (bw == null) {
					logger.debug("2 category {} {} bw {}", category != null ? category.getName() : null, cat, bw);
					// no body weight - no contradiction
					return true;
				} else if (bw != null && category == null) {
					logger.debug("3 category {} {} bw {}", category, cat, bw);
					return false;
				}
				Double min = category.getMinimumWeight();
				Double max = category.getMaximumWeight();
				logger.debug("comparing {} ]{},{}] with body weight {}", category.getName(), min, max, bw);
				return (bw > min && bw <= max);
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_body_weight"));
		bindingBuilder.withValidator(v1);

		// check that category is consistent with age
		Validator<Category> v2 = Validator.from((category) -> {
			try {
				Binding<Athlete, ?> catBinding = binder.getBinding("category").get();
				Category cat = (Category) catBinding.getField().getValue();
				Integer age = getAgeFromFields();
				if (category == null && age == null) {
					logger.debug("1 category {} {} age {}", category, cat, age);
					return true;
				} else if (age == null) {
					logger.debug("2 category {} {} age {}", category != null ? category.getName() : null, cat, age);
					// no body weight - no contradiction
					return true;
				}
				if (category != null && age != null) {
					int min = category.getAgeGroup().getMinAge();
					int max = category.getAgeGroup().getMaxAge();
					logger.debug("comparing {} [{},{}] with age {}", category.getName(), min, max, age);
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
			try {
				if (category == null) {
					return true;
				}
				Binding<Athlete, ?> genderBinding = binder.getBinding("gender").get();
				ComboBox<Gender> genderCombo = (ComboBox<Gender>) genderBinding.getField();
				Gender g = genderCombo.getValue();
				Gender catGender = category != null ? category.getGender() : null;
				logger.debug("categoryValidation: validating gender {} vs category {}: {} {}", g, catGender,
				        catGender == g);
				if (g == null) {
					// no gender - no contradiction
					return true;
				}
				catGenderOk = catGender == g;
				if (catGenderOk && !genderCatOk) {
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
			try {
				if (isIgnoreErrors() || initialCategory == null
				        || (getEditedAthlete().getBodyWeight() == null && category == null)) {
					return true;
				}
				logger.debug("initialCategory = {}  new category = {}", initialCategory,
				        getEditedAthlete().getCategory());
				return category != null && category.sameAs(initialCategory);
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("CategoryChange_MustForce", initialCategory));
		bindingBuilder.withValidator(v4);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateFullBirthDate(Binder.BindingBuilder bindingBuilder) {
		LocalDateField ldtf = (LocalDateField) bindingBuilder.getField();
		Validator<LocalDate> fv = ldtf.formatValidation(OwlcmsSession.getLocale());
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
	protected void validateGender(Binder.BindingBuilder bindingBuilder) {
		// check that category is consistent with gender
		Validator<Gender> v2 = Validator.from((g) -> {
			try {
				if (g == null) {
					return true;
				}
				Binding<Athlete, ?> catBinding = binder.getBinding("category").get();
				ComboBox<Category> categoryCombo = (ComboBox<Category>) catBinding.getField();
				Category category = categoryCombo.getValue();
				Gender catGender = category != null ? category.getGender() : null;
				logger.trace("genderValidation: validating gender {} vs category {}: {}", g, catGender, catGender == g);
				genderCatOk = catGender == null || catGender == g;
				if (genderCatOk && !catGenderOk) {
					// turn off message if present.
					logger.debug("checking category");
					catBinding.validate();
				}
				return genderCatOk;
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
			}
			return true;
		}, Translator.translate("Category_no_match_gender"));
		bindingBuilder.withValidator(v2);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void validateYearOfBirth(Binder.BindingBuilder bindingBuilder) {
		String message = Translator.translate("InvalidYearFormat");
		RegexpValidator re = new RegexpValidator(message, "|((19|20)[0-9][0-9])");
		bindingBuilder.withNullRepresentation("");
		bindingBuilder.withValidator(re);
		yobConverter = new StringToIntegerConverter(message) {
			@Override
			protected NumberFormat getFormat(java.util.Locale locale) {
				NumberFormat format = NumberFormat.getIntegerInstance();
				format.setGroupingUsed(false);
				return format;
			}
		};
		bindingBuilder.withConverter(yobConverter);
	}

}