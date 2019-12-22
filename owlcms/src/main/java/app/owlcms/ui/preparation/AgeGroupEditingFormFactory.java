/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.HasValue;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
class AgeGroupEditingFormFactory extends OwlcmsCrudFormFactory<AgeGroup> {
    AgeGroupEditingFormFactory(Class<AgeGroup> domainType) {
        super(domainType);
    }

    @Override
    public AgeGroup add(AgeGroup AgeGroup) {
        AgeGroupRepository.save(AgeGroup);
        return AgeGroup;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        binder.forField(field);
        super.bindField(field, property, propertyType);
    }

    @Override
    public String buildCaption(CrudOperation operation, AgeGroup domainObject) {
        if (domainObject.getName() == null || domainObject.getName().isEmpty()) {
            return "";
        } else {
            return domainObject.getName();
        }
    }

    @Override
    public void delete(AgeGroup AgeGroup) {
        AgeGroupRepository.delete(AgeGroup);
    }

    @Override
    public Collection<AgeGroup> findAll() {
        // will not be called
        return null;
    }

    @Override
    public AgeGroup update(AgeGroup AgeGroup) {
        return AgeGroupRepository.save(AgeGroup);
    }

}