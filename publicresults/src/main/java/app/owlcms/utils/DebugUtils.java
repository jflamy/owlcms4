/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import org.slf4j.LoggerFactory;

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
