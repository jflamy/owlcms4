/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.athlete;

import java.text.MessageFormat;
import java.util.Locale;

import org.ledocte.owlcms.data.competition.Competition;

public class RuleViolationException extends RuntimeException {
    private static final long serialVersionUID = 8965943679108964933L;
    private String messageKey;
    private Object[] messageFormatData;
    private Locale locale;

    public RuleViolationException(String s, Object... objs) {
        super(s);
        this.messageKey = s;
        this.messageFormatData = objs;
    }

    public RuleViolationException(Locale l, String s, Object... objs) {
        super(s);
        this.setLocale(l);
        this.messageKey = s;
        this.messageFormatData = objs;
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    @Override
    public String getLocalizedMessage() {
        final Locale locale1 = (this.locale == null ? Competition.getDefaultLocale() : this.locale);
        final String messageTemplate = Messages.getString(this.messageKey, locale1);
        return MessageFormat.format(messageTemplate, messageFormatData);
    }

    public String getLocalizedMessage(Locale locale1) {
        final String messageTemplate = Messages.getString(this.messageKey, locale1);
        return MessageFormat.format(messageTemplate, messageFormatData);
    }

    public Locale getLocale() {
        return this.locale;
    }

}
