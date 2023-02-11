/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

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
        setLeadersDisplay(isDefaultLeadersDisplay());
        setRecordsDisplay(isDefaultRecordsDisplay());
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