/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
class GroupEditingFormFactory extends OwlcmsCrudFormFactory<Group> {
    GroupEditingFormFactory(Class<Group> domainType) {
        super(domainType);
    }

    @Override
    public Group add(Group group) {
        GroupRepository.save(group);
        return group;
    }

    /**
     * @see org.vaadin.crudui.form.impl.form.factory.DefaultCrudFormFactory#buildCaption(org.vaadin.crudui.crud.CrudOperation,
     *      java.lang.Object)
     */
    @Override
    public String buildCaption(CrudOperation operation, Group domainObject) {
        if (domainObject.getName() == null || domainObject.getName().isEmpty()) {
            return "";
        } else {
            return domainObject.getName();
        }
    }

    @Override
    public void delete(Group group) {
        GroupRepository.delete(group);
    }

    @Override
    public Collection<Group> findAll() {
        // implemented on grid
        return null;
    }

    @Override
    public Group update(Group group) {
        GroupContent.logger.debug("saving group {} {}", group.getName(), group.getCompetitionTime());
        return GroupRepository.save(group);
    }

}