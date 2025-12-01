/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors.websocket;

/**
 * Attempt status values for display. Maps internal state to consistent string values
 * that downstream consumers (trackers, scoreboards) can rely on.
 * 
 * Status values sent over WebSocket:
 * - "good" - Successful lift (green display)
 * - "bad" - Failed lift (red display)
 * - "current" - Pending attempt for current athlete (should blink)
 * - "next" - Pending attempt for next athlete (highlighted)
 * - "request" - Pending attempt for other athletes
 * - "empty" - No data for this attempt
 */
public enum AttemptStatus {
	GOOD("good"),       // Successful lift
	BAD("bad"),         // Failed lift  
	CURRENT("current"), // Pending attempt for current athlete (should blink)
	NEXT("next"),       // Pending attempt for next athlete
	REQUEST("request"), // Pending attempt for other athletes
	EMPTY("empty");     // No data for this attempt
	
	private final String value;
	
	AttemptStatus(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	/**
	 * Map lift order rank to pending attempt status.
	 * @param liftOrderRank 1=current athlete, 2=next athlete, 0=other
	 * @return appropriate pending status
	 */
	public static AttemptStatus fromLiftOrderRank(int liftOrderRank) {
		switch (liftOrderRank) {
			case 1: return CURRENT;
			case 2: return NEXT;
			default: return REQUEST;
		}
	}
}
