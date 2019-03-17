/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.preparation;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collection;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;
import org.vaadin.crudui.form.impl.field.provider.ComboBoxProvider;

import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudLayout;
import app.owlcms.ui.crudui.OwlcmsGridCrud;
import app.owlcms.ui.home.ContentWrapping;
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
		implements CrudListener<Group>, ContentWrapping {
	
	final private static Logger logger = (Logger)LoggerFactory.getLogger(GroupContent.class);
	static {logger.setLevel(Level.DEBUG);}


	/**
	 * Instantiates the Group grid.
	 */
	public GroupContent() {
		OwlcmsCrudFormFactory<Group> crudFormFactory = createFormFactory();
		GridCrud<Group> crud = createGrid(crudFormFactory);
//		defineFilters(crud);
//		defineQueries(crud);
		fillHW(crud, this);
	}
	
//	/**
//	 * Define how to populate the athlete grid
//	 * 
//	 * @param crud
//	 */
//	protected void defineQueries(GridCrud<Group> crud) {
//		crud.setFindAllOperation(
//			DataProvider.fromCallbacks(
//				query -> GroupRepository
//					.findFiltered(nameFilter.getValue(), ageDivisionFilter.getValue(), activeFilter.getValue(), query.getOffset(), query.getLimit())
//					.stream(),
//				query -> GroupRepository.countFiltered(nameFilter.getValue(), ageDivisionFilter.getValue(), activeFilter.getValue())));
//	}
	
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
			new LocalDateTimeRenderer<Group>(
				Group::getWeighInTime,
				DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
				.withLocale(this.getLocale())))
			.setHeader("Weigh-in Time");
		grid.addColumn(
			new LocalDateTimeRenderer<Group>(
				Group::getCompetitionTime,
				DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
				.withLocale(this.getLocale())))
			.setHeader("Start Time");
		grid.addColumn(Group::getPlatform)
			.setHeader("Platform");

		GridCrud<Group> crud = new OwlcmsGridCrud<Group>(Group.class,
				new OwlcmsCrudLayout(Group.class),
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
	 * The content and ordering of the editing form
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
				if ("competitionTime".equals(property)) {
					competitionTimeValidation(bindingBuilder);
					bindingBuilder.bind(property);
				} else if ("weighInTime".equals(property)) {
					weighInTimeValidation(bindingBuilder);
					bindingBuilder.bind(property);
				} else {
					super.bindField(field, property, propertyType);
				}
			}
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			protected void competitionTimeValidation(Binder.BindingBuilder bindingBuilder) {
				Validator<LocalDateTime> v = Validator.from(
					ld -> {
						LocalDateTime now = LocalDateTime.now();
						int compare = ld.compareTo(now);
						logger.debug("start {}  {}  {}", ld, now, compare);
						return compare >= 0; // ld is two hours after app start, ld > now for the first two hours
						},
					"Competition cannot be in the past");
				bindingBuilder.withValidator(v);
			}
			
			@SuppressWarnings({ "rawtypes", "unchecked" })
			protected void weighInTimeValidation(Binder.BindingBuilder bindingBuilder) {
				Validator<LocalDateTime> v = Validator.from(
					ld -> {
						LocalDateTime now = LocalDateTime.now();
						int compare = ld.compareTo(now);
						logger.debug("weighin {}  {}  {}", ld, now, compare);
						return compare <= 0;  // ld <= now (set when app starts)
						},
					"Weigh-in cannot be in the past");
				bindingBuilder.withValidator(v);
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
	 * The pencil button on the toolbar triggers an edit.
	 * 
	 * This method is called when the pop-up is closed with Update
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Group update(Group Group) {
		return GroupRepository.save(Group);
	}

	/**
	 * The delete button on the toolbar triggers this method
	 * 
	 * (or the one in the form)
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
	
//	/**
//	 * The filters at the top of the grid
//	 * 
//	 * @param crud the grid that will be filtered.
//	 */
//	protected void defineFilters(GridCrud<Group> crud) {
//		nameFilter.setPlaceholder("Name");
//		nameFilter.setClearButtonVisible(true);
//		nameFilter.setValueChangeMode(ValueChangeMode.EAGER);
//		nameFilter.addValueChangeListener(e -> {
//			crud.refreshGrid();
//		});
//		crud.getCrudLayout()
//			.addFilterComponent(nameFilter);
//
//		ageDivisionFilter.setPlaceholder("Age Division");
//		ageDivisionFilter.setItems(AgeDivision.findAll());
//		ageDivisionFilter.setItemLabelGenerator(AgeDivision::name);
//		ageDivisionFilter.addValueChangeListener(e -> {
//			crud.refreshGrid();
//		});
//		crud.getCrudLayout()
//			.addFilterComponent(ageDivisionFilter);
//		crud.getCrudLayout()
//			.addToolbarComponent(new Label(""));
//		
//		activeFilter.addValueChangeListener(e -> {
//			crud.refreshGrid();
//		});
//		activeFilter.setLabel("Active");
//		activeFilter.setAriaLabel("Active Categories Only");
//		crud.getCrudLayout()
//			.addFilterComponent(activeFilter);
//
//		Button clearFilters = new Button(null, VaadinIcon.ERASER.create());
//		clearFilters.addClickListener(event -> {
//			ageDivisionFilter.clear();
//		});
//		crud.getCrudLayout()
//			.addFilterComponent(clearFilters);
//	}
}
