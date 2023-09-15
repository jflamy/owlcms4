/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.nui.displays.attemptboards.AbstractAttemptBoardPage;

@SuppressWarnings("serial")
@Tag("decision-board-template")
@JsModule("./components/DecisionBoard.js")
@JsModule("./components/AudioContext.js")

public class DecisionBoard extends AbstractAttemptBoard {

	private AbstractAttemptBoardPage wrapper;

	public DecisionBoard(AbstractAttemptBoardPage page) {
		super();
		this.wrapper = page;
		this.wrapper.setBoard(this);
	}

	@Override
	protected void checkImages() {
		athletePictures = false;
		teamFlags = false;
	}
	
	@Override
	public AbstractAttemptBoardPage getWrapper() {
		return wrapper;
	}
}
