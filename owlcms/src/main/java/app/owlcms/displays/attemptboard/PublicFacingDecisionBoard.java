/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.init.OwlcmsSession;

@SuppressWarnings("serial")
@Tag("decision-board-template")
@JsModule("./components/DecisionBoard.js")
@JsModule("./components/AudioContext.js")
@Route("displays/publicFacingDecision")
@Push
@Theme(value = Lumo.class, variant = Lumo.DARK)
public class PublicFacingDecisionBoard extends AttemptBoard {

    public PublicFacingDecisionBoard() {
        super();
        setPublicFacing(true);
        setShowBarbell(false);
        setSilenced(isSilencedByDefault());
        breakTimer.setParent("DecisionBoard");
    }
    
    @Override
    protected void checkImages() {
        athletePictures = false;
        teamFlags = false;
    }

    @Override
    public String getPageTitle() {
        return getTranslation("Decision_PF_") + OwlcmsSession.getFopNameIfMultiple();
    }

    @Override
    public boolean isSilencedByDefault() {
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.AttemptBoard#onAttach(com.vaadin.flow. component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        decisions.setPublicFacing(false);
    }

}
