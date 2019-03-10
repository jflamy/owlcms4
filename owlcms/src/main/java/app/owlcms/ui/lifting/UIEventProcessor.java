package app.owlcms.ui.lifting;

import java.util.Optional;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;

import app.owlcms.state.UIEvent;

public interface UIEventProcessor {
	
	/**
	 * Access the ui safely
	 * 
	 * Do nothing if the event originates from ourselves -- if we stop the clock on the timekeeper device, there is no need to
	 * obey the command to stop the clock on all the devices that will be given as a result.  The event for updating the current
	 * lifter will be separate.
	 * 
	 * @param attachedComponent the component we are updating (any of them if several)
	 * @param uiEventBus the bus on which we are listening
	 * @param e the event we received
	 * @param originatingUI the UI on which the action that triggered the event chain occurred.
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