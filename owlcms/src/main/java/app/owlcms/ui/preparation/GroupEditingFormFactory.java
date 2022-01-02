/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.time.LocalDateTime;
import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder.Binding;

import app.owlcms.components.fields.LocalDateTimePicker;
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType) {
        if (property.equals("weighInTime")) {
            field.addValueChangeListener(e -> {
                Binding<Group, ?> ctBinding = binder.getBinding("competitionTime").get();
                LocalDateTime weighinTime = (LocalDateTime) e.getValue();
                if (ctBinding != null) {
                    LocalDateTimePicker field2 = (LocalDateTimePicker) ctBinding.getField();
                    field2.setValue(weighinTime.plusHours(2));
                } else {
                    throw new RuntimeException("competitionTime field not defined");
                }
            });
        }
        super.bindField(field, property, propertyType);
    }

}