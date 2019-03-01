package org.ledocte.owlcms.ui.lifting;

import java.util.Optional;

import org.ledocte.owlcms.state.UIEvent;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.server.Command;

public interface UIEventProcessor {

	/* (non-Javadoc)
	 * @see org.ledocte.owlcms.ui.lifting.UIEventProcessor#updateGrid(org.ledocte.owlcms.state.UIEvent.LiftingOrderUpdated)
	 */
	// @Subscribe		implementing class must add this annotation
	public static void uiAccess(Component c, EventBus b, UIEvent e, Command r) {
		Optional<UI> ui2 = c.getUI();
		if (ui2.isPresent()) {
			try {
				ui2.get().access(r);
			} catch (UIDetachedException e1) {
				if (b != null) b.unregister(c);
			}
		} else {
			if (b != null) b.unregister(c);
		}
	}

}