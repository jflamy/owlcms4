/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.shared;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dialog.Dialog;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakDialog extends Dialog {
    final private Logger logger = (Logger) LoggerFactory.getLogger(BreakDialog.class);
    private BreakManagement content;
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * Used by the announcer -- tries to guess what type of break is pertinent based on field of play.
     *
     * @param origin the origin
     */
    public BreakDialog(Object origin) {
        this.addAttachListener((e) -> {
            content = new BreakManagement(origin, this);
            this.removeAll();
            this.add(content);
        });

        this.addDialogCloseActionListener((e) -> {
            this.removeAll();
            this.close();

            // defensive, should have been unregistered already
            try {
                OwlcmsSession.getFop().getUiEventBus().unregister(content);
            } catch (Exception e1) {
            }

            try {
                OwlcmsSession.getFop().getFopEventBus().unregister(content);
            } catch (Exception e1) {
            }
            content.cleanup();
            content = null;

        });
    }

    /**
     * @param origin
     * @param brt
     * @param cdt
     */
    public BreakDialog(Object origin, BreakType brt, CountdownType cdt) {

        this.addAttachListener((e) -> {
            content = new BreakManagement(origin, brt, cdt, this);
            this.removeAll();
            this.add(content);
        });

        this.addDialogCloseActionListener((e) -> {
            this.removeAll();
            this.close();
            // defensive, should have been unregistered already
            try {
                OwlcmsSession.getFop().getUiEventBus().unregister(content);
            } catch (Exception e1) {
            }
            try {
                OwlcmsSession.getFop().getFopEventBus().unregister(content);
            } catch (Exception e1) {
            }
            content.cleanup();
            content = null;
        });
    }

}
