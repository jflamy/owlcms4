/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.athlete;

import java.text.MessageFormat;
import java.util.Locale;

import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Messages;

/**
 * The Class RuleViolationException.
 */
public class RuleViolationException extends RuntimeException {
    private static final long serialVersionUID = 8965943679108964933L;
    private String messageKey;
    private Object[] messageFormatData;
    private Locale locale;

    /**
     * Instantiates a new rule violation exception.
     *
     * @param s the s
     * @param objs the objs
     */
    public RuleViolationException(String s, Object... objs) {
        super(s);
        this.messageKey = s;
        this.messageFormatData = objs;
    }

    /**
     * Instantiates a new rule violation exception.
     *
     * @param l the l
     * @param s the s
     * @param objs the objs
     */
    public RuleViolationException(Locale l, String s, Object... objs) {
        super(s);
        this.setLocale(l);
        this.messageKey = s;
        this.messageFormatData = objs;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#getMessage()
     */
    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }

    /**
     * Sets the locale.
     *
     * @param locale the new locale
     */
    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    /* (non-Javadoc)
     * @see java.lang.Throwable#getLocalizedMessage()
     */
    @Override
    public String getLocalizedMessage() {
        final Locale locale1 = (this.locale == null ? Competition.getCurrent().getDefaultLocale() : this.locale);
        final String messageTemplate = Messages.getString(this.messageKey, locale1);
        return MessageFormat.format(messageTemplate, messageFormatData);
    }

    /**
     * Gets the localized message.
     *
     * @param locale1 the locale 1
     * @return the localized message
     */
    public String getLocalizedMessage(Locale locale1) {
        final String messageTemplate = Messages.getString(this.messageKey, locale1);
        return MessageFormat.format(messageTemplate, messageFormatData);
    }

    /**
     * Gets the locale.
     *
     * @return the locale
     */
    public Locale getLocale() {
        return this.locale;
    }

}
