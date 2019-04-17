/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.ui.preparation;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Locale;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;

import app.owlcms.components.crudui.OwlcmsCrudFormFactory;
import app.owlcms.components.crudui.OwlcmsGridCrud;
import app.owlcms.components.crudui.OwlcmsGridLayout;
import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.ContentWrapping;
import app.owlcms.ui.shared.AppLayoutAware;
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
public class GroupContent extends VerticalLayout
		implements CrudListener<Group>, ContentWrapping, AppLayoutAware {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(GroupContent.class);
	static {logger.setLevel(Level.INFO);}

	private OwlcmsRouterLayout routerLayout;


	/**
	 * Instantiates the Group grid.
	 */
	public GroupContent() {
		OwlcmsCrudFormFactory<Group> crudFormFactory = createFormFactory();
		GridCrud<Group> crud = createGrid(crudFormFactory);
//		defineFilters(grid);
		fillHW(crud, this);
	}
	
	/**
	 * The columns of the grid
	 * 
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		Grid<Group> grid = new Grid<Group>(Group.class, false);
		grid.addColumn(Group::getName).setHeader("Name");
		grid.addColumn(
			LocalDateTimeField.getRenderer(
				Group::getWeighInTime,this.getLocale()))
			.setHeader("Weigh-in Time");
		grid.addColumn(
			LocalDateTimeField.getRenderer(
				Group::getCompetitionTime,this.getLocale()))
			.setHeader("Start Time");
		grid.addColumn(Group::getPlatform)
			.setHeader("Platform");

		GridCrud<Group> crud = new OwlcmsGridCrud<Group>(Group.class,
				new OwlcmsGridLayout(Group.class),
				crudFormFactory,
				grid);
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		return crud;
	}

	/**
	 * Define the form used to edit a given Group.
	 * 
	 * @return the form factory that will create the actual form on demand
	 */
	private OwlcmsCrudFormFactory<Group> createFormFactory() {
		OwlcmsCrudFormFactory<Group> editingFormFactory = createGroupEditingFormFactory();
		createFormLayout(editingFormFactory);
		return editingFormFactory;
	}
	
	/**
	 * The content and ordering of the editing form.
	 *
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	protected void createFormLayout(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		crudFormFactory.setVisibleProperties("name",
				"weighInTime",
				"competitionTime",
				"platform");
		crudFormFactory.setFieldCaptions("Name",
			"Weigh-in Time",
			"Start Time",
			"Platform");
		crudFormFactory.setFieldProvider("platform",
            new ComboBoxProvider<>("Platform", PlatformRepository.findAll(), new TextRenderer<>(Platform::getName), Platform::getName));
		crudFormFactory.setFieldType("weighInTime", LocalDateTimeField.class);
		crudFormFactory.setFieldType("competitionTime", LocalDateTimeField.class);
	}

	/**
	 * Create the actual form generator with all the conversions and validations required
	 * 
	 * {@link AthletesContent#createAthleteEditingFormFactory} for example of redefinition of bindField
	 * 
	 * @return the actual factory, with the additional mechanisms to do validation
	 */
	private OwlcmsCrudFormFactory<Group> createGroupEditingFormFactory() {
		return new OwlcmsCrudFormFactory<Group>(Group.class) {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			protected void bindField(HasValue field, String property, Class<?> propertyType) {
				Binder.BindingBuilder bindingBuilder = binder.forField(field);
				Locale locale = OwlcmsSession.getLocale();
				
				if ("competitionTime".equals(property)) {
					LocalDateTimeField ldtf = (LocalDateTimeField)field;
					Validator<LocalDateTime> fv = ldtf.formatValidation(locale);
					bindingBuilder.withValidator(fv).bind(property);
				} else if ("weighInTime".equals(property)) {
					LocalDateTimeField ldtf = (LocalDateTimeField)field;
					Validator<LocalDateTime> fv = ldtf.formatValidation(locale);
					bindingBuilder.withValidator(fv).bind(property);
				} else {
					super.bindField(field, property, propertyType);
				}
			}
		};
	}

	/**
	 * The plus button on the toolbar triggers an add
	 * 
	 * This method is called when the pop-up is closed.
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Group add(Group Group) {
		GroupRepository.save(Group);
		return Group;
	}

	/**
	 * This method is called when the dialog is closed with the update button
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Group update(Group Group) {
		return GroupRepository.save(Group);
	}

	/**
	 * The delete button on the toolbar triggers this method
	 * (or the delete button in the form)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Group Group) {
		GroupRepository.delete(Group);
	}

	/**
	 * The refresh button on the toolbar
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Group> findAll() {
		return GroupRepository.findAll();
	}
	
	@Override
	public OwlcmsRouterLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
}
