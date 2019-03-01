package org.ledocte.owlcms.ui.lifting;

import java.util.Optional;

import org.ledocte.owlcms.state.UIEvent;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;

public interface UIEventProcessor {
	
	/**
	 * Access the ui safely
	 * 
	 * Do nothing if the event originates from ourselves
	 * 
	 * @param attachedComponent
	 * @param uiEventBus
	 * @param e
	 * @param originatingUI
	 * @param command
	 */
	public static void uiAccess(Component attachedComponent, EventBus uiEventBus, UIEvent e, UI originatingUI, Command command) {
		Optional<UI> ui2 = attachedComponent.getUI();
		if (ui2.isPresent()) {
			try {
				UI ui = ui2.get();
				if (ui == originatingUI) return;
				ui.access(command);
			} catch (UIDetachedException e1) {
				if (uiEventBus != null) uiEventBus.unregister(attachedComponent);
			}
		} else {
			if (uiEventBus != null) uiEventBus.unregister(attachedComponent);
		}
	}

	static void uiAccess(Component attachedComponent, EventBus uiEventBus, UIEvent e, Command command) {
		UIEventProcessor.uiAccess(attachedComponent, uiEventBus, e, null, command);
	}
	
	static void uiAccess(Component attachedComponent, EventBus uiEventBus, Command command) {
		UIEventProcessor.uiAccess(attachedComponent, uiEventBus, null, null, command);
	}

}