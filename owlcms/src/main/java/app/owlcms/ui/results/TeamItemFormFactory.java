package app.owlcms.ui.results;

import java.util.Collection;

import org.vaadin.crudui.crud.CrudOperation;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.ui.lifting.AthleteCardFormFactory;
import app.owlcms.ui.shared.CustomFormFactory;

public class TeamItemFormFactory implements CustomFormFactory<TeamTreeItem> {
    
    private AthleteCardFormFactory acff = new AthleteCardFormFactory(Athlete.class, null);

    @Override
    public TeamTreeItem add(TeamTreeItem athlete) {
        acff.add(athlete.getAthlete());
        return null;
    }

    @Override
    public Binder<TeamTreeItem> buildBinder(CrudOperation operation, TeamTreeItem doNotUse) {
        return acff.buildBinder(operation, null);
    }

    @Override
    public String buildCaption(CrudOperation operation, TeamTreeItem aFromDb) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Component buildFooter(CrudOperation operation, TeamTreeItem unused,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> unused2, ComponentEventListener<ClickEvent<Button>> unused3,
            boolean shortcutEnter, Button... buttons) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Component buildNewForm(CrudOperation operation, TeamTreeItem aFromDb, boolean readOnly,
            ComponentEventListener<ClickEvent<Button>> cancelButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> updateButtonClickListener,
            ComponentEventListener<ClickEvent<Button>> deleteButtonClickListener, Button... buttons) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Button buildOperationButton(CrudOperation operation, TeamTreeItem domainObject,
            ComponentEventListener<ClickEvent<Button>> callBack) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public TextField defineOperationTrigger(CrudOperation operation, TeamTreeItem domainObject,
            ComponentEventListener<ClickEvent<Button>> action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void delete(TeamTreeItem notUsed) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Collection<TeamTreeItem> findAll() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setErrorLabel(BinderValidationStatus<?> validationStatus, boolean updateFieldStatus) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public TeamTreeItem update(TeamTreeItem athleteFromDb) {
        // TODO Auto-generated method stub
        return null;
    }

}
