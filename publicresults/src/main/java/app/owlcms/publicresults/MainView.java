/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.elements.unload.UnloadObserverPR;
import app.owlcms.i18n.Translator;
import app.owlcms.prutils.SafeEventBusRegistrationPR;
import app.owlcms.uievents.UpdateEvent;
import ch.qos.logback.classic.Logger;

@Route
public class MainView extends VerticalLayout implements SafeEventBusRegistrationPR, HasDynamicTitle {

    static Text text;

    private static Logger logger = (Logger) LoggerFactory.getLogger(MainView.class);
    private UI ui;

    private UnloadObserverPR eventObserver;

    public MainView() {
        this.setId("owlcmsTemplate");
        logger.debug("mainView");
        text = new Text(Translator.translate("WaitingForSite"));
        this.ui = UI.getCurrent();
        if (this.ui != null) {
            buildHomePage();
        }
    }

    @Subscribe
    public void update(UpdateEvent e) {
        if (this.ui == null) {
            logger.error("ui is null!?");
            return;
        }
        this.ui.access(() -> {
            buildHomePage();
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logger.debug("onAttach");
        super.onAttach(attachEvent);
        this.ui = UI.getCurrent();
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());
        this.getEventObserver().setTitle(getPageTitle());
        // so the page expires
        visibilityStatus(false);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        try {
            UpdateReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
    }

    private void buildHomePage() {
        // we cache the last update received for each field of play, indexed by fop name
        Set<String> fopNames = UpdateReceiverServlet.getUpdateCache().keySet();
        if (fopNames.size() == 0 || this.ui == null) {
            removeAll();
            add(text);
        } else if (fopNames.size() == 1) {
            logger.debug("single platform, proceeding to scoreboard");
            String fop = fopNames.stream().findFirst().get();
            this.ui.getPage().executeJs("window.location.href='results?fop=" + fop + "'");
        } else {
            createButtons(fopNames);
        }
    }

    private void createButtons(Set<String> fopNames) {
        removeAll();
        UpdateEvent updateEvent = UpdateReceiverServlet.getUpdateCache().entrySet().stream().findFirst().orElse(null)
                .getValue();
        if (updateEvent == null) {
            return;
        }

        H3 title = new H3(updateEvent.getCompetitionName());
        add(title);
        fopNames.stream().sorted().forEach(fopName -> {
            Button fopButton = new Button(Translator.translate("Platform") + " " + fopName,
                    buttonClickEvent -> {
                        this.ui.getPage().executeJs("window.location.href='results?fop=" + fopName + "'");
                    });
            add(fopButton);
        });
    }

    @ClientCallable
    public void visibilityStatus(boolean visible) {
        logger.debug("visibilityStatus: {} {} {}", visible, this.getClass().getSimpleName(),
                System.identityHashCode(this));
        UnloadObserverPR eventObserver = getEventObserver();
        if (visible) {
            eventObserver.setActivityTime();
        } else {
            eventObserver.setInactivityTime();
        }
    }

    @Override
    public String getPageTitle() {
        return Translator.translate("OWLCMS_Displays");
    }

    @Override
    public void setEventObserver(UnloadObserverPR uo) {
        this.eventObserver = uo;
    }

    @Override
    public UnloadObserverPR getEventObserver() {
        return this.eventObserver;
    }
}
