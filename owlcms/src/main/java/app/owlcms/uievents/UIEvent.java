/*******************************************************************************
 * Copyright (category) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPError;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.fieldofplay.TimingPolicy;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * UIEvents are triggered in response to field of play events (FOPEvents). Each field of play has an associated uiEventBus on which the user interface commands
 * are posted. The various browsers subscribe to UIEvents and react accordingly.
 *
 * @author owlcms
 */

public class UIEvent {

	static public class ButtonTest extends UIEvent {
		private FOPEvent fopEvent;
		public String format;
		public Object[] infos;

		public ButtonTest(FOPEvent e, FieldOfPlay fop,  Object origin, String format, Object... infos) {
			super(origin, fop);
			this.setFopEvent(e);
			this.format = format;
			this.infos = infos;
		}

		public FOPEvent getFopEvent() {
			return fopEvent;
		}

		public void setFopEvent(FOPEvent fopEvent) {
			this.fopEvent = fopEvent;
		}
		
	}

	static public class BarbellOrPlatesChanged extends UIEvent {
		public BarbellOrPlatesChanged(Object object, FieldOfPlay fop) {
			super(object, fop);
		}
	}

	/**
	 * Class BreakDone.
	 */
	static public class BreakDone extends UIEvent {

		private BreakType breakType;

		/**
		 * Instantiates a new break done.
		 *
		 * @param origin    the origin
		 * @param breakType
		 * @param fop       originating field of play
		 */
		public BreakDone(Object origin, BreakType breakType, FieldOfPlay fop) {
			super(origin, fop);
			this.setBreakType(breakType);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public BreakType getBreakType() {
			return this.breakType;
		}

		public void setBreakType(BreakType breakType) {
			this.breakType = breakType;
		}
	}

	/**
	 * Class BreakPaused.
	 */
	static public class BreakPaused extends UIEvent {

		protected BreakType breakType;
		protected CountdownType countdownType;
		protected LocalDateTime end;
		protected boolean indefinite;
		protected Integer timeRemaining;
		private boolean displayToggle;

		public BreakPaused(Integer millisRemaining, Object origin, boolean displayToggle, BreakType bt,
		        CountdownType ct, FieldOfPlay fop) {
			super(origin, fop);
			this.timeRemaining = millisRemaining;
			this.indefinite = (ct != null && ct == CountdownType.INDEFINITE) || (millisRemaining == null);
			this.breakType = bt;
			this.countdownType = ct;
			this.setDisplayToggle(displayToggle);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public BreakType getBreakType() {
			return this.breakType;
		}

		public int getMillis() {
			return (this.timeRemaining != null ? this.timeRemaining : 0);
		}

		public Integer getTimeRemaining() {
			return this.timeRemaining;
		}

		/**
		 * @return true if is a request for toggling display (and not an actual break start)
		 */
		public boolean isDisplayToggle() {
			return this.displayToggle;
		}

		/**
		 * @return true if break lasts indefinitely and timeRemaining should be ignored
		 */
		public boolean isIndefinite() {
			return this.indefinite;
		}

		/**
		 * @param displayToggle true to request switching to Break Timer
		 */
		public void setDisplayToggle(boolean displayToggle) {
			this.displayToggle = displayToggle;
		}

		@Override
		public String toString() {
			return "UIEvent.BreakPaused [displayToggle=" + this.displayToggle + ", timeRemaining=" + this.timeRemaining
			        + ", indefinite=" + this.indefinite + ", end=" + this.end + ", breakType=" + this.breakType
			        + ", countdownType="
			        + this.countdownType + "]";
		}

	}

	/**
	 * Class BreakSetTime
	 */
	static public class BreakSetTime extends UIEvent {

		protected BreakType breakType;
		protected CountdownType countdownType;
		protected LocalDateTime end;
		protected boolean indefinite;
		protected Integer timeRemaining;

		/**
		 * DURATION break
		 *
		 * @param bt
		 * @param ct
		 * @param timeRemaining
		 * @param indefinite
		 * @param origin
		 * @param trace
		 * @param fop           originating field of play
		 */
		public BreakSetTime(BreakType bt, CountdownType ct, Integer timeRemaining, LocalDateTime end,
		        boolean indefinite, Object origin, String trace, FieldOfPlay fop) {
			super(origin, fop);
			this.timeRemaining = timeRemaining;
			this.indefinite = indefinite;
			this.end = end;
			this.breakType = bt;
			this.countdownType = ct;
			this.trace = trace;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public BreakType getBreakType() {
			return this.breakType;
		}

		public LocalDateTime getEnd() {
			return this.end;
		}

		public Integer getTimeRemaining() {
			return this.timeRemaining;
		}

		/**
		 * @return true if break lasts indefinitely and timeRemaining should be ignored
		 */
		public boolean isIndefinite() {
			return this.indefinite;
		}
	}

	/**
	 * Class BreakStarted.
	 */
	// MUST NOT EXTEND otherwise subscription triggers on supertype as well
	static public class BreakStarted extends UIEvent {

		protected BreakType breakType;
		protected CountdownType countdownType;
		protected LocalDateTime end;
		protected boolean indefinite;
		protected Integer timeRemaining;
		private boolean displayToggle;
		private Boolean paused;

		public BreakStarted(Integer millisRemaining, Object origin, boolean displayToggle, BreakType bt,
		        CountdownType ct, String trace, Boolean paused, FieldOfPlay fop) {
			super(origin, fop);
			this.timeRemaining = millisRemaining;
			this.indefinite = (ct != null && ct == CountdownType.INDEFINITE) || (millisRemaining == null);
			setBreakType(bt);
			this.countdownType = ct;
			this.setDisplayToggle(displayToggle);
			this.setPaused(paused);
			this.trace = trace;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public BreakType getBreakType() {
			// logger.debug("BreakStarted getBreakType {}",breakType);
			return this.breakType;
		}

		public int getMillis() {
			return (getTimeRemaining());
		}

		public Boolean getPaused() {
			return this.paused;
		}

		public Integer getTimeRemaining() {
			return this.timeRemaining;
		}

		/**
		 * @return true if is a request for toggling display (and not an actual break start)
		 */
		public boolean isDisplayToggle() {
			return this.displayToggle;
		}

		/**
		 * @return true if break lasts indefinitely and timeRemaining should be ignored
		 */
		public boolean isIndefinite() {
			return this.indefinite;
		}

		public final void setBreakType(BreakType breakType) {
			// logger.debug("BreakStarted getBreakType {}",breakType);
			this.breakType = breakType;
		}

		/**
		 * @param displayToggle true to request switching to Break Timer
		 */
		public void setDisplayToggle(boolean displayToggle) {
			this.displayToggle = displayToggle;
		}

		public void setPaused(Boolean paused) {
			this.paused = paused;
		}

		@Override
		public String toString() {
			return "UIEvent.BreakStarted [displayToggle=" + this.displayToggle + ", timeRemaining=" + this.timeRemaining
			        + ", indefinite=" + this.indefinite + ", end=" + this.end + ", breakType=" + this.breakType
			        + ", countdownType="
			        + this.countdownType + "]";
		}

	}

	static public class Broadcast extends UIEvent {

		private String message;

		public Broadcast(String string, Object origin, FieldOfPlay fop) {
			super(origin, fop);
			this.setMessage(string);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public String getMessage() {
			return this.message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

	/**
	 * Class CeremonyDone.
	 */
	static public class CeremonyDone extends UIEvent {

		private CeremonyType ceremonyType;

		/**
		 * Instantiates a new break done.
		 *
		 * @param origin    the origin
		 * @param fop       originating field of play
		 * @param breakType
		 */
		public CeremonyDone(CeremonyType ceremonyType, Object origin, FieldOfPlay fop) {
			super(origin, fop);
			this.setCeremonyType(ceremonyType);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public CeremonyType getCeremonyType() {
			return this.ceremonyType;
		}

		public void setCeremonyType(CeremonyType ceremonyType) {
			this.ceremonyType = ceremonyType;
		}

	}

	/**
	 * Class BreakStarted.
	 */
	static public class CeremonyStarted extends UIEvent {

		private Category ceremonyCategory;
		private Group ceremonySession;
		private CeremonyType ceremonyType;
		private Championship championship;
		private AgeGroup ageGroup;

		public CeremonyStarted(CeremonyType ceremonyType, Group ceremonySession, Category ceremonyCategory, String trace,
		        Object origin, FieldOfPlay fop) {
			super(origin, fop);
			this.setCeremonyType(ceremonyType);
			this.setCeremonySession(ceremonySession);
			this.setCeremonyCategory(ceremonyCategory);
			AgeGroup ageGroup = ceremonyCategory != null ? ceremonyCategory.getAgeGroup() : null;
			this.setCeremonyAgeGroup(ageGroup);
			this.setCeremonyChampionship(ageGroup != null ? ageGroup.getChampionship() : null);
			this.trace = trace;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if ((obj == null) || (getClass() != obj.getClass())) {
				return false;
			}
			CeremonyStarted other = (CeremonyStarted) obj;
			return Objects.equals(this.ceremonyCategory, other.ceremonyCategory)
			        && Objects.equals(this.ceremonySession, other.ceremonySession)
			        && this.ceremonyType == other.ceremonyType;
		}

		public AgeGroup getAgeGroup() {
			return this.ageGroup;
		}

		public Category getCeremonyCategory() {
			return this.ceremonyCategory;
		}

		public Group getCeremonySession() {
			return this.ceremonySession;
		}

		public CeremonyType getCeremonyType() {
			return this.ceremonyType;
		}

		public Championship getChampionship() {
			return this.championship;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.ceremonyCategory, this.ceremonySession, this.ceremonyType);
		}

		public void setAgeGroup(AgeGroup ageGroup) {
			this.ageGroup = ageGroup;
		}

		public void setCeremonySession(Group ceremonyGroup2) {
			this.ceremonySession = ceremonyGroup2;
		}

		public void setCeremonyType(CeremonyType ceremonyType) {
			this.ceremonyType = ceremonyType;
		}

		public void setChampionship(Championship championship) {
			this.championship = championship;
		}

		@Override
		public String toString() {
			return "CeremonyStarted [ceremonyType=" + this.ceremonyType + ", ceremonyCategory=" + this.ceremonyCategory
			        + ", ceremonySession=" + this.ceremonySession + "]";
		}

		private void setCeremonyAgeGroup(AgeGroup ageGroup) {
			this.setAgeGroup(ageGroup);
		}

		private void setCeremonyCategory(Category ceremonyCategory2) {
			this.ceremonyCategory = ceremonyCategory2;
		}

		private void setCeremonyChampionship(Championship championship) {
			this.championship = championship;
		}
	}

	/**
	 * Class ExplicitDecision.
	 */
	static public class Decision extends UIEvent {

		/** decision. */
		public Boolean decision = null;
		/** ref 1. */
		public Boolean ref1;
		/** ref 2. */
		public Boolean ref2;
		/** ref 3. */
		public Boolean ref3;
		private boolean singleLight;
		private TimingPolicy timingPolicy;
		private InputKind inputKind;

		/**
		 * Instantiates a new referee decision.
		 *
		 * @param decision the decision
		 * @param ref1     the ref 1
		 * @param ref2     the ref 2
		 * @param ref3     the ref 3
		 * @param origin   the origin
		 * @param singleLight 
		 */
		public Decision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, Object origin, FieldOfPlay fop, boolean singleLight) {
			super(a, origin, fop);
			this.decision = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
			this.setTrace(() -> LoggerUtils.stackTrace());
			this.setSingleLight(singleLight);
			if (isSingleLight()) {
				if (this.ref1 != null) {
					this.ref2 = this.ref1;
					this.ref1 = null;
				} else if (this.ref3 != null) {
					this.ref2 = this.ref3;
					this.ref3 = null;
				}
			}
		}

		public Decision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
		        Object origin, FieldOfPlay fop, boolean singleLight, TimingPolicy timingPolicy, InputKind inputKind) {
			this(a, decision, ref1, ref2, ref3, origin, fop, singleLight);
			this.timingPolicy = timingPolicy;
			this.inputKind = inputKind;
		}

		public InputKind getInputKind() {
			return this.inputKind;
		}

		public boolean isSingleLight() {
			return this.singleLight;
		}

		public TimingPolicy getTimingPolicy() {
			return this.timingPolicy;
		}

		public void setSingleLight(boolean singleLight) {
			this.singleLight = singleLight;
		}
	}

	static public class InitialDecision extends UIEvent {

		public Boolean decision;
		public Boolean ref1;
		public Boolean ref2;
		public Boolean ref3;
		private boolean singleLight;
		private TimingPolicy timingPolicy;
		private InputKind inputKind;

		public InitialDecision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
		        Object origin, FieldOfPlay fop, boolean singleLight) {
			super(a, origin, fop);
			this.decision = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
			this.singleLight = singleLight;
			this.setTrace(() -> LoggerUtils.stackTrace());
		}

		public InitialDecision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
		        Object origin, FieldOfPlay fop, boolean singleLight, TimingPolicy timingPolicy, InputKind inputKind) {
			this(a, decision, ref1, ref2, ref3, origin, fop, singleLight);
			this.timingPolicy = timingPolicy;
			this.inputKind = inputKind;
		}

		public InputKind getInputKind() {
			return this.inputKind;
		}

		public boolean isSingleLight() {
			return this.singleLight;
		}

		public TimingPolicy getTimingPolicy() {
			return this.timingPolicy;
		}
	}

	/**
	 * Class DecisionReset.
	 */
	static public class DecisionReset extends UIEvent {

		/**
		 * Instantiates a new decision reset.
		 *
		 * @param origin the origin
		 */
		public DecisionReset(Athlete a, Object origin, FieldOfPlay fop) {
			super(a, origin, fop);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}
	}

	/**
	 * Class DownSignal.
	 */
	static public class DownSignal extends UIEvent {

		/**
		 * Instantiates a new down signal.
		 *
		 * @param origin the origin
		 * @param fop    originating field of play
		 */
		public DownSignal(Object origin, FieldOfPlay fop) {
			super(origin, fop);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}
	}

	static public class GlobalRankingUpdated extends UIEvent {
		public GlobalRankingUpdated(Object object, FieldOfPlay fop) {
			super(object, fop);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}
	}

	static public class GroupDone extends UIEvent {

		private Group group;

		/**
		 * Instantiates a new athlete announced.
		 *
		 * @param ui      the ui
		 * @param fop     originating field of play
		 * @param athlete the athlete
		 */
		public GroupDone(Group group, UI ui, String stackTrace, FieldOfPlay fop) {
			super(ui, fop);
			this.setGroup(group);
			this.trace = stackTrace;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Group getGroup() {
			return this.group;
		}

		public void setGroup(Group group) {
			this.group = group;
		}
	}

	static public class JuryNotification extends UIEvent {

		private JuryDeliberationEventType deliberationEventType;
		private Boolean newRecord;
		private Boolean reversal;
		private boolean waitForAnnouncer = false;
		private Integer actualLift;
		private Integer reasonCode;
		private boolean panelReview;
		private boolean chairEntered;
		private boolean speakerEntered;
		private boolean panelDecision;
		/** referees' review votes when the decision was applied (null cells when not voted) */
		private Boolean[] reviewRefereeVotes;
		/** jurors' votes when the decision was applied (null cells when not voted) */
		private Boolean[] reviewJuryVotes;

		public JuryNotification(Athlete athleteUnderReview, Object origin,
		        JuryDeliberationEventType deliberationEventType, Boolean reversal, Boolean newRecord,
		        boolean waitForAnnouncer, FieldOfPlay fop, Integer actualLift) {
			super(athleteUnderReview, origin, fop);
			this.setDeliberationEventType(deliberationEventType);
			this.setReversal(reversal);
			this.setNewRecord(newRecord != null && newRecord);
			this.setTrace(() -> LoggerUtils.stackTrace());
			this.waitForAnnouncer = waitForAnnouncer;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
			this.setActualLift(actualLift);
			this.logger.trace("JuryNotification wait {} newRecord {} {}", waitForAnnouncer, getNewRecord(), getTrace());
		}

		/**
		 * Instantiates a new Notification.
		 *
		 * @param origin the origin
		 * @param fop    originating field of play
		 */
		public JuryNotification(Athlete a, Object origin, String notificationString, String fopEventString, FieldOfPlay fop) {
			super(a, origin, fop);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
			this.logger.trace("JuryNotification notificationString {} {}", notificationString, getTrace());
		}

		public Integer getActualLift() {
			return this.actualLift;
		}

		/** @return the jury's No Lift reason code, null if none was given */
		public Integer getReasonCode() {
			return this.reasonCode;
		}

		public void setReasonCode(Integer reasonCode) {
			this.reasonCode = reasonCode;
		}

		/**
		 * @return the deliberationEventType
		 */
		public JuryDeliberationEventType getDeliberationEventType() {
			return this.deliberationEventType;
		}

		public boolean getNewRecord() {
			return this.newRecord;
		}

		/**
		 * @return the reversal
		 */
		public Boolean getReversal() {
			return this.reversal;
		}

		public boolean isWaitForAnnouncer() {
			return this.waitForAnnouncer;
		}

		public void setActualLift(Integer actualLift) {
			this.actualLift = actualLift;
		}

		/**
		 * @param deliberationEventType the deliberationEventType to set
		 */
		public void setDeliberationEventType(JuryDeliberationEventType deliberationEventType) {
			this.deliberationEventType = deliberationEventType;
		}

		/**
		 * @param reversal the reversal to set
		 */
		public void setReversal(Boolean reversal) {
			this.reversal = reversal;
		}

		public void setWaitForAnnouncer(boolean waitForAnnouncer) {
			this.waitForAnnouncer = waitForAnnouncer;
		}

		/**
		 * @return true if this START_DELIBERATION was started automatically by the seven-person Adjudication Panel
		 *         (the panel majority differs from the referees' decision)
		 */
		public boolean isPanelReview() {
			return this.panelReview;
		}

		public void setPanelReview(boolean panelReview) {
			this.panelReview = panelReview;
		}

		/**
		 * @return the translation key announcing this START_DELIBERATION / CHALLENGE: the automatic Adjudication Panel
		 *         Review, a deliberation by the panel (four-member jury with VPT) or by the jury
		 */
		public String getDeliberationStartedKey() {
			if (this.deliberationEventType == null) {
				return "JuryNotification.START_DELIBERATION";
			}
			if (this.deliberationEventType == JuryDeliberationEventType.START_DELIBERATION) {
				if (this.panelReview) {
					return "JuryNotification.PanelReview";
				}
				if (getFop() != null && getFop().isPanelReviewing()) {
					return "JuryNotification.PanelDeliberationStarted";
				}
			}
			return "JuryNotification." + this.deliberationEventType.name();
		}

		/**
		 * @return true if the Jury Chair entered this GOOD_LIFT/BAD_LIFT decision instead of the seven-person panel vote
		 */
		public boolean isChairEntered() {
			return this.chairEntered;
		}

		public void setChairEntered(boolean chairEntered) {
			this.chairEntered = chairEntered;
		}

		/**
		 * @return true if this GOOD_LIFT/BAD_LIFT decision was entered by the Speaker (as relayed by the Technical
		 *         Controller), not by the jury
		 */
		public boolean isSpeakerEntered() {
			return this.speakerEntered;
		}

		public void setSpeakerEntered(boolean speakerEntered) {
			this.speakerEntered = speakerEntered;
		}

		/**
		 * @return true if this GOOD_LIFT/BAD_LIFT decision was reached under the seven-person Adjudication Panel
		 *         (four-member jury with VPT: the referees vote again), false if the jury alone decided
		 */
		public boolean isPanelDecision() {
			return this.panelDecision;
		}

		public void setPanelDecision(boolean panelDecision) {
			this.panelDecision = panelDecision;
		}

		/**
		 * @return the translation key for the "reversal" / "confirmation" status, worded for the panel or the jury
		 *         that decided; null when the decision is neither
		 */
		public String getReversalStatusKey() {
			if (this.reversal == null) {
				return null;
			}
			if (this.panelDecision) {
				return this.reversal ? "JuryNotification.PanelReversal" : "JuryNotification.PanelConfirmed";
			}
			return this.reversal ? "JuryNotification.Reversal" : "JuryNotification.Confirmed";
		}

		/** @return the referees' review votes when the decision was applied, null if not recorded */
		public Boolean[] getReviewRefereeVotes() {
			return this.reviewRefereeVotes;
		}

		/** @return the jurors' votes when the decision was applied, null if not recorded */
		public Boolean[] getReviewJuryVotes() {
			return this.reviewJuryVotes;
		}

		/**
		 * Record the votes as they stand when the decision is applied, so that listeners running later (asynchronous
		 * buses) do not read votes the FOP has since cleared.
		 */
		public void setReviewVotes(Boolean[] refereeVotes, Boolean[] juryVotes) {
			this.reviewRefereeVotes = refereeVotes != null ? refereeVotes.clone() : null;
			this.reviewJuryVotes = juryVotes != null ? juryVotes.clone() : null;
		}

		private void setNewRecord(Boolean newRecord) {
			this.newRecord = newRecord;
		}

	}

	/**
	 * The clock start was refused because the jury has not finished voting on the previous lift (seven-person
	 * Adjudication Panel). The deciding console offers to start the clock anyway; the other consoles show a
	 * notification. Sent again with pending == false when the jury votes are complete or abandoned.
	 */
	static public class JuryVotesPending extends UIEvent {

		private final boolean pending;

		public JuryVotesPending(Athlete athleteUnderReview, boolean pending, Object origin, FieldOfPlay fop) {
			super(athleteUnderReview, origin, fop);
			this.pending = pending;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public boolean isPending() {
			return this.pending;
		}

	}

	/**
	 * An explicit Jury Control Resume command cleared the jury cycle. Jury displays and physical jury devices clear
	 * their referee and jury decisions immediately.
	 */
	static public class JuryDisplayReset extends UIEvent {

		public JuryDisplayReset(Object origin, FieldOfPlay fop) {
			super(fop != null ? fop.getAthleteUnderReview() : null, origin, fop);
		}

	}

	/** The number of jurors on the platform has changed: the jury pages adapt their layout. */
	static public class JurySizeChanged extends UIEvent {

		public JurySizeChanged(Object origin, FieldOfPlay fop) {
			super(origin, fop);
		}

	}

	/**
	 * Traditional 3-5 member jury: the jury decision is a No Lift, so the Jury Chair must give the technical rule
	 * before the decision is sent.
	 */
	static public class JuryRuleRequired extends UIEvent {

		private boolean forVote;
		private boolean cancelled;

		public JuryRuleRequired(Athlete athleteUnderReview, Object origin, FieldOfPlay fop) {
			super(athleteUnderReview, origin, fop);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		/**
		 * @param forVote   true when the rule is asked for the No Lift result of a jury vote (cancelling then
		 *                  cancels the vote result), false when the Jury Chair pressed No Lift
		 * @param cancelled true when the request is withdrawn
		 */
		public JuryRuleRequired(Athlete athleteUnderReview, boolean forVote, boolean cancelled, Object origin,
		        FieldOfPlay fop) {
			this(athleteUnderReview, origin, fop);
			this.forVote = forVote;
			this.cancelled = cancelled;
		}

		public boolean isForVote() {
			return this.forVote;
		}

		public boolean isCancelled() {
			return this.cancelled;
		}

	}

	/**
	 * Seven-person Adjudication Panel review vote: referee devices must re-arm and request a decision. ref 0 means
	 * all referees (a new vote, all votes cleared); 1..3 re-calls one referee who has not voted, without clearing.
	 */
	static public class PanelVoteCalled extends UIEvent {

		private final int ref;

		public PanelVoteCalled(int ref, Object origin, FieldOfPlay fop) {
			super(origin, fop);
			this.ref = ref;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public int getRef() {
			return this.ref;
		}

	}

	/**
	 * Status of the seven-person Adjudication Panel review vote. Until all seven officials have voted, only who has
	 * voted is published; the votes and the majority are published together when the vote is complete.
	 */
	static public class PanelVoteUpdate extends UIEvent {

		private final boolean open;
		private final boolean[] voted;
		private final Boolean[] votes;
		private final Boolean majority;
		private final boolean publicReveal;
		private final Integer refereeUpdated;

		/**
		 * @param open     true while the vote is open
		 * @param voted    who has voted: referees 1..3 then jury members 1..4
		 * @param votes    the seven votes in the same order, null until all seven are in
		 * @param majority the panel majority, null until all seven are in
		 */
		public PanelVoteUpdate(boolean open, boolean[] voted, Boolean[] votes, Boolean majority, Object origin,
		        FieldOfPlay fop) {
			this(open, voted, votes, majority, false, null, origin, fop);
		}

		/**
		 * @param publicReveal  true when the decision may be revealed on public boards
		 * @param refereeUpdated zero-based referee index whose review vote was just received, null otherwise
		 */
		public PanelVoteUpdate(boolean open, boolean[] voted, Boolean[] votes, Boolean majority,
		        boolean publicReveal, Integer refereeUpdated, Object origin, FieldOfPlay fop) {
			super(origin, fop);
			this.open = open;
			this.voted = voted;
			this.votes = votes;
			this.majority = majority;
			this.publicReveal = publicReveal;
			this.refereeUpdated = refereeUpdated;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Boolean getMajority() {
			return this.majority;
		}

		public boolean[] getVoted() {
			return this.voted;
		}

		public Boolean[] getVotes() {
			return this.votes;
		}

		public Integer getRefereeUpdated() {
			return this.refereeUpdated;
		}

		public boolean isComplete() {
			return this.votes != null;
		}

		public boolean isOpen() {
			return this.open;
		}

		public boolean isPublicReveal() {
			return this.publicReveal;
		}
	}

	static public class JuryUpdate extends UIEvent {

		private Boolean collective;
		private Boolean[] juryMemberDecision;
		private int jurySize;
		private Integer juryMemberUpdated;
		private boolean reviewVote;

		public JuryUpdate(Object origin, boolean collective, Boolean[] juryMemberDecision, int jurySize, FieldOfPlay fop) {
			super(origin, fop);
			this.collective = collective;
			// snapshot: the FOP keeps updating its array while this event is delivered asynchronously
			this.juryMemberDecision = juryMemberDecision == null ? null : juryMemberDecision.clone();
			this.jurySize = jurySize;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public JuryUpdate(Object origin, int i, Boolean[] juryMemberDecision2, int jurySize, FieldOfPlay fop) {
			super(origin, fop);
			this.collective = null;
			this.juryMemberUpdated = i;
			this.juryMemberDecision = juryMemberDecision2 == null ? null : juryMemberDecision2.clone();
			this.jurySize = jurySize;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		/**
		 * @return the collective
		 */
		public Boolean getCollective() {
			return this.collective;
		}

		/**
		 * @return the juryMemberDecision
		 */
		public Boolean[] getJuryMemberDecision() {
			return this.juryMemberDecision;
		}

		/**
		 * @return the juryMemberUpdated
		 */
		public Integer getJuryMemberUpdated() {
			return this.juryMemberUpdated;
		}

		/**
		 * @return true for a vote of the review vote: for the jury displays and devices only, public boards show the
		 *         review vote from {@link PanelVoteUpdate} (revealed with the decision)
		 */
		public boolean isReviewVote() {
			return this.reviewVote;
		}

		public void setReviewVote(boolean reviewVote) {
			this.reviewVote = reviewVote;
		}

		/**
		 * @return the jurySize
		 */
		public int getJurySize() {
			return this.jurySize;
		}

	}

	/**
	 * Class LiftingOrderUpdated.
	 */
	static public class LiftingOrderUpdated extends UIEvent {

		private Athlete changingAthlete;
		private boolean currentDisplayAffected;
		private List<Athlete> displayOrder;
		private boolean displayToggle;
		private boolean inBreak;
		private List<Athlete> liftingOrder;
		private Athlete nextAthlete;
		private Athlete previousAthlete;
		private Integer timeAllowed;
		private Integer newWeight;
		private boolean timerShouldRun;
		private int timerMillisRemaining;
		private boolean timerStateValid;

		/**
		 * Instantiates a new lifting order updated command.
		 *
		 * @param athlete         the current athlete after recalculation
		 * @param nextAthlete     the next athlete that will lift (cannot be the same as athlete)
		 * @param previousAthlete the last athlete to have lifted (can be the same as athlete)
		 * @param changingAthlete the athlete who triggered the lifting update
		 * @param liftingOrder    the lifting order
		 * @param displayOrder    the display order
		 * @param timeAllowed     the time allowed
		 * @param displayToggle   if true, just update display according to lifting order.
		 * @param origin          the origin
		 * @param newWeight       newly requested weight, null if no change from previous
		 * @param fop             originating field of play
		 */
		public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete,
		        Athlete changingAthlete, List<Athlete> liftingOrder, List<Athlete> displayOrder, Integer timeAllowed,
		        boolean currentDisplayAffected, boolean displayToggle, Object origin, boolean inBreak,
		        Integer newWeight, FieldOfPlay fop) {
			super(athlete, origin, fop);
			this.setTrace(() -> LoggerUtils.stackTrace());
			this.nextAthlete = nextAthlete;
			this.previousAthlete = previousAthlete;
			this.changingAthlete = changingAthlete;
			this.timeAllowed = timeAllowed;
			this.liftingOrder = liftingOrder;
			this.displayOrder = displayOrder;
			this.setCurrentDisplayAffected(currentDisplayAffected);
			this.setDisplayToggle(displayToggle);
			this.setInBreak(inBreak);
			this.setNewWeight(newWeight);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Athlete getChangingAthlete() {
			return this.changingAthlete;
		}

		/**
		 * Gets the display order.
		 *
		 * @return the display order
		 */
		public List<Athlete> getDisplayOrder() {
			return this.displayOrder;
		}

		/**
		 * Gets the lifting order.
		 *
		 * @return the lifting order
		 */
		public List<Athlete> getLiftingOrder() {
			return this.liftingOrder;
		}

		public Integer getNewWeight() {
			return this.newWeight;
		}

		/**
		 * Authoritative timer truth captured on the FOP thread when this recompute was pushed: should
		 * the athlete clock be running ({@code true}) or stopped ({@code false})? Used by clients to
		 * re-assert the correct timer state when a {@code StartTime}/{@code StopTime} was reordered,
		 * dropped, or never emitted (see reason 5 in doWeightChange).
		 *
		 * @return true if the athlete clock should be running
		 */
		public boolean isTimerShouldRun() {
			return this.timerShouldRun;
		}

		public void setTimerShouldRun(boolean timerShouldRun) {
			this.timerShouldRun = timerShouldRun;
		}

		/**
		 * @return the authoritative milliseconds remaining on the athlete clock at the time of this recompute
		 */
		public int getTimerMillisRemaining() {
			return this.timerMillisRemaining;
		}

		public void setTimerMillisRemaining(int timerMillisRemaining) {
			this.timerMillisRemaining = timerMillisRemaining;
		}

		/**
		 * @return true if the timer truth carried by this event is meaningful (false during a break, when
		 *         clients must not re-assert the athlete clock)
		 */
		public boolean isTimerStateValid() {
			return this.timerStateValid;
		}

		public void setTimerStateValid(boolean timerStateValid) {
			this.timerStateValid = timerStateValid;
		}

		/**
		 * Gets the next athlete.
		 *
		 * @return the next athlete
		 */
		public Athlete getNextAthlete() {
			return this.nextAthlete;
		}

		/**
		 * Gets the previous athlete.
		 *
		 * @return the previous athlete
		 */
		public Athlete getPreviousAthlete() {
			return this.previousAthlete;
		}

		/**
		 * Gets the time allowed.
		 *
		 * @return the timeAllowed
		 */
		public Integer getTimeAllowed() {
			return this.timeAllowed;
		}

		/**
		 * @return true if the current event requires to stop the timer
		 */
		public boolean isCurrentDisplayAffected() {
			return this.currentDisplayAffected;
		}

		public boolean isDisplayToggle() {
			return this.displayToggle;
		}

		public boolean isInBreak() {
			return this.inBreak;
		}

		public void setDisplayToggle(boolean displayToggle) {
			this.displayToggle = displayToggle;
		}

		public void setInBreak(boolean inBreak) {
			this.inBreak = inBreak;
		}

		public void setNewWeight(Integer newWeight) {
			this.newWeight = newWeight;
		}

		private void setCurrentDisplayAffected(boolean currentDisplayAffected) {
			this.currentDisplayAffected = currentDisplayAffected;
		}

	}

	/**
	 * Class Notification.
	 */
	static public class Notification extends UIEvent {

		public enum Level {
			ERROR, WARNING, SUCCESS, INFO;
		}

		public static final int NORMAL_DURATION = 3000;
		private String fopEventString;
		private String notificationString;
		private Level level;
		private String[] infos;
		private Integer msDuration;

		public Notification(Athlete curAthlete, Object origin, FOPEvent e, FOPState state, Notification.Level level, FieldOfPlay fop) {
			super(curAthlete, origin, fop);
			this.setFopEventString(e.getClass().getSimpleName());
			this.setNotificationString(state.toString());
			this.level = level;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		/**
		 * Instantiates a new Notification.
		 *
		 * @param origin the origin
		 */
		public Notification(
		        Athlete a,
		        Object origin,
		        Notification.Level level,
		        String notificationString,
		        Integer msDuration,
		        FieldOfPlay fop,
		        String... infos) {
			super(a, origin, fop);
			this.setNotificationString(notificationString);
			this.setFopEventString(this.fopEventString);
			this.setLevel(level);
			this.setInfos(infos);
			this.setMsDuration(msDuration);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public com.vaadin.flow.component.notification.Notification doNotification() {
			com.vaadin.flow.component.notification.Notification n = new com.vaadin.flow.component.notification.Notification();
			Div div = new Div();
			String close = "\u00A0\u00A0\u00A0\u2715";
			div.addClickListener(click -> n.close());
			if (getFopEventString() != null && !getFopEventString().isEmpty()) {
				div.setText(FOPError.translateMessage(getNotificationString(), getFopEventString()) + close);
			} else {
				div.getElement().setProperty("innerHTML", Translator.translate(getNotificationString(), (Object[]) getInfos()) + close);
			}
			div.getStyle().set("font-size", "large");
			n.add(div);

			switch (getLevel()) {
				case ERROR:
					n.setPosition(Position.MIDDLE);
					n.addThemeVariants(NotificationVariant.LUMO_ERROR);
					break;
				case INFO:
					n.setPosition(Position.BOTTOM_START);
					n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
					break;
				case SUCCESS:
					n.setPosition(Position.BOTTOM_START);
					n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
					break;
				case WARNING:
					n.setPosition(Position.TOP_START);
					n.getElement().getThemeList().add("warning");
					break;
			}
			n.setDuration(getMsDuration() != null ? getMsDuration() : NORMAL_DURATION);
			n.open();
			return n;
		}

		public String getFopEventString() {
			return this.fopEventString;
		}

		public String[] getInfos() {
			return this.infos;
		}

		public Level getLevel() {
			return this.level;
		}

		public Integer getMsDuration() {
			return this.msDuration;
		}

		public String getNotificationString() {
			return this.notificationString;
		}

		public void setFopEventString(String fopEventString) {
			this.fopEventString = fopEventString;
		}

		public void setLevel(Level level) {
			this.level = level;
		}

		public void setNotificationString(String notificationString) {
			this.notificationString = notificationString;
		}

		private void setInfos(String[] infos) {
			this.infos = infos;
		}

		private void setMsDuration(Integer msDuration) {
			this.msDuration = msDuration;
		}
	}

	/**
	 * Class Notification.
	 */
	static public class RecordNotification extends UIEvent {

		public enum Level {
			SUCCESS, INFO;
		}

		public static final int NORMAL_DURATION = 3000;
		private String notificationString;
		private Level level;
		private String[] infos;
		private Integer msDuration;
		private String title;
		private boolean newRecord;

		/**
		 * Instantiates a new Notification.
		 *
		 * @param origin the origin
		 * @param string
		 */
		public RecordNotification(
		        Athlete a,
		        Object origin,
		        RecordNotification.Level level,
		        String title,
		        String notificationString,
		        Integer msDuration,
		        boolean newRecord,
		        FieldOfPlay fop,
		        String... infos) {
			super(a, origin, fop);
			this.setNotificationString(notificationString);
			this.setTitle(title);
			this.setLevel(level);
			this.setInfos(infos);
			this.setMsDuration(msDuration);
			this.setNewRecord(newRecord);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public com.vaadin.flow.component.notification.Notification doNotification() {
			return showNotification(this.title, this.getNotificationString());
		}

		public String[] getInfos() {
			return this.infos;
		}

		public Level getLevel() {
			return this.level;
		}

		public Integer getMsDuration() {
			return this.msDuration;
		}

		public String getNotificationString() {
			return this.notificationString;
		}

		public boolean isNewRecord() {
			return this.newRecord;
		}

		public void setLevel(Level level) {
			this.level = level;
		}

		public void setNotificationString(String notificationString) {
			this.notificationString = notificationString;
		}

		public com.vaadin.flow.component.notification.Notification showNotification(String title, String text) {
			Div titleAndCloseDiv = new Div();
			titleAndCloseDiv.setWidthFull();
			titleAndCloseDiv.getStyle().set("display", "flex").set("align-items", "center");

			Span titleSpan = new Span(title);
			titleSpan.getStyle().set("flex-grow", "1");
			titleSpan.getStyle().set("font-size", "1.6em");

			Span closeSpan = new Span("\u2715");
			Style closeStyle = closeSpan.getStyle();
			closeStyle.setCursor("pointer");
			closeStyle.setFontSize("1.6em");
			closeStyle.setMarginLeft("auto");

			HorizontalLayout titleLayout = new HorizontalLayout(titleSpan, closeSpan);
			titleLayout.setWidthFull();
			titleLayout.setAlignItems(Alignment.CENTER);

			Div textDiv = new Div();
			textDiv.getElement().setProperty("innerHTML", "<nobr>" + text + "</nobr>");
			textDiv.getStyle().set("padding-top", "var(--lumo-space-s)"); // Add some spacing
			textDiv.getStyle().set("font-size", "1.4em");
			textDiv.getStyle().set("line-height", "1.4");

			Div notificationContent = new Div(titleLayout, textDiv);
			notificationContent.getStyle().set("display", "flex").set("flex-direction", "column");
			notificationContent.setWidthFull();

			com.vaadin.flow.component.notification.Notification notification = new com.vaadin.flow.component.notification.Notification(notificationContent);
			notification.setDuration(this.msDuration);
			closeSpan.addClickListener(event -> notification.close());

			switch (getLevel()) {
				case INFO:
					notification.setPosition(Position.BOTTOM_END);
					notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
					break;
				case SUCCESS:
					notification.setPosition(Position.BOTTOM_END);
					notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
					break;
			}
			notification.open();
			return notification;
		}

		private void setInfos(String[] infos) {
			this.infos = infos;
		}

		private void setMsDuration(Integer msDuration) {
			this.msDuration = msDuration;
		}

		private void setNewRecord(boolean newRecord) {
			this.newRecord = newRecord;
		}

		private void setTitle(String title) {
			this.title = title;
		}
	}

	/**
	 * Individual referee decision.
	 *
	 * No subclassing wrt ExplicitDecision because @Subscribe must be distinct.
	 *
	 * @author owlcms
	 */
	static public class RefereeUpdate extends UIEvent {
		public Boolean ref1;
		public Long ref1Time;
		public Boolean ref2;
		public Long ref2Time;
		public Boolean ref3;
		public Long ref3Time;
		public boolean singleLight;

		public RefereeUpdate(Athlete a, Boolean ref1, Boolean ref2, Boolean ref3, Long long1,
		        Long long2, Long long3, Object origin, boolean singleLight, FieldOfPlay fop) {
			super(a, origin, fop);
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
			this.ref1Time = long1;
			this.ref2Time = long2;
			this.ref3Time = long3;
			this.singleLight = singleLight;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
			if (singleLight) {
				if (this.ref1 != null) {
					this.ref2 = this.ref1;
					this.ref1 = null;
				} else if (this.ref3 != null) {
					this.ref2 = this.ref3;
					this.ref3 = null;
				}
			}
			this.logger.debug("RefereeUpdate {} {} {}\n{}", ref1, ref2, ref3, this.trace);
		}

		public boolean isSingleLight() {
			return singleLight;
		}

		public void setSingleLight(boolean singleLight) {
			this.singleLight = singleLight;
		}
	}

	/**
	 * Class DecisionReset.
	 */
	static public class ResetOnNewClock extends UIEvent {

		/**
		 * Instantiates a new decision reset.
		 *
		 * @param origin the origin
		 * @param fop    originating field of play
		 */
		public ResetOnNewClock(Athlete a, Object origin, FieldOfPlay fop) {
			super(a, origin, fop);
		}
	}

	/**
	 * Class SetTime.
	 */
	static public class SetTime extends UIEvent {

		private Integer timeRemaining;

		/**
		 * Instantiates a new sets the time.
		 *
		 * @param timeRemaining the time remaining
		 * @param origin        the origin
		 * @param fop           originating field of play
		 */
		public SetTime(Integer timeRemaining, Object origin, String trace, FieldOfPlay fop) {
			super(origin, fop);
			this.timeRemaining = timeRemaining;
			this.trace = trace;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		/**
		 * Gets the time remaining.
		 *
		 * @return the time remaining
		 */
		public Integer getTimeRemaining() {
			return this.timeRemaining;
		}

	}

	static public class SnatchDone extends UIEvent {

		private Group group;

		/**
		 * Instantiates a new athlete announced.
		 *
		 * @param ui      the ui
		 * @param fop     originating field of play
		 * @param athlete the athlete
		 */
		public SnatchDone(Group group, UI ui, String stackTrace, FieldOfPlay fop) {
			super(ui, fop);
			this.setGroup(group);
			this.trace = stackTrace;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Group getGroup() {
			return this.group;
		}

		public void setGroup(Group group) {
			this.group = group;
		}
	}

	public static class StartLifting extends UIEvent {
		private Group group;

		public StartLifting(Group group, Object object, FieldOfPlay fop) {
			super(object, fop);
			this.setGroup(group);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Group getGroup() {
			return this.group;
		}

		public void setGroup(Group group) {
			this.group = group;
		}
	}

	/**
	 * Class StartTime.
	 */
	static public class StartTime extends UIEvent {

		private boolean serverSound;
		private Integer timeRemaining;
		private long start;
		private long end;

		/**
		 * Instantiates a new start time.
		 *
		 * @param timeRemaining the time remaining
		 * @param origin        the origin
		 * @param serverSound
		 * @param fop           originating field of play
		 */
		public StartTime(Integer timeRemaining, Object origin, boolean serverSound, FieldOfPlay fop) {
			super(origin, fop);
			this.start = System.currentTimeMillis();
			this.end = this.start + timeRemaining;
			this.timeRemaining = timeRemaining;
			this.serverSound = serverSound;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public StartTime(Integer timeRemaining, Object origin, boolean serverSound, String stackTrace, FieldOfPlay fop) {
			this(timeRemaining, origin, serverSound, fop);
		}

		public long getEnd() {
			return this.end;
		}

		public long getStart() {
			return this.start;
		}

		/**
		 * Gets the time remaining.
		 *
		 * @return the time remaining
		 */
		public Integer getTimeRemaining() {
			return this.timeRemaining;
		}

		public boolean isServerSound() {
			return this.serverSound;
		}

		public void setEnd(long end) {
			this.end = end;
		}

		public void setStart(long start) {
			this.start = start;
		}

	}

	/**
	 * Class StopTime.
	 */
	static public class StopTime extends UIEvent {

		private int timeRemaining;

		/**
		 * Instantiates a new stop time.
		 *
		 * @param timeRemaining the time remaining
		 * @param origin        the origin
		 * @param fop           originating field of play
		 */
		public StopTime(int timeRemaining, Object origin, FieldOfPlay fop) {
			super(origin, fop);
			this.timeRemaining = timeRemaining;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		/**
		 * Gets the time remaining.
		 *
		 * @return the time remaining
		 */
		public Integer getTimeRemaining() {
			return this.timeRemaining;
		}
	}

	static public class SummonRef extends UIEvent {

		public int ref;

		public SummonRef(int refNum, boolean b, Object origin, FieldOfPlay fop) {
			// ref 1..3 ; 4 is technical controller.
			super(origin, fop);
			this.ref = refNum;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

	}

	public static class SwitchGroup extends UIEvent {
		private Group group;
		private FOPState state;

		public SwitchGroup(Group group2, FOPState state, Athlete curAthlete, Object origin, FieldOfPlay fop) {
			super(curAthlete, origin, fop);
			this.setGroup(group2);
			this.setAthlete(curAthlete);
			this.setState(state);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Group getGroup() {
			return this.group;
		}

		public FOPState getState() {
			return this.state;
		}

		public void setGroup(Group group) {
			this.group = group;
		}

		public void setState(FOPState state) {
			this.state = state;
		}
	}

	static public class TimeRemainingSeconds extends UIEvent {

		private int secondsRemaining;

		/**
		 * Instantiates a new athlete warning milestone event in seconds.
		 *
		 * @param origin    the origin
		 * @param secondsRemaining milestone value in seconds
		 * @param fop       originating field of play
		 */
		public TimeRemainingSeconds(Object origin, int secondsRemaining, FieldOfPlay fop) {
			super(origin, fop);
			this.secondsRemaining = secondsRemaining;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public int getSecondsRemaining() {
			return this.secondsRemaining;
		}

		public void setSecondsRemaining(int secondsRemaining) {
			this.secondsRemaining = secondsRemaining;
		}
	}

	static public class BreakTimeRemainingSeconds extends UIEvent {

		private int secondsRemaining;

		/**
		 * Instantiates a new break warning milestone event in seconds.
		 *
		 * @param origin    the origin
		 * @param secondsRemaining milestone value in seconds
		 * @param fop       originating field of play
		 */
		public BreakTimeRemainingSeconds(Object origin, int secondsRemaining, FieldOfPlay fop) {
			super(origin, fop);
			this.secondsRemaining = secondsRemaining;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public int getSecondsRemaining() {
			return this.secondsRemaining;
		}

		public void setSecondsRemaining(int secondsRemaining) {
			this.secondsRemaining = secondsRemaining;
		}
	}

	public static class VideoRefresh extends UIEvent {
		private Group group;
		private Category category;
		private AgeGroup ageGroup;
		private Championship championship;

		public VideoRefresh(Object origin, Group g, Category c, FieldOfPlay fop) {
			this(origin, g, null, null, c, fop);
		}

		public VideoRefresh(Object origin, Group group, Championship championship, AgeGroup ageGroup, Category category,
		        FieldOfPlay fop) {
			super(origin, fop);
			this.setGroup(group);
			this.setCategory(category);
			this.ageGroup = ageGroup != null ? ageGroup : category != null ? category.getAgeGroup() : null;
			this.championship = championship != null ? championship
			        : this.ageGroup != null ? this.ageGroup.getChampionship() : null;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Category getCategory() {
			return this.category;
		}

		public AgeGroup getAgeGroup() {
			return this.ageGroup;
		}

		public Championship getChampionship() {
			return this.championship;
		}

		public Group getGroup() {
			return this.group;
		}

		public void setCategory(Category c) {
			this.category = c;
		}

		public void setGroup(Group g) {
			this.group = g;
		}
	}

	static public class WakeUpRef extends UIEvent {

		public boolean on;
		public int ref;

		public WakeUpRef(int lastRef, boolean b, Object origin, FieldOfPlay fop) {
			super(origin, fop);
			this.ref = lastRef;
			this.on = b;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

	}

	protected String trace;
	Logger logger = (Logger) LoggerFactory.getLogger(UIEvent.class);
	private Athlete athlete;
	private Object origin;
	private FieldOfPlay fop;
	private long sequence;

	private UIEvent(Athlete athlete, Object origin, FieldOfPlay fop) {
		this(origin, fop);
		this.athlete = athlete;
	}

	private UIEvent(Object origin, FieldOfPlay fop) {
		this.origin = origin;
		this.setFop(fop);
	}

	/**
	 * Gets the athlete.
	 *
	 * @return the athlete
	 */
	public Athlete getAthlete() {
		return this.athlete;
	}

	public FieldOfPlay getFop() {
		return this.fop;
	}

	/**
	 * Gets the origin.
	 *
	 * @return the originating object
	 */
	public Object getOrigin() {
		return this.origin;
	}

	public String getTrace() {
		return this.trace;
	}

	public void setAthlete(Athlete athlete) {
		this.athlete = athlete;
	}

	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	/**
	 * Per-FOP monotonic sequence number, assigned when the event is pushed out. Used by clients to
	 * drop timer events that arrive out of order on the asynchronous UI event bus.
	 *
	 * @return the sequence number (0 if never pushed)
	 */
	public long getSequence() {
		return this.sequence;
	}

	public void setSequence(long sequence) {
		this.sequence = sequence;
	}

	protected void setTrace(Supplier<String> stackTrace) {
		if (StartupUtils.isTraceSetting()) {
			this.trace = stackTrace.get();
		}
	}

}
