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
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FOPEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
//FIXME set the group from URL if the FOP has no group set.

@SuppressWarnings("serial")
@Route(value = "group/announcer", layout = AthleteGridLayout.class)
public class AnnouncerContent extends AthleteGridContent {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.DEBUG);
	}
	
	public AnnouncerContent() {
		setTopBarTitle("Announcer");
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
		OwlcmsSession.withFop(fop -> {
			fop.getEventBus()
				.post(new FOPEvent.WeightChange(grid.getUI().get(), savedAthlete));
		});
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
	 * Annoncer keeps the group in the URL
	 * Normally there is only one announcer. If we have to restart the program
	 * the announcer screen will have the URL correctly set.  if there is no current 
	 * group in the FOP, the announcer will (exceptionally set it)
	 * 
	 * @see app.owlcms.ui.group.AthleteGridContent#isIgnoreGroup()
	 */
	@Override
	public boolean isIgnoreGroup() {
		logger.trace("AnnouncerContent ignoreGroup false");
		return false;
	}
	
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {
		Button announce = new Button(AvIcons.MIC.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.AthleteAnnounced(announcerBar.getUI().get()));
			});
		});
		announce.getElement().setAttribute("theme", "primary icon");
		Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.TimeStartedManually(announcerBar.getUI().get()));
			});
		});
		start.getElement().setAttribute("theme", "primary icon");
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.TimeStoppedManually(announcerBar.getUI().get()));
			});
		});
		stop.getElement().setAttribute("theme", "primary icon");
		Button _1min = new Button("1:00", (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.ForceTime(60000,announcerBar.getUI().get()));
			});
		});
		_1min.getElement().setAttribute("theme", "icon");
		Button _2min = new Button("2:00", (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.ForceTime(120000,announcerBar.getUI().get()));
			});
		});
		_2min.getElement().setAttribute("theme", "icon");
		HorizontalLayout buttons = new HorizontalLayout(
				announce,
				start,
				stop,
				_1min,
				_2min);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}
	
	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		//FIXME: timer does not reset correctly after decision
		Button good = new Button(IronIcons.DONE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getEventBus().post(new FOPEvent.RefereeDecision(announcerBar.getUI().get() ,true, true, true, true));
				fop.getEventBus().post(new FOPEvent.DecisionReset(announcerBar.getUI().get()));
			});
		});
		good.getElement().setAttribute("theme", "success icon");
		Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getEventBus().post(new FOPEvent.RefereeDecision(announcerBar.getUI().get() ,false, false, false, false));
				fop.getEventBus().post(new FOPEvent.DecisionReset(announcerBar.getUI().get()));
			});
		});
		bad.getElement().setAttribute("theme", "error icon");
		HorizontalLayout decisions = new HorizontalLayout(
				good,
				bad);
		return decisions;
	}
	
}
