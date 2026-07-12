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

import app.owlcms.components.elements.TimerElement;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

// @formatter:off
public interface SafeEventBusRegistration {

	Map<Object, Set<EventBus>> BUS_REGISTRY = Collections.synchronizedMap(new WeakHashMap<>());

	Logger logger = (Logger) LoggerFactory.getLogger(SafeEventBusRegistration.class);

	public static int registeredSubscriberCount(EventBus uiEventBus, String simpleClassName) {
		if (uiEventBus == null || simpleClassName == null) {
			return 0;
		}
		synchronized (BUS_REGISTRY) {
			int count = 0;
			for (Map.Entry<Object, Set<EventBus>> entry : BUS_REGISTRY.entrySet()) {
				Object subscriber = entry.getKey();
				Set<EventBus> registeredBuses = entry.getValue();
				if (subscriber != null && simpleClassName.equals(subscriber.getClass().getSimpleName())
				        && registeredBuses != null && registeredBuses.contains(uiEventBus)) {
					count++;
				}
			}
			return count;
		}
	}

	public static int registeredControlAthleteTimerCount(EventBus uiEventBus) {
		if (uiEventBus == null) {
			return 0;
		}
		synchronized (BUS_REGISTRY) {
			int count = 0;
			for (Map.Entry<Object, Set<EventBus>> entry : BUS_REGISTRY.entrySet()) {
				Object subscriber = entry.getKey();
				Set<EventBus> registeredBuses = entry.getValue();
				if (subscriber instanceof TimerElement timerElement && timerElement.isAthleteTimerOnControlPageForDiagnostics()
				        && registeredBuses != null && registeredBuses.contains(uiEventBus)) {
					count++;
				}
			}
			return count;
		}
	}

	public static String registeredTimerDetails(EventBus uiEventBus) {
		if (uiEventBus == null) {
			return "[]";
		}
		synchronized (BUS_REGISTRY) {
			StringBuilder details = new StringBuilder("[");
			for (Map.Entry<Object, Set<EventBus>> entry : BUS_REGISTRY.entrySet()) {
				Object subscriber = entry.getKey();
				Set<EventBus> registeredBuses = entry.getValue();
				if (subscriber instanceof TimerElement timerElement && registeredBuses != null
				        && registeredBuses.contains(uiEventBus)) {
					if (details.length() > 1) {
						details.append(", ");
					}
					details.append(timerElement.describeTimerForDiagnostics());
				}
			}
			details.append("]");
			return details.toString();
		}
	}

	public static void unregisterSubscriber(Object subscriber, EventBus uiEventBus) {
		if (uiEventBus == null) {
			return;
		}
		boolean removedFromRegistry = false;
		try {
			uiEventBus.unregister(subscriber);
		} catch (Exception ex) {
		}
		synchronized (BUS_REGISTRY) {
			Set<EventBus> registeredBuses = BUS_REGISTRY.get(subscriber);
			if (registeredBuses != null) {
				removedFromRegistry = registeredBuses.remove(uiEventBus);
				if (registeredBuses.isEmpty()) {
					BUS_REGISTRY.remove(subscriber);
				}
			}
		}
		if (Config.getCurrent().featureSwitch(FeatureSwitch.CLOCK_TRACES)) {
			logger.warn("uiBus OUT subscriber={} class={} bus={} registryHad={} {}",
			        Integer.toHexString(System.identityHashCode(subscriber)),
			        subscriber != null ? subscriber.getClass().getSimpleName() : "null", uiEventBus.identifier(),
			        removedFromRegistry, LoggerUtils.whereFrom());
		}
	}

	public static void registerSubscriber(Object subscriber, EventBus uiEventBus) {
		if (uiEventBus == null) {
			return;
		}
		try {
			uiEventBus.register(subscriber);
			synchronized (BUS_REGISTRY) {
				BUS_REGISTRY.computeIfAbsent(subscriber, key -> new HashSet<>()).add(uiEventBus);
			}
			if (Config.getCurrent().featureSwitch(FeatureSwitch.CLOCK_TRACES)) {
				logger.warn("uiBus IN subscriber={} class={} bus={} {}",
				        Integer.toHexString(System.identityHashCode(subscriber)),
				        subscriber != null ? subscriber.getClass().getSimpleName() : "null", uiEventBus.identifier(),
				        LoggerUtils.whereFrom());
			}
		} catch (Exception ex) {
			logger.error("Failed to register subscriber {} on UI bus {}", subscriber, uiEventBus.identifier(), ex);
		}
	}

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
				// TEMPORARY (timer-gap) trace: confirm the control timer stays registered.
				if (Config.getCurrent().featureSwitch(FeatureSwitch.CLOCK_TRACES) && "AthleteTimerElement".equals(c.getClass().getSimpleName())) {
				logger.warn("uiBus already-registered {} bus={} {}",
					        Integer.toHexString(System.identityHashCode(c)), uiEventBus.identifier(),
					        LoggerUtils.whereFrom());
				}
				return uiEventBus; // already on the correct bus
			}
		}

		// TEMPORARY (timer-gap) trace: a fresh register here means the component was OFF the bus.
		if (Config.getCurrent().featureSwitch(FeatureSwitch.CLOCK_TRACES) && "AthleteTimerElement".equals(c.getClass().getSimpleName())) {
			logger.warn("uiBus FRESH-register {} bus={} {}",
			        Integer.toHexString(System.identityHashCode(c)), uiEventBus.identifier(),
			        LoggerUtils.whereFrom());
		}

		unregisterSubscriber(c, uiEventBus);
		registerSubscriber(c, uiEventBus);

		// Do NOT overwrite session FOP here to avoid cross-FOP contamination
		// OwlcmsSession.setFop(fop);

		if (ui == null) {
			logger.error("No UI found for component {}", c.getClass().getName());
			return uiEventBus;
		}
		ui.addBeforeLeaveListener((e) -> {
			// TEMPORARY (timer-gap) trace
			if (Config.getCurrent().featureSwitch(FeatureSwitch.CLOCK_TRACES) && "AthleteTimerElement".equals(c.getClass().getSimpleName())) {
				logger.warn("uiBus beforeLeave-unregister {} bus={}",
				        Integer.toHexString(System.identityHashCode(c)), uiEventBus.identifier());
			}
			unregister(c, uiEventBus);
		});
		ui.addDetachListener((e) -> {
			// trace the unregistration
			logger.debug("automatic: unregister {} class={} from {}", Integer.toHexString(System.identityHashCode(c)), c.getClass().getSimpleName(), uiEventBus.identifier());
			// TEMPORARY (timer-gap) trace
			if (Config.getCurrent().featureSwitch(FeatureSwitch.CLOCK_TRACES) && "AthleteTimerElement".equals(c.getClass().getSimpleName())) {
				logger.warn("uiBus uiDetach-unregister {} bus={}",
				        Integer.toHexString(System.identityHashCode(c)), uiEventBus.identifier());
			}
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
		unregisterSubscriber(subscriber, uiEventBus);
		registerSubscriber(subscriber, uiEventBus);

		return uiEventBus;
	}

	public default void unregister(Object subscriber, EventBus uiEventBus) {
		if (uiEventBus == null) {
			return;
		}
		unregisterSubscriber(subscriber, uiEventBus);
	}

}