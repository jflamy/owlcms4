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
            if (change.equals("beforeunload")) {
                // actual killing is handled by the detach listener
                try {
                    unregister(c, bus);
                    logger.warn("{}: unregister {} from {}", change, c.getClass().getSimpleName(), bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
                UnloadObserverPR.remove();
            } else if (change.equals("visibilityHidden")) {
                // switching tabs or minimizing window. no visible scoreboard
                setInactivityTimer();
                try {
                    logger.warn("{}: setInactivityTimer {} from {}", change, c.getClass().getSimpleName(), bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            } else if (change.equals("blur")) {
                // blurring can occur when switching the active window and still watching.
                // we probably don't want to do anything
                try {
                    logger.warn("{}: do nothing from {}", change, c.getClass().getSimpleName(), bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            } else if (change.equals("visibilityShown")) {
                cancelInactivityTimer();
                try {
                    logger.warn("{}: cancelInactivityTimer {} from {}", change, c.getClass().getSimpleName(), bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            } else if (change.equals("focus")) {
                cancelInactivityTimer();
                try {
                    logger.warn("{}: cancelInactivityTimer {} from {}", change, c.getClass().getSimpleName(), bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            } else {
                logger.error("{}: unexpected event {} {}", change, c.getClass().getSimpleName(), System.identityHashCode(c));
            }
        });

        ui.access(() -> {
            ui.add(eventObserver);

            ui.addBeforeLeaveListener((e) -> {
                // navigating via a link, don't kill session, clean-up this page.
                // should only happen on the main page.
                try {
                    unregister(c, bus);
                    ui.access(() -> {
                        UnloadObserverPR.remove();
                        ui.removeAll();
                    });
                    logger.warn("leaving: unregistering {} {}", c.getClass().getSimpleName(),
                            System.identityHashCode(c));
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            });
            ui.addDetachListener((e) -> {
                // actually leaving the page. kill publicresults session.
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

    public void cancelInactivityTimer();

    public void setInactivityTimer();

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