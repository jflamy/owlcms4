/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/timekeeper", layout = AthleteGridLayout.class)
public class TimekeeperContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TimekeeperContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	public TimekeeperContent() {
		super();
		setTopBarTitle("Timekeeper");	
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
	
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {

		Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.TimeStarted(this.getOrigin())));
		});
		start.getElement().setAttribute("theme", "primary");
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.TimeStopped(this.getOrigin())));
		});
		stop.getElement().setAttribute("theme", "primary");
		Button _1min = new Button("1:00", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.ForceTime(60000,this.getOrigin())));
		});
		_1min.getElement().setAttribute("theme", "icon");
		_1min.getElement().setAttribute("title", "Reset to 1 min");
		Button _2min = new Button("2:00", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.ForceTime(120000,this.getOrigin())));
		});
		_2min.getElement().setAttribute("theme", "icon");
		_2min.getElement().setAttribute("title", "Reset to 2 min");
		Button breakButton = new Button(IronIcons.ALARM.create(), (e) -> {
			(new BreakDialog(this)).open();
		});
		breakButton.getElement().setAttribute("theme", "icon");
		breakButton.getElement().setAttribute("title", "Break Timer");
		HorizontalLayout buttons = new HorizontalLayout(
				start,
				stop,
				_1min,
				_2min,
				breakButton);
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

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return "Timekeeper";
	}
	
}
