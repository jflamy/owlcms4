/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.validator.StringLengthValidator;

import app.owlcms.components.fields.LocalDateTimePicker;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.CustomFormFactory;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class GroupEditingFormFactory
        extends OwlcmsCrudFormFactory<Group>
        implements CustomFormFactory<Group> {

    @SuppressWarnings("unused")
    private Logger logger = (Logger) LoggerFactory.getLogger(GroupEditingFormFactory.class);
    private GroupContent origin;

    GroupEditingFormFactory(Class<Group> domainType, GroupContent origin) {
        super(domainType);
        this.origin = origin;
    }

    @Override
    public Group add(Group Group) {
        GroupRepository.add(Group);
        return Group;
    }

    @Override
    public Binder<Group> buildBinder(CrudOperation operation, Group domainObject) {
        return super.buildBinder(operation, domainObject);
    }

    @Override
    public String buildCaption(CrudOperation operation, Group domainObject) {
        String name = domainObject.getName();
        if (name == null || name.isEmpty()) {
            return Translator.translate("Group");
        } else {
            return Translator.translate("Group") + " " + domainObject.getName();
        }
    }

    @Override
    public Component buildFooter(CrudOperation operation, Group domainObject,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> postOperationCallBack,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, boolean shortcutEnter,
            Button... buttons) {
        return super.buildFooter(operation, domainObject, cancelButtonClickListener, postOperationCallBack,
                deleteButtonClickListener, false, buttons);
    }

    @Override
    public Component buildNewForm(CrudOperation operation, Group aFromDb, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

        FormLayout formLayout = new FormLayout();
        binder = buildBinder(null, aFromDb);

        TextField nameField = new TextField(Translator.translate("Name"));
        formLayout.add(nameField);
        int maxLength = 16;
        binder.forField(nameField)
                .withValidator(
                        new StringLengthValidator(Translator.translate("CodeMustBeShort", maxLength), 1, maxLength))
                .bind(Group::getName, Group::setName);

        ComboBox<Platform> platformField = new ComboBox<>(Translator.translate("Platform"));
        platformField.setSizeUndefined();
        platformField.setDataProvider(new ListDataProvider<>(PlatformRepository.findAll()));
        platformField.setItemLabelGenerator(Platform::getName);
        platformField.setClearButtonVisible(true);
        formLayout.add(platformField);
        binder.forField(platformField).bind(Group::getPlatform, Group::setPlatform);

        TextField descriptionField = new TextField(Translator.translate("Group.Description"));
        descriptionField.setSizeFull();
        formLayout.add(descriptionField);
        formLayout.setColspan(descriptionField, 2);
        binder.forField(descriptionField)
                .withNullRepresentation("")
                .bind(Group::getDescription, Group::setDescription);

        addRuler(formLayout);

        LocalDateTimePicker weighInTimeField = new LocalDateTimePicker();
        weighInTimeField.setLabel(Translator.translate("WeighInTime"));
        formLayout.add(weighInTimeField);
        binder.forField(weighInTimeField)
                .bind(Group::getWeighInTime, Group::setWeighInTime);

        LocalDateTimePicker competitionTimeField = new LocalDateTimePicker();
        competitionTimeField.setLabel(Translator.translate("StartTime"));
        formLayout.add(competitionTimeField);
        binder.forField(competitionTimeField)
                .bind(Group::getCompetitionTime, Group::setCompetitionTime);

        TextField weighIn1 = new TextField(Translator.translate("Weighin1"));
        formLayout.add(weighIn1);
        binder.forField(weighIn1)
                .withNullRepresentation("")
                .bind(Group::getWeighIn1, Group::setWeighIn1);

        TextField weighIn2 = new TextField(Translator.translate("Weighin2"));
        formLayout.add(weighIn2);
        binder.forField(weighIn2)
                .withNullRepresentation("")
                .bind(Group::getWeighIn2, Group::setWeighIn2);

        addRuler(formLayout);

        TextField announcer = new TextField(Translator.translate("Announcer"));
        formLayout.add(announcer);
        binder.forField(announcer)
                .withNullRepresentation("")
                .bind(Group::getAnnouncer, Group::setAnnouncer);

        TextField marshall = new TextField(Translator.translate("Marshall"));
        formLayout.add(marshall);
        binder.forField(marshall)
                .withNullRepresentation("")
                .bind(Group::getMarshall, Group::setMarshall);
        
        TextField marshal2 = new TextField(Translator.translate("Marshal2"));
        formLayout.add(marshal2);
        binder.forField(marshal2)
                .withNullRepresentation("")
                .bind(Group::getMarshal2, Group::setMarshal2);

        TextField technicalController = new TextField(Translator.translate("TechnicalController"));
        formLayout.add(technicalController);
        binder.forField(technicalController)
                .withNullRepresentation("")
                .bind(Group::getTechnicalController, Group::setTechnicalController);
        
        TextField technicalController2 = new TextField(Translator.translate("TechnicalController2"));
        formLayout.add(technicalController2);
        binder.forField(technicalController2)
                .withNullRepresentation("")
                .bind(Group::getTechnicalController2, Group::setTechnicalController2);

        TextField timeKeeper = new TextField(Translator.translate("Timekeeper"));
        formLayout.add(timeKeeper);
        binder.forField(timeKeeper)
                .withNullRepresentation("")
                .bind(Group::getTimeKeeper, Group::setTimeKeeper);

        addRuler(formLayout);

        TextField referee1 = new TextField(Translator.translate("Referee1"));
        formLayout.add(referee1);
        binder.forField(referee1)
                .withNullRepresentation("")
                .bind(Group::getReferee1, Group::setReferee1);

        TextField referee2 = new TextField(Translator.translate("Referee2"));
        formLayout.add(referee2);
        binder.forField(referee2)
                .withNullRepresentation("")
                .bind(Group::getReferee2, Group::setReferee2);

        TextField referee3 = new TextField(Translator.translate("Referee3"));
        formLayout.add(referee3);
        binder.forField(referee3)
                .withNullRepresentation("")
                .bind(Group::getReferee3, Group::setReferee3);

        addRuler(formLayout);

        TextField jury1 = new TextField(Translator.translate("Jury1"));
        formLayout.add(jury1);
        binder.forField(jury1)
                .withNullRepresentation("")
                .bind(Group::getJury1, Group::setJury1);

        TextField jury2 = new TextField(Translator.translate("Jury2"));
        formLayout.add(jury2);
        binder.forField(jury2)
                .withNullRepresentation("")
                .bind(Group::getJury2, Group::setJury2);

        TextField jury3 = new TextField(Translator.translate("Jury3"));
        formLayout.add(jury3);
        binder.forField(jury3)
                .withNullRepresentation("")
                .bind(Group::getJury3, Group::setJury3);

        TextField jury4 = new TextField(Translator.translate("Jury4"));
        formLayout.add(jury4);
        binder.forField(jury4)
                .withNullRepresentation("")
                .bind(Group::getJury4, Group::setJury4);

        TextField jury5 = new TextField(Translator.translate("Jury5"));
        formLayout.add(jury5);
        binder.forField(jury5)
                .withNullRepresentation("")
                .bind(Group::getJury5, Group::setJury5);

        binder.readBean(aFromDb);

        Component footerLayout = this.buildFooter(operation, aFromDb, cancelButtonClickListener,
                updateButtonClickListener, deleteButtonClickListener, false);

        VerticalLayout mainLayout = new VerticalLayout(formLayout, footerLayout);
        mainLayout.setHorizontalComponentAlignment(Alignment.END, footerLayout);
        mainLayout.setMargin(false);
        mainLayout.setPadding(false);
        return mainLayout;
    }

    @Override
    public Button buildOperationButton(CrudOperation operation, Group domainObject,
            ComponentEventListener<ClickEvent<Button>> gridCallBackAction) {
        return super.buildOperationButton(operation, domainObject, gridCallBackAction);
    }

    @Override
    public TextField defineOperationTrigger(CrudOperation operation, Group domainObject,
            ComponentEventListener<ClickEvent<Button>> action) {
        return super.defineOperationTrigger(operation, domainObject, action);
    }

    @Override
    public void delete(Group ageGroup) {
        GroupRepository.delete(ageGroup);
    }

    @Override
    public Collection<Group> findAll() {
        // will not be called, handled by the grid.
        return null;
    }

    @Override
    public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean showErrorOnFields) {
        return super.setErrorLabel(validationStatus, showErrorOnFields);
    }

    /**
     * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
     */
    @Override
    public Group update(Group ageGroup) {
        Group saved = GroupRepository.save(ageGroup);
        // logger.trace("saved {}", saved.getCategories().get(0).longDump());
        origin.closeDialog();
//        origin.highlightResetButton();
        return saved;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        binder.forField(field);
        super.bindField(field, property, propertyType);
    }

    private void addRuler(FormLayout formLayout) {
        Paragraph hr11 = new Paragraph();
        hr11.add("\u0020");
        hr11.add(new Hr());
        formLayout.add(hr11);
        formLayout.setColspan(hr11, 2);
    }

}