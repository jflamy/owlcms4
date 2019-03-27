/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package app.owlcms.ui.group;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FOPEvent;
import app.owlcms.state.FieldOfPlayState;
import app.owlcms.ui.home.QueryParameterReader;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "group/marshall", layout = MarshallLayout.class)
public class MarshallContent extends BaseContent implements QueryParameterReader {

	// @SuppressWarnings("unused")
	final private Logger logger = (Logger) LoggerFactory.getLogger(MarshallContent.class);
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	private void initLoggers() {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.DEBUG);
	}
	
	public MarshallContent() {
		initLoggers();
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
	
	@Override
	public boolean isIgnoreGroup() {
		logger.trace("MarshallContent ignoreGroup true");
		// follow group from FOP, do not add group to URL
		return true;
	}
}
