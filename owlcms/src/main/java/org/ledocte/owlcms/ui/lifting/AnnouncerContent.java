/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package org.ledocte.owlcms.ui.lifting;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.AthleteRepository;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.group.GroupRepository;
import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.FOPEvent;
import org.ledocte.owlcms.state.FieldOfPlayState;
import org.ledocte.owlcms.state.UIEvent;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudLayout;
import org.ledocte.owlcms.ui.crudui.OwlcmsGridCrud;
import org.ledocte.owlcms.ui.home.ContentWrapping;
import org.ledocte.owlcms.ui.home.QueryParameterReader;
import org.ledocte.owlcms.ui.home.SafeEventBusRegistration;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "group/announcer", layout = AnnouncerLayout.class)
public class AnnouncerContent extends VerticalLayout
		implements CrudListener<Athlete>, QueryParameterReader, ContentWrapping, SafeEventBusRegistration {

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

	/**
	 * Instantiates a new announcer content.
	 * Does nothing. Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public AnnouncerContent() {
	}

	/**
	 * Process URL parameters, including query parameters
	 * @see org.ledocte.owlcms.ui.home.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
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
	 * @see com.vaadin.flow.component.Component#onDetach(com.vaadin.flow.component.DetachEvent)
	 */
	@Override
	protected void onDetach(DetachEvent detachEvent) {
		logger.trace("detaching AnnouncerContent");
		OwlcmsSession.withFop(fop -> {
			EventBus uiEventBus = fop.getUiEventBus();
			logger.debug("<<<<< unregistering {} from {}", this, uiEventBus.identifier());
			uiEventBus.unregister(this);
		});
	}

	@Subscribe
	public void updateGrid(UIEvent.LiftingOrderUpdated e) {
		Optional<UI> ui2 = this.getUI();
		if (ui2.isPresent()) {
			try {
				ui2.get().access(() -> {
					crud.refreshGrid();
				});
			} catch (UIDetachedException e1) {
				if (uiEventBus != null) uiEventBus.unregister(this);
			}
		} else {
			if (uiEventBus != null) uiEventBus.unregister(this);
		}
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
		
		Select<Group> select = new Select<Group>();
		select.setItems(GroupRepository.findAll());
		select.setTextRenderer(Group::getName);
		select.setPlaceholder("Select a Group");
		select.setEmptySelectionAllowed(true);
		OwlcmsSession.withFop((fop) -> {
			select.setValue(fop.getGroup());
		});
		select.addValueChangeListener(e -> {
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
			.addToolbarComponent(select);

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
			.post(new FOPEvent.LiftingOrderUpdated(crud.getUI().get()));
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
			return fop.getLifters();
		} else {
			// no field of play, no group, empty list
			return ImmutableList.of();
		}
	}
}
