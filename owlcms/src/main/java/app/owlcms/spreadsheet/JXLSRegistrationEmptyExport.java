/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Return empty list with the current information in the database for groups and platforms, but no athletes
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSRegistrationEmptyExport extends JXLSRegistrationExport {

    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSRegistrationEmptyExport.class);
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

    /**
     * @param ui
     */
    public JXLSRegistrationEmptyExport(UI ui) {
        super(ui);
    }
    
    @Override
    protected boolean isEmptyOk() {
        return true;
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        // no athletes - create an empty so the reporting works
        return List.of();
    }
    
}
