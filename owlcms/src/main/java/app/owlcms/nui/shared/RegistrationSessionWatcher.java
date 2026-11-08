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

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.uievents.UIEvent;

/**
 * Closes a full registration form when the athlete's session becomes in progress while the form is open. The session
 * starts with the first athlete clock (a StartTime event) or when a session that already has completed attempts is
 * selected (a SwitchGroup event); both are posted on the platform's UI event bus after the platform has recorded the
 * start. Introductions and countdowns post neither, so they do not close the form, and neither does any other session.
 * <p>
 * The form is never saved or converted: its unsaved changes are discarded, the user is told, and reopening the athlete
 * offers restricted athlete information (and an independent Card on Supervisor/results). Full registration saves
 * still check the session, since this notification is asynchronous.
 */
final class RegistrationSessionWatcher {
	private final Component owner;
	private final Group storedGroup;
	private final Consumer<FieldOfPlay> onStarted;
	private final List<EventBus> buses = new ArrayList<>();
	private boolean closed;

	/**
	 * @param owner       the form; the watcher listens while it is attached
	 * @param storedGroup the athlete's session when the form was opened
	 * @param onStarted   runs on the UI thread, once, with the platform where the session is in progress
	 */
	RegistrationSessionWatcher(Component owner, Group storedGroup, Consumer<FieldOfPlay> onStarted) {
		this.owner = owner;
		this.storedGroup = storedGroup;
		this.onStarted = onStarted;
		owner.addAttachListener(e -> register());
		owner.addDetachListener(e -> unregister());
	}

	@Subscribe
	public void onClockStarted(UIEvent.StartTime e) {
		check();
	}

	@Subscribe
	public void onSessionSelected(UIEvent.SwitchGroup e) {
		check();
	}

	private synchronized void register() {
		unregister();
		this.closed = false;
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

	private void check() {
		FieldOfPlay fop = OwlcmsFactory.getFOPSessionInProgress(this.storedGroup);
		if (fop == null) {
			return;
		}
		synchronized (this) {
			if (this.closed) {
				return;
			}
			this.closed = true;
		}
		try {
			UI ui = this.owner.getUI().orElse(null);
			if (ui != null) {
				ui.access(() -> this.onStarted.accept(fop));
			}
		} catch (UIDetachedException e) {
			unregister();
		}
	}
}
