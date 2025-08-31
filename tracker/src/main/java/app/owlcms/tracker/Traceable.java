/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tracker;

import java.util.Map.Entry;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public interface Traceable {

    public default void tracePairs(Set<Entry<String, String[]>> pairs) {
        Level level = getLogger().getLevel();
        try {
            getLogger().setLevel(Level.TRACE);
            for (Entry<String, String[]> pair : pairs) {
                if (pair.getKey().contentEquals("updateKey")) {
                    var val = pair.getValue()[0];
                    getLogger()./**/trace("    {} = {}", pair.getKey(),
                            val != null ? "masked " + val.length() : "masked null value");
                } else {
                    getLogger()./**/trace("    {} = {}", pair.getKey(), pair.getValue()[0]);
                }
            }
        } finally {
            getLogger().setLevel(level);
        }
    }

    public Logger getLogger();

}