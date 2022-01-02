/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.sound.Speakers;
import app.owlcms.ui.crudui.OwlcmsComboBoxProvider;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.lifting.TCContent;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/platforms", layout = PlatformLayout.class)
public class PlatformContent extends VerticalLayout implements CrudListener<Platform>, OwlcmsContent {

    final static Logger logger = (Logger) LoggerFactory.getLogger(PlatformContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private OwlcmsRouterLayout routerLayout;
    private OwlcmsCrudFormFactory<Platform> editingFormFactory;

    /**
     * Instantiates the Platform crudGrid.
     */
    public PlatformContent() {
        OwlcmsCrudFormFactory<Platform> crudFormFactory = createFormFactory();
        GridCrud<Platform> crud = createGrid(crudFormFactory);
        // defineFilters(crudGrid);
        fillHW(crud, this);
    }

    @Override
    public Platform add(Platform domainObjectToAdd) {
        return editingFormFactory.add(domainObjectToAdd);
    }

    @Override
    public void delete(Platform domainObjectToDelete) {
        editingFormFactory.delete(domainObjectToDelete);
    }

    /**
     * The refresh button on the toolbar
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<Platform> findAll() {
        return PlatformRepository.findAll();
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Preparation_Platforms");
    }

    @Override
    public OwlcmsRouterLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public Platform update(Platform domainObjectToUpdate) {
        return editingFormFactory.update(domainObjectToUpdate);
    }

    /**
     * The content and ordering of the editing form.
     *
     * @param crudFormFactory the factory that will create the form using this information
     */
    protected void createFormLayout(OwlcmsCrudFormFactory<Platform> crudFormFactory) {
        crudFormFactory.setVisibleProperties("name", "soundMixerName");
        crudFormFactory.setFieldCaptions(getTranslation("PlatformName"), getTranslation("Speakers"));
        List<String> outputNames = Speakers.getOutputNames();
        outputNames.add(0, getTranslation("UseBrowserSound"));
        crudFormFactory.setFieldProvider("soundMixerName", new OwlcmsComboBoxProvider<>(outputNames));
    }

    /**
     * The columns of the crudGrid
     *
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected GridCrud<Platform> createGrid(OwlcmsCrudFormFactory<Platform> crudFormFactory) {
        Grid<Platform> grid = new Grid<>(Platform.class, false);
        grid.addColumn(Platform::getName).setHeader(getTranslation("Name"));
        grid.addColumn(Platform::getSoundMixerName).setHeader(getTranslation("Speakers"));
        grid.addColumn(new ComponentRenderer<>(p -> {
            Button technical = openInNewTab(TCContent.class, getTranslation("PlatesCollarBarbell"), p.getName());
            return technical;
        })).setHeader(getTranslation("PlatesCollarBarbell")).setWidth("0");

        GridCrud<Platform> crud = new OwlcmsCrudGrid<>(Platform.class, new OwlcmsGridLayout(Platform.class),
                crudFormFactory, grid);
        crud.setCrudListener(this);
        crud.setClickRowToUpdate(true);
        return crud;
    }

    /**
     * Define the form used to edit a given Platform.
     *
     * @return the form factory that will create the actual form on demand
     */
    private OwlcmsCrudFormFactory<Platform> createFormFactory() {
        editingFormFactory = createPlatformEditingFactory();
        createFormLayout(editingFormFactory);
        return editingFormFactory;
    }

    /**
     * Create the actual form generator with all the conversions and validations required
     *
     * {@link RegistrationContent#createAthleteEditingFormFactory} for example of redefinition of bindField
     *
     * @return the actual factory, with the additional mechanisms to do validation
     */
    private OwlcmsCrudFormFactory<Platform> createPlatformEditingFactory() {
        return new PlatformEditingFormFactory(Platform.class);
    }

    private <T extends Component & HasUrlParameter<String>> String getWindowOpenerFromClass(Class<T> targetClass,
            String parameter) {
        return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "?fop=" + parameter
                + "','" + targetClass.getSimpleName() + "')";
    }

    private <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass,
            String label, String parameter) {
        Button button = new Button(label);
        button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter));
        return button;
    }
}
