package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.state.FieldOfPlayState;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.UI;

public interface UIEventListener {

	public default EventBus listenToUIEvents(FieldOfPlayState fop) {
		EventBus uiEventBus = fop.getUiEventBus();
		uiEventBus
			.register(this);
		UI current = UI.getCurrent();
		current
			.addBeforeLeaveListener((e) -> {
				uiEventBus.unregister(this);
			});
		current.addDetachListener((e) -> {
				uiEventBus.unregister(this);
			});
		return uiEventBus;
	}

}