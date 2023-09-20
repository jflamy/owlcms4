/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;
import ch.qos.logback.classic.Logger;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings("serial")
@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class ResultsNoLeaders extends Results {

	Logger logger = (Logger) LoggerFactory.getLogger(ResultsNoLeaders.class);

	public ResultsNoLeaders(WarmupNoLeadersPage page) {
		super();
	}

}