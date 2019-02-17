/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.data.athlete;

import java.text.MessageFormat;
import java.util.Locale;

import org.ledocte.owlcms.data.competition.Competition;
import org.ledocte.owlcms.i18n.Messages;

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
