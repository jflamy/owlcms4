/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.group;

import java.util.Optional;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;

import app.owlcms.fieldofplay.UIEvent;

public interface UIEventProcessor {
	
	/**
	 * Access the ui safely
	 * 
	 * Do nothing if the event originates from ourselves -- if we stop the clock on the timekeeper device, there is no need to
	 * obey the command to stop the clock on all the other devices, since we are the cause of that event.
	 * 
	 * @param attachedComponent the component we are updating (any of them if several)
	 * @param uiEventBus the bus on which we are listening
	 * @param e the event we received
	 * @param selfOrigin our reference element -- for composite objects, we will likely use the parent of the hierarchy
	 * @param eventOrigin the element on which the action that triggered the event chain occurred.
	 * @param command
	 */
	public static void uiAccess(Component attachedComponent, EventBus uiEventBus, UIEvent e, Object selfOrigin, Object eventOrigin, Command command) {
		Optional<UI> attachedUI = attachedComponent.getUI();
		// for locking purposes, we want the UI associated with the component.
		if (attachedUI.isPresent()) {
			try {
				if (eventOrigin != null && eventOrigin.equals(selfOrigin)) return;
				attachedUI.get().access(command);
			} catch (UIDetachedException e1) {
				if (uiEventBus != null) uiEventBus.unregister(attachedComponent);
			}
		} else if (uiEventBus != null) {
			uiEventBus.unregister(attachedComponent);
		}
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
		UIEventProcessor.uiAccess(attachedComponent, uiEventBus, e, 1, (e != null ? e.getOrigin() : null), command);
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
		// The use of different numbers as selfOrigin and eventOrigin implies that we always execute the command.
		UIEventProcessor.uiAccess(attachedComponent, uiEventBus, null, 1, 2, command);
	}

}