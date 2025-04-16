/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

import app.owlcms.nui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Logger;

/**
 * ExplicitDecision display element.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("css-ticker")
@JsModule("./components/Ticker.js")
public class TickerElement extends LitTemplate
        implements SafeEventBusRegistration {

	public void setText(String text) {
		Logger logger = (Logger) LoggerFactory.getLogger(TickerElement.class);
		logger.debug("calling beep");
		getElement().setProperty("text", text);
	}

}
