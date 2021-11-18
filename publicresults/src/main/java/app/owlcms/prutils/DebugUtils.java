/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.prutils;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class DebugUtils {

    static Logger logger = (Logger) LoggerFactory.getLogger("garbageCollection");

    final static String LINESEPARATOR = System.getProperty("line.separator");

    public static void gc() {
        final String where = LoggerUtils.whereFrom();
        new Thread(() -> {
            try {
                logger.debug("clearing memory {}", where);
                System.gc();
                Thread.sleep(500);
                System.gc();
            } catch (InterruptedException e) {
            }

        }).start();
    }
}
