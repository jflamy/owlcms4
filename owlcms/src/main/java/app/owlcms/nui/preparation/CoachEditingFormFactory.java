package app.owlcms.nui.preparation;

import java.util.Collection;

import org.vaadin.crudui.form.CrudFormConfiguration;
import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import app.owlcms.data.coach.Coach;
import app.owlcms.data.coach.CoachRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;

@SuppressWarnings("serial")
public class CoachEditingFormFactory extends OwlcmsCrudFormFactory<Coach> {

    public CoachEditingFormFactory(Class<Coach> domainClass) {
        super(domainClass);
    }

    @Override
    public Coach add(Coach coach) {
        CoachRepository.save(coach);
        return coach;
    }

    @Override
    public void delete(Coach coach) {
        CoachRepository.delete(coach);
    }

    @Override
    public Collection<Coach> findAll() {
        // implemented on grid
        return null;
    }

    @Override
    public Coach update(Coach coach) {
        return CoachRepository.save(coach);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected void bindField(HasValue field, String property, Class<?> propertyType, CrudFormConfiguration c) {
        // Add any custom binding logic here if needed
        super.bindField(field, property, propertyType, c);
    }

    public FormLayout coachLayout() {
        FormLayout layout = new FormLayout();

        TextField lastNameTextField = new TextField(Translator.translate("LastName"));
        layout.add(lastNameTextField);
        this.binder.forField(lastNameTextField)
                .withNullRepresentation("")
                .bind(Coach::getLastName, Coach::setLastName);

        TextField firstNameTextField = new TextField(Translator.translate("FirstName"));
        layout.add(firstNameTextField);
        this.binder.forField(firstNameTextField)
                .withNullRepresentation("")
                .bind(Coach::getFirstName, Coach::setFirstName);

        TextField membershipIdTextField = new TextField(Translator.translate("Membership"));
        layout.add(membershipIdTextField);
        this.binder.forField(membershipIdTextField)
                .withNullRepresentation("")
                .bind(Coach::getMembershipId, Coach::setMembershipId);

        TextField teamTextField = new TextField(Translator.translate("Team"));
        layout.add(teamTextField);
        this.binder.forField(teamTextField)
                .withNullRepresentation("")
                .bind(Coach::getTeam, Coach::setTeam);

        return layout;
    }

    @Override
    public String buildCaption(CrudOperation operation, Coach domainObject) {
        if (operation.equals(CrudOperation.ADD)) {
            return Translator.translate("Add") + " " + Translator.translate("Coach");
        } else if (operation.equals(CrudOperation.UPDATE)) {
            return Translator.translate("Update") + " " + Translator.translate("Coach");
        } else if (operation.equals(CrudOperation.DELETE)) {
            return Translator.translate("Delete") + " " + Translator.translate("Coach");
        }
        return super.buildCaption(operation, domainObject);
    }

    @Override
    public Component buildNewForm(CrudOperation operation, Coach aFromList,
            boolean readOnly, ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> operationButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {

        // This signature mirrors the parent; reuse existing behavior
        this.binder = buildBinder(operation, aFromList);

        Component footer = this.buildFooter(operation, aFromList, cancelButtonClickListener, operationButtonClickListener,
                deleteButtonClickListener, true);

        Component form = coachLayout();
        var mainLayout = new VerticalLayout(form, footer);
        this.binder.readBean(aFromList);
        return mainLayout;
    }
}
