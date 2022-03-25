/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 *
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/groups", layout = GroupLayout.class)
public class GroupContent extends VerticalLayout implements CrudListener<Group>, OwlcmsContent {

    final static Logger logger = (Logger) LoggerFactory.getLogger(GroupContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private OwlcmsRouterLayout routerLayout;
    private OwlcmsCrudFormFactory<Group> editingFormFactory;

    /**
     * Instantiates the Group crudGrid.
     */
    public GroupContent() {
        editingFormFactory = new GroupEditingFormFactory(Group.class, this);
        GridCrud<Group> crud = createGrid(editingFormFactory);
//		defineFilters(crudGrid);
        fillHW(crud, this);
    }

    @Override
    public Group add(Group domainObjectToAdd) {
        return editingFormFactory.add(domainObjectToAdd);
    }

    @Override
    public void delete(Group domainObjectToDelete) {
        editingFormFactory.delete(domainObjectToDelete);
    }

    /**
     * The refresh button on the toolbar
     *
     * @see org.vaadin.crudui.crud.CrudListener#findAll()
     */
    @Override
    public Collection<Group> findAll() {
        return GroupRepository.findAll().stream().sorted(Group::compareToWeighIn).collect(Collectors.toList());
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Preparation_Groups");
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
    public Group update(Group domainObjectToUpdate) {
        return editingFormFactory.update(domainObjectToUpdate);
    }

//    /**
//     * The content and ordering of the editing form.
//     *
//     * @param crudFormFactory the factory that will create the form using this information
//     */
//    protected void createFormLayout(OwlcmsCrudFormFactory<Group> crudFormFactory) {
//        crudFormFactory.setVisibleProperties("name", "description", "platform", "weighInTime", "competitionTime", "weighIn1",
//                "weighIn2", "announcer",
//                "marshall", "technicalController", "timeKeeper", "referee1", "referee2", "referee3", "jury1", "jury2",
//                "jury3", "jury4", "jury5");
//        crudFormFactory.setFieldCaptions(getTranslation("Name"), getTranslation("Group.Description"), getTranslation("Platform"),
//                getTranslation("WeighInTime"), getTranslation("StartTime"),
//                getTranslation("Weighin1"), getTranslation("Weighin2"),
//                getTranslation("Announcer"),
//                getTranslation("Marshall"), getTranslation("TechnicalController"), getTranslation("Timekeeper"),
//                getTranslation("Referee1"), getTranslation("Referee2"), getTranslation("Referee3"),
//                getTranslation("Jury1"), getTranslation("Jury2"), getTranslation("Jury3"), getTranslation("Jury4"),
//                getTranslation("Jury5"));
//        crudFormFactory.setFieldProvider("platform", 
//                new OwlcmsComboBoxProvider<>(getTranslation("Platform"), PlatformRepository.findAll(), new TextRenderer<>(Platform::getName), Platform::getName));
//        crudFormFactory.setFieldType("weighInTime", LocalDateTimePicker.class);
//        crudFormFactory.setFieldType("competitionTime", LocalDateTimePicker.class);
//    }

    /**
     * The columns of the crudGrid
     *
     * @param crudFormFactory what to call to create the form for editing an athlete
     * @return
     */
    protected GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
        Grid<Group> grid = new Grid<>(Group.class, false);
        grid.addColumn(Group::getName).setHeader(getTranslation("Name")).setComparator(Group::compareTo);
        grid.addColumn(Group::getDescription).setHeader(getTranslation("Group.Description"));
        grid.addColumn(Group::size).setHeader(getTranslation("GroupSize")).setTextAlign(ColumnTextAlign.CENTER);
        grid.addColumn(LocalDateTimeField.getRenderer(Group::getWeighInTime, this.getLocale()))
                .setHeader(getTranslation("WeighInTime")).setComparator(Group::compareToWeighIn);
        grid.addColumn(LocalDateTimeField.getRenderer(Group::getCompetitionTime, this.getLocale()))
                .setHeader(getTranslation("StartTime"));
        grid.addColumn(Group::getPlatform).setHeader(getTranslation("Platform"));

        GridCrud<Group> crud = new OwlcmsCrudGrid<>(Group.class, new OwlcmsGridLayout(Group.class),
                crudFormFactory, grid);
        crud.setCrudListener(this);
        crud.setClickRowToUpdate(true);
        return crud;
    }
//
//    /**
//     * Define the form used to edit a given Group.
//     *
//     * @return the form factory that will create the actual form on demand
//     */
//    private OwlcmsCrudFormFactory<Group> createFormFactory() {
//        editingFormFactory = createGroupEditingFormFactory();
//        createFormLayout(editingFormFactory);
//        return editingFormFactory;
//    }
//
//    /**
//     * Create the actual form generator with all the conversions and validations required
//     *
//     * {@link RegistrationContent#createAthleteEditingFormFactory} for example of redefinition of bindField
//     *
//     * @return the actual factory, with the additional mechanisms to do validation
//     */
//    private OwlcmsCrudFormFactory<Group> createGroupEditingFormFactory() {
//        return new GroupEditingFormFactory(Group.class);
//    }

    public void closeDialog() {
    }
}
