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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.technicalofficial.TOLevel;
import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
import app.owlcms.data.technicalofficial.OfficialRole;
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

        Checkbox activeCheckbox = new Checkbox(Translator.translate("TechnicalOfficial.Active"));
        activeCheckbox.setValue(false); // Default to false
        technicalOfficialLayout.add(activeCheckbox);
        this.binder.forField(activeCheckbox)
                .bind(TechnicalOfficial::isActive, TechnicalOfficial::setActive);

        ComboBox<TechnicalOfficial.Role> roleComboBox = new ComboBox<>(Translator.translate("TechnicalOfficial.Accreditation"));
        technicalOfficialLayout.add(roleComboBox);
        roleComboBox.setItems(TechnicalOfficial.Role.values());
        roleComboBox.setItemLabelGenerator(role -> 
            Translator.translate("TO.Role." + role.name())
        );
        roleComboBox.setValue(TechnicalOfficial.Role.TECHNICAL_OFFICIAL); // Default value
        this.binder.forField(roleComboBox)
                .bind(TechnicalOfficial::getRole, TechnicalOfficial::setRole);

        ComboBox<OfficialRole> officialRoleComboBox = new ComboBox<>(Translator.translate("TechnicalOfficial.OfficialRole"));
        technicalOfficialLayout.add(officialRoleComboBox);
        officialRoleComboBox.setItems(OfficialRole.values());
        officialRoleComboBox.setItemLabelGenerator(role -> 
            Translator.translate("OfficialRole." + role.name())
        );
        officialRoleComboBox.setClearButtonVisible(true);
        this.binder.forField(officialRoleComboBox)
                .bind(TechnicalOfficial::getOfficialRole, TechnicalOfficial::setOfficialRole);

        TextField lastNameTextField = new TextField(Translator.translate("LastName"));
        technicalOfficialLayout.add(lastNameTextField);
        this.binder.forField(lastNameTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getLastName, TechnicalOfficial::setLastName);

        TextField firstNameTextField = new TextField(Translator.translate("FirstName"));
        technicalOfficialLayout.add(firstNameTextField);
        this.binder.forField(firstNameTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getFirstName, TechnicalOfficial::setFirstName);

        ComboBox<Integer> teamComboBox = new ComboBox<>(Translator.translate("Team"));
        teamComboBox.setItems(1, 2, 3, 4); // Max four teams
        teamComboBox.setAllowCustomValue(false); // typing filters, selection constrained to 1-4
        teamComboBox.setClearButtonVisible(true);
        technicalOfficialLayout.add(teamComboBox);
        this.binder.forField(teamComboBox)
            .bind(TechnicalOfficial::getTechnicalOfficialTeam, TechnicalOfficial::setTechnicalOfficialTeam);

        ComboBox<TOLevel> levelComboBox = new ComboBox<>(Translator.translate("TechnicalOfficial.Level"));
        technicalOfficialLayout.add(levelComboBox);
        levelComboBox.setItems(TOLevel.values());
        levelComboBox.setItemLabelGenerator(level -> 
            Translator.translate("TOLevel." + level.name())
        );
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
        
        TextField affiliationTextField = new TextField(Translator.translate("TechnicalOfficial.Affiliation"));
        technicalOfficialLayout.add(affiliationTextField);
        this.binder.forField(affiliationTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getAffiliation, TechnicalOfficial::setAffiliation);

        TextField iwfIdTextField = new TextField(Translator.translate("TechnicalOfficial.IWFId"));
        technicalOfficialLayout.add(iwfIdTextField);
        this.binder.forField(iwfIdTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getIwfId, TechnicalOfficial::setIwfId);

        return technicalOfficialLayout;
    }

    @Override
    public String buildCaption(CrudOperation operation, TechnicalOfficial domainObject) {
        if (operation.equals(CrudOperation.ADD)) {
            return Translator.translate("Add") + " " + Translator.translate("TechnicalOfficial");
        } else if (operation.equals(CrudOperation.UPDATE)) {
            return Translator.translate("Update") +  " " + Translator.translate("TechnicalOfficial");
        } else if (operation.equals(CrudOperation.DELETE)) {
            return Translator.translate("Delete") + " " + Translator.translate("TechnicalOfficial");
        }
        return super.buildCaption(operation, domainObject);
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