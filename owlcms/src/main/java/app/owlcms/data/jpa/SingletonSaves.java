/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.util.Map;

import javax.persistence.CacheRetrieveMode;
import javax.persistence.EntityManager;

import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.StationResolver;

/**
 * Shared machinery for the stale-save check on the configuration singletons (Config, Competition, RecordConfig).
 *
 * Each singleton row carries the time of its last save and who made it. A form keeps the stamp it was loaded with; a
 * save made from a form whose stamp no longer matches the row is refused. Comparing the stamps, writing the row and
 * publishing the saved instance happen under {@link #LOCK}, so an older saved object can never replace a newer one in
 * memory. Nothing done under the lock may wait on a UI, the network or another lock.
 */
public final class SingletonSaves {

	/** One lock for the three singletons: the Competition page saves Competition and a Config switch together. */
	public static final Object LOCK = new Object();

	private static final int SAVED_BY_LENGTH = 255;
	private static final Map<String, Object> BYPASS_CACHE = Map.of("javax.persistence.cache.retrieveMode",
	        CacheRetrieveMode.BYPASS);

	private SingletonSaves() {
	}

	/** Reads the row from the database, not from the second-level cache. */
	public static <T> T findFresh(EntityManager em, Class<T> type, Object id) {
		return id == null ? null : em.find(type, id, BYPASS_CACHE);
	}

	/** Strictly increasing: two saves in the same millisecond still get different stamps. */
	public static long nextStamp(Long previous) {
		long now = System.currentTimeMillis();
		return previous == null ? now : Math.max(now, previous + 1);
	}

	/**
	 * Who saved, as shown in the stale-save message: never the session fingerprint. Null when nobody can be named
	 * for sure: in PIN mode there is no person, so only a station that is certain is named (declared explicitly,
	 * or from a page listed by exact class). A station guessed from the page's package is not named; the message
	 * then says only that the settings were changed.
	 */
	public static String savedByLabel(AuditOrigin origin) {
		if (origin == null) {
			return "system";
		}
		AuditActor actor = origin.actor();
		String label;
		if (isSet(actor.user())) {
			label = isSet(actor.station()) && !"SYSTEM".equals(actor.station())
			        ? actor.user() + "@" + actor.displayStation()
			        : actor.user();
		} else if (!isSet(actor.station()) || (actor.inferred() && !StationResolver.isExactPage(actor.station()))) {
			return null;
		} else if ("SYSTEM".equals(actor.station())) {
			label = "system";
		} else {
			label = actor.displayStation();
		}
		return label.length() > SAVED_BY_LENGTH ? label.substring(0, SAVED_BY_LENGTH) : label;
	}

	private static boolean isSet(String value) {
		return value != null && !value.isBlank() && !"-".equals(value);
	}
}
