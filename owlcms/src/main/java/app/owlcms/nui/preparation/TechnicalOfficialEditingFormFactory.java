/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;

import org.vaadin.crudui.form.CrudFormConfiguration;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.group.Group;
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

    private FormLayout technicalOfficialLayout() {
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

        TextField emailTextField = new TextField(Translator.translate("TechnicalOfficial.Email"));
        technicalOfficialLayout.add(emailTextField);
        this.binder.forField(emailTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getEmail, TechnicalOfficial::setEmail);

        TextField phoneTextField = new TextField(Translator.translate("TechnicalOfficial.Phone"));
        technicalOfficialLayout.add(phoneTextField);
        this.binder.forField(phoneTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getPhoneNumber, TechnicalOfficial::setPhoneNumber);

        TextField categoryTextField = new TextField(Translator.translate("TechnicalOfficial.Category"));
        technicalOfficialLayout.add(categoryTextField);
        this.binder.forField(categoryTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getCategory, TechnicalOfficial::setCategory);

        TextField notesTextField = new TextField(Translator.translate("TechnicalOfficial.Notes"));
        notesTextField.setWidth("100%");
        technicalOfficialLayout.add(notesTextField);
        technicalOfficialLayout.setColspan(notesTextField, 2);
        this.binder.forField(notesTextField)
                .withNullRepresentation("")
                .bind(TechnicalOfficial::getNotes, TechnicalOfficial::setNotes);

        return technicalOfficialLayout;
    }
}