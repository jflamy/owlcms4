/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
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

import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.TranslationProvider;
import app.owlcms.init.OwlcmsSession;
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
public class GroupContent extends VerticalLayout
		implements CrudListener<Group>, OwlcmsContent {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(GroupContent.class);
	static {logger.setLevel(Level.INFO);}

	private OwlcmsRouterLayout routerLayout;
	private OwlcmsCrudFormFactory<Group> editingFormFactory;


	/**
	 * Instantiates the Group crudGrid.
	 */
	public GroupContent() {
		OwlcmsCrudFormFactory<Group> crudFormFactory = createFormFactory();
		GridCrud<Group> crud = createGrid(crudFormFactory);
//		defineFilters(crudGrid);
		fillHW(crud, this);
	}
	
	/**
	 * The columns of the crudGrid
	 * 
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<Group> createGrid(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		Grid<Group> grid = new Grid<Group>(Group.class, false);
		grid.addColumn(Group::getName).setHeader(TranslationProvider.getTranslation("GroupContent.0")); //$NON-NLS-1$
		grid.addColumn(
			LocalDateTimeField.getRenderer(
				Group::getWeighInTime,this.getLocale()))
			.setHeader(TranslationProvider.getTranslation("GroupContent.1")); //$NON-NLS-1$
		grid.addColumn(
			LocalDateTimeField.getRenderer(
				Group::getCompetitionTime,this.getLocale()))
			.setHeader(TranslationProvider.getTranslation("GroupContent.2")); //$NON-NLS-1$
		grid.addColumn(Group::getPlatform)
			.setHeader(TranslationProvider.getTranslation("GroupContent.3")); //$NON-NLS-1$

		GridCrud<Group> crud = new OwlcmsCrudGrid<Group>(Group.class,
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
		editingFormFactory = createGroupEditingFormFactory();
		createFormLayout(editingFormFactory);
		return editingFormFactory;
	}
	
	/**
	 * The content and ordering of the editing form.
	 *
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	protected void createFormLayout(OwlcmsCrudFormFactory<Group> crudFormFactory) {
		crudFormFactory.setVisibleProperties("name", //$NON-NLS-1$
				"weighInTime", //$NON-NLS-1$
				"competitionTime", //$NON-NLS-1$
				"platform", //$NON-NLS-1$
				"announcer", //$NON-NLS-1$
				"marshall", //$NON-NLS-1$
				"technicalController", //$NON-NLS-1$
				"timeKeeper", //$NON-NLS-1$
				"referee1", //$NON-NLS-1$
				"referee2", //$NON-NLS-1$
				"referee3", //$NON-NLS-1$
				"jury1", //$NON-NLS-1$
				"jury2", //$NON-NLS-1$
				"jury3", //$NON-NLS-1$
				"jury4", //$NON-NLS-1$
				"jury5"); //$NON-NLS-1$
		crudFormFactory.setFieldCaptions(TranslationProvider.getTranslation("GroupContent.20"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.21"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.22"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.23"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.24"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.25"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.26"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.27"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.28"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.29"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.30"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.31"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.32"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.33"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.34"), //$NON-NLS-1$
				TranslationProvider.getTranslation("GroupContent.35")); //$NON-NLS-1$
		crudFormFactory.setFieldProvider("platform", //$NON-NLS-1$
				new ComboBoxProvider<>(TranslationProvider.getTranslation("GroupContent.37"), PlatformRepository.findAll(), new TextRenderer<>(Platform::getName), Platform::getName)); //$NON-NLS-1$
		crudFormFactory.setFieldType("weighInTime", LocalDateTimeField.class); //$NON-NLS-1$
		crudFormFactory.setFieldType("competitionTime", LocalDateTimeField.class); //$NON-NLS-1$
	}

	/**
	 * Create the actual form generator with all the conversions and validations required
	 * 
	 * {@link RegistrationContent#createAthleteEditingFormFactory} for example of redefinition of bindField
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
				
				if ("competitionTime".equals(property)) { //$NON-NLS-1$
					LocalDateTimeField ldtf = (LocalDateTimeField)field;
					Validator<LocalDateTime> fv = ldtf.formatValidation(locale);
					bindingBuilder.withValidator(fv).bind(property);
				} else if ("weighInTime".equals(property)) { //$NON-NLS-1$
					LocalDateTimeField ldtf = (LocalDateTimeField)field;
					Validator<LocalDateTime> fv = ldtf.formatValidation(locale);
					bindingBuilder.withValidator(fv).bind(property);
				} else {
					super.bindField(field, property, propertyType);
				}
			}
			
			@Override
			public Group add(Group Group) {
				GroupRepository.save(Group);
				return Group;
			}

			@Override
			public Group update(Group Group) {
				return GroupRepository.save(Group);
			}

			@Override
			public void delete(Group Group) {
				GroupRepository.delete(Group);
			}

			@Override
			public Collection<Group> findAll() {
				// implemented on grid
				return null;
			}
		};
	}



	public Group add(Group domainObjectToAdd) {
		return editingFormFactory.add(domainObjectToAdd);
	}

	public Group update(Group domainObjectToUpdate) {
		return editingFormFactory.update(domainObjectToUpdate);
	}

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
	
	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return TranslationProvider.getTranslation("GroupContent.42"); //$NON-NLS-1$
	}
}
