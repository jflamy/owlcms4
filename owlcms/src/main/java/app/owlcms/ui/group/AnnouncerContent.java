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
import app.owlcms.data.group.Group;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FOPEvent;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */

@SuppressWarnings("serial")
@Route(value = "group/announcer", layout = AthleteGridLayout.class)
public class AnnouncerContent extends AthleteGridContent {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	public AnnouncerContent() {
		super();
		setTopBarTitle("Announcer");
	}
	
	/**
	 * The URL contains the group, contrary to other screens.
	 * 
	 * Normally there is only one announcer. If we have to restart the program
	 * the announcer screen will have the URL correctly set.  if there is no current 
	 * group in the FOP, the announcer will (exceptionally set it)
	 * 
	 * @see app.owlcms.ui.shared.AthleteGridContent#isIgnoreGroup()
	 */
	@Override
	public boolean isIgnoreGroup() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar()
	 */
	@Override
	protected void createTopBar() {
		super.createTopBar();
		// this hides the back arrow
		getAppLayout().setMenuVisible(false);
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.group.AthleteGridContent#createGroupSelect()
	 */
	@Override
	public void createGroupSelect() {
		super.createGroupSelect();
		groupSelect.setReadOnly(false);
		OwlcmsSession.withFop((fop) -> {
			Group group = fop.getGroup();
			logger.debug("select setting group to {}",group);
			groupSelect.setValue(group);
		});
		groupSelect.addValueChangeListener(e -> {
			// the group management logic and filtering is attached to a
			// hidden field in the grid part of the page
			Group group = e.getValue();
			logger.debug("select setting filter group to {}",group);
			getGroupFilter().setValue(group);
		});
	}
	
	
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {
		Button announce = new Button(AvIcons.MIC.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.AthleteAnnounced(this.getOrigin()));
			});
		});
		announce.getElement().setAttribute("theme", "primary icon");
		Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.TimeStartedManually(this.getOrigin()));
			});
		});
		start.getElement().setAttribute("theme", "primary icon");
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.TimeStoppedManually(this.getOrigin()));
			});
		});
		stop.getElement().setAttribute("theme", "primary icon");
		Button _1min = new Button("1:00", (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.ForceTime(60000,this.getOrigin()));
			});
		});
		_1min.getElement().setAttribute("theme", "icon");
		Button _2min = new Button("2:00", (e) -> {
			OwlcmsSession.withFop(fop -> {fop.getEventBus()
				.post(new FOPEvent.ForceTime(120000,this.getOrigin()));
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
				fop.getEventBus().post(new FOPEvent.RefereeDecision(this.getOrigin(),true, true, true, true));
				fop.getEventBus().post(new FOPEvent.DecisionReset(this.getOrigin()));
			});
		});
		good.getElement().setAttribute("theme", "success icon");
		Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getEventBus().post(new FOPEvent.RefereeDecision(this.getOrigin() ,false, false, false, false));
				fop.getEventBus().post(new FOPEvent.DecisionReset(this.getOrigin()));
			});
		});
		bad.getElement().setAttribute("theme", "error icon");
		HorizontalLayout decisions = new HorizontalLayout(
				good,
				bad);
		return decisions;
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
				.post(new FOPEvent.WeightChange(this.getOrigin(), savedAthlete));
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
	
}
