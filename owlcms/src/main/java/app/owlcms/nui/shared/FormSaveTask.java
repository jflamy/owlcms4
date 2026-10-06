/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;

import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * Runs persistence outside the UI lock, preserving the audit actor and cancelling work for a closed form.
 */
public final class FormSaveTask {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(FormSaveTask.class);
	private final Component owner;
	private final AtomicBoolean active = new AtomicBoolean();
	private boolean pending;

	public FormSaveTask(Component owner) {
		this.owner = owner;
		owner.addAttachListener(e -> this.active.set(true));
		owner.addDetachListener(e -> this.active.set(false));
	}

	public boolean isActive() {
		return this.active.get();
	}

	public <T> void execute(Supplier<T> operation, Consumer<T> completed, Consumer<Exception> failed) {
		if (this.pending || !this.active.get()) {
			return;
		}
		UI ui = this.owner.getUI().orElseThrow();
		AuditActor actor = AuditContext.actor() != null ? AuditContext.actor() : AuditActor.fromCurrentUi();
		String cause = AuditContext.cause();
		this.pending = true;
		this.owner.getElement().setEnabled(false);
		Thread.ofVirtual().name("AthleteFormSave").start(() -> {
			try {
				T result = AuditContext.call(actor, cause, operation);
				ui.accessLater(() -> finish(() -> completed.accept(result)), () -> this.active.set(false)).run();
			} catch (Exception e) {
				LoggerUtils.logError(logger, e);
				ui.accessLater(() -> finish(() -> failed.accept(e)), () -> this.active.set(false)).run();
			}
		});
	}

	private void finish(Runnable action) {
		this.pending = false;
		if (this.active.get() && this.owner.isAttached()) {
			this.owner.getElement().setEnabled(true);
			action.run();
		}
	}
}
