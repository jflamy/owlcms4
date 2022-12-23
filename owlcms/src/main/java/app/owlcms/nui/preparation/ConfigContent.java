/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

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
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.data.config.Config;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.IPInterfaceUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "npreparation/config", layout = OwlcmsLayout.class)
public class ConfigContent extends Composite<VerticalLayout>
        implements CrudLayout, OwlcmsContent, CrudListener<Config> {

    Logger logger = (Logger) LoggerFactory.getLogger(ConfigContent.class);
    private OwlcmsCrudFormFactory<Config> factory;
    private OwlcmsLayout routerLayout;

    /**
     * Instantiates a new preparation navigation content.
     */
    public ConfigContent() {
        initLoggers();
        IPInterfaceUtils urlFinder = new IPInterfaceUtils();
        try {
            urlFinder.checkInterfaces("http", StartupUtils.getServerPort(), false);
        } catch (SocketException e) {
            LoggerUtils.logError(logger, e);
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
    public FlexLayout createMenuArea() {
        return new FlexLayout();
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

    @Override
    public String getMenuTitle() {
        return getPageTitle();
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Config.Title");
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

    @Override
    public void setHeaderContent() {
        routerLayout.setMenuTitle(getPageTitle());
        routerLayout.showLocaleDropdown(true);
        routerLayout.setDrawerOpened(false);
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
    
    @Override
    public void setPadding(boolean b) {
        // not needed
    }

}
