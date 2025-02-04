/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.technicalofficial.TOLevel;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
class TechnicalOfficialEditingFormFactory extends OwlcmsCrudFormFactory<TechnicalOfficial> {
    TechnicalOfficialEditingFormFactory(Class<TechnicalOfficial> domainType) {
        super(domainType);
    }

    @Override
    public TechnicalOfficial add(TechnicalOfficial official) {
        TechnicalOfficialRepository.save(official);
        return official;
    }

    @Override
    public void delete(TechnicalOfficial official) {
        TechnicalOfficialRepository.delete(official);
    }

    @Override
    public Collection<TechnicalOfficial> findAll() {
        // implemented on grid
        return null;
    }

    @Override
    public TechnicalOfficial update(TechnicalOfficial official) {
        return TechnicalOfficialRepository.save(official);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
        // Add any custom binding logic here if needed
        super.bindField(field, property, propertyType, c);
    }

    public FormLayout technicalOfficialLayout() {
        FormLayout technicalOfficialLayout = new FormLayout();

        TextField lastNameTextField = new TextField(Translator.translate("TechnicalOfficial.LastName"));
        technicalOfficialLayout.add(lastNameTextField);
        this.binder.forField(lastNameTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getLastName, TechnicalOfficial::setLastName);

        TextField firstNameTextField = new TextField(Translator.translate("TechnicalOfficial.FirstName"));
        technicalOfficialLayout.add(firstNameTextField);
        this.binder.forField(firstNameTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getFirstName, TechnicalOfficial::setFirstName);

        ComboBox<TOLevel> levelComboBox = new ComboBox<>(Translator.translate("TechnicalOfficial.Level"));
        technicalOfficialLayout.add(levelComboBox);
        levelComboBox.setItems(TOLevel.values());
        this.binder.forField(levelComboBox)
                .bind(TechnicalOfficial::getLevel, TechnicalOfficial::setLevel);

        TextField federationIdTextField = new TextField(Translator.translate("TechnicalOfficial.FederationId"));
        technicalOfficialLayout.add(federationIdTextField);
        this.binder.forField(federationIdTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getFederationId, TechnicalOfficial::setFederationId);

        TextField federationTextField = new TextField(Translator.translate("TechnicalOfficial.Federation"));
        technicalOfficialLayout.add(federationTextField);
        this.binder.forField(federationTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getFederation, TechnicalOfficial::setFederation);

        TextField iwfIdTextField = new TextField(Translator.translate("TechnicalOfficial.IWFId"));
        technicalOfficialLayout.add(iwfIdTextField);
        this.binder.forField(iwfIdTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getIwfId, TechnicalOfficial::setIwfId);

        return technicalOfficialLayout;
    }

    @Override
    public Component buildNewForm(CrudOperation operation, TechnicalOfficial aFromList, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

        this.binder = buildBinder(operation, aFromList);

        Component footer = this.buildFooter(operation, aFromList, cancelButtonClickListener,
                operationButtonClickListener, deleteButtonClickListener, true);

        Component form = technicalOfficialLayout();
        var mainLayout = new VerticalLayout(form, footer);
        this.binder.readBean(aFromList);
        return mainLayout;
    }
}