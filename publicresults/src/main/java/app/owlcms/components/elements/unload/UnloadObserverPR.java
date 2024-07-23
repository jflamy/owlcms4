/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements.unload;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

import app.owlcms.displays.scoreboard.ResultsPR;
import app.owlcms.prutils.SafeEventBusRegistrationPR;
import app.owlcms.prutils.SessionCleanup;
import app.owlcms.publicresults.MainView;
import ch.qos.logback.classic.Logger;

/**
 * This class is the API for a WebComponent that gets added to the main view of
 * a page.
 * See {@link SafeEventBusRegistrationPR}
 * 
 * This class updates a map in the VaadinSession that contains the activity
 * status for all pages in the session.
 * 
 * A {@link SessionCleanup} thread runs and expires all pages based on an
 * inactivity
 * policy.
 * 
 * Server-side component that listens to {@code beforeunload} events. Based on
 * <a href=
 * "https://vaadin.com/forum/thread/17523194/unsaved-changes-detect-page-exit-or-reload">the
 * code by Kaspar
 * Scherrer and Stuart Robinson</a>. This component will broadcast events on
 * {@code beforeunload} event in the browser.
 * If {@link #isQueryingOnUnload()} is {@code true}, before the event the user
 * will be prompted about leaving the page.
 * However, there is no way to find out what the user selected. If
 * {@link #isQueryingOnUnload()} is {@code false}, the
 * event on the server will be called just before the page is unloaded. Note
 * that the component must be present in the
 * DOM structure in the browser for the event to be received on the server.
 */
@JsModule("./unload/unload-observerPR.js")
@Tag("unload-observer-pr")
public final class UnloadObserverPR extends LitTemplate {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(UnloadObserverPR.class);

    private boolean queryingOnUnload;

    private boolean clientInitialised;

    private UI ui;

    private Component component;

    private URL url;

    private String title;

    /**
     * Creates the unload observer and by default queries the user on unloading the
     * page.
     * 
     * @param current
     */
    public UnloadObserverPR(UI current, Component c) {
        this(true, current, c);
    }

    /**
     * Creates the unload observer.
     *
     * @param queryOnUnload Whether or not to query the user on unloading the page.
     */
    public UnloadObserverPR(boolean queryOnUnload, UI ui, Component c) {
        this.setQueryingOnUnload(queryOnUnload);
        this.ui = ui;
        this.component = c;
        ui.getPage().fetchCurrentURL(u -> setUrl(u));
        this.title = "";
//        logger.debug("UnloadObserverPR={} (getElement()={}) component={} {}",
//                System.identityHashCode(this), System.identityHashCode(this.getElement()),
//                c.getClass().getSimpleName(), System.identityHashCode(c));
    }

    /**
     * Adds an {@link UnloadListenerPR}.
     *
     * @param listener Listener to add.
     * @return A {@link Registration} that can be used to stop listening to the
     *         event.
     */
    public Registration addEventListener(UnloadListenerPR listener) {
        return this.getEventBus().addListener(UnloadEventPR.class, listener);
    }

    /**
     * Returns whether or not querying on document unload will happen.
     *
     * @return {@code true} when unloading the document from browser window results
     *         in showing a browser-native
     *         confirmation dialog and notifying {@link UnloadListenerPR}s;
     *         {@code false} otherwise.
     */
    public boolean isQueryingOnUnload() {
        return this.queryingOnUnload;
    }

    public void setActivityTime() {
        if (!okClass()) {
            return;
        }
        logger.debug("{} active {} {}", title, component.getClass().getSimpleName(), System.identityHashCode(component));
        VaadinSession vs = VaadinSession.getCurrent();
        vs.access(() -> {
            var im = getInactivityMap(vs);
            im.put(this, System.currentTimeMillis());
        });
    }

    public void setInactivityTime() {
        if (!okClass()) {
            return;
        }
        logger.debug("{} inactive {} {}", title, component.getClass().getSimpleName(), System.identityHashCode(component));
        VaadinSession vs = VaadinSession.getCurrent();
        vs.access(() -> {
            var im = getInactivityMap(vs);
            var value = im.get(this);
            if (value != null && value > 0) {
                // do not reset if already inactive
                im.put(this, -System.currentTimeMillis());
            }
        });
    }

    private boolean okClass() {
        Component component2 = this.getComponent();
        boolean okClass = (component2 instanceof MainView) || (component2 instanceof ResultsPR);
        return okClass;
    }

    public void setGoneTime() {
        if (!okClass()) {
            return;
        }
        // mark for immediate removal
        logger.debug("{} gone {} {}", title, component.getClass().getSimpleName(), System.identityHashCode(component));
        // don't recreate an entry for if already removed by other processing.
        VaadinSession vs = VaadinSession.getCurrent();
        vs.access(() -> {
            var im = getInactivityMap(vs);
            var val = im.get(this);
            if (val != null) {
                if (ui != null) {
                    VaadinSession vs1 = VaadinSession.getCurrent();
                    vs1.access(() -> {
                        var im1 = getInactivityMap(vs1);
                        im1.put(this, (long) 0);
                    });
                }
            }
        });
    }

    /**
     * Controls whether or not there should be querying when the document is going
     * to be unloaded.
     *
     * @param queryingOnUnload When {@code true}, {@link UnloadListenerPR}s
     *                         registered
     *                         through
     *                         {@link #addEventListener(UnloadListenerPR)} will be
     *                         notified and document unloading can be
     *                         prevented. When {@code false}, nothing will happen
     *                         when the document gets unloaded.
     */
    public void setQueryingOnUnload(boolean queryingOnUnload) {
        if (queryingOnUnload != this.queryingOnUnload) {
            this.queryingOnUnload = queryingOnUnload;
            this.getElement().getNode().runWhenAttached(ui -> ui.beforeClientResponse(this,
                    context -> this.getElement().callJsFunction("queryOnUnload", this.queryingOnUnload)));
        }
    }

    @ClientCallable
    public void unloadHappened(String change) {
        this.fireUnloadEvent(new UnloadEventPR(this, false, change));
    }

    @ClientCallable
    public void visibilityChange(String change) {
        if (!okClass()) {
            return;
        }
        this.fireUnloadEvent(new UnloadEventPR(this, true, change));
    }

    /**
     * Shortcut for {@code withQueryingOnUnload(false)}.
     *
     * @return This.
     * @see #withQueryingOnUnload(boolean)
     */
    public UnloadObserverPR withoutQueryingOnUnload() {
        return this.withQueryingOnUnload(false);
    }

    /**
     * Shortcut for {@code withQueryingOnUnload(true)}.
     *
     * @return This.
     * @see #withQueryingOnUnload(boolean)
     */
    public UnloadObserverPR withQueryingOnUnload() {
        return this.withQueryingOnUnload(true);
    }

    /**
     * Chains {@link #setQueryingOnUnload(boolean)} and returns itself.
     *
     * @param value Whether or not to query on document unload.
     * @return This.
     * @see #setQueryingOnUnload(boolean)
     */
    public UnloadObserverPR withQueryingOnUnload(boolean value) {
        this.setQueryingOnUnload(value);
        return this;
    }

    /**
     * Fires the {@link UnloadEventPR}.
     *
     * @param event Event to fire.
     */
    protected void fireUnloadEvent(UnloadEventPR event) {
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

    private Map<UnloadObserverPR, Long> getInactivityMap(VaadinSession vs) {
        @SuppressWarnings("unchecked")
        var im = (Map<UnloadObserverPR, Long>) vs.getAttribute("inactivityMap");
        if (im == null) {
            im = new HashMap<>();
            vs.setAttribute("inactivityMap", im);
        }
        return im;
    }

    public void doReload(String reloadTitle, String reloadText, String reloadLabel, String reloadUrl) {
        Element element = this.getElement();
        ui = this.getUi();
        logger.debug("   doReload tab={} element={} {}", System.identityHashCode(element),
                System.identityHashCode(element), reloadUrl);
        element.setProperty("reloadTitle", reloadTitle);
        element.setProperty("reloadText", reloadText);
        element.setProperty("reloadUrl", reloadUrl);
        element.setProperty("reloadLabel", reloadLabel);
    }

    public UI getUi() {
        return ui;
    }

    public void setUi(UI ui) {
        this.ui = ui;
    }

    public Component getComponent() {
        return this.component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public String getTitle() {
        return this.title;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setTitle(String title) {
        //logger.trace("----------------- setTitle {} {}", title, LoggerUtils.whereFrom());
        this.title = title;
    }
}