/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

/**
 * A save from a form opened before someone else saved the same settings. Nothing from the refused save was written.
 */
@SuppressWarnings("serial")
public class StaleSingletonException extends RuntimeException {

	public enum Kind {
		CONFIG,
		COMPETITION,
		RECORD_CONFIG,
		AGE_GROUP
	}

	private final Kind kind;
	private final String savedBy;

	/** @param savedBy who made the save that the form missed; null or blank when unknown */
	public StaleSingletonException(Kind kind, String savedBy) {
		super(kind + " was saved after the page was opened" + (savedBy != null ? " by " + savedBy : ""));
		this.kind = kind;
		this.savedBy = savedBy;
	}

	public Kind getKind() {
		return this.kind;
	}

	public String getSavedBy() {
		return this.savedBy;
	}

	/** The exception itself or the first one in its cause chain, null if none. */
	public static StaleSingletonException find(Throwable t) {
		for (Throwable cur = t; cur != null; cur = cur.getCause()) {
			if (cur instanceof StaleSingletonException stale) {
				return stale;
			}
			if (cur.getCause() == cur) {
				break;
			}
		}
		return null;
	}
}
