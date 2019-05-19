/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.UIEvent;
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

	private DecisionElement decisions;
	
	public JuryContent() {
		// this just calls init, which we override.
		super();
	}

	@Override
	protected void init() {
		this.setBoxSizing(BoxSizing.BORDER_BOX);
		this.setSizeFull();
		setTopBarTitle("Jury");
		
		buildRefereeBox(this);
		buildJuryBox(this, 3);
	}

	private void buildRefereeBox(VerticalLayout container) {
		Label label = new Label("Referee Decisions");
		H3 labelWrapper = new H3(label);
		labelWrapper.setHeight("5%");
		container.add(labelWrapper);
		
		decisions = new DecisionElement();
		Div decisionWrapper = new Div(decisions);
		decisionWrapper.getStyle().set("width", "50%");
		
		HorizontalLayout top = new HorizontalLayout(decisionWrapper);
		top.setBoxSizing(BoxSizing.BORDER_BOX);
		top.setJustifyContentMode(JustifyContentMode.CENTER);
		top.getStyle().set("background-color", "black");
		top.setHeight("40%");
		top.setWidthFull();
		top.setPadding(true);
		top.setMargin(true);

		container.add(top);
		container.setAlignSelf(Alignment.CENTER, top);
	}
	
	private void buildJuryBox(VerticalLayout container, int nbJurors) {
		Label bottomLabel = new Label("Jury Decisions");
		H3 labelWrapper = new H3(bottomLabel);
		labelWrapper.setHeight("5%");
		container.add(labelWrapper);
		
		HorizontalLayout bottom = new HorizontalLayout();
		bottom.setBoxSizing(BoxSizing.BORDER_BOX);
		bottom.setJustifyContentMode(JustifyContentMode.AROUND);
		bottom.setHeight("40%");
		bottom.setWidthFull();
		bottom.setPadding(true);
		bottom.setMargin(true);
		
		for (int i = 0; i < nbJurors; i++) {
			Icon icon = VaadinIcon.CIRCLE_THIN.create();
			icon.setSize("100%");
			bottom.add(icon);
			Shortcuts.addShortcutListener(icon, () -> {logger.warn("0");}, Key.DIGIT_0, (KeyModifier)null);
			
		}

		container.add(bottom);
		container.setAlignSelf(Alignment.CENTER, bottom);
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
		if (decisions != null) {
			decisions.getElement().callFunction("showDecisions", false, true, true, false);
		}
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
	
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
				this.getOrigin(), e.getOrigin());
		OwlcmsSession.withFop((fop) -> doUpdateTopBar(fop.getCurAthlete(), 0));
	}
	
	@Subscribe
	public void down(UIEvent.DownSignal e) {
		// Ignore down signal
	}
	
}
