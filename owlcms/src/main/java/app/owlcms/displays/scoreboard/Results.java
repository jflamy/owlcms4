/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.template.Id;

import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class Results
 *
 * Show results scoreboard for a session, including records and leaders
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class Results extends BaseResults {
	
	protected final Logger logger = (Logger) LoggerFactory.getLogger(Results.class);

	@Id("breakTimer")
	private BreakTimerElement breakTimer; // WebComponent, injected by Vaadin
	@Id("decisions")
	private DecisionElement decisions; // WebComponent, injected by Vaadin
	@Id("timer")
	private AthleteTimerElement timer; // WebComponent, injected by Vaadin
	private final Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

	public Results() {
		this.uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
		this.getElement().setProperty("scoreboardType", this.getClass().getSimpleName());
		overrideColors(this.getElement());
	}
}