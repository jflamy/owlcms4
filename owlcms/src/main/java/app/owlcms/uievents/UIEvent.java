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
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPError;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * UIEvents are triggered in response to field of play events (FOPEvents). Each field of play has an associated
 * uiEventBus on which the user interface commands are posted. The various browsers subscribe to UIEvents and react
 * accordingly.
 *
 * @author owlcms
 */

public class UIEvent {

	static public class BarbellOrPlatesChanged extends UIEvent {
		public BarbellOrPlatesChanged(Object object) {
			super(object);
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
		 */
		public BreakDone(Object origin, BreakType breakType) {
			super(origin);
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
		        CountdownType ct) {
			super(origin);
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
		 */
		public BreakSetTime(BreakType bt, CountdownType ct, Integer timeRemaining, LocalDateTime end,
		        boolean indefinite, Object origin, String trace) {
			super(origin);
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
		        CountdownType ct, String trace, Boolean paused) {
			super(origin);
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

		public Broadcast(String string, Object origin) {
			super(origin);
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
		 * @param breakType
		 */
		public CeremonyDone(CeremonyType ceremonyType, Object origin) {
			super(origin);
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
		        Object origin) {
			super(origin);
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

		private void setCeremonyChampionship(Championship championship) {
			this.championship=championship;
		}

		private void setCeremonyAgeGroup(AgeGroup ageGroup) {
			this.setAgeGroup(ageGroup);
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

		public Category getCeremonyCategory() {
			return this.ceremonyCategory;
		}

		public Group getCeremonySession() {
			return this.ceremonySession;
		}

		public CeremonyType getCeremonyType() {
			return this.ceremonyType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.ceremonyCategory, this.ceremonySession, this.ceremonyType);
		}

		public void setCeremonySession(Group ceremonyGroup2) {
			this.ceremonySession = ceremonyGroup2;
		}

		public void setCeremonyType(CeremonyType ceremonyType) {
			this.ceremonyType = ceremonyType;
		}

		@Override
		public String toString() {
			return "CeremonyStarted [ceremonyType=" + this.ceremonyType + ", ceremonyCategory=" + this.ceremonyCategory
			        + ", ceremonySession=" + this.ceremonySession + "]";
		}

		private void setCeremonyCategory(Category ceremonyCategory2) {
			this.ceremonyCategory = ceremonyCategory2;
		}

		public Championship getChampionship() {
			return championship;
		}

		public void setChampionship(Championship championship) {
			this.championship = championship;
		}

		public AgeGroup getAgeGroup() {
			return ageGroup;
		}

		public void setAgeGroup(AgeGroup ageGroup) {
			this.ageGroup = ageGroup;
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

		/**
		 * Instantiates a new referee decision.
		 *
		 * @param decision the decision
		 * @param ref1     the ref 1
		 * @param ref2     the ref 2
		 * @param ref3     the ref 3
		 * @param origin   the origin
		 */
		public Decision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, Object origin) {
			super(a, origin);
			this.decision = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
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
		public DecisionReset(Athlete a, Object origin) {
			super(a, origin);
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
		 */
		public DownSignal(Object origin) {
			super(origin);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}
	}

	static public class GlobalRankingUpdated extends UIEvent {
		public GlobalRankingUpdated(Object object) {
			super(object);
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
		 * @param athlete the athlete
		 * @param ui      the ui
		 */
		public GroupDone(Group group, UI ui, String stackTrace) {
			super(ui);
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
		private boolean requestForAnnounce = false;

		public JuryNotification(Athlete athleteUnderReview, Object origin,
		        JuryDeliberationEventType deliberationEventType, Boolean reversal, Boolean newRecord,
		        boolean requestForAnnounce) {
			super(athleteUnderReview, origin);
			this.setDeliberationEventType(deliberationEventType);
			this.setReversal(reversal);
			this.setNewRecord(newRecord != null && newRecord);
			this.setTrace(() -> LoggerUtils.stackTrace());
			this.requestForAnnounce = requestForAnnounce;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		/**
		 * Instantiates a new Notification.
		 *
		 * @param origin the origin
		 */
		public JuryNotification(Athlete a, Object origin, String notificationString, String fopEventString) {
			super(a, origin);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
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

		public boolean isRequestForAnnounce() {
			return this.requestForAnnounce;
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

		private void setNewRecord(Boolean newRecord) {
			this.newRecord = newRecord;
		}

	}

	static public class JuryUpdate extends UIEvent {

		private Boolean collective;
		private Boolean[] juryMemberDecision;
		private int jurySize;
		private Integer juryMemberUpdated;

		public JuryUpdate(Object origin, boolean collective, Boolean[] juryMemberDecision, int jurySize) {
			super(origin);
			this.collective = collective;
			this.juryMemberDecision = juryMemberDecision;
			this.jurySize = jurySize;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public JuryUpdate(Object origin, int i, Boolean[] juryMemberDecision2, int jurySize) {
			super(origin);
			this.collective = null;
			this.juryMemberUpdated = i;
			this.juryMemberDecision = juryMemberDecision2;
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
		 */
		public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete,
		        Athlete changingAthlete, List<Athlete> liftingOrder, List<Athlete> displayOrder, Integer timeAllowed,
		        boolean currentDisplayAffected, boolean displayToggle, Object origin, boolean inBreak,
		        Integer newWeight) {
			super(athlete, origin);
			this.setTrace(() -> LoggerUtils.stackTrace());
			this.nextAthlete = nextAthlete;
			this.previousAthlete = previousAthlete;
			this.changingAthlete = changingAthlete;
			this.timeAllowed = timeAllowed;
			this.liftingOrder = liftingOrder;
			this.displayOrder = displayOrder;
			this.currentDisplayAffected = currentDisplayAffected;
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

		public Notification(Athlete curAthlete, Object origin, FOPEvent e, FOPState state, Notification.Level level) {
			super(curAthlete, origin);
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
		        String... infos) {
			super(a, origin);
			this.setNotificationString(notificationString);
			this.setFopEventString(this.fopEventString);
			this.setLevel(level);
			this.setInfos(infos);
			this.setMsDuration(msDuration);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public void doNotification() {
			com.vaadin.flow.component.notification.Notification n = new com.vaadin.flow.component.notification.Notification();
			Div div = new Div();
			String close = "\u00A0\u00A0\u00A0\u2715";
			div.addClickListener(click -> n.close());
			if (getFopEventString() != null && !getFopEventString().isEmpty()) {
				div.setText(FOPError.translateMessage(getNotificationString(), getFopEventString()) + close);
			} else {
				div.setText(Translator.translate(getNotificationString(), (Object[]) getInfos()) + close);
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

		public RefereeUpdate(Athlete a, Boolean ref1, Boolean ref2, Boolean ref3, Long long1,
		        Long long2, Long long3, Object origin) {
			super(a, origin);
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
			this.ref1Time = long1;
			this.ref2Time = long2;
			this.ref3Time = long3;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
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
		 */
		public ResetOnNewClock(Athlete a, Object origin) {
			super(a, origin);
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
		 */
		public SetTime(Integer timeRemaining, Object origin, String trace) {
			super(origin);
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
		 * @param athlete the athlete
		 * @param ui      the ui
		 */
		public SnatchDone(Group group, UI ui, String stackTrace) {
			super(ui);
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

		public StartLifting(Group group, Object object) {
			super(object);
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
		 */
		public StartTime(Integer timeRemaining, Object origin, boolean serverSound) {
			super(origin);
			this.start = System.currentTimeMillis();
			this.end = this.start + timeRemaining;
			this.timeRemaining = timeRemaining;
			this.serverSound = serverSound;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public StartTime(Integer timeRemaining, Object origin, boolean serverSound, String stackTrace) {
			this(timeRemaining, origin, serverSound);
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
		 */
		public StopTime(int timeRemaining, Object origin) {
			super(origin);
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

		public SummonRef(int refNum, boolean b, Object origin) {
			// ref 1..3 ; 4 is technical controller.
			super(origin);
			this.ref = refNum;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

	}

	public static class SwitchGroup extends UIEvent {
		private Group group;
		private FOPState state;

		public SwitchGroup(Group group2, FOPState state, Athlete curAthlete, Object origin) {
			super(curAthlete, origin);
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

	static public class TimeRemaining extends UIEvent {

		private int timeRemaining;

		/**
		 * Instantiates a new break done.
		 *
		 * @param origin    the origin
		 * @param breakType
		 */
		public TimeRemaining(Object origin, int timeRemaining) {
			super(origin);
			this.timeRemaining = timeRemaining;
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public int getTimeRemaining() {
			return this.timeRemaining;
		}

		public void setTimeRemaining(int timeRemaining) {
			this.timeRemaining = timeRemaining;
		}
	}

	public static class VideoRefresh extends UIEvent {
		private Group group;
		private Category category;

		public VideoRefresh(Object origin, Group g, Category c) {
			super(origin);
			this.setGroup(g);
			this.setCategory(c);
			if (this.trace == null || this.trace.isBlank()) {
				this.setTrace(() -> LoggerUtils.stackTrace());
			}
		}

		public Category getCategory() {
			return this.category;
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

		public WakeUpRef(int lastRef, boolean b, Object origin) {
			super(origin);
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

	private UIEvent(Athlete athlete, Object origin) {
		this(origin);
		this.athlete = athlete;
	}

	private UIEvent(Object origin) {
		this.origin = origin;
	}

	/**
	 * Gets the athlete.
	 *
	 * @return the athlete
	 */
	public Athlete getAthlete() {
		return this.athlete;
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

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	protected void setTrace(Supplier<String> stackTrace) {
		if (StartupUtils.isTraceSetting()) {
			this.trace = stackTrace.get();
		}
	}

}
