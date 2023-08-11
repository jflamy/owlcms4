/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements.unload;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.shared.Registration;

/**
 * Server-side component that listens to {@code beforeunload} events. Based on
 * <a href="https://vaadin.com/forum/thread/17523194/unsaved-changes-detect-page-exit-or-reload">the code by Kaspar
 * Scherrer and Stuart Robinson</a>. This component will broadcast events on {@code beforeunload} event in the browser.
 * If {@link #isQueryingOnUnload()} is {@code true}, before the event the user will be prompted about leaving the page.
 * However, there is no way to find out what the user selected. If {@link #isQueryingOnUnload()} is {@code false}, the
 * event on the server will be called just before the page is unloaded. Note that the component must be present in the
 * DOM structure in the browser for the event to be received on the server.
 *
 * Warning: this class is a {@link UI}-scoped; the class is final, the constructors are private and there is at most one
 * instance per UI.
 *
 * Warning: when the page is unloaded, the UI instance should be removed to prevent memory leaks. See {@link #remove()}.
 *
 * @author Kaspar Scherrer, Stuart Robinson; adapted to web-component by miki@vaadin.com ; UI-scope by jf@jflamy.dev
 * @since 2020-04-29
 */
@JsModule("./unload/unload-observer.js")
@Tag("unload-observer")
public final class UnloadObserver extends LitTemplate {

    private static final String UNLOAD_OBSERVER = "owlcms_unload_observer";

    /**
     * Returns the current instance. Will create one using default no-arg constructor if none is present yet.
     *
     * @return An instance of {@link UnloadObserver}.
     */
    public static UnloadObserver get() {
        UI current = UI.getCurrent();
        UnloadObserver obs = (UnloadObserver) ComponentUtil.getData(current, UNLOAD_OBSERVER);
        if (obs == null) {
            obs = new UnloadObserver();
            ComponentUtil.setData(current, UNLOAD_OBSERVER, obs);
        }
        return obs;
    }

    /**
     * Returns the current instance. Will create one if needed and set its {@link #setQueryingOnUnload(boolean)}.
     *
     * @param queryingOnUnload Whether or not query at page close.
     * @return An instance of {@link UnloadObserver}.
     */
    public static UnloadObserver get(boolean queryingOnUnload) {
        UnloadObserver obs = get();
        obs.setQueryingOnUnload(queryingOnUnload);
        return obs;
    }

    /**
     * Cleans up the thread-local variable. This method is called automatically when the component receives
     * {@code unload} event.
     */
    public static void remove() {
        UI current = UI.getCurrent();
        if (current != null && get() != null) {
            ComponentUtil.setData(current, UNLOAD_OBSERVER, null);
        }
    }

    private boolean queryingOnUnload;

    private boolean clientInitialised;

    /**
     * Creates the unload observer and by default queries the user on unloading the page.
     */
    private UnloadObserver() {
        this(true);
    }

    /**
     * Creates the unload observer.
     *
     * @param queryOnUnload Whether or not to query the user on unloading the page.
     */
    private UnloadObserver(boolean queryOnUnload) {
        super();
        this.setQueryingOnUnload(queryOnUnload);
    }

    /**
     * Adds an {@link UnloadListener}.
     *
     * @param listener Listener to add.
     * @return A {@link Registration} that can be used to stop listening to the event.
     */
    public Registration addUnloadListener(UnloadListener listener) {
        return this.getEventBus().addListener(UnloadEvent.class, listener);
    }

    /**
     * Returns whether or not querying on document unload will happen.
     *
     * @return {@code true} when unloading the document from browser window results in showing a browser-native
     *         confirmation dialog and notifying {@link UnloadListener}s; {@code false} otherwise.
     */
    public boolean isQueryingOnUnload() {
        return this.queryingOnUnload;
    }

    /**
     * Controls whether or not there should be querying when the document is going to be unloaded.
     *
     * @param queryingOnUnload When {@code true}, {@link UnloadListener}s registered through
     *                         {@link #addUnloadListener(UnloadListener)} will be notified and document unloading can be
     *                         prevented. When {@code false}, nothing will happen when the document gets unloaded.
     */
    public void setQueryingOnUnload(boolean queryingOnUnload) {
        if (queryingOnUnload != this.queryingOnUnload) {
            this.queryingOnUnload = queryingOnUnload;
            this.getElement().getNode().runWhenAttached(ui -> ui.beforeClientResponse(this,
                    context -> this.getElement().callJsFunction("queryOnUnload", this.queryingOnUnload)));
        }
    }

    /**
     * Shortcut for {@code withQueryingOnUnload(false)}.
     *
     * @return This.
     * @see #withQueryingOnUnload(boolean)
     */
    public UnloadObserver withoutQueryingOnUnload() {
        return this.withQueryingOnUnload(false);
    }

    /**
     * Shortcut for {@code withQueryingOnUnload(true)}.
     *
     * @return This.
     * @see #withQueryingOnUnload(boolean)
     */
    public UnloadObserver withQueryingOnUnload() {
        return this.withQueryingOnUnload(true);
    }

    /**
     * Chains {@link #setQueryingOnUnload(boolean)} and returns itself.
     *
     * @param value Whether or not to query on document unload.
     * @return This.
     * @see #setQueryingOnUnload(boolean)
     */
    public UnloadObserver withQueryingOnUnload(boolean value) {
        this.setQueryingOnUnload(value);
        return this;
    }

    /**
     * Fires the {@link UnloadEvent}.
     *
     * @param event Event to fire.
     */
    protected void fireUnloadEvent(UnloadEvent event) {
        this.getEventBus().fireEvent(event);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (!this.clientInitialised) {
            this.getElement().callJsFunction("initObserver");
            this.clientInitialised = true;
        }
        super.onAttach(attachEvent);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        this.clientInitialised = false;
        super.onDetach(detachEvent);
    }

    @ClientCallable
    private void unloadAttempted() {
        this.fireUnloadEvent(new UnloadEvent(this, true));
    }

    @ClientCallable
    private void unloadHappened() {
        this.fireUnloadEvent(new UnloadEvent(this, false));
    }
}