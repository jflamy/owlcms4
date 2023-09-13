/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.displays.attemptboards.AbstractAttemptBoardPage;
import app.owlcms.nui.displays.attemptboards.AthleteFacingDecisionBoardPage;

@SuppressWarnings("serial")
@Tag("decision-board-template")
@JsModule("./components/DecisionBoard.js")
@JsModule("./components/AudioContext.js")

public class AthleteFacingDecisionBoard extends AbstractAttemptBoard {

	private AbstractAttemptBoardPage wrapper;

	public AthleteFacingDecisionBoard(AthleteFacingDecisionBoardPage page) {
		super();
		decisions.setPublicFacing(false);
		setPublicFacing(false);
		setShowBarbell(false);
		decisions.setDontReset(false);
		setSilenced(false);
		setDownSilenced(false);
		this.wrapper = page;
		this.wrapper.setBoard(this);
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Decision_AF_") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected void checkImages() {
		athletePictures = false;
		teamFlags = false;
	}
	
	@Override
	public AbstractDisplayPage getWrapper() {
		return wrapper;
	}



}
