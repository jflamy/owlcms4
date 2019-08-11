/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.util.List;

import app.owlcms.data.athlete.Athlete;

public class DebugUtils {

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

}
