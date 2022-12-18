/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.ArrayList;
import java.util.Collection;

import javax.naming.OperationNotSupportedException;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.CrudOperation;
import org.vaadin.crudui.layout.CrudLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.data.competition.Competition;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "npreparation/competition", layout = OwlcmsLayout.class)
public class CompetitionContent extends Composite<VerticalLayout>
        implements CrudLayout, OwlcmsContent, CrudListener<Competition> {

    Logger logger = (Logger) LoggerFactory.getLogger(CompetitionContent.class);
    private OwlcmsCrudFormFactory<Competition> factory;
    private OwlcmsLayout routerLayout;

    /**
     * Instantiates a new preparation navigation content.
     */
    public CompetitionContent() {
        initLoggers();
        factory = createFormFactory();
        Component form = factory.buildNewForm(CrudOperation.UPDATE, Competition.getCurrent(), false, null, event -> {
        });
        fillH(form, getContent());
    }
    
    @Override
    public void setHeaderContent() {
        routerLayout.setTopBarTitle(getPageTitle());
        routerLayout.showLocaleDropdown(true);
        routerLayout.setDrawerOpened(false);
    }

    @Override
    public Competition add(Competition domainObjectToAdd) {
        // implemented by factory
        throw new RuntimeException(new OperationNotSupportedException());
    }

    @Override
    public void addFilterComponent(Component component) {
    }

    @Override
    public void addToolbarComponent(Component component) {
    }

    @Override
    public void delete(Competition domainObjectToDelete) {
        // not used
        factory.delete(domainObjectToDelete);
    }

    @Override
    public Collection<Competition> findAll() {
        ArrayList<Competition> arrayList = new ArrayList<>();
        arrayList.add(Competition.getCurrent());
        return arrayList;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("EditCompetitionInformation");
    }

    @Override
    public OwlcmsLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void hideForm() {
    }

    public void initLoggers() {
        logger.setLevel(Level.INFO);
    }

    /**
     * @see org.vaadin.crudui.layout.CrudLayout#setMainComponent(com.vaadin.flow.component.Component)
     */
    @Override
    public void setMainComponent(Component component) {
        getContent().removeAll();
        getContent().add(component);
    }

    @Override
    public void setRouterLayout(OwlcmsLayout routerLayout) {
        this.routerLayout = routerLayout;
    }


    @Override
    public void showDialog(String caption, Component form) {
    }

    @Override
    public void showForm(CrudOperation operation, Component form, String caption) {
        getContent().removeAll();
        getContent().add(form);
    }

    @Override
    public Competition update(Competition domainObjectToUpdate) {
        // implemented by factory
        throw new RuntimeException(new OperationNotSupportedException());
    }

    /**
     * Define the form used to edit a given athlete.
     *
     * @return the form factory that will create the actual form on demand
     */
    protected OwlcmsCrudFormFactory<Competition> createFormFactory() {
//        CompetitionEditingFormFactory competitionEditingFormFactory = new CompetitionEditingFormFactory(Competition.class);
//        createFormLayout(competitionEditingFormFactory);
        OwlcmsCrudFormFactory<Competition> competitionEditingFormFactory = new CompetitionEditingFormFactory(
                Competition.class, this);
        return competitionEditingFormFactory;
    }

}
