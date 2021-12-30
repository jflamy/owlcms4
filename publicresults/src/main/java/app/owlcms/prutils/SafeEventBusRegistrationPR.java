/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.prutils;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import app.owlcms.components.elements.unload.UnloadObserver;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

// @formatter:off
public interface SafeEventBusRegistrationPR {

	Logger logger = (Logger) LoggerFactory.getLogger(SafeEventBusRegistrationPR.class);

	public default EventBus eventBusRegister(Component c, EventBus bus) {

		{logger.setLevel(Level.INFO);}

		logger.debug("registering {} on bus {} {}",c, bus.identifier(), LoggerUtils.whereFrom());
		UI ui = c.getUI().get();
		bus.register(c);

        UnloadObserver unloadObserver = UnloadObserver.get(false);
        unloadObserver.addUnloadListener((e) -> {
            logger.debug("closing: unregister {} from {}", c, bus.identifier());
            try {bus.unregister(c);} catch (Exception ex) {}
            UnloadObserver.remove();
        });
        ui.add(unloadObserver);

		ui.addBeforeLeaveListener((e) -> {
			logger.debug("leaving: unregister {} from {}", c, bus.identifier());
			try {bus.unregister(c);} catch (Exception ex) {}
		});
		ui.addDetachListener((e) -> {
			logger.debug("detaching: unregister {} from {}", c, bus.identifier());
			try {bus.unregister(c);} catch (Exception ex) {}
		});
		return bus;
	}

}