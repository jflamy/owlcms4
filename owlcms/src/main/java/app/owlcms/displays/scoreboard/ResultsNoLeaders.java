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

import app.owlcms.nui.displays.scoreboards.ResultsNoLeadersPage;
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
	
	public ResultsNoLeaders(ResultsNoLeadersPage page) {
		this.setWrapper(page);
		logger.warn("-----1 {} {}",page,getWrapper());
		getWrapper().setBoard(this);
		logger.warn("-----2 {} {}",this, getWrapper().getBoard());
		getWrapper().setDarkMode(true);
		logger.warn("-----3 {} {}",page.isDarkMode(), getWrapper().isDarkMode());
		getTimer().setOrigin(this);

	}

}