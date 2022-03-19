/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import app.owlcms.components.elements.unload.UnloadObserver;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

// @formatter:off
public interface SafeEventBusRegistration {

	Logger logger = (Logger) LoggerFactory.getLogger(SafeEventBusRegistration.class);


//	public default EventBus fopEventBusRegister(Component c, FieldOfPlay fop) {
//
//		{logger.setLevel(Level.INFO);}
//
//		UI ui = c.getUI().get();
//		EventBus fopEventBus = fop.getFopEventBus();
//		fopEventBus.register(c);
//
//        UnloadObserver unloadObserver = UnloadObserver.get(false);
//        unloadObserver.addUnloadListener((e) -> {
//            logger.debug("closing {}: unregister {} from {}", e.getSource(), c, fopEventBus.identifier());
//            try {fopEventBus.unregister(c);} catch (Exception ex) {}
//          });
//		ui.addBeforeLeaveListener((e) -> {
//			logger.debug("leaving {}: unregister {} from {}", e.getSource(), c, fopEventBus.identifier());
//			try {fopEventBus.unregister(c);} catch (Exception ex) {}
//		});
//		ui.addDetachListener((e) -> {
//			logger.debug("{} detaching: unregister {} from {}", e, c, fopEventBus.identifier());
//			try {fopEventBus.unregister(c);} catch (Exception ex) {}
//		});
//		return fopEventBus;
//	}

	public default EventBus uiEventBusRegister(Component c, FieldOfPlay fop) {

		{logger.setLevel(Level.INFO);}
		UI ui = null;
		if (c.getUI().isPresent()) {
		    ui = c.getUI().get();
		}
//		else {
//		    ui = UI.getCurrent();
//		}
		EventBus uiEventBus = fop.getUiEventBus();
		uiEventBus.register(c);
	    logger.trace("registering {} on bus {} {}",c, uiEventBus.identifier(), LoggerUtils.whereFrom());

        UnloadObserver unloadObserver = UnloadObserver.get(false);
        unloadObserver.addUnloadListener((e) -> {
            logger.trace("closing: unregister {} from {}", c, uiEventBus.identifier());
            unregister(c, uiEventBus);
            UnloadObserver.remove();
        });
        ui.add(unloadObserver);

		ui.addBeforeLeaveListener((e) -> {
			logger.trace("leaving: unregister {} from {}", c, uiEventBus.identifier());
			unregister(c, uiEventBus);
		});
		ui.addDetachListener((e) -> {
			logger.debug("detaching: unregister {} from {}", c, uiEventBus.identifier());
			unregister(c, uiEventBus);
		});
		return uiEventBus;
	}


    public default void unregister(Component c, EventBus uiEventBus) {
        logger.trace("explicit: unregister {} from {}", c, uiEventBus.identifier());
        try {uiEventBus.unregister(c);} catch (Exception ex) {}
    }

}