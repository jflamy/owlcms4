/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.fieldofplay.BreakType;
import app.owlcms.ui.lifting.BreakManagement.CountdownType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakDialog extends Dialog {
    final private Logger logger = (Logger) LoggerFactory.getLogger(BreakDialog.class);;
    {
        logger.setLevel(Level.INFO);
    };

    /**
     * Used by the announcer -- tries to guess what type of break is pertinent based
     * on field of play.
     *
     * @param origin the origin
     */
    BreakDialog(Object origin) {
        VerticalLayout content = new BreakManagement(origin, this);
        this.add(content);
    }

    /**
     * @param origin
     * @param brt
     * @param cdt
     */
    BreakDialog(Object origin, BreakType brt, CountdownType cdt) {
        VerticalLayout content = new BreakManagement(origin, brt, cdt, this);
        this.add(content);
    }

}
