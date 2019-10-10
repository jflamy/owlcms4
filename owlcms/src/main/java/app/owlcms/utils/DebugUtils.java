/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import ch.qos.logback.classic.Logger;

public class DebugUtils {
    
    static Logger logger = (Logger) LoggerFactory.getLogger("garbageCollection");

	/**
	 * @param lifterList
	 * @return ordered printout of lifters, one per line.
	 */
	public static String longDump(List<Athlete> lifterList) {
	    StringBuffer sb = new StringBuffer();
	    for (Athlete lifter : lifterList) {
	        sb.append(lifter.longDump());
	        sb.append(LINESEPARATOR);
	    }
	    return sb.toString();
	}

	/**
	 * @param lifterList
	 * @return ordered printout of lifters, one per line.
	 */
	public static String shortDump(List<? extends Athlete> lifterList) {
	    StringBuffer sb = new StringBuffer();
	    for (Athlete lifter : lifterList) {
	        sb.append(lifter.getLastName() + " " + lifter.getFirstName()
	            + " " + lifter.getNextAttemptRequestedWeight()
	            + " " + (lifter.getAttemptsDone() + 1)
	            + " " + lifter.getLotNumber());
	        sb.append(LINESEPARATOR);
	    }
	    return sb.toString();
	}

	/**
	 * @param lifterList
	 * @return ordered printout of lifters, one per line.
	 */
	static String longDump(List<Athlete> lifterList, boolean includeTimeStamp) {
	    StringBuffer sb = new StringBuffer();
	    for (Athlete lifter : lifterList) {
	    	sb.append(lifter.longDump());
	        sb.append(LINESEPARATOR);
	    }
	    return sb.toString();
	}

	final static String LINESEPARATOR = System.getProperty("line.separator");

    public static void gc() {
        final String where = LoggerUtils.whereFrom();
        new Thread(() -> {
            try {
                logger.warn("clearing memory {}", where);
                System.gc();
                Thread.sleep(500);
                System.gc();
            } catch (InterruptedException e) {
            }

        }).start();
    }
}
