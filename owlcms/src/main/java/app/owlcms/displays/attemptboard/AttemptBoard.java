/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;

@SuppressWarnings({ "serial", "deprecation" })
@Tag("attempt-board-template")
@JsModule("./components/AttemptBoard.js")
@JsModule("./components/AudioContext.js")
@JsModule("./components/TimerElement.js")
@JsModule("./components/DecisionElement.js")
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/plates.css")

public class AttemptBoard extends AbstractAttemptBoard {

	public AttemptBoard() {
		super();
	}
}
