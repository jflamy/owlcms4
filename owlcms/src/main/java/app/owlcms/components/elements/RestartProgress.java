/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import app.owlcms.Main;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

@SuppressWarnings({ "serial", "deprecation" })
@Tag("restart-progress")
@JsModule("./components/RestartProgress.js")
public class RestartProgress extends LitTemplate {

	private static final long RESTART_WAIT_MILLIS = 10_000L;

	public RestartProgress(String message) {
		getElement().setProperty("phase", "importing");
		setMessage(message);
	}

	public void beginRestart(String message, long startDelayMillis) {
		setMessage(message);
		long startupDuration = Main.getStartupDurationMillis();
		getElement().setProperty("expectedDurationMillis", RESTART_WAIT_MILLIS + startupDuration);
		getElement().setProperty("startDelayMillis", startDelayMillis);
		getElement().setProperty("phase", "restarting");
	}

	private void setMessage(String message) {
		getElement().setProperty("message", message);
	}

}
