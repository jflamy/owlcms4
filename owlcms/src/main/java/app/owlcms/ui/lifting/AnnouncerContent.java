/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.lifting;

import java.text.MessageFormat;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */

@SuppressWarnings("serial")
@Route(value = "lifting/announcer", layout = AthleteGridLayout.class)
public class AnnouncerContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.TRACE);
	}

	public AnnouncerContent() {
		super();
		defineFilters(crudGrid);
		setTopBarTitle("Announcer");
	}
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.group.AthleteGridContent#createGroupSelect() */
	@Override
	public void createGroupSelect() {
		super.createGroupSelect();
		groupSelect.setReadOnly(false);
		OwlcmsSession.withFop((fop) -> {
			Group group = fop.getGroup();
			logger.debug("select setting group to {}", group);
			groupSelect.setValue(group);
			getGroupFilter().setValue(group);
		});
		groupSelect.addValueChangeListener(e -> {
			// the group management logic and filtering is attached to a
			// hidden field in the crudGrid part of the page
			Group group = e.getValue();
			logger.debug("select setting filter group to {}", group);
			getGroupFilter().setValue(group);
		});
	}
	
	@Override
	public Component createReset() {
		reset = new Button(IronIcons.REFRESH.create(), (e) ->
			OwlcmsSession.withFop((fop) -> {
				Group group = fop.getGroup();
				logger.debug("resetting {} from database", group);
				fop.switchGroup(group,this);
			}));
		reset.getElement().setAttribute("title", "Reload group from database.");
		reset.getElement().setAttribute("theme", "secondary contrast small icon");
		return reset;
	}

	/**
	 * The URL contains the group, contrary to other screens.
	 *
	 * Normally there is only one announcer. If we have to restart the program the announcer screen will
	 * have the URL correctly set. if there is no current group in the FOP, the announcer will
	 * (exceptionally set it)
	 *
	 * @see app.owlcms.ui.shared.AthleteGridContent#isIgnoreGroupFromURL()
	 */
	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {
		Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus()
				.post(new FOPEvent.TimeStarted(this.getOrigin()));
			});
		});
		start.getElement().setAttribute("theme", "primary icon");
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus()
				.post(new FOPEvent.TimeStopped(this.getOrigin()));
			});
		});
		stop.getElement().setAttribute("theme", "primary icon");
		Button _1min = new Button("1:00", (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus()
				.post(new FOPEvent.ForceTime(60000, this.getOrigin()));
			});
		});
		_1min.getElement().setAttribute("theme", "icon");
		Button _2min = new Button("2:00", (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus()
				.post(new FOPEvent.ForceTime(120000, this.getOrigin()));
			});
		});
		_2min.getElement().setAttribute("theme", "icon");
		Button breakButton = new Button(IronIcons.ALARM.create(), (e) -> {
			(new BreakDialog(this)).open();
		});
		breakButton.getElement().setAttribute("theme", "icon");
		breakButton.getElement().setAttribute("title", "Time to start countdown / Break Timer");

		HorizontalLayout buttons = new HorizontalLayout(
			//				announce,
			start,
			stop,
			_1min,
			_2min,
			breakButton);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar() */
	@Override
	protected void createTopBar() {
		super.createTopBar();
		// this hides the back arrow
		getAppLayout().setMenuVisible(false);
	}


	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		Button good = new Button(IronIcons.DONE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus().post(new FOPEvent.RefereeDecision(fop.getCurAthlete(), this.getOrigin(), true, true, true, true));
				fop.getFopEventBus().post(new FOPEvent.DecisionReset(this.getOrigin()));
			});
		});
		good.getElement().setAttribute("theme", "success icon");
		
		Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus().post(new FOPEvent.RefereeDecision(fop.getCurAthlete(), this.getOrigin(), false, false, false, false));
				fop.getFopEventBus().post(new FOPEvent.DecisionReset(this.getOrigin()));
			});
		});
		bad.getElement().setAttribute("theme", "error icon");
		
		HorizontalLayout decisions = new HorizontalLayout(
			good,
			bad);
		return decisions;
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return "Announcer";
	}
	
	@Subscribe
	public void slaveRefereeDecision(UIEvent.RefereeDecision e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			int d = e.decision ? 1 : 0;
			String text = MessageFormat.format("{0,choice,0#No Lift|1#Good Lift} for {1}", d, e.getAthlete().getFullName());
			
			Notification n = new Notification();
			// Notification theme styling is done in META-INF/resources/frontend/styles/shared-styles.html
			String themeName = e.decision?"success":"error";
			n.getElement().getThemeList().add(themeName);

			Div label = new Div();
			label.add(text);
			label.addClickListener((event)-> n.close());
			label.setSizeFull();
			label.getStyle().set("font-size", "large");
			n.add(label);
			n.setPosition(Position.TOP_END);
			n.setDuration(5000);
			n.open();
		});
	}

}
