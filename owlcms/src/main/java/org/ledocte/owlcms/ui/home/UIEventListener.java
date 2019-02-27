package org.ledocte.owlcms.ui.home;

import org.ledocte.owlcms.state.FieldOfPlayState;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.UI;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

// @formatter:off
public interface UIEventListener {
	
	Logger logger = (Logger) LoggerFactory.getLogger(UIEventListener.class);


	public default EventBus uiEventRegister(UI current, FieldOfPlayState fop) {
		
		{logger.setLevel(Level.DEBUG);}
		
		EventBus uiEventBus = fop.getUiEventBus();
		uiEventBus.register(this);
		current.addBeforeLeaveListener((e) -> {
			logger.debug("leaving : unregister {} from {}", e.getSource(), uiEventBus.identifier());
			try {uiEventBus.unregister(this);} catch (Exception ex) {}
		});
		current.addDetachListener((e) -> {
			logger.debug("detaching: unregister {} from {}", e.getSource(), uiEventBus.identifier());
			try {uiEventBus.unregister(this);} catch (Exception ex) {}
		});
		return uiEventBus;
	}

}