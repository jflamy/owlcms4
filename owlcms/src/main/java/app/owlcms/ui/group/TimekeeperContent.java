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

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FOPEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "group/timekeeper", layout = AthleteGridLayout.class)
public class TimekeeperContent extends AthleteGridContent {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TimekeeperContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	public TimekeeperContent() {
		setTopBarTitle("Timekeeper");	
	}
	
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {

		Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.TimeStartedManually(this.getOrigin())));
		});
		start.getElement().setAttribute("theme", "primary");
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.TimeStoppedManually(this.getOrigin())));
		});
		stop.getElement().setAttribute("theme", "primary");
		Button _1min = new Button("1:00", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.ForceTime(60000,this.getOrigin())));
		});
		_1min.getElement().setAttribute("theme", "icon");
		Button _2min = new Button("2:00", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.ForceTime(120000,this.getOrigin())));
		});
		_2min.getElement().setAttribute("theme", "icon");
		HorizontalLayout buttons = new HorizontalLayout(
				start,
				stop,
				_1min,
				_2min);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {;
		// do nothing;
	}
	
}
