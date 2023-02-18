/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;

import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 *
 * Defines the toolbar and the table for editing data on groups.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/groups", layout = OwlcmsLayout.class)
public class GroupContent extends VerticalLayout implements CrudListener<Group>, OwlcmsContent {

	final static Logger logger = (Logger) LoggerFactory.getLogger(GroupContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private OwlcmsCrudFormFactory<Group> editingFormFactory;
	private OwlcmsLayout routerLayout;

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

	public void closeDialog() {
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
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

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("Preparation_Groups");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
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

	@Override
	public Group update(Group domainObjectToUpdate) {
		return editingFormFactory.update(domainObjectToUpdate);
	}

	private <T extends Component> String getWindowOpenerFromClass(Class<T> targetClass,
	        String parameter) {
		return "window.open('" + URLUtils.getUrlFromTargetClass(targetClass) + "?group="
		        + URLEncoder.encode(parameter, StandardCharsets.UTF_8)
		        + "','" + targetClass.getSimpleName() + "')";
	}

	private <T extends Component> Button openInNewTab(Class<T> targetClass,
	        String label, String parameter) {
		Button button = new Button(label);
		button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter));
		return button;
	}

	/**
	 * The columns of the crudGrid
	 *
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		Grid<Group> grid = new Grid<>(Group.class, false);
		grid.getThemeNames().add("row-stripes");
		grid.addColumn(Group::getName).setHeader(getTranslation("Name")).setComparator(Group::compareTo);
		grid.addColumn(Group::getDescription).setHeader(getTranslation("Group.Description"));
		grid.addColumn(Group::size).setHeader(getTranslation("GroupSize")).setTextAlign(ColumnTextAlign.CENTER);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getWeighInTime, this.getLocale()))
		        .setHeader(getTranslation("WeighInTime")).setComparator(Group::compareToWeighIn);
		grid.addColumn(LocalDateTimeField.getRenderer(Group::getCompetitionTime, this.getLocale()))
		        .setHeader(getTranslation("StartTime"));
		grid.addColumn(Group::getPlatform).setHeader(getTranslation("Platform"));
		String translation = getTranslation("EditAthletes");
		int tSize = translation.length();
		grid.addColumn(new ComponentRenderer<>(p -> {
			Button technical = openInNewTab(RegistrationContent.class, translation, p.getName());
			technical.addThemeVariants(ButtonVariant.LUMO_SMALL);
			return technical;
		})).setHeader("").setWidth(tSize + "ch");

		GridCrud<Group> crud = new OwlcmsCrudGrid<>(Group.class, new OwlcmsGridLayout(Group.class),
		        crudFormFactory, grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		return crud;
	}

}
