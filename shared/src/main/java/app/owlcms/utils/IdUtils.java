/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

public class IdUtils {

    /**
     * Compute a unique Id based on current system clock, at nanosecond granularity
     *
     * Used when it is necessary to create a complex relationship between objects that have not been persisted yet (e.g.
     * new athletes wrt eligible categories and selected category)
     *
     * @return
     */
    public static Long getTimeBasedId() {
        long ix = (System.currentTimeMillis() << 20) | (System.nanoTime() & 0xFFFFFL);
        // System.out.println("computed "+ix);
        return ix;
    }

    public static Long getTimeBasedId(Object o) {
        long ix = (System.currentTimeMillis() << 20) | (System.nanoTime() & 0xFFFFFL);
        // System.out.println("computed "+System.identityHashCode(o)+" "+ix);
        return ix;
    }

}
