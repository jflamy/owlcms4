/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.UIEvent.LiftingOrderUpdated;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.BreakDialog.BreakType;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/jury", layout = AthleteGridLayout.class)
public class JuryContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	public JuryContent() {
		super();
	}

	@Override
	protected void init() {
		setTopBarTitle("Jury");
		Label topLabel = new Label("Referee Decisions");
		Label bottomLabel = new Label("Jury Decisions");
		SplitLayout split = new SplitLayout(topLabel,bottomLabel);
		split.setOrientation(Orientation.VERTICAL);
		fillHW(split,this);
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

		Button stopCompetition = new Button("Stop Competition", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.BreakStarted(BreakType.JURY, 0, this.getOrigin())));
		});
		stopCompetition.getElement().setAttribute("theme", "secondary");
		
		Button resumeCompetition = new Button("Resume Competition", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.StartLifting(this.getOrigin())));
		});
		resumeCompetition.getElement().setAttribute("theme", "secondary");

		Button breakButton = new Button(IronIcons.ALARM.create(), (e) -> {
			(new BreakDialog(this)).open();
		});
		breakButton.getElement().setAttribute("theme", "icon");
		breakButton.getElement().setAttribute("title", "Break Timer");
		HorizontalLayout buttons = new HorizontalLayout(
				stopCompetition,
				resumeCompetition
		);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}
	
	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		Button good = new Button(IronIcons.DONE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus().post(new FOPEvent.JuryDecision(fop.getCurAthlete(), this.getOrigin(), true));
			});
		});
		good.getElement().setAttribute("theme", "success");
		
		Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus().post(new FOPEvent.JuryDecision(fop.getCurAthlete(), this.getOrigin(), false));
			});
		});
		bad.getElement().setAttribute("theme", "error");
		
		HorizontalLayout decisions = new HorizontalLayout(
			good,
			bad);
		return decisions;
	}

	@Override
	public void updateGrid(LiftingOrderUpdated e) {
		// ignore
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
		return "Jury";
	}
	
}
