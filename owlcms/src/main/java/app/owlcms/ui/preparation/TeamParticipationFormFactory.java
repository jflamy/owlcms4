/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TeamParticipationFormFactory
        extends OwlcmsCrudFormFactory<Athlete>
        implements CustomFormFactory<Athlete> {

    private TeamSelectionContent origin;
    @SuppressWarnings("unused")
    private Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupRepository.class);

    TeamParticipationFormFactory(Class<Athlete> domainType, TeamSelectionContent teamSelectionContent) {
        super(domainType);
        this.origin = teamSelectionContent;
    }

    @Override
    public Athlete add(Athlete athlete) {
        //return AthleteRepository.save(athlete);
        throw new UnsupportedOperationException("forbidden attempted addition of participation");
    }

    @Override
    public Binder<Athlete> buildBinder(CrudOperation operation, Athlete domainObject) {
        return super.buildBinder(operation, domainObject);
    }

    @Override
    public String buildCaption(CrudOperation operation, Athlete athlete) {
        String identification = athlete.getFullId();
        if (identification == null || identification.isEmpty()) {
            return Translator.translate(TeamSelectionContent.TITLE);
        } else {
            return Translator.translate("TeamSelectionContent.TITLE") + " " + identification;
        }
    }

    @Override
    public Component buildFooter(CrudOperation operation, Athlete domainObject,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
            Button... buttons) {
        return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
                deleteButtonClickListener, false, buttons);
    }

    @Override
    public Component buildNewForm(CrudOperation operation, Athlete aFromDb, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new ResponsiveStep("0", 1, LabelsPosition.ASIDE));
        formLayout.setWidth("50em");

        binder = buildBinder(null, aFromDb);

//        TextField codeField = new TextField();
//        formLayout.addFormItem(codeField, Translator.translate("AgeGroupCode"));
//        binder.forField(codeField)
//                .withNullRepresentation("")
//                .withValidator(new StringLengthValidator(Translator.translate("CodeMustBeShort"), 0, 5))
//                .bind(Athlete::getCode, Athlete::setCode);
//
//        ComboBox<AgeDivision> ageDivisionField = new ComboBox<>();
//        ageDivisionField.setDataProvider(new ListDataProvider<>(Arrays.asList(AgeDivision.values())));
//        ageDivisionField.setItemLabelGenerator((ad) -> Translator.translate("Division." + ad.name()));
//        binder.forField(ageDivisionField).bind(Athlete::getAgeDivision, Athlete::setAgeDivision);
//        formLayout.addFormItem(ageDivisionField, Translator.translate("AgeDivision"));
//
//        
//        String message = Translator.translate("AgeFormat");
//        TextField minAgeField = new TextField();
//        formLayout.addFormItem(minAgeField, Translator.translate("MinimumAge"));
//        binder.forField(minAgeField)
//                .withNullRepresentation("")
//                .withConverter(new StringToIntegerConverter(message))
//                .withValidator(new IntegerRangeValidator(message, 0, 999))
//                .bind(Athlete::getMinAge, Athlete::setMinAge);
//
//        TextField maxAgeField = new TextField();
//        formLayout.addFormItem(maxAgeField, Translator.translate("MaximumAge"));
//        binder.forField(maxAgeField)
//                .withNullRepresentation("")
//                .withConverter(new StringToIntegerConverter(message))
//                .withValidator(new IntegerRangeValidator(message, 0, 999))
//                .bind(Athlete::getMaxAge, Athlete::setMaxAge);
//
//        ComboBox<Gender> genderField = new ComboBox<>();
//        genderField.setDataProvider(new ListDataProvider<>(Arrays.asList(Gender.mfValues())));
//        binder.forField(genderField).bind(Athlete::getGender, Athlete::setGender);
//        formLayout.addFormItem(genderField, Translator.translate("Gender"));
//
//        catField = new CategoryGridField(aFromDb);
//        catField.setWidthFull();
//
//        binder.forField(catField).bind(Athlete::getCategories, Athlete::setCategories);
//        formLayout.addFormItem(catField, Translator.translate("BodyWeightCategories"));

        binder.readBean(aFromDb);
//        if (minAgeField.getValue().isEmpty()) {
//            minAgeField.setValue("0");
//        }
//        if (maxAgeField.getValue().isEmpty()) {
//            maxAgeField.setValue("999");
//        }
//        if (genderField.getValue() == null) {
//            genderField.setValue(Gender.F);
//        }
//        if (ageDivisionField.getValue() == null) {
//            ageDivisionField.setValue(AgeDivision.U);
//        }

        Component footerLayout = this.buildFooter(operation, aFromDb, cancelButtonClickListener,
                updateButtonClickListener, deleteButtonClickListener, false);

        VerticalLayout mainLayout = new VerticalLayout(formLayout, footerLayout);
        mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        return mainLayout;
    }

    @Override
    public Button buildOperationButton(CrudOperation operation, Athlete domainObject,
            ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
        return super.buildOperationButton(operation, domainObject, gridCallBackAction);
    }

    @Override
    public TextField defineOperationTrigger(CrudOperation operation, Athlete domainObject,
            ComponentEventListener<ClickEvent<Button>> action) {
        return super.defineOperationTrigger(operation, domainObject, action);
    }

    @Override
    public void delete(Athlete athlete) {
        throw new UnsupportedOperationException("attempted deletion of participation");
    }

    @Override
    public Collection<Athlete> findAll() {
        // will not be called, handled by the grid.
        throw new UnsupportedOperationException("attempted retrieval from editing form");
    }

    @Override
    public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
        return super.setErrorLabel(validationStatus, showErrorOnFields);
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
     */
    @Override
    public Athlete update(Athlete ageGroup) {
        Athlete saved = AthleteRepository.save(ageGroup);
        //logger.trace("saved {}", saved.getCategories().get(0).longDump());
        origin.closeDialog();
        origin.highlightResetButton();
        return saved;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        binder.forField(field);
        super.bindField(field, property, propertyType);
    }
    
}
