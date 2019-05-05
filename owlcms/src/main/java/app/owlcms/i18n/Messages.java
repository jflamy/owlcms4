/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package app.owlcms.i18n;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Messages {
    private static final String BUNDLE_NAME = "i18n.messages"; //$NON-NLS-1$

    // private static final ResourceBundle RESOURCE_BUNDLE =
    // ResourceBundle.getBundle(BUNDLE_NAME);

    private Messages() {
    }

    public static String getString(String key, Locale locale) {
        try {
            // ResourceBundle caches the bundles, so this is not as inefficient
            // as it seems.
            return ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        } catch (MissingResourceException e) {
            return '«' + key + '»';
        }
    }

    public static String getStringNullIfMissing(String key, Locale locale) {
        try {
            // ResourceBundle caches the bundles, so this is not as inefficient
            // as it seems.
            return ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        } catch (MissingResourceException e) {
            return null;
        }
    }

    public static String getStringWithException(String key, Locale locale) throws MissingResourceException {
        return ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
    }
}
