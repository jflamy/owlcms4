/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.displays.scoreboard.ResultsPR;
import app.owlcms.i18n.Translator;
import app.owlcms.uievents.UpdateEvent;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

@Route

public class MainView extends VerticalLayout {

    static Text text;

    private static Logger logger = (Logger) LoggerFactory.getLogger(MainView.class);
    private UI ui;

    public MainView() {
        logger.debug("mainView");
        text = new Text(Translator.translate("WaitingForSite"));
        ui = UI.getCurrent();
        if (ui != null) {
            buildHomePage();
        }
    }

    @Subscribe
    public void update(UpdateEvent e) {
        if (ui == null) {
            logger.error("ui is null!?");
            return;
        }
        ui.access(() -> {
            buildHomePage();
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logger.debug("onAttach");
        super.onAttach(attachEvent);
        ui = UI.getCurrent();
        UpdateReceiverServlet.getEventBus().register(this);
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
        if (fopNames.size() == 0 || ui == null) {
            removeAll();
            add(text);
        } else if (fopNames.size() == 1) {
            logger.debug("single platform, proceeding to scoreboard");
            Map<String, String> parameterMap = new HashMap<>();
            String fop = fopNames.stream().findFirst().get();
            parameterMap.put("FOP", fop);
            // ui.navigate("displays/resultsLeader", QueryParameters.simple(parameterMap));
            ui.getPage().executeJs("window.location.href='results?fop=" + fop + "'");
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
            Button fopButton = new Button(getTranslation("Platform") + " " + fopName,
                    buttonClickEvent -> {
                        String url = URLUtils.getRelativeURLFromTargetClass(ResultsPR.class);
                        HashMap<String, List<String>> params = new HashMap<>();
                        params.put("fop", Arrays.asList(fopName));
                        QueryParameters parameters = new QueryParameters(params);
                        UI.getCurrent().navigate(url, parameters);
                    });
            add(fopButton);
        });
    }

}
