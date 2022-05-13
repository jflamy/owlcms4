/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSRegistration extends JXLSWorkbookStreamSource {

    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSRegistration.class);
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

    public JXLSRegistration(UI ui) {
        super();
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        return getLocalizedTemplate("/templates/registration/RegistrationTemplate", ".xls", locale);
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        getReportingBeans();
        return ImmutableList.of(new Athlete());
    }

}
