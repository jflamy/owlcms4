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

import app.owlcms.components.elements.DecisionElementPR;
import app.owlcms.components.elements.TimerElementPR;
import app.owlcms.components.elements.unload.UnloadObserverPR;
import app.owlcms.displays.scoreboard.ResultsPR;
import app.owlcms.publicresults.MainView;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public interface SafeEventBusRegistrationPR {

    public default UnloadObserverPR getEventObserver() {
        return null;
    }

    public default void setEventObserver(UnloadObserverPR uo) {
        return;
    }

    public static final int INACTIVITY_DELAY = 15 * 1000;
    Logger logger = (Logger) LoggerFactory.getLogger(SafeEventBusRegistrationPR.class);

    /**
     * @param c
     * @param bus
     * @return
     */
    public default EventBus eventBusRegister(Component c, EventBus bus) {

        UI ui = c.getUI().get();

        if (bus != null) {
            logger.debug("registering {} {} on bus {} {}", c.getClass().getSimpleName(), System.identityHashCode(c),
                    bus.identifier(), LoggerUtils.whereFrom());
            bus.register(c);
        }

        UnloadObserverPR eventObserver = new UnloadObserverPR(false, ui, c);
        setEventObserver(eventObserver);
        eventObserver.setActivityTime();

        // If needed, create the repeating task to cleanup things;
        // one singleton per session.
        if (c instanceof MainView || c instanceof ResultsPR) {
            // the reload form from these pages will be used.
            SessionCleanup.create();
        }

        eventObserver.addEventListener((e) -> {
            String change = e.getChange();

            if (!(this instanceof MainView)) {
                if (change.equals("beforeunload")) {
                    // actual killing is handled by the detach listener
                    try {
                        eventObserver.setInactivityTime();
                        unregister(c, bus);
                        logger.debug("{}: unregister {} from {}", change, c.getClass().getSimpleName(),
                                bus.identifier());
                    } catch (Exception ex) {
                        LoggerUtils.logError(logger, ex, true);
                    }
                    return;
                }
            }

            if (this instanceof TimerElementPR || this instanceof DecisionElementPR) {
                return;
            }

            if (change.equals("visibilityHidden") || change.equals("pageHide") || change.equals("unload")) {
                // switching tabs or minimizing window. no visible scoreboard
                eventObserver.setInactivityTime();
                try {
                    logger.debug("{}: setInactivityTime {} from {}", change, c.getClass().getSimpleName(),
                            bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            }

            else if (change.equals("blur")) {
//                // blurring can occur when switching the active window and still watching.
//                // we probably don't want to do anything
//                try {
//                    logger.debug("{}: do nothing from {}", change, c.getClass().getSimpleName(), bus.identifier());
//                } catch (Exception ex) {
//                    LoggerUtils.logError(logger, ex, true);
//                }
            }

            else if (change.equals("visibilityShown")) {
                eventObserver.setActivityTime();
                try {
                    logger.debug("{}: resetInactivityTime {} from {}", change, c.getClass().getSimpleName(),
                            bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            }

            else if (change.equals("focus")) {
                // visibility changes seem to be sufficient
//                eventObserver.resetInactivityTime(ui, c);
//                try {
//                    logger.debug("{}: resetInactivityTime {} from {}", change, c.getClass().getSimpleName(),
//                            bus.identifier());
//                } catch (Exception ex) {
//                    LoggerUtils.logError(logger, ex, true);
//                }
            }

            else {
                logger.error("{}: unexpected event {} {}", change, c.getClass().getSimpleName(),
                        System.identityHashCode(c));
                // actual killing is handled by the detach listener
                try {
                    eventObserver.setInactivityTime();
                    unregister(c, bus);
                    logger.debug("{}: unregister {} from {}", change, c.getClass().getSimpleName(),
                            bus.identifier());
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            }
        });

        ui.access(() -> {
//            logger.trace("adding eventObserver {} {}", System.identityHashCode(eventObserver),
//                    System.identityHashCode(eventObserver.getElement()));
            ui.add(eventObserver);
            ui.addBeforeLeaveListener((e) -> {
                // navigating via a link, don't kill session, clean-up this page.
                // should only happen on the main page.
                try {
                    eventObserver.setGoneTime();
                    unregister(c, bus);
                    ui.access(() -> {
                        // UnloadObserverPR.remove(ui);
                        ui.removeAll();
                    });
                    logger.debug("leaving: unregistering {} {}", c.getClass().getSimpleName(),
                            System.identityHashCode(c));
                } catch (Exception ex) {
                    LoggerUtils.logError(logger, ex, true);
                }
            });

            ui.addDetachListener((e) -> {
                // actually left the page
                eventObserver.setGoneTime();
            });
        });
        return bus;
    }

    public default void unregister(Component c, EventBus bus) {
        try {
            bus.unregister(c);
        } catch (IllegalArgumentException e) {
            logger.debug("unregister of {} {} already done.", c.getClass().getSimpleName(), System.identityHashCode(c));
        } catch (Exception e) {
            logger.debug("unregister of {} {} failed: {}", c.getClass().getSimpleName(), System.identityHashCode(c),
                    e.getClass());
        }
    }

    public default void invalidate(VaadinSession vaadinSession, WrappedSession httpSession) {
        if (httpSession != null) {
            httpSession.invalidate();
        }
        if (vaadinSession != null) {
            vaadinSession.close();
        }
    }

}