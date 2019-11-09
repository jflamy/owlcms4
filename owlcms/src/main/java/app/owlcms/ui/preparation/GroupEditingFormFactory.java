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
    public Group add(Group group) {
        GroupRepository.save(group);
        return group;
    }

    @Override
    public Group update(Group group) {
        GroupContent.logger.debug("saving group {} {}", group.getName(), group.getCompetitionTime());
        return GroupRepository.save(group);
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

}