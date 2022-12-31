/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.lifting;

import java.util.Optional;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;

import app.owlcms.uievents.UIEvent;

public interface UIEventProcessor {

    /**
     * Access the ui safely
     *
     * Do nothing if the event originates from ourselves -- if we stop the clock on the timekeeper device, there is no
     * need to obey the command to stop the clock on all the other devices, since we are the cause of that event.
     *
     * @param attachedComponent the component we are updating (any of them if several)
     * @param uiEventBus        the bus on which we are listening
     * @param e                 the event we received
     * @param selfOrigin        our reference element -- for composite objects, we will likely use the parent of the
     *                          hierarchy
     * @param command
     */
    public static void uiAccessIgnoreIfSelfOrigin(Component attachedComponent, EventBus uiEventBus, UIEvent e,
            Object selfOrigin, Command command) {

        // for locking purposes, we want the UI associated with the component (we don't know who owns
        // our thread and what UI.getCurrent() is.
        Optional<UI> attachedUI = attachedComponent.getUI();
        if (attachedUI.isPresent()) {
            try {
                Object eventOrigin = e != null ? e.getOrigin() : null;
                if (eventOrigin != null && eventOrigin.equals(selfOrigin)) {
                    return;
                }
                UI ui = attachedUI.get();
                if (ui != null) {
                    ui.access(command);
                } else {
                    // can't happen in theory, but does in practice !?
                    UI.getCurrent().access(command);
                }
            } catch (UIDetachedException e1) {
                if (uiEventBus != null) {
                    uiEventBus.unregister(attachedComponent);
                }
            }
        } else if (uiEventBus != null) {
            try {
                uiEventBus.unregister(attachedComponent);
            } catch (Exception exc) {
                // ignore
            }
        }
    }

    /**
     * Access the UI safely.
     *
     * This version does not care about events.
     *
     * @param attachedComponent the component we are updating (any of them if several)
     * @param uiEventBus
     * @param command
     */
    static void uiAccess(Component attachedComponent, EventBus uiEventBus, Command command) {
        // The use of different numbers as selfOrigin and eventOrigin implies that we
        // always execute the command.
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(attachedComponent, uiEventBus, null, 1, command);
    }

    /**
     * Access the UI safely, as a result of an event.
     *
     * The command is performed even if it results from ourself.
     *
     * @param attachedComponent the component we are updating (any of them if several)
     * @param uiEventBus
     * @param e
     * @param command
     */
    static void uiAccess(Component attachedComponent, EventBus uiEventBus, UIEvent e, Command command) {
        // The use of a number as selfOrigin means that we always execute the command.
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(attachedComponent, uiEventBus, e, 1,
                command);
    }

}