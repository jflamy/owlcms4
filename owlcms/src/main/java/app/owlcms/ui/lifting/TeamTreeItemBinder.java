package app.owlcms.ui.lifting;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.data.binder.Binder;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.team.TeamTreeItem;

@SuppressWarnings("serial")
public class TeamTreeItemBinder extends Binder<TeamTreeItem> {

    private Binder<Athlete> athleteBinder;

    @Override
    public <FIELDVALUE> BindingBuilder<TeamTreeItem, FIELDVALUE> forField(HasValue<?, FIELDVALUE> field) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setBean(TeamTreeItem bean) {
        // TODO Auto-generated method stub
        athleteBinder.setBean(bean.getAthlete());
    }

}
