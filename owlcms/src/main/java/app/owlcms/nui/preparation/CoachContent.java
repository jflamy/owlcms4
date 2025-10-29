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
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
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
import org.vaadin.crudui.crud.CrudOperation;

/**
 * Class CoachContent.
 *
 * Defines the toolbar and the table for editing data on coaches.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/coaches", layout = OwlcmsLayout.class)
public class CoachContent extends BaseContent implements CrudListener<Coach>, OwlcmsContent {

    /**
     * Custom CrudGrid to handle focus management after save operations
     */
    private final class CoachCrudGrid extends OwlcmsCrudGrid<Coach> {
        
        private CoachCrudGrid(Class<Coach> domainType, OwlcmsGridLayout crudLayout,
                OwlcmsCrudFormFactory<Coach> owlcmsCrudFormFactory, Grid<Coach> grid) {
            super(domainType, crudLayout, owlcmsCrudFormFactory, grid);
        }

        @Override
        protected void saveCallBack(OwlcmsCrudGrid<Coach> owlcmsCrudGrid, String successMessage,
                CrudOperation operation, Coach coach) {
            try {
                owlcmsCrudGrid.getGrid().asSingleSelect().clear();
                owlcmsCrudGrid.getOwlcmsGridLayout().hideForm();
                refreshGrid();
                Notification.show(successMessage);
                focusOutsideThenBackToTriggeringItem();
            } catch (Exception e) {
                logger.error("Error in save callback", e);
            }
        }
    }

    final static Logger logger = (Logger) LoggerFactory.getLogger(CoachContent.class);
    static {
        logger.setLevel(Level.INFO);
    }
    private OwlcmsCrudFormFactory<Coach> editingFormFactory;
    private OwlcmsLayout routerLayout;
    private FlexLayout topBar;
    private GridCrud<Coach> crud;
    private Grid<Coach> grid;
    private TextField lastNameFilter = new TextField();
    private String lastNameValue;

    public CoachContent() {
        OwlcmsCrudFormFactory<Coach> crudFormFactory = createFormFactory();
        crud = createGrid(crudFormFactory);
        defineFilters(crud);
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

        // Coach Credentials button
        Div coachCredentialsButton = DocumentsContent.createCoachCredentialsButton();

        Hr hr = new Hr();
        hr.setWidthFull();
        hr.getStyle().set("margin", "0");
        hr.getStyle().set("padding", "0");

        FlexLayout buttons = new FlexLayout(
                new NativeLabel(Translator.translate("Coaches.ImportExport")),
                exportCoaches,
                uploadCustom,
                hr,
                new NativeLabel(Translator.translate("Credentials")),
                coachCredentialsButton);
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
        Collection<Coach> coaches = CoachRepository.findAll();
        
        // Apply last name filter if present
        if (lastNameValue != null && !lastNameValue.trim().isEmpty()) {
            String filterLower = lastNameValue.toLowerCase().trim();
            coaches = coaches.stream()
                .filter(coach -> {
                    String lastName = coach.getLastName();
                    return lastName != null && lastName.toLowerCase().contains(filterLower);
                })
                .collect(java.util.stream.Collectors.toList());
        }
        
        return coaches;
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
        this.grid = new Grid<>(Coach.class, false);
        this.grid.getThemeNames().add("row-stripes");
        this.grid.addColumn(Coach::getLastName).setHeader(Translator.translate("LastName"));
        this.grid.addColumn(Coach::getFirstName).setHeader(Translator.translate("FirstName"));
        this.grid.addColumn(Coach::getTeam).setHeader(Translator.translate("Team"));
        this.grid.addColumn(Coach::getMembershipId).setHeader(Translator.translate("Membership"));        GridCrud<Coach> crud = new CoachCrudGrid(Coach.class, new OwlcmsGridLayout(Coach.class),
                crudFormFactory, this.grid);
        crud.setCrudListener(this);
        crud.setClickRowToUpdate(true);
        return crud;
    }

    /**
     * The filters at the top of the crudGrid
     *
     * @param crud the crudGrid that will be filtered.
     */
    protected void defineFilters(GridCrud<Coach> crud) {
        this.lastNameFilter.setPlaceholder(Translator.translate("LastName"));
        this.lastNameFilter.setClearButtonVisible(true);
        this.lastNameFilter.setValueChangeMode(ValueChangeMode.EAGER);
        this.lastNameFilter.addValueChangeListener(e -> {
            this.lastNameValue = e.getValue();
            crud.refreshGrid();
        });
        this.lastNameFilter.setWidth("15em");
        crud.getCrudLayout().addFilterComponent(this.lastNameFilter);
    }

    private OwlcmsCrudFormFactory<Coach> createFormFactory() {
        this.editingFormFactory = new CoachEditingFormFactory(Coach.class);
        return this.editingFormFactory;
    }

    @Override
    public boolean isIgnoreFopFromURL() {
        return true;
    }
}

