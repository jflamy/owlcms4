/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.athlete;

import java.util.Locale;

import org.slf4j.LoggerFactory;

import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * The Class RuleViolationException.
 */
public class RuleViolationException extends RuntimeException {
    @SuppressWarnings("unused")

    private static final Logger logger = (Logger) LoggerFactory.getLogger(RuleViolationException.class);

    private static final long serialVersionUID = 8965943679108964933L;
    protected String messageKey;
    protected Object[] messageFormatData;

    @SuppressWarnings("serial")
    public class DeclarationValueTooSmall extends RuleViolationException {
        public DeclarationValueTooSmall(String s, Object... objs) {
            super(s, objs);
        }
    }

    /**
     * Instantiates a new rule violation exception.
     *
     * @param s    the s
     * @param objs the objs
     */
    public RuleViolationException(String s, Object... objs) {
        super(s);
        this.messageKey = s;
        this.messageFormatData = objs;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
        final Locale locale1 = OwlcmsSession.getLocale();
        final String fopName = OwlcmsSession.getFopName();
        return "FOP " + fopName + " " + Translator.translate(this.messageKey, locale1, messageFormatData);
    }

    /**
     * Gets the localized message.
     *
     * @param locale1 the locale 1
     * @return the localized message
     */
    public String getLocalizedMessage(Locale locale1) {
        return Translator.translate(this.messageKey, locale1, messageFormatData);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }

    public String getMessageKey() {
        return messageKey;
    }
}
