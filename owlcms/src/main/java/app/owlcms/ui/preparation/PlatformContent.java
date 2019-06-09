/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.fields.LocalDateTimeField;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudGrid;
import app.owlcms.ui.crudui.OwlcmsGridLayout;
import app.owlcms.ui.shared.AppLayoutAware;
import app.owlcms.ui.shared.ContentWrapping;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class CategoryContent.
 * 
 * Defines the toolbar and the table for editing data on categories.
 */
@SuppressWarnings("serial")
@Route(value = "preparation/platforms", layout = PlatformLayout.class)
public class PlatformContent extends VerticalLayout
		implements CrudListener<Platform>, ContentWrapping, AppLayoutAware, HasDynamicTitle {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(PlatformContent.class);
	static {logger.setLevel(Level.INFO);}

	private OwlcmsRouterLayout routerLayout;
	private OwlcmsCrudFormFactory<Platform> editingFormFactory;


	/**
	 * Instantiates the Platform crudGrid.
	 */
	public PlatformContent() {
		OwlcmsCrudFormFactory<Platform> crudFormFactory = createFormFactory();
		GridCrud<Platform> crud = createGrid(crudFormFactory);
//		defineFilters(crudGrid);
		fillHW(crud, this);
	}
	
	/**
	 * The columns of the crudGrid
	 * 
	 * @param crudFormFactory what to call to create the form for editing an athlete
	 * @return
	 */
	protected GridCrud<Platform> createGrid(OwlcmsCrudFormFactory<Platform> crudFormFactory) {
		Grid<Platform> grid = new Grid<Platform>(Platform.class, false);
		grid.addColumn(Platform::getName).setHeader("Name");

		GridCrud<Platform> crud = new OwlcmsCrudGrid<Platform>(Platform.class,
				new OwlcmsGridLayout(Platform.class),
				crudFormFactory,
				grid);
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
	 * The content and ordering of the editing form.
	 *
	 * @param crudFormFactory the factory that will create the form using this information
	 */
	protected void createFormLayout(OwlcmsCrudFormFactory<Platform> crudFormFactory) {
		crudFormFactory.setVisibleProperties("name");
		crudFormFactory.setFieldCaptions("Platform Name");
		crudFormFactory.setFieldProvider("platform",
				new ComboBoxProvider<>("Platform", PlatformRepository.findAll(), new TextRenderer<>(Platform::getName), Platform::getName));
		crudFormFactory.setFieldType("weighInTime", LocalDateTimeField.class);
		crudFormFactory.setFieldType("competitionTime", LocalDateTimeField.class);
	}

	/**
	 * Create the actual form generator with all the conversions and validations required
	 * 
	 * {@link RegistrationContent#createAthleteEditingFormFactory} for example of redefinition of bindField
	 * 
	 * @return the actual factory, with the additional mechanisms to do validation
	 */
	private OwlcmsCrudFormFactory<Platform> createPlatformEditingFactory() {
		return new OwlcmsCrudFormFactory<Platform>(Platform.class) {
			@SuppressWarnings({ "rawtypes" })
			@Override
			protected void bindField(HasValue field, String property, Class<?> propertyType) {
				super.bindField(field, property, propertyType);
			}
			
			@Override
			public Platform add(Platform Platform) {
				PlatformRepository.save(Platform);
				return Platform;
			}

			@Override
			public Platform update(Platform Platform) {
				return PlatformRepository.save(Platform);
			}

			@Override
			public void delete(Platform Platform) {
				PlatformRepository.delete(Platform);
			}

			@Override
			public Collection<Platform> findAll() {
				// implemented on grid
				return null;
			}
		};
	}



	public Platform add(Platform domainObjectToAdd) {
		return editingFormFactory.add(domainObjectToAdd);
	}

	public Platform update(Platform domainObjectToUpdate) {
		return editingFormFactory.update(domainObjectToUpdate);
	}

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
		return "Preparation - Platforms";
	}
}
