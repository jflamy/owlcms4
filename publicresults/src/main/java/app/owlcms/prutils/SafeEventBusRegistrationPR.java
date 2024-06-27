/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.prutils;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import app.owlcms.components.elements.unload.UnloadObserverPR;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface SafeEventBusRegistrationPR {

    Logger logger = (Logger) LoggerFactory.getLogger(SafeEventBusRegistrationPR.class);

    public default EventBus eventBusRegister(Component c, EventBus bus) {

        logger.setLevel(Level.INFO);
        UI ui = c.getUI().get();

        if (bus != null) {
            logger.debug("registering {} on bus {} {}", c, bus.identifier(), LoggerUtils.whereFrom());
            bus.register(c);
        }
        UnloadObserverPR unloadObserver = UnloadObserverPR.get(true);
        unloadObserver.addUnloadListener((e) -> {
            if (e.getChange().equals("beforeunload")) {
                try {
                    bus.unregister(c);
                    logger.warn("closing: unregister {} from {}", c.getClass().getSimpleName(), bus.identifier());
                } catch (Exception ex) {
                }
                UnloadObserverPR.remove();
            } else if (e.getChange().equals("visibilityHidden")) {
                logger.warn("visibilityHidden {} {}", c.getClass().getSimpleName(), System.identityHashCode(c));
            } else if (e.getChange().equals("visibilityShown")) {
                logger.warn("visibilityShown {} {}", c.getClass().getSimpleName(), System.identityHashCode(c));
            } else {
                logger.error(e.getChange());
            }
        });
        ui.add(unloadObserver);

        ui.addBeforeLeaveListener((e) -> {
            try {
                bus.unregister(c);
                logger.warn("leaving: unregister {} {}", c.getClass().getSimpleName(), System.identityHashCode(c));
            } catch (Exception ex) {
            }
        });
        ui.addDetachListener((e) -> {
            try {
                logger.warn("invalidating: invalidating session for {} {}", c.getClass().getSimpleName(), System.identityHashCode(c));
                VaadinSession vaadinSession = VaadinSession.getCurrent();
                WrappedSession httpSession = vaadinSession.getSession();
                invalidate(vaadinSession, httpSession);
                bus.unregister(c);
                logger.warn("detaching: unregister {} {}", c.getClass().getSimpleName(), System.identityHashCode(c));
            } catch (Exception ex) {
            }
        });
        return bus;
    }

    public default void invalidate(VaadinSession vaadinSession, WrappedSession httpSession) {
        httpSession.invalidate();
        vaadinSession.close();
    }

}