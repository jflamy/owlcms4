/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.monitor;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class Monitor
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings("serial")
@Tag("monitor-template")
@JsModule("./components/Monitor.js")
@Route("displays/monitor")
@Theme(value = Lumo.class)
@Push
public class Monitor extends PolymerTemplate<Monitor.MonitorModel> implements FOPParameters,
        SafeEventBusRegistration, UIEventProcessor {

    /**
     * unused
     */
    public interface MonitorModel extends TemplateModel {
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(Monitor.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private EventBus uiEventBus;
    private Location location;
    private UI locationUI;
    private String currentFOP;
    private FOPState currentState = FOPState.INACTIVE;
    private FOPState previousState = FOPState.INACTIVE;
    private BreakType currentBreakType;
    private BreakType previousBreakType;
    private String title;
    private String prevTitle;

    /**
     * Instantiates a new results board.
     */
    public Monitor() {
        OwlcmsFactory.waitDBInitialized();
        this.getElement().getStyle().set("width", "100%");
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    public String getPageTitle() {
        String string = computePageTitle();
        return string;
    }

    @Override
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    /**
     * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
     */
    @Override
    public boolean isShowInitialDialog() {
        return false;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    @Subscribe
    public void slaveUIEvent(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            if (syncWithFOP(e)) {
                // significant transition
                doUpdate();
            }
        });
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        // fop obtained via FOPParameters interface default methods.
        OwlcmsSession.withFop(fop -> {
            init();
            // sync with current status of FOP
            syncWithFOP(null);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
        doUpdate();
    }

    void uiLog(UIEvent e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
    }

    @SuppressWarnings("unused")
    private String computeLiftType(Athlete a) {
        if (a == null) {
            return "";
        }
        String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
                : Translator.translate("Snatch");
        return liftType;
    }

    private String computePageTitle() {
        StringBuilder pageTitle = new StringBuilder();
        if (currentState == FOPState.INACTIVE || currentState == FOPState.BREAK) {
            pageTitle.append("break=");
        } else {
            pageTitle.append("state=");
        }
        pageTitle.append(currentState.name());
        if (currentState == FOPState.BREAK && currentBreakType != null) {
            pageTitle.append(".");
            pageTitle.append(currentBreakType.name());
        }
        pageTitle.append(";");
        pageTitle.append("previous=");
        pageTitle.append(previousState.name());
        if (previousState == FOPState.BREAK && previousBreakType != null) {
            pageTitle.append(".");
            pageTitle.append(previousBreakType.name());
        }
        pageTitle.append(";");
        pageTitle.append("fop=");
        pageTitle.append(currentFOP);

        String string = pageTitle.toString();
        return string;
    }

    private void doUpdate() {
        title = computePageTitle();
        if (!title.contentEquals(prevTitle)) {
            this.getElement().setProperty("title", title);
            this.getElement().callJsFunction("setTitle", title);
            logger.warn("monitor update {}", title);
            prevTitle = title;
        }
    }

    private Object getOrigin() {
        return this;
    }

    private void init() {
        OwlcmsSession.withFop(fop -> {
            logger.trace("{}Starting monitoring", fop.getLoggingName());
            setId("scoreboard-" + fop.getName());
        });
    }

    private boolean syncWithFOP(UIEvent e) {
        boolean significant = true;
        OwlcmsSession.withFop(fop -> {
            currentFOP = fop.getName();

            if (fop.getState() != currentState) {
                previousState = currentState;
                currentState = fop.getState();
                previousBreakType = currentBreakType;
                currentBreakType = fop.getBreakType();
            } else if (fop.getState() == FOPState.BREAK) {
                if (fop.getBreakType() != currentBreakType) {
                    previousBreakType = currentBreakType;
                    currentBreakType = fop.getBreakType();
                }
            }
        });
        if (previousState == FOPState.DECISION_VISIBLE && currentState == FOPState.BREAK
                && currentBreakType == BreakType.GROUP_DONE) {
//            logger.debug("ignoring visible -> done");
//            significant = false;
        }
        return significant;
    }
}
