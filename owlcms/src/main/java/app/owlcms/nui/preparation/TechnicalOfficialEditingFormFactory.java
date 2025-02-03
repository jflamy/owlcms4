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

import app.owlcms.data.technicalofficial.TechnicalOfficial;
import app.owlcms.data.technicalofficial.TechnicalOfficialRepository;
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
}