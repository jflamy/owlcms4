/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.shared;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.DoubleRangeValidator;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.data.value.ValueChangeMode;

import app.owlcms.components.fields.BodyWeightField;
import app.owlcms.components.fields.LocalDateField;
import app.owlcms.components.fields.ValidationUtils;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public final class AthleteRegistrationFormFactory extends OwlcmsCrudFormFactory<Athlete> {
    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteRegistrationFormFactory.class);

    private Athlete editedAthlete = null;

    private boolean catGenderOk;

    private boolean genderCatOk;

    public AthleteRegistrationFormFactory(Class<Athlete> domainType) {
        super(domainType);
    }

    @Override
    public Component buildNewForm(CrudOperation operation, Athlete domainObject, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener) {
        Component form = super.buildNewForm(operation, domainObject, readOnly, cancelButtonClickListener,
                operationButtonClickListener, deleteButtonClickListener);
        filterCategories(domainObject.getCategory());
        return form;
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
     */
    @Override
    public Athlete add(Athlete athlete) {
        AthleteRepository.save(athlete);
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
        if (a.getLastName() == null && a.getFirstName() == null)
            return null;
        // If null, CrudLayout.showForm will build its own, for backward compatibility
        return a.getFullId();
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
     */
    @Override
    public void delete(Athlete athlete) {
        AthleteRepository.delete(athlete);
    }

    @SuppressWarnings({ "unchecked"})
    private void filterCategories(Category category) {
        Binding<Athlete, ?> categoryBinding = binder.getBinding("category").get();
        ComboBox<Category> categoryField = (ComboBox<Category>) categoryBinding.getField();
        Binding<Athlete, ?> genderBinding = binder.getBinding("gender").get();
        ComboBox<Gender> genderField = (ComboBox<Gender>) genderBinding.getField();
        ListDataProvider<Category> listDataProvider = new ListDataProvider<Category>(
                CategoryRepository.findActive(genderField.getValue()));
        categoryField.setDataProvider(listDataProvider);
        categoryField.setValue(category);
        genderField.addValueChangeListener((vc) -> {
            ListDataProvider<Category> listDataProvider2 = new ListDataProvider<Category>(
                    CategoryRepository.findActive(genderField.getValue()));
            categoryField.setDataProvider(listDataProvider2);
        });
    }

    @Override
    public Collection<Athlete> findAll() {
        throw new UnsupportedOperationException(); // should be called on the grid, not on the form
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
     */
    @Override
    public Athlete update(Athlete athlete) {
        AthleteRepository.save(athlete);
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
                        ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(), year));
                    } else {
                        ageGroupField.setValue(null);
                    }
                }
            });
            bindingBuilder.bind(property);
        } else if ("yearOfBirth".equals(property)) {
            validateYearOfBirth(bindingBuilder);
            HasValue<?, ?> bdateField = bindingBuilder.getField();
            bdateField.addValueChangeListener((e) -> {
                Integer year = Integer.parseInt((String) e.getValue());
                HasValue<?, ?> genderField = binder.getBinding("gender").get().getField();
                Optional<Binding<Athlete, ?>> magBinding = binder.getBinding("mastersAgeGroup");
                if (magBinding.isPresent()) {
                    HasValue<?, String> ageGroupField = (HasValue<?, String>) magBinding.get().getField();
                    Gender gender = (Gender) genderField.getValue();
                    if (gender != null) {
                        ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(), year));
                    } else {
                        ageGroupField.setValue(null);
                    }
                }
            });
            bindingBuilder.bind(property);
        } else if ("category".equals(property)) {
            validateCategory(bindingBuilder);
            bindingBuilder.bind(property);
        } else if ("gender".equals(property)) {
            validateGender(bindingBuilder);
            HasValue<?, ?> genderField = bindingBuilder.getField();
            genderField.addValueChangeListener((e) -> {
                Gender gender = (Gender) e.getValue();
                if (Competition.getCurrent().isUseBirthYear()) {
                    setAgeGroupFromYearOfBirth(gender);
                } else {
                    setAgeGroupFromFullBirthDate(gender);
                }
            });
            bindingBuilder.bind(property);
        } else if (property.endsWith("Declaration")) {
            logger.debug(property);
            TextField declField = (TextField) bindingBuilder.getField();
            declField.setPattern("^(-?\\d+)|()$"); // optional minus and at least one digit, or empty.
            declField.setPreventInvalidInput(true);
            declField.setValueChangeMode(ValueChangeMode.ON_BLUR);
            // ON_BLUR seems to be broken
//			declField.addValueChangeListener((vc) -> {
//				binder.getBinding(property).get().validate(true);
//				logger.debug("BLUR validation of {}", property);
//			});

            bindingBuilder.withValidator(
                    ValidationUtils.<String>checkUsing((unused) -> Athlete.validateStartingTotalsRule(editedAthlete,
                            getIntegerFieldValue("snatch1Declaration"), getIntegerFieldValue("cleanJerk1Declaration"),
                            getIntegerFieldValue("qualifyingTotal")), ""));
            bindingBuilder.bind(property);
        } else {
            super.bindField(field, property, propertyType);
        }
    }

    @SuppressWarnings("unchecked")
    public void setAgeGroupFromFullBirthDate(Gender gender) {
        Optional<Binding<Athlete, ?>> fbdBinding = binder.getBinding("fullBirthDate");
        HasValue<?, LocalDate> dateField = (HasValue<?, LocalDate>) fbdBinding.get().getField();
        Optional<Binding<Athlete, ?>> agBinding = binder.getBinding("mastersAgeGroup");
        if (agBinding.isPresent()) {
            HasValue<?, String> ageGroupField = (HasValue<?, String>) agBinding.get().getField();
            LocalDate date = dateField.getValue();
            if (gender != null && date != null) {
                int year = date.getYear();
                ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(), year));
            } else {
                ageGroupField.setValue("");
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void setAgeGroupFromYearOfBirth(Gender gender) {
        Optional<Binding<Athlete, ?>> yobBinding = binder.getBinding("yearOfBirth");
        HasValue<?, String> yobField = (HasValue<?, String>) yobBinding.get().getField();
        Optional<Binding<Athlete, ?>> agBinding = binder.getBinding("mastersAgeGroup");
        if (agBinding.isPresent()) {
            HasValue<?, String> ageGroupField = (HasValue<?, String>) agBinding.get().getField();
            Integer year = Integer.parseInt((String) yobField.getValue());
            if (gender != null && year != null) {
                ageGroupField.setValue(editedAthlete.getMastersAgeGroup(gender.name(), year));
            } else {
                ageGroupField.setValue("");
            }
        }
    }

    /**
     * Create a binder with our special validation status handling
     * 
     * @see org.vaadin.crudui.form.AbstractAutoGeneratedCrudFormFactory#buildBinder(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object)
     */
    @Override
    protected Binder<Athlete> buildBinder(CrudOperation operation, Athlete domainObject) {
        editedAthlete = domainObject;
        binder = super.buildBinder(operation, domainObject);
        setValidationStatusHandler(true);
        return binder;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void validateBodyWeight(Binder.BindingBuilder bindingBuilder, boolean isRequired) {
        Validator<Double> v1 = new DoubleRangeValidator(Translator.translate("Weight_under_350"), 0.0D, 350.0D);
        // check wrt body category
        Validator<Double> v2 = Validator.from((weight) -> {
            if (!isRequired && weight == null)
                return true;
            // inconsistent selection is signaled on the category dropdown since the weight
            // is a factual measure
            Binding<Athlete, ?> categoryBinding = binder.getBinding("category").get();
            categoryBinding.validate(true);
            return true;
        }, Translator.translate("BodyWeight_no_match_category"));
        bindingBuilder.withValidator(v1);
        bindingBuilder.withValidator(v2);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void validateCategory(Binder.BindingBuilder bindingBuilder) {

        // check that category is consistent with body weight
        Validator<Category> v1 = Validator.from((category) -> {
            if (category == null)
                return true;
            try {
                Binding<Athlete, ?> bwBinding = binder.getBinding("bodyWeight").get();
                Double bw = (Double) bwBinding.getField().getValue();
                if (bw == null)
                    // no body weight - no contradiction
                    return true;
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

        // check that category is consistent with gender
        Validator<Category> v2 = Validator.from((category) -> {
            try {
                if (category == null)
                    return true;
                Binding<Athlete, ?> genderBinding = binder.getBinding("gender").get();
                ComboBox<Gender> genderCombo = (ComboBox<Gender>) genderBinding.getField();
                Gender g = (Gender) genderCombo.getValue();
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
                    logger.warn("checking gender");
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
            if (ld == null)
                return true;
            return ld.compareTo(LocalDate.now()) <= 0;
        }, Translator.translate("BirthDate_cannot_future"));
        bindingBuilder.withValidator(v);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void validateYearOfBirth(Binder.BindingBuilder bindingBuilder) {
        String message = Translator.translate("InvalidYearFormat");
        RegexpValidator re = new RegexpValidator(message, "(19|20)[0-9][0-9]");
        bindingBuilder.withValidator(re);
        StringToIntegerConverter converter = new StringToIntegerConverter(message) {
            @Override
            protected NumberFormat getFormat(java.util.Locale locale) {
                NumberFormat format = NumberFormat.getIntegerInstance();
                format.setGroupingUsed(false);
                return format;
            };
        };
        bindingBuilder.withConverter(converter);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void validateGender(Binder.BindingBuilder bindingBuilder) {
        // check that category is consistent with gender
        Validator<Gender> v2 = Validator.from((g) -> {
            try {
                if (g == null)
                    return true;
                Binding<Athlete, ?> catBinding = binder.getBinding("category").get();
                ComboBox<Category> categoryCombo = (ComboBox<Category>) catBinding.getField();
                Category category = (Category) categoryCombo.getValue();
                Gender catGender = category != null ? category.getGender() : null;
                logger.debug("genderValidation: validating gender {} vs category {}: {}", g, catGender, catGender == g);
                genderCatOk = catGender == null || catGender == g;
                if (genderCatOk && !catGenderOk) {
                    // turn off message if present.
                    logger.warn("checking category");
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

    @SuppressWarnings("unchecked")
    private Integer getIntegerFieldValue(String property) {
        Optional<Binding<Athlete, ?>> binding = binder.getBinding(property);
        HasValue<?, String> field = (HasValue<?, String>) binding.get().getField();
        return Athlete.zeroIfInvalid(field.getValue());
    }
}