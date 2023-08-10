/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.DecisionReset;

@SuppressWarnings("serial")
@Tag("decision-board-template")
@JsModule("./components/DecisionBoard.js")
@JsModule("./components/AudioContext.js")
@Route("displays/publicFacingDecision")

public class PublicFacingDecisionBoard extends AttemptBoard {

	public PublicFacingDecisionBoard() {
		super();
		setPublicFacing(true);
		setShowBarbell(false);
		setSilenced(isSilencedByDefault());
		breakTimer.setParent("DecisionBoard");
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Decision_PF_") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public boolean isSilencedByDefault() {
		return true;
	}

	@Override
	protected void checkImages() {
		athletePictures = false;
		teamFlags = false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.displays.attemptboard.AttemptBoard#onAttach(com.vaadin.flow.
	 * component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		checkVideo(Config.getCurrent().getParamStylesDir()+"/video/decisionboard.css", routeParameter, this);
		decisions.setPublicFacing(true);
		setPublicFacing(true);
		setShowBarbell(false);
		decisions.setDontReset(true);
		setSilenced(isSilencedByDefault());
	}
	
	protected void doEmpty() {
		FieldOfPlay fop2 = OwlcmsSession.getFop();
		boolean inactive = fop2 == null || fop2.getState() == FOPState.INACTIVE;
		this.getElement().callJsFunction("clear");
		this.getElement().setProperty("inactiveBlockStyle", (inactive ? "display:grid" : "display:none"));
		this.getElement().setProperty("activeGridStyle", (inactive ? "display:none" : "display:grid"));
		this.getElement().setProperty("inactiveClass", (inactive ? "bigTitle" : ""));
		this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
	}
	
	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> syncWithFOP(OwlcmsSession.getFop()));
	}
	
	@Subscribe
	@Override
	public void slaveDecisionReset(DecisionReset e) {
		// do nothing.  Wait for new clock.
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		this.getElement().setProperty("hideBecauseDecision", "hideBecauseDecision");
	}

	@Subscribe
	@Override
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		// do nothing.  Wait for new clock.
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		this.getElement().setProperty("hideBecauseDecision", "hideBecauseDecision");
	}
	
	@Subscribe
	public void slaveEvent(UIEvent e) {
		// do nothing.  Wait for new clock.
		uiEventLogger.debug("*** {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
	}
}
