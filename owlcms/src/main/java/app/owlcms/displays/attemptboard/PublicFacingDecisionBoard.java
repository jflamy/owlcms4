/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

import app.owlcms.data.config.Config;
import app.owlcms.init.OwlcmsSession;

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
		checkVideo(Config.getCurrent().getStylesDirectory()+"/video/decisionboard.css", routeParameter, this);
		decisions.setPublicFacing(true);
		setPublicFacing(true);
		setShowBarbell(false);
		setSilenced(isSilencedByDefault());
		

	}

}
