/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

/**
 * Filter enumerations for record management interface
 */
public class RecordFilters {

	public enum ProvisionalFilter {
		ALL,
		PROVISIONAL,
		OFFICIAL;

		public String getKey() {
			return "RecordEvent." + this.name();
		}
	}
	
	public enum CurrentHistoryFilter {
		CURRENT,
		HISTORY;

		public String getKey() {
			return "RecordEvent." + this.name();
		}
	}
}
