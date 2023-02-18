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

import app.owlcms.init.OwlcmsSession;

@SuppressWarnings("serial")
@Tag("attempt-board-template")
@JsModule("./components/AttemptBoard.js")
@JsModule("./components/AudioContext.js")
@Route("displays/athleteFacingAttempt")

public class AthleteFacingAttemptBoard extends AttemptBoard {

	public AthleteFacingAttemptBoard() {
		super();
		setPublicFacing(false);
	}

	@Override
	public String getPageTitle() {
		return getTranslation("AttemptAF") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public boolean isSilencedByDefault() {
		return false;
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
		decisions.setPublicFacing(false);
	}
}
