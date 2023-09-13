/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.attemptboards.AbstractAttemptBoardPage;
import app.owlcms.nui.displays.attemptboards.AthleteFacingDecisionBoardPage;

/**
 * Java API for LitElement display for countdown and decisions
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
@Tag("decision-board-template")
@JsModule("./components/AttemptBoard.js")
@JsModule("./components/AudioContext.js")
@JsModule("./components/TimerElement.js")
@JsModule("./components/DecisionElement.js")
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/plates.css")

public class AthleteFacingDecisionBoard extends AbstractAttemptBoard {

	private AbstractAttemptBoardPage wrapper;

	public AthleteFacingDecisionBoard(AthleteFacingDecisionBoardPage athleteFacingDecisionBoardWrapper) {
		super();
		setPublicFacing(false);
		setShowBarbell(false);
		setSilenced(false);
		setDownSilenced(false);
		breakTimer.setParent("DecisionBoard");
		this.setWrapper(athleteFacingDecisionBoardWrapper);
		wrapper.setBoard(this);
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Decision_AF_") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public AbstractAttemptBoardPage getWrapper() {
		return this.wrapper;
	}

	@Override
	public boolean isPublicFacing() {
		return isPublicFacing();
	}

	public void setWrapper(AbstractAttemptBoardPage wrapper) {
		this.wrapper = wrapper;
	}

	@Override
	protected void checkImages() {
		athletePictures = false;
		teamFlags = false;
	}


	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		decisions.setPublicFacing(false);
	}
}
