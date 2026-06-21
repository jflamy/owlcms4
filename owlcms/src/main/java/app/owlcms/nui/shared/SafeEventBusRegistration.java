/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import app.owlcms.fieldofplay.FieldOfPlay;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

// @formatter:off
public interface SafeEventBusRegistration {

	Map<Object, Set<EventBus>> BUS_REGISTRY = Collections.synchronizedMap(new WeakHashMap<>());

	Logger logger = (Logger) LoggerFactory.getLogger(SafeEventBusRegistration.class);

	public default EventBus uiEventBusRegister(Component c, FieldOfPlay fop) {

		{logger.setLevel(Level.INFO);}
		if (fop == null) {
			logger.error("uiEventBusRegister called with null FOP for component {}", c.getClass().getName());
			return null;
		}

		UI ui = c.getUI().orElse(null);
		EventBus uiEventBus = fop.getUiEventBus();

		synchronized (BUS_REGISTRY) {
			Set<EventBus> registeredBuses = BUS_REGISTRY.get(c);
			if (registeredBuses != null && registeredBuses.contains(uiEventBus)) {
				return uiEventBus; // already on the correct bus
			}
		}

		try {
			uiEventBus.unregister(c);
		} catch (Exception ignored) {
		}
		try {
			// trace the registration
			logger.debug("automatic: register {} class={} from {}", Integer.toHexString(System.identityHashCode(c)), c.getClass().getSimpleName(), uiEventBus.identifier());
			uiEventBus.register(c);
			synchronized (BUS_REGISTRY) {
				BUS_REGISTRY.computeIfAbsent(c, key -> new HashSet<>()).add(uiEventBus);
			}
		} catch (Exception ex) {
			logger.error("Failed to register component {} on UI bus {}", c.getClass().getName(), uiEventBus.identifier(), ex);
		}

		// Do NOT overwrite session FOP here to avoid cross-FOP contamination
		// OwlcmsSession.setFop(fop);

		if (ui == null) {
			logger.error("No UI found for component {}", c.getClass().getName());
			return uiEventBus;
		}
		ui.addBeforeLeaveListener((e) -> {
			unregister(c, uiEventBus);
		});
		ui.addDetachListener((e) -> {
			// trace the unregistration
			logger.debug("automatic: unregister {} class={} from {}", Integer.toHexString(System.identityHashCode(c)), c.getClass().getSimpleName(), uiEventBus.identifier());
			unregister(c, uiEventBus);
		});
		return uiEventBus;
	}

	public default EventBus uiEventBusRegisterNoUI(Object subscriber, FieldOfPlay fop) {
		{logger.setLevel(Level.INFO);}
		if (fop == null) {
			logger.error("uiEventBusRegister called with null FOP for subscriber {}", subscriber);
			return null;
		}
		EventBus uiEventBus = fop.getUiEventBus();
		synchronized (BUS_REGISTRY) {
			Set<EventBus> registeredBuses = BUS_REGISTRY.get(subscriber);
			if (registeredBuses != null && registeredBuses.contains(uiEventBus)) {
				return uiEventBus;
			}
		}
		try {
			uiEventBus.unregister(subscriber);
		} catch (Exception ignored) {
		}
		try {
			uiEventBus.register(subscriber);
			synchronized (BUS_REGISTRY) {
				BUS_REGISTRY.computeIfAbsent(subscriber, key -> new HashSet<>()).add(uiEventBus);
			}
		} catch (Exception ex) {
			logger.error("Failed to register subscriber {} on UI bus {}", subscriber, uiEventBus.identifier(), ex);
		}

		return uiEventBus;
	}

    public default void unregister(Object subscriber, EventBus uiEventBus) {
		if (uiEventBus == null) {
			return;
		}
		logger.trace("explicit: unregister {} from {}", subscriber, uiEventBus.identifier());
		try {uiEventBus.unregister(subscriber);} catch (Exception ex) {}
		synchronized (BUS_REGISTRY) {
			Set<EventBus> registeredBuses = BUS_REGISTRY.get(subscriber);
			if (registeredBuses == null) {
				return;
			}
			registeredBuses.remove(uiEventBus);
			if (registeredBuses.isEmpty()) {
				BUS_REGISTRY.remove(subscriber);
			}
		}
    }

}