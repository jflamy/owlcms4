/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;

import app.owlcms.access.ImportLatch;
import app.owlcms.access.ImportLatch.ActiveSession;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.uievents.UIEvent;

/** Keeps import controls synchronized with platforms selecting or releasing lifting sessions. */
public final class ImportSessionWatcher {
	private final Component owner;
	private final Consumer<List<ActiveSession>> onActiveSessionsChanged;
	private final List<EventBus> buses = new ArrayList<>();

	public ImportSessionWatcher(Component owner, Consumer<List<ActiveSession>> onActiveSessionsChanged) {
		this.owner = owner;
		this.onActiveSessionsChanged = onActiveSessionsChanged;
		owner.addAttachListener(e -> register());
		owner.addDetachListener(e -> unregister());
		if (owner.isAttached()) {
			register();
		}
	}

	@Subscribe
	public void onSessionChanged(UIEvent.SwitchGroup e) {
		UI ui = this.owner.getUI().orElse(null);
		if (ui == null) {
			return;
		}
		try {
			ui.access(() -> this.onActiveSessionsChanged.accept(ImportLatch.activeSessions()));
		} catch (UIDetachedException e1) {
			unregister();
		}
	}

	private synchronized void register() {
		unregister();
		if (OwlcmsFactory.getFopByName() == null) {
			return;
		}
		for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
			EventBus bus = fop.getUiEventBus();
			SafeEventBusRegistration.registerSubscriber(this, bus);
			this.buses.add(bus);
		}
	}

	private synchronized void unregister() {
		for (EventBus bus : this.buses) {
			SafeEventBusRegistration.unregisterSubscriber(this, bus);
		}
		this.buses.clear();
	}
}
