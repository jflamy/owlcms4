/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Stage;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The status line shown by the OBS monitor of one platform (state, previous state, lift type, platform). Holds no
 * Vaadin objects; {@link OBSMonitor} publishes the titles as its window title.
 */
public class OBSMonitorStatus {

	static class Status {
		BreakType breakType;
		CeremonyType ceremonyType;
		Boolean decision;
		FOPState state;
		boolean challengedRecords;
		Stage liftType;

		Status(FOPState state, BreakType breakType, CeremonyType ceremonyType, Boolean decision,
		        boolean challengedRecords, Stage liftType) {
			this.state = state;
			this.breakType = breakType;
			this.ceremonyType = ceremonyType;
			this.decision = decision;
			this.challengedRecords = challengedRecords;
			this.liftType = liftType;
		}

		@Override
		public String toString() {
			return "Status [breakType=" + this.breakType + ", ceremonyType=" + this.ceremonyType + ", decision="
			        + this.decision
			        + ", state=" + this.state + ", challengedRecords=" + this.challengedRecords + ", liftType="
			        + this.liftType + "]";
		}
	}

	static final int HISTORY_SIZE = 3;

	private final Logger logger = (Logger) LoggerFactory.getLogger(OBSMonitor.class);
	private final FieldOfPlay fop;
	private final List<Status> history = new LinkedList<>();
	private BreakType currentBreakType;
	private CeremonyType currentCeremony;
	private Boolean currentDecision;
	private boolean currentChallengedRecords;
	private FOPState currentState;
	private Stage currentLiftType;
	private BreakType previousBreakType;
	private CeremonyType previousCeremony;
	private Boolean previousDecision;
	private boolean previousChallengedRecords;
	private FOPState previousState;
	private String prevTitle;
	private String title;
	private long expiryBeforeChangingStatus;

	/**
	 * The platform a monitor reports on: the one named by the {@code fop} parameter of its URL. Monitors for different
	 * platforms are normally open in the same browser, so nothing else may select it. Only when the competition has a
	 * single platform may the URL omit it.
	 *
	 * @param urlParameters the query parameters of the monitor's URL
	 * @param platforms     the registered platforms
	 * @return the platform to monitor, null if the URL must name one and does not name an existing one
	 */
	public static FieldOfPlay fopToMonitor(Map<String, List<String>> urlParameters,
	        Collection<FieldOfPlay> platforms) {
		List<String> names = urlParameters.get(FOPParameters.FOP);
		String name = names != null && !names.isEmpty() ? names.get(0) : null;
		if (name != null && !name.isBlank()) {
			FieldOfPlay named = byName(platforms, name);
			if (named == null) {
				try {
					// the name may still be URL-encoded
					named = byName(platforms, URLDecoder.decode(name, StandardCharsets.UTF_8));
				} catch (IllegalArgumentException e) {
					// not encoded
				}
			}
			if (named != null) {
				return named;
			}
		}
		return platforms.size() == 1 ? platforms.iterator().next() : null;
	}

	private static FieldOfPlay byName(Collection<FieldOfPlay> platforms, String name) {
		return platforms.stream().filter(p -> name.equals(p.getName())).findFirst().orElse(null);
	}

	public OBSMonitorStatus(FieldOfPlay fop) {
		this.logger.setLevel(Level.DEBUG);
		this.fop = fop;
		// we need two items on the stack (current + previous)
		doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
		doPush(new Status(FOPState.INACTIVE, null, null, null, false, null));
	}

	public FieldOfPlay getFop() {
		return this.fop;
	}

	/** The title computed last. */
	public synchronized String getTitle() {
		return this.title;
	}

	/**
	 * Synchronize with the platform when the monitor is shown.
	 *
	 * @return the title to publish, or null if it is the same as the one already published
	 */
	public synchronized String attach() {
		sync(null);
		return nextTitle();
	}

	/**
	 * @return the title to publish as a result of the event, or null if the event does not change it
	 */
	public synchronized String onEvent(UIEvent e) {
		if (e instanceof UIEvent.SetTime) {
			return null;
		}
		return sync(e) ? nextTitle() : null;
	}

	/** Milliseconds to wait before replacing a decision title, so that the decision stays visible. */
	public long waitBeforeChangingStatus() {
		return this.expiryBeforeChangingStatus - System.currentTimeMillis();
	}

	@Override
	public String toString() {
		return "OBSMonitorStatus [fop=" + this.fop.getName() + ", history=" + this.history + ", title="
		        + this.title + ", prevTitle=" + this.prevTitle + "]";
	}

	private String computePageTitle() {
		StringBuilder pageTitle = new StringBuilder();
		computeValues();

		if (this.currentState == FOPState.INACTIVE || this.currentState == FOPState.BREAK) {
			pageTitle.append("break=");
		} else {
			pageTitle.append("state=");
		}
		pageTitle.append(this.currentState.name());

		if (this.currentState == FOPState.BREAK && this.currentCeremony != null) {
			pageTitle.append(".");
			pageTitle.append(this.currentCeremony.name());
		} else if (this.currentState == FOPState.BREAK && this.currentBreakType != null) {
			pageTitle.append(".");
			pageTitle.append(this.currentBreakType.name());
		} else if (this.currentState == FOPState.DECISION_VISIBLE) {
			pageTitle.append(".");
			pageTitle.append(
			        this.currentDecision == null ? "UNDECIDED" : (this.currentDecision ? "GOOD_LIFT" : "BAD_LIFT"));
			if (Boolean.TRUE.equals(this.currentDecision) && this.currentChallengedRecords) {
				pageTitle.append(".NEW_RECORD");
			} else if (this.previousState == FOPState.DECISION_VISIBLE && Boolean.TRUE.equals(this.previousDecision)
			        && this.previousChallengedRecords) {
				// special case where state changes too quickly;
				pageTitle.append(".NEW_RECORD");
			}
			this.expiryBeforeChangingStatus = System.currentTimeMillis() + Competition.decisionVisibleDuration;
		} else if (this.currentChallengedRecords) {
			pageTitle.append(".RECORD_ATTEMPT");
		}

		pageTitle.append(";");
		pageTitle.append("previous=");
		pageTitle.append(this.previousState.name());
		if (this.previousState == FOPState.BREAK && this.previousCeremony != null) {
			pageTitle.append(".");
			pageTitle.append(this.previousCeremony.name());
		} else if (this.previousState == FOPState.BREAK && this.previousBreakType != null) {
			pageTitle.append(".");
			pageTitle.append(this.previousBreakType.name());
		} else if (this.previousState == FOPState.DECISION_VISIBLE) {
			pageTitle.append(".");
			pageTitle.append(
			        this.previousDecision == null ? "UNDECIDED" : (this.previousDecision ? "GOOD_LIFT" : "BAD_LIFT"));
		}

		if (this.currentLiftType != null) {
			pageTitle.append(";");
			pageTitle.append("liftType=");
			pageTitle.append(this.currentLiftType.toString());
		}
		pageTitle.append(";");
		pageTitle.append("fop=");
		pageTitle.append(this.fop.getName());

		return pageTitle.toString();
	}

	private void computeValues() {
		Status h0 = this.history.size() > 0 ? this.history.get(0) : null;
		Status h1 = this.history.size() > 1 ? this.history.get(1) : null;
		this.logger.debug("-- normal {} {}", h0, h1);
		this.currentState = h0 != null ? h0.state : null;
		this.currentBreakType = h0 != null ? h0.breakType : null;
		this.currentCeremony = h0 != null ? h0.ceremonyType : null;
		this.currentDecision = h0 != null ? h0.decision : null;
		this.currentChallengedRecords = h0 != null ? h0.challengedRecords : false;
		this.currentLiftType = h0 != null ? h0.liftType : null;

		this.previousState = h1 != null ? h1.state : null;
		this.previousBreakType = h1 != null ? h1.breakType : null;
		this.previousCeremony = h1 != null ? h1.ceremonyType : null;
		this.previousDecision = h1 != null ? h1.decision : null;
		this.previousChallengedRecords = h1 != null ? h1.challengedRecords : false;
	}

	private void doPush(Status status) {
		this.history.add(0, status);
		if (this.history.size() > HISTORY_SIZE) {
			this.history.remove(HISTORY_SIZE);
		}
	}

	private boolean isNotEmpty(List<RecordEvent> list) {
		return list != null && !list.isEmpty();
	}

	/**
	 * Compute the title; only a change in its state part (before the first ";") is worth publishing.
	 */
	private String nextTitle() {
		this.title = computePageTitle();
		String comparisonTitle = this.title.substring(0, this.title.indexOf(";"));
		String comparisonPrevTitle = this.prevTitle != null ? this.prevTitle.substring(0, this.prevTitle.indexOf(";"))
		        : null;
		this.logger.debug("comparing comparisonTitle={} with comparisonPrevTitle={}", comparisonTitle,
		        comparisonPrevTitle);
		if (comparisonTitle.isBlank() || comparisonTitle.equals(comparisonPrevTitle)) {
			this.logger.debug("---- monitor duplicate {}", this.title);
			return null;
		}
		this.prevTitle = this.title;
		return this.title;
	}

	/**
	 * The type of the lift the status is about.
	 * <ul>
	 * <li>A decision that follows an attempt is about the lift that was on the clock. The decision may be shown before
	 * the lift is recorded, so the athlete under review cannot be used yet.</li>
	 * <li>A jury or challenge break, and the decision that ends it, are about the last lift of the athlete under
	 * review. That athlete stays the same until the next decision, in particular through the break before clean and
	 * jerk.</li>
	 * <li>Otherwise, the next lift of the current athlete.</li>
	 * </ul>
	 */
	private static Stage liftStage(FieldOfPlay fop, Status latest) {
		FOPState state = fop.getState();
		BreakType breakType = fop.getBreakType();
		boolean attemptInProgress = latest.state == FOPState.TIME_RUNNING || latest.state == FOPState.TIME_STOPPED
		        || latest.state == FOPState.DOWN_SIGNAL_VISIBLE;
		if (state == FOPState.DECISION_VISIBLE && attemptInProgress && latest.liftType != null) {
			return latest.liftType;
		}
		boolean review = state == FOPState.BREAK
		        && (breakType == BreakType.JURY || breakType == BreakType.CHALLENGE);
		if (review || state == FOPState.DECISION_VISIBLE) {
			Athlete reviewed = fop.getAthleteUnderReview();
			int liftsDone = reviewed != null ? reviewed.getAttemptsDone() : 0;
			if (liftsDone > 0) {
				return liftsDone <= 3 ? Stage.SNATCH : Stage.CLEANJERK;
			}
		}
		return fop.getCurrentStage();
	}

	private boolean sync(UIEvent e) {
		FieldOfPlay fop = this.fop;
		Status now;
		// read under the platform's lock: one platform operation can pass through several states (for example the
		// next athlete, then the break before clean and jerk); only the state at the end of the operation is real
		Status latest = this.history.get(0);
		synchronized (fop) {
			now = new Status(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), fop.getGoodLift(),
			        isNotEmpty(fop.getChallengedRecords()), liftStage(fop, latest));
		}
		boolean newRecord = e instanceof UIEvent.JuryNotification jn && jn.getNewRecord();
		boolean stateChanged = now.state != latest.state;
		boolean recordsChanged = now.challengedRecords != latest.challengedRecords;
		this.logger.debug("{}>>>>>>OBSMonitor event {} fop {} history {} recordsChanged {}",
		        FieldOfPlay.getLoggingName(fop), e != null ? e.getClass().getSimpleName() : null, now.state,
		        latest.state, recordsChanged);
		boolean significant = false;
		if (e instanceof UIEvent.DecisionReset) {
			// this event does not change state, and should always be ignored. However, because it can occur very
			// close to the lifter update, and we have asynchronous events, there is a possibility that it comes
			// late and out of order. So we ignore it explicitly.
			this.logger.debug("{}>>>>>>OBSMonitor DecisionReset ignored", FieldOfPlay.getLoggingName(fop));
		} else if (stateChanged || recordsChanged) {
			now.challengedRecords = now.challengedRecords || newRecord;
			doPush(now);
			significant = true;
		} else if (now.state == FOPState.BREAK
		        && (now.breakType != latest.breakType || now.ceremonyType != latest.ceremonyType)) {
			now.decision = null;
			doPush(now);
			significant = true;
		}
		this.logger.debug("{}>>>>>>OBSMonitor sync significant {}", FieldOfPlay.getLoggingName(fop), significant);
		return significant;
	}
}
