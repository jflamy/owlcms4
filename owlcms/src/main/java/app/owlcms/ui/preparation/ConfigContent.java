/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.net.SocketException;
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

import app.owlcms.data.config.Config;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.IPInterfaceUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/config", layout = ConfigLayout.class)
public class ConfigContent extends Composite<VerticalLayout>
        implements CrudLayout, OwlcmsContent, CrudListener<Config> {

    Logger logger = (Logger) LoggerFactory.getLogger(ConfigContent.class);
    private OwlcmsRouterLayout routerLayout;
    private OwlcmsCrudFormFactory<Config> factory;

    /**
     * Instantiates a new preparation navigation content.
     */
    public ConfigContent() {
        initLoggers();
        IPInterfaceUtils urlFinder = new IPInterfaceUtils();
        try {
            urlFinder.checkInterfaces("http", StartupUtils.getServerPort(), false);
        } catch (SocketException e) {
            LoggerUtils.logError(logger,e);
        }
        factory = createFormFactory();
        Component form = factory.buildNewForm(CrudOperation.UPDATE, Config.getCurrent(), false, null, event -> {
        });
        fillH(form, getContent());
    }

    @Override
    public Config add(Config domainObjectToAdd) {
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
    public void delete(Config domainObjectToDelete) {
        // not used
        factory.delete(domainObjectToDelete);
    }

    @Override
    public Collection<Config> findAll() {
        ArrayList<Config> arrayList = new ArrayList<>();
        arrayList.add(Config.getCurrent());
        return arrayList;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Config.Title");
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
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
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
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
    public Config update(Config domainObjectToUpdate) {
        // implemented by factory
        throw new RuntimeException(new OperationNotSupportedException());
    }

    /**
     * Define the form used to edit a given athlete.
     *
     * @return the form factory that will create the actual form on demand
     */
    protected OwlcmsCrudFormFactory<Config> createFormFactory() {
//        ConfigEditingFormFactory competitionEditingFormFactory = new ConfigEditingFormFactory(Config.class);
//        createFormLayout(competitionEditingFormFactory);
        OwlcmsCrudFormFactory<Config> competitionEditingFormFactory = new ConfigEditingFormFactory(
                Config.class, this);
        return competitionEditingFormFactory;
    }

}
