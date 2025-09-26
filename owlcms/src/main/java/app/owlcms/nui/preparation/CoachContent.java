/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 ******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.data.coach.Coach;
import app.owlcms.data.coach.CoachRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CoachContent.
 *
 * Defines the toolbar and the table for editing data on coaches.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/coaches", layout = OwlcmsLayout.class)
public class CoachContent extends BaseContent implements CrudListener<Coach>, OwlcmsContent {

    final static Logger logger = (Logger) LoggerFactory.getLogger(CoachContent.class);
    static {
        logger.setLevel(Level.INFO);
    }
    private OwlcmsCrudFormFactory<Coach> editingFormFactory;
    private OwlcmsLayout routerLayout;
    private FlexLayout topBar;
    private GridCrud<Coach> crud;

    public CoachContent() {
        OwlcmsCrudFormFactory<Coach> crudFormFactory = createFormFactory();
        crud = createGrid(crudFormFactory);
        fillHW(crud, this);
    }

    @Override
    public Coach add(Coach domainObjectToAdd) {
        return this.editingFormFactory.add(domainObjectToAdd);
    }

    public FlexLayout createMenuArea() {
        this.topBar = new FlexLayout();

        // Export current coaches button using the age groups pattern
        Div exportCoaches = DownloadButtonFactory.createDynamicXLSXDownloadButton(
                "Coaches",
                Translator.translate("Coaches.Export"),
                new XLSXCoachExport(UI.getCurrent()));
        exportCoaches.getStyle().set("margin-left", "1em");

        Button uploadCustom = new Button(Translator.translate("Coaches.Upload"),
                new Icon(VaadinIcon.UPLOAD_ALT),
                buttonClickEvent -> {
                    CoachUploadDialog dialog = new CoachUploadDialog();
                    dialog.setCallback(() -> refreshGrid());
                    dialog.open();
                });

        FlexLayout buttons = new FlexLayout(
                new NativeLabel(Translator.translate("Coaches.ImportExport")),
                exportCoaches,
                uploadCustom);
        buttons.getStyle().set("flex-wrap", "wrap");
        buttons.getStyle().set("gap", "1ex");
        buttons.getStyle().set("margin-left", "5em");
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

        this.topBar.getStyle().set("flex", "100 1");
        this.topBar.add(buttons);
        this.topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        this.topBar.setAlignItems(FlexComponent.Alignment.CENTER);

        return this.topBar;
    }

    private Object refreshGrid() {
        crud.refreshGrid();
        return null;
    }

    @Override
    public void delete(Coach domainObjectToDelete) {
        this.editingFormFactory.delete(domainObjectToDelete);
    }

    @Override
    public Collection<Coach> findAll() {
        return CoachRepository.findAll();
    }

    @Override
    public String getMenuTitle() {
        return Translator.translate("Coaches");
    }

    @Override
    public String getPageTitle() {
        return Translator.translate("Coaches");
    }

    @Override
    public OwlcmsLayout getRouterLayout() {
        return this.routerLayout;
    }

    @Override
    public void setRouterLayout(OwlcmsLayout routerLayout) {
        this.routerLayout = routerLayout;
    }

    @Override
    public Coach update(Coach domainObjectToUpdate) {
        return this.editingFormFactory.update(domainObjectToUpdate);
    }

    protected void createFormLayout(OwlcmsCrudFormFactory<Coach> crudFormFactory) {
        ((CoachEditingFormFactory) crudFormFactory).coachLayout();
    }

    protected GridCrud<Coach> createGrid(OwlcmsCrudFormFactory<Coach> crudFormFactory) {
        Grid<Coach> grid = new Grid<>(Coach.class, false);
        grid.getThemeNames().add("row-stripes");
        grid.addColumn(Coach::getFullName).setHeader(Translator.translate("Name"));
        grid.addColumn(Coach::getTeam).setHeader(Translator.translate("Team"));
        grid.addColumn(Coach::getMembershipId).setHeader(Translator.translate("Membership"));

        GridCrud<Coach> crud = new OwlcmsCrudGrid<>(Coach.class, new OwlcmsGridLayout(Coach.class),
                crudFormFactory, grid);
        crud.setCrudListener(this);
        crud.setClickRowToUpdate(true);
        return crud;
    }

    private OwlcmsCrudFormFactory<Coach> createFormFactory() {
        this.editingFormFactory = new CoachEditingFormFactory(Coach.class);
        return this.editingFormFactory;
    }
}
