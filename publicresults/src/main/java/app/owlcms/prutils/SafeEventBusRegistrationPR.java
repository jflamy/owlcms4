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
            logger.warn("registering {} {} on bus {} {}", c.getClass().getSimpleName(), System.identityHashCode(c),
                    bus.identifier(), LoggerUtils.whereFrom());
            bus.register(c);
        }
        
        UnloadObserverPR eventObserver = UnloadObserverPR.get(true);
        eventObserver.addEventListener((e) -> {
            String change = e.getChange();
            if (change.equals("beforeunload") || change.equals("blur")) {
                try {
                    unregister(c, bus);
                    logger.warn("{}: unregister {} from {}", change, c.getClass().getSimpleName(), bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
                UnloadObserverPR.remove();
            } else {
                logger.warn("{} {} {}", change, c.getClass().getSimpleName(), System.identityHashCode(c));
            }
        });
        
        ui.access(() -> {
            ui.add(eventObserver);

            ui.addBeforeLeaveListener((e) -> {
                try {
                    unregister(c, bus);
                    ui.access(() -> {
                        UnloadObserverPR.remove();
                        ui.removeAll();
                    });
                    logger.warn("leaving: unregistered {} {}", c.getClass().getSimpleName(),
                            System.identityHashCode(c));
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            });
            ui.addDetachListener((e) -> {
                try {
                    logger.warn("invalidating: invalidating session for {} {}", c.getClass().getSimpleName(),
                            System.identityHashCode(c));
                    ui.access(() -> {
                        UnloadObserverPR.remove();
                        ui.removeAll();
                    });
                    VaadinSession vaadinSession = VaadinSession.getCurrent();
                    WrappedSession httpSession = vaadinSession.getSession();
                    invalidate(vaadinSession, httpSession);
                    unregister(c, bus);
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            });
        });
        return bus;
    }

    public default void unregister(Component c, EventBus bus) {
        try {
            bus.unregister(c);
        } catch (IllegalArgumentException e) {
            logger.warn("unregister of {} {} already done.", c.getClass().getSimpleName(), System.identityHashCode(c));
        } catch (Exception e) {
            logger.warn("unregister of {} {} failed: {}", c.getClass().getSimpleName(), System.identityHashCode(c),
                    e.getClass());
        }
    }

    public default void invalidate(VaadinSession vaadinSession, WrappedSession httpSession) {
        httpSession.invalidate();
        vaadinSession.close();
    }

}