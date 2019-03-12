/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package app.owlcms.ui.lifting;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.github.appreciated.app.layout.router.AppLayoutRouterLayout;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FOPEvent;
import app.owlcms.state.FieldOfPlayState;
import app.owlcms.state.UIEvent;
import app.owlcms.ui.appLayout.AppLayoutContent;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.ui.crudui.OwlcmsCrudLayout;
import app.owlcms.ui.crudui.OwlcmsGridCrud;
import app.owlcms.ui.home.ContentWrapping;
import app.owlcms.ui.home.QueryParameterReader;
import app.owlcms.ui.home.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "group/announcer", layout = AnnouncerLayout.class)
public class AnnouncerContent extends VerticalLayout
		implements CrudListener<Athlete>, QueryParameterReader, ContentWrapping, SafeEventBusRegistration, UIEventProcessor, AppLayoutContent {

	// @SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("owlcms.uiEventLogger");
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.DEBUG);
	}

	private Location location;
	private UI locationUI;
	private GridCrud<Athlete> crud;
	private EventBus uiEventBus;
	private AppLayoutRouterLayout parentLayout;
	
//	private TextField lastNameFilter = new TextField();
//	private ComboBox<AgeDivision> ageDivisionFilter = new ComboBox<>();
//	private ComboBox<Category> categoryFilter = new ComboBox<>();
	private ComboBox<Group> groupFilter = new ComboBox<>();

	/**
	 * Instantiates a new announcer content.
	 * Does nothing. Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public AnnouncerContent() {
	}

	/**
	 * Process URL parameters, including query parameters
	 * @see app.owlcms.ui.home.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		QueryParameterReader.super.setParameter(event, parameter);
		location = event.getLocation();
		locationUI = event.getUI();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		logger.trace("attaching AnnouncerContent");
		crud = getGridCrud();
		fillHW(crud, this);
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			fop.switchGroup(fop.getGroup());
			crud.refreshGrid();
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.lifting.UIEventProcessor#updateGrid(app.owlcms.state.UIEvent.LiftingOrderUpdated)
	 */
	@Subscribe
	public void updateGrid(UIEvent.LiftingOrderUpdated e) {
		UIEventProcessor.uiAccess(crud, uiEventBus, e, () -> {
			crud.refreshGrid();
		});
	}
	
	/**
	 * Gets the grid crud.
	 *
	 * @return the grid crud
	 */
	public GridCrud<Athlete> getGridCrud() {
		OwlcmsCrudFormFactory<Athlete> crudFormFactory = new AthleteCardFormFactory(Athlete.class);

		Grid<Athlete> grid = new Grid<Athlete>(Athlete.class, false);
		ThemeList themes = grid.getThemeNames();
		themes.add("compact");
		themes.add("row-stripes");
		grid.setColumns("lastName", "firstName", "team", "category", "nextAttemptRequestedWeight", "attemptsDone");
		grid.getColumnByKey("lastName")
			.setHeader("Last Name");
		grid.getColumnByKey("firstName")
			.setHeader("First Name");
		grid.getColumnByKey("team")
			.setHeader("Team");
		grid.getColumnByKey("category")
			.setHeader("Category");
		grid.getColumnByKey("nextAttemptRequestedWeight")
			.setHeader("Requested Weight");
		grid.getColumnByKey("attemptsDone")
			.setHeader("Attempts Done");

		OwlcmsCrudLayout owlcmsCrudLayout = new OwlcmsCrudLayout(Athlete.class);
		GridCrud<Athlete> crud = new OwlcmsGridCrud<Athlete>(Athlete.class,
				owlcmsCrudLayout,
				crudFormFactory,
				grid) {
			@Override
			protected void initToolbar() {}
			@Override
			protected void updateButtons() {}
		};
		
		groupFilter.setPlaceholder("Group");
		groupFilter.setItems(GroupRepository.findAll());
		groupFilter.setItemLabelGenerator(Group::getName);
		// hide because the top bar has it
		groupFilter.getStyle().set("display", "none");
		OwlcmsSession.withFop((fop) -> {
			groupFilter.setValue(fop.getGroup());
		});
		groupFilter.addValueChangeListener(e -> {
				Group newGroup = e.getValue();
				logger.debug("manually switching group to {}",newGroup != null ? newGroup.getName() : null);
				OwlcmsSession.withFop((fop) -> {
					fop.switchGroup(newGroup);
				});
				crud.refreshGrid();
				updateURLLocation(locationUI, location, newGroup);
				
		});
		
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		crud.getCrudLayout()
			.addToolbarComponent(groupFilter);

		return crud;
	}

	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<String, List<String>>(location.getQueryParameters().getParameters());
		params.put("fop",Arrays.asList(OwlcmsSession.getFop().getName()));
		if (newGroup != null) {
			params.put("group",Arrays.asList(newGroup.getName()));
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete Athlete) {
		//FIXME: should do a persist, not a merge
		AthleteRepository.save(Athlete);
		return Athlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete Athlete) {
		Athlete savedAthlete = AthleteRepository.save(Athlete);
		FieldOfPlayState fop = (FieldOfPlayState) OwlcmsSession.getAttribute("fop");
		fop.getEventBus()
			.post(new FOPEvent.WeightChange(crud.getUI().get(), savedAthlete));
		return savedAthlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {
		AthleteRepository.delete(Athlete);
	}

	/**
	 * Get the content of the grid.
	 * Invoked by refreshGrid.
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		FieldOfPlayState fop = OwlcmsSession.getFop();
		if (fop != null) {
			return fop.getLiftingOrder();
		} else {
			// no field of play, no group, empty list
			return ImmutableList.of();
		}
	}

	/**
	 * @return the groupFilter
	 */
	public ComboBox<Group> getGroupFilter() {
		return groupFilter;
	}

	@Override
	public AppLayoutRouterLayout getParentLayout() {
		return parentLayout;
	}

	@Override
	public void setParentLayout(AppLayoutRouterLayout parentLayout) {
		this.parentLayout = parentLayout;
	}

	public void refresh() {
		crud.refreshGrid();
	}
}
