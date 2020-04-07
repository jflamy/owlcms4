/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.BindingValidationStatus;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.value.ValueChangeMode;

import app.owlcms.components.NavigationPage;
import app.owlcms.components.fields.BodyWeightField;
import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.displays.athletecard.AthleteCard;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public final class AthleteRegistrationFormFactory extends OwlcmsCrudFormFactory<Athlete> implements NavigationPage {
    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteRegistrationFormFactory.class);

    private Athlete editedAthlete = null;

    private boolean catGenderOk;

    private boolean genderCatOk;

    private Button printButton;

    private Button hiddenButton;

    private StringToIntegerConverter yobConverter;

    private boolean checkOther20kgFields;

    public AthleteRegistrationFormFactory(Class<Athlete> domainType) {
        super(domainType);
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
     */
    @Override
    public Athlete add(Athlete athlete) {
        AthleteRepository.save(athlete);
        enablePrint(athlete);
        return athlete;
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

    @Override
    public Component buildNewForm(CrudOperation operation, Athlete aFromList, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {
        printButton = new Button(Translator.translate("AthleteCard"), IronIcons.PRINT.create());

        setEditedAthlete(AthleteRepository.findById(aFromList.getId()));
        hiddenButton = new Button("doit");
        hiddenButton.getStyle().set("visibility", "hidden");
        enablePrint(getEditedAthlete());
        printButton.setThemeName("secondary success");

        // ensure that writeBean() is called; this horror is due to the fact that we
        // must open a new window from the client side, and cannot save on click.
        printButton.addClickListener(click -> {
            try {
                binder.writeBean(getEditedAthlete());
                this.update(getEditedAthlete());
                hiddenButton.clickInClient();
            } catch (ValidationException e) {
                binder.validate();
            }

        });

        Component form = super.buildNewForm(operation, getEditedAthlete(), readOnly, cancelButtonClickListener,
                operationButtonClickListener, deleteButtonClickListener, hiddenButton, printButton);
        filterCategories(getEditedAthlete().getCategory());
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
        if (domainObject.getId() == null) {
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
    public String getPageTitle() {
        // not used
        return null;
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        // not used
        return null;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        // not used
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
            valid = !s.hasErrors();
            s.notifyBindingValidationStatusHandlers();
        });
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
     */
    @Override
    public Athlete update(Athlete athlete) {
        AthleteRepository.save(athlete);
//        logger.debug("saved id={} {} {} {}", athlete.getId(), athlete.getSnatch1Declaration(),
//                athlete.getCleanJerk1Declaration());
//        logger.debug("merged id={} {} {}", merged.getId(), merged.getSnatch1Declaration(),
//                merged.getCleanJerk1Declaration());
        return athlete;
    }

    /**
     * Add the field-level validations
     *
     * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#bindField(com.vaadin.flow.component.HasValue,
     *      java.lang.String, java.lang.Class)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        Binder.BindingBuilder bindingBuilder = binder.forField(field);

        if ("bodyWeight".equals(property)) {
            validateBodyWeight(bindingBuilder, ((BodyWeightField) field).isRequired());
            bindingBuilder.bind(property);
        } else if ("fullBirthDate".equals(property)) {
            validateFullBirthDate(bindingBuilder);
//            field.addValueChangeListener((e) -> {
//                Gender gender = getGenderFieldValue();
//                setAgeGroupFromFullBirthDate(gender);
//            });
            bindingBuilder.bind(property);
        } else if ("yearOfBirth".equals(property)) {
            validateYearOfBirth(bindingBuilder);
            ((TextField) field).setValueChangeMode(ValueChangeMode.EAGER);
//            field.addValueChangeListener((event) -> {
//                Result<Integer> yR = yobConverter.convertToModel((String) event.getValue(),
//                        new ValueContext(OwlcmsSession.getLocale()));
//                yR.ifOk((r) -> {
//                    Gender gender = getGenderFieldValue();
//                    setAgeGroupFromYearOfBirth(gender);
//                });
//            });
            bindingBuilder.bind(property);
        } else if ("category".equals(property)) {
            validateCategory(bindingBuilder);
            bindingBuilder.bind(property);
        } else if ("gender".equals(property)) {
            validateGender(bindingBuilder);
//            field.addValueChangeListener((e) -> {
//                Gender gender = (Gender) e.getValue();
//                if (Competition.getCurrent().isUseBirthYear()) {
//                    setAgeGroupFromYearOfBirth(gender);
//                } else {
//                    setAgeGroupFromFullBirthDate(gender);
//                }
//            });
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
        } else {
            super.bindField(field, property, propertyType);
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
        setValidationStatusHandler(true);
        return binder;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void validateBodyWeight(Binder.BindingBuilder bindingBuilder, boolean isRequired) {
        Validator<Double> v1 = new DoubleRangeValidator(Translator.translate("Weight_under_350"), 0.1D, 350.0D);
//        // check wrt body category
//        Validator<Double> v2 = Validator.from((weight) -> {
//            return (!isRequired && weight == null) || (weight != null && weight > 0.0);
//            // no need to do further validation because changing body weight resets the
//            // filter
//            // category drop-down, which causes a validation of the category.
//        }, Translator.translate("BodyWeight_no_match_category"));
        bindingBuilder.withValidator(v1);
//        bindingBuilder.withValidator(v2);
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
                    logger.debug("2 category {} {} bw {}", category.getName(), cat, bw);
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
                logger.error(LoggerUtils.stackTrace(e));
            }
            return true;
        }, Translator.translate("Category_no_match_body_weight"));
        bindingBuilder.withValidator(v1);

        // check that category is consistent with age
        Validator<Category> v11 = Validator.from((category) -> {
            try {
                Binding<Athlete, ?> catBinding = binder.getBinding("category").get();
                Category cat = (Category) catBinding.getField().getValue();
                Integer age = getAgeFromFields();
                if (category == null && age == null) {
                    logger.debug("1 category {} {} age {}", category, cat, age);
                    return true;
                } else if (age == null) {
                    logger.debug("2 category {} {} age {}", category.getName(), cat, age);
                    // no body weight - no contradiction
                    return true;
                }
//                else if (age != null && category == null) {
//                    logger.debug("3 category {} {} age {}", category, cat, age);
//                    return false;
//                }
                if (category != null && age != null) {
                    int min = category.getAgeGroup().getMinAge();
                    int max = category.getAgeGroup().getMaxAge();
                    logger.debug("comparing {} [{},{}] with age {}", category.getName(), min, max, age);
                    return (age >= min && age <= max);
                } else {
                    return true;
                }
            } catch (Exception e) {
                logger.error(LoggerUtils.stackTrace(e));
            }
            return true;
        }, Translator.translate("Category_no_match_age"));
        bindingBuilder.withValidator(v11);

        // check that category is consistent with gender
        Validator<Category> v2 = Validator.from((category) -> {
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
                logger.error(LoggerUtils.stackTrace(e));
            }
            return true;
        }, Translator.translate("Category_no_match_gender"));
        bindingBuilder.withValidator(v2);
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
                logger.error(LoggerUtils.stackTrace(e));
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

//    @SuppressWarnings("unchecked")
//    public void setAgeGroupFromFullBirthDate(Gender gender) {
//        Optional<Binding<Athlete, ?>> fbdBinding = binder.getBinding("fullBirthDate");
//        HasValue<?, LocalDate> dateField = (HasValue<?, LocalDate>) fbdBinding.get().getField();
//        Optional<Binding<Athlete, ?>> agBinding = binder.getBinding("mastersAgeGroup");
//        if (agBinding.isPresent()) {
//            HasValue<?, String> ageGroupField = (HasValue<?, String>) agBinding.get().getField();
//            LocalDate date = dateField.getValue();
//            if (gender != null && date != null) {
//                ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(), date.getYear()));
//            } else {
//                ageGroupField.setValue("");
//            }
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    public void setAgeGroupFromYearOfBirth(Gender gender) {
//        Optional<Binding<Athlete, ?>> yobBinding = binder.getBinding("yearOfBirth");
//        HasValue<?, String> yobField = (HasValue<?, String>) yobBinding.get().getField();
//        Optional<Binding<Athlete, ?>> agBinding = binder.getBinding("mastersAgeGroup");
//        if (agBinding.isPresent()) {
//            HasValue<?, String> ageGroupField = (HasValue<?, String>) agBinding.get().getField();
//            Result<Integer> yR = yobConverter.convertToModel(yobField.getValue(),
//                    new ValueContext(OwlcmsSession.getLocale()));
//            yR.ifOk((year) -> {
//                if (gender != null && year != null) {
//                    ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(), year));
//                } else {
//                    ageGroupField.setValue("");
//                }
//            });
//            yR.ifError((e) -> ageGroupField.setValue(""));
//        }
//    }

//    private void setMastersAgeGroupFromYOB(Integer year) {
//        HasValue<?, ?> genderField = binder.getBinding("gender").get().getField();
//        Optional<Binding<Athlete, ?>> magBinding = binder.getBinding("mastersAgeGroup");
//        if (magBinding.isPresent()) {
//            @SuppressWarnings("unchecked")
//            HasValue<?, String> ageGroupField = (HasValue<?, String>) magBinding.get().getField();
//            Gender gender = (Gender) genderField.getValue();
//            if (gender != null && year != null) {
//                ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(), year, AgeDivision.DEFAULT));
//            } else {
//                ageGroupField.setValue("");
//            }
//        }
//    }

    private void clearErrors(String otherProp1, String otherProp2) {
        Binding<Athlete, ?> prop1Binding = binder.getBinding(otherProp1).get();
        ((TextField) prop1Binding.getField()).setInvalid(false);
        Binding<Athlete, ?> prop2Binding = binder.getBinding(otherProp2).get();
        ((TextField) prop2Binding.getField()).setInvalid(false);
    }

    private void configure20kgWeightField(HasValue<?, ?> field) {
        TextField textField = (TextField) field;
        textField.setValueChangeMode(ValueChangeMode.ON_BLUR);
        textField.setPattern("^(-?\\d+)|()$"); // optional minus and at least one digit, or empty.
        textField.setPreventInvalidInput(true);
        textField.addValueChangeListener((e) -> {
            setCheckOther20kgFields(true);
        });
    }

    @SuppressWarnings({ "unchecked" })
    private void filterCategories(Category category) {
        Binding<Athlete, ?> genderBinding = binder.getBinding("gender").get();
        ComboBox<Gender> genderField = (ComboBox<Gender>) genderBinding.getField();

        Binding<Athlete, ?> categoryBinding = binder.getBinding("category").get();
        ComboBox<Category> categoryField = (ComboBox<Category>) categoryBinding.getField();

        Binding<Athlete, ?> bodyWeightBinding = binder.getBinding("bodyWeight").get();
        BodyWeightField bodyWeightField = (BodyWeightField) bodyWeightBinding.getField();

        Collection<Category> allEligible = CategoryRepository.findByGenderAgeBW(genderField.getValue(),
                getAgeFromFields(), bodyWeightField.getValue());
        ListDataProvider<Category> listDataProvider = new ListDataProvider<>(allEligible);
        categoryField.setDataProvider(listDataProvider);

        genderField.addValueChangeListener((vc) -> {
            Category cat = categoryField.getValue();
            ListDataProvider<Category> listDataProvider2 = new ListDataProvider<>(
                    CategoryRepository.findByGenderAgeBW(genderField.getValue(), getAgeFromFields(),
                            bodyWeightField.getValue()));
            categoryField.setDataProvider(listDataProvider2);
            categoryField.setValue(cat); // forces a validation and a meaningful message
        });

        if (Competition.getCurrent().isUseBirthYear()) {
            Optional<Binding<Athlete, ?>> yobBinding = binder.getBinding("yearOfBirth");
            HasValue<?, String> yobField = (HasValue<?, String>) yobBinding.get().getField();
            // Workaround for bug
            // https://stackoverflow.com/questions/55532055/java-casting-java-11-throws-lambdaconversionexception-while-1-8-does-not
            ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
                Category cat = categoryField.getValue();
                ListDataProvider<Category> listDataProvider2 = new ListDataProvider<>(
                        CategoryRepository.findByGenderAgeBW(genderField.getValue(), getAgeFromFields(),
                                bodyWeightField.getValue()));
                categoryField.setDataProvider(listDataProvider2);
                categoryField.setValue(cat); // forces a validation and a meaningful message
            };
            yobField.addValueChangeListener(listener);
        } else {
            Optional<Binding<Athlete, ?>> fbdBinding = binder.getBinding("fullBirthDate");
            HasValue<?, LocalDate> dateField = (HasValue<?, LocalDate>) fbdBinding.get().getField();
            ValueChangeListener<ValueChangeEvent<?>> listener = (vc) -> {
                Category cat = categoryField.getValue();
                ListDataProvider<Category> listDataProvider2 = new ListDataProvider<>(
                        CategoryRepository.findByGenderAgeBW(genderField.getValue(), getAgeFromFields(),
                                bodyWeightField.getValue()));
                categoryField.setDataProvider(listDataProvider2);
                categoryField.setValue(cat); // forces a validation and a meaningful message
            };
            dateField.addValueChangeListener(listener);
        }

        bodyWeightField.addValueChangeListener((vc) -> {
            if (bodyWeightField.isInvalid()) {
                return;
            }
            Category cat = categoryField.getValue();
            Collection<Category> findActiveEligible = CategoryRepository.findByGenderAgeBW(genderField.getValue(),
                    getAgeFromFields(), bodyWeightField.getValue());
            ListDataProvider<Category> listDataProvider2 = new ListDataProvider<>(findActiveEligible);
            categoryField.setDataProvider(listDataProvider2);
            categoryField.setValue(cat);
        });

        categoryField.setValue(category);
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

    private boolean isCheckOther20kgFields() {
        return checkOther20kgFields;
    }

    private void setCheckOther20kgFields(boolean checkOther20kgFields) {
        logger.debug("checkOther20kgFields={} {}", checkOther20kgFields, LoggerUtils.whereFrom());
        this.checkOther20kgFields = checkOther20kgFields;
    }

    private boolean validateStartingTotals(String mainProp, String otherProp1, String otherProp2) {
        try {
            logger.debug("before {} validation", mainProp);
            Athlete.validateStartingTotalsRule(getEditedAthlete(), getIntegerFieldValue("snatch1Declaration"),
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

    private Athlete getEditedAthlete() {
        return editedAthlete;
    }

    private void setEditedAthlete(Athlete editedAthlete) {
        this.editedAthlete = editedAthlete;
    }

}