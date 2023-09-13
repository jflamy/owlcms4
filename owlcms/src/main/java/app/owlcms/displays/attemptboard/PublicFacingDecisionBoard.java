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

import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displayselection.AbstractDisplayWrapper;
import app.owlcms.nui.displayselection.AttemptBoardPage;
import app.owlcms.nui.displayselection.PublicFacingDecisionBoardPage;
import app.owlcms.nui.displayselection.SoundEntries;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.DecisionReset;

/**
 * This version is typically used for streaming.
 * The referee order is as seen from the jury table, 1 is on the left.
 * The decision lights stay on instead of resetting.
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
@Tag("decision-board-template")
@JsModule("./components/DecisionBoard.js")
@JsModule("./components/AudioContext.js")
@Route("displays/publicFacingDecision")

public class PublicFacingDecisionBoard extends AttemptBoard implements SoundEntries {

	private AttemptBoardPage wrapper;
	
	public PublicFacingDecisionBoard(PublicFacingDecisionBoardPage publicFacingDecisionBoardWrapper) {
		this.wrapper = publicFacingDecisionBoardWrapper;
	}


	@Override
	public String getPageTitle() {
		return getTranslation("Decision_PF_") + OwlcmsSession.getFopNameIfMultiple();
	}


	@Subscribe
	@Override
	public void slaveDecisionReset(DecisionReset e) {
		// do nothing. Wait for new clock.
	}

	@Subscribe
	public void slaveEvent(UIEvent e) {
		// do nothing. Wait for new clock.
	}

	@Subscribe
	@Override
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		// do nothing. Wait for new clock.
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> syncWithFOP(OwlcmsSession.getFop()));
	}

	@Override
	protected void checkImages() {
		athletePictures = false;
		teamFlags = false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.displays.attemptboard.AttemptBoard#onAttach(com.vaadin.flow. component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		checkVideo(Config.getCurrent().getParamStylesDir() + "/video/decisionboard.css", routeParameter, this);
		decisions.setPublicFacing(true);
		setPublicFacing(true);
		setShowBarbell(false);
		decisions.setDontReset(true);
		decisions.setSilenced(wrapper.isSilencedByDefault());
		athleteTimer.setSilenced(wrapper.isSilencedByDefault());
	}
	

	@Override
	public AbstractDisplayWrapper getWrapper() {
		return wrapper;
	}
}
