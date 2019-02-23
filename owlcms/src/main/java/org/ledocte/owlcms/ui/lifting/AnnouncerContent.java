/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package org.ledocte.owlcms.ui.lifting;

import java.util.Collection;
import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.AthleteRepository;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.group.GroupRepository;
import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.FOPEvent;
import org.ledocte.owlcms.state.FieldOfPlayState;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudLayout;
import org.ledocte.owlcms.ui.crudui.OwlcmsGridCrud;
import org.ledocte.owlcms.ui.home.ContentWrapping;
import org.ledocte.owlcms.ui.home.QueryParameterReader;
import org.slf4j.LoggerFactory;
import org.vaadin.crudui.crud.CrudListener;
import org.vaadin.crudui.crud.impl.GridCrud;

import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.Route;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "group/announcer", layout = AnnouncerLayout.class)
public class AnnouncerContent extends VerticalLayout
		implements CrudListener<Athlete>, QueryParameterReader, ContentWrapping {

	// @SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
	static {
		logger.setLevel(Level.DEBUG);
	}

	/**
	 * Instantiates a new announcer content.
	 */
	public AnnouncerContent() {
		GridCrud<Athlete> crud = getGridCrud();
		fillHW(crud, this);
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

		GridCrud<Athlete> crud = new OwlcmsGridCrud<Athlete>(Athlete.class,
				new OwlcmsCrudLayout(Athlete.class),
				crudFormFactory,
				grid) {
			@Override
			protected void initToolbar() {
			}

			@Override
			protected void updateButtons() {
			}
		};
		crud.setCrudListener(this);
		crud.setClickRowToUpdate(true);
		crud.getCrudLayout()
			.addToolbarComponent(new Label("toolbar stuff goes here"));

		return crud;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete Athlete) {
		AthleteRepository.save(Athlete);
		return Athlete;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete Athlete) {
		if (Athlete.getLastName()
			.equals("Ross")) {
			throw new RuntimeException("A simulated error has occurred");
		}

		Athlete savedAthlete = AthleteRepository.save(Athlete);
		FieldOfPlayState fop = (FieldOfPlayState) OwlcmsSession.getAttribute("fop");
		fop.getEventBus()
			.post(new FOPEvent.LiftingOrderUpdated());
		return savedAthlete;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {
		AthleteRepository.delete(Athlete);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.vaadin.crudui.crud.CrudListener#findAll()
	 */
	@Override
	public Collection<Athlete> findAll() {
		FieldOfPlayState fop = (FieldOfPlayState) OwlcmsSession.getAttribute("fop");
		if (fop != null) {
			Group group = fop.getGroup();
//			if (group == null) {
//				group = getFirstGroupForFOP();
//			}
			fop.switchGroup(group);
		}
		if (fop != null) {
			traceCurrentAthletes(fop);
			return fop.getLifters();
		} else {
			// no field of play, no group, empty list
			return ImmutableList.of();
		}
	}

	protected Group getFirstGroupForFOP() {
		Group group;
		List<Group> findAll = GroupRepository.findAll();
		group = (findAll.size() > 0 ? findAll.get(0) : null);
		return group;
	}

	protected void traceCurrentAthletes(FieldOfPlayState fop) {
		if (logger.isTraceEnabled()) {
			for (Athlete a : fop.getLifters()) {
				logger.trace("{}, {} -- {}", a.getLastName(), a.getFirstName(), fop.getGroup());
			}
		}
	}

}
