/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An age-group save refused because it could change athletes of a session that is in progress on a platform. Nothing
 * was written.
 */
@SuppressWarnings("serial")
public class SessionsInProgressException extends RuntimeException {

	/** A session in progress and the platform that is running it. */
	public record SessionPlatform(String session, String platform) {
		@Override
		public String toString() {
			return session + " (" + platform + ")";
		}
	}

	private final List<SessionPlatform> conflicts;

	public SessionsInProgressException(List<SessionPlatform> conflicts) {
		super("sessions in progress: " + conflicts.stream().map(SessionPlatform::toString).collect(Collectors.joining(", ")));
		this.conflicts = List.copyOf(conflicts);
	}

	public List<SessionPlatform> getConflicts() {
		return this.conflicts;
	}
}
