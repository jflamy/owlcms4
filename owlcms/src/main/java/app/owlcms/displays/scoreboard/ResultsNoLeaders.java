/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings("serial")
@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")
@Route("displays/results")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@Push
public class ResultsNoLeaders extends Results {

    /**
     * Instantiates a new results board.
     */
    public ResultsNoLeaders() {
        OwlcmsFactory.waitDBInitialized();
        timer.setOrigin(this);
        setDarkMode(true);
        setDefaultLeadersDisplay(false);
        setDefaultRecordsDisplay(false);
    }

    @Override
    public String getPageTitle() {
        return getTranslation("Scoreboard") + OwlcmsSession.getFopNameIfMultiple();
    }

//    @Override
//    protected void computeLeaders(boolean done) {
//        UI.getCurrent().access(() -> {
//            this.getElement().setPropertyJson("leaders", Json.createNull());
//            this.getElement().setProperty("leaderLines", 1); // must be > 0
//        });
//    }
}