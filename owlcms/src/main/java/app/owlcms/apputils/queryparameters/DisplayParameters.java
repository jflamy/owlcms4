/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils.queryparameters;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Navigating to a component that implements this interface will trigger the setParameter method. The setParameter
 * method will invoke readParams method to actually process the query parameter.
 *
 * Display pages extend an abstract class that stores all the options.
 *
 * @author jflamy
 *
 */
public interface DisplayParameters extends ContentParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(DisplayParameters.class);
	public static final String CATEGORY = "cat";
	public static final String DARK = "dark";
	public static final String FONTSIZE = "em";
	public static final String LIGHT = "light";
	public static final String PUBLIC = "public";
	public static final String SOUND = "sound";
	public static final String RECORDS = "records";
	public static final String LEADERS = "leaders";
	public static final String TEAMWIDTH = "tw";
	public static final String ABBREVIATED = "abb";
	public static final String VIDEO = "video";

	public default void pushEmSize() {
	}

	public default void pushTeamWidth() {
	}

	public default Double getEmFontSize() {
		return 1.0D;
	}

	public String getRouteParameter();

	public default Double getTeamWidth() {
		return 0.0D;
	}

	public default boolean isAbbreviatedName() {
		return false;
	}

	public boolean isDarkMode();

	public default boolean isDefaultLeadersDisplay() {
		return false;
	}

	public default boolean isDefaultRecordsDisplay() {
		return false;
	}

	public default boolean isLeadersDisplay() {
		return false;
	}

	/**
	 * @return true if the display can switch during breaks (for example, to medals)
	 */
	public default boolean isPublicDisplay() {
		return false;
	}

	public default boolean isRecordsDisplay() {
		return false;
	}

	public boolean isVideo();

	public void setAbbreviatedName(boolean b);

	public void setDarkMode(boolean dark);

	public void setEmFontSize(Double emFontSize);

	public void setLeadersDisplay(boolean showLeaders);

	public void setPublicDisplay(boolean publicDisplay);

	public void setRecordsDisplay(boolean showRecords);

	public void setTeamWidth(Double tw);

}
