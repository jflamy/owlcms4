/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

// @formatter:off
public interface SafeEventBusRegistration {

	Logger logger = (Logger) LoggerFactory.getLogger(SafeEventBusRegistration.class);

	public default EventBus uiEventBusRegister(Component c, FieldOfPlay fop) {

		{logger.setLevel(Level.INFO);}
		UI ui = null;
		if (c.getUI().isPresent()) {
		    ui = c.getUI().get();
		}

		EventBus uiEventBus = fop.getUiEventBus();
		OwlcmsSession.setFop(fop);
		uiEventBus.register(c);
	    //logger.debug("==== registering {} on bus {} {}",c, uiEventBus.identifier(), LoggerUtils.whereFrom());

		if (ui == null) {
			logger.error("No UI found for component {}", c.getClass().getName());
			return uiEventBus;
		}
		ui.addBeforeLeaveListener((e) -> {
			//logger.debug("leaving: unregister {} from {}", c, uiEventBus.identifier());
			unregister(c, uiEventBus);
		});
		ui.addDetachListener((e) -> {
			//logger.debug("detaching: unregister {} from {}", c, uiEventBus.identifier());
			unregister(c, uiEventBus);
		});
		return uiEventBus;
	}

    public default void unregister(Component c, EventBus uiEventBus) {
        logger.trace("explicit: unregister {} from {}", c, uiEventBus.identifier());
        try {uiEventBus.unregister(c);} catch (Exception ex) {}
    }

}