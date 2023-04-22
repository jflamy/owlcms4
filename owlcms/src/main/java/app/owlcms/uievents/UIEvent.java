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

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPError;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * UIEvents are triggered in response to field of play events (FOPEvents). Each
 * field of play has an associated uiEventBus on which the user interface
 * commands are posted. The various browsers subscribe to UIEvents and react
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
		}

		public BreakType getBreakType() {
			return breakType;
		}

		public void setBreakType(BreakType breakType) {
			this.breakType = breakType;
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
		}

		public int getTimeRemaining() {
			return timeRemaining;
		}

		public void setTimeRemaining(int timeRemaining) {
			this.timeRemaining = timeRemaining;
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
		}

		public BreakType getBreakType() {
			return breakType;
		}

		public int getMillis() {
			return (timeRemaining != null ? timeRemaining : 0);
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

		/**
		 * @return true if is a request for toggling display (and not an actual break
		 *         start)
		 */
		public boolean isDisplayToggle() {
			return displayToggle;
		}

		/**
		 * @return true if break lasts indefinitely and timeRemaining should be ignored
		 */
		public boolean isIndefinite() {
			return indefinite;
		}

		/**
		 * @param displayToggle true to request switching to Break Timer
		 */
		public void setDisplayToggle(boolean displayToggle) {
			this.displayToggle = displayToggle;
		}

		@Override
		public String toString() {
			return "UIEvent.BreakPaused [displayToggle=" + displayToggle + ", timeRemaining=" + timeRemaining
			        + ", indefinite=" + indefinite + ", end=" + end + ", breakType=" + breakType + ", countdownType="
			        + countdownType + "]";
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
			// logger.trace("BreakSetTime setting to {} from {}", getTimeRemaining(),
			// trace);
		}

		public BreakType getBreakType() {
			return breakType;
		}

		public LocalDateTime getEnd() {
			return end;
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

		/**
		 * @return true if break lasts indefinitely and timeRemaining should be ignored
		 */
		public boolean isIndefinite() {
			return indefinite;
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
			this.breakType = bt;
			this.countdownType = ct;
			this.setDisplayToggle(displayToggle);
			this.setPaused(paused);
			this.setTrace(trace);
		}

		public BreakType getBreakType() {
			return breakType;
		}

		public int getMillis() {
			return (getTimeRemaining());
		}

		public Boolean getPaused() {
			return paused;
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

		/**
		 * @return true if is a request for toggling display (and not an actual break
		 *         start)
		 */
		public boolean isDisplayToggle() {
			return displayToggle;
		}

		/**
		 * @return true if break lasts indefinitely and timeRemaining should be ignored
		 */
		public boolean isIndefinite() {
			return indefinite;
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
			return "UIEvent.BreakStarted [displayToggle=" + displayToggle + ", timeRemaining=" + timeRemaining
			        + ", indefinite=" + indefinite + ", end=" + end + ", breakType=" + breakType + ", countdownType="
			        + countdownType + "]";
		}

	}

	static public class Broadcast extends UIEvent {

		private String message;

		public Broadcast(String string, Object origin) {
			super(origin);
			this.setMessage(string);
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

	}

	/**
	 * Class BreakDone.
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
		}

		public CeremonyType getCeremonyType() {
			return ceremonyType;
		}

		public void setCeremonyType(CeremonyType ceremonyType) {
			this.ceremonyType = ceremonyType;
		}

	}

	/**
	 * Class BreakStarted.
	 */
	// MUST NOT EXTEND otherwise subscription triggers on supertype as well
	static public class CeremonyStarted extends UIEvent {

		private Category ceremonyCategory;
		private Group ceremonyGroup;
		private CeremonyType ceremonyType;

		public CeremonyStarted(CeremonyType ceremonyType, Group ceremonyGroup, Category ceremonyCategory, String trace,
		        Object origin) {
			super(origin);
			this.setCeremonyType(ceremonyType);
			this.setCeremonyGroup(ceremonyGroup);
			this.setCeremonyCategory(ceremonyCategory);
			this.setTrace(trace);
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
			return Objects.equals(ceremonyCategory, other.ceremonyCategory)
			        && Objects.equals(ceremonyGroup, other.ceremonyGroup) && ceremonyType == other.ceremonyType;
		}

		public Category getCeremonyCategory() {
			return ceremonyCategory;
		}

		public Group getCeremonyGroup() {
			return this.ceremonyGroup;
		}

		public CeremonyType getCeremonyType() {
			return ceremonyType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(ceremonyCategory, ceremonyGroup, ceremonyType);
		}

		public void setCeremonyGroup(Group ceremonyGroup2) {
			this.ceremonyGroup = ceremonyGroup2;
		}

		public void setCeremonyType(CeremonyType ceremonyType) {
			this.ceremonyType = ceremonyType;
		}

		@Override
		public String toString() {
			return "CeremonyStarted [ceremonyType=" + ceremonyType + ", ceremonyCategory=" + ceremonyCategory
			        + ", ceremonyGroup=" + ceremonyGroup + "]";
		}

		private void setCeremonyCategory(Category ceremonyCategory2) {
			this.ceremonyCategory = ceremonyCategory2;
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
		}
	}

	static public class GlobalRankingUpdated extends UIEvent {
		public GlobalRankingUpdated(Object object) {
			super(object);
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
			this.setTrace(stackTrace);
		}

		public Group getGroup() {
			return group;
		}

		public void setGroup(Group group) {
			this.group = group;
		}
	}

	static public class JuryNotification extends UIEvent {

		private JuryDeliberationEventType deliberationEventType;
		private Boolean newRecord;
		private Boolean reversal;

		public JuryNotification(Athlete athleteUnderReview, Object origin,
		        JuryDeliberationEventType deliberationEventType, Boolean reversal, Boolean newRecord) {
			super(athleteUnderReview, origin);
			this.setDeliberationEventType(deliberationEventType);
			this.setReversal(reversal);
			this.setNewRecord(newRecord != null && newRecord);
			this.setTrace(LoggerUtils.stackTrace());
		}

		/**
		 * Instantiates a new Notification.
		 *
		 * @param origin the origin
		 */
		public JuryNotification(Athlete a, Object origin, String notificationString, String fopEventString) {
			super(a, origin);
		}

		/**
		 * @return the deliberationEventType
		 */
		public JuryDeliberationEventType getDeliberationEventType() {
			return deliberationEventType;
		}

		public boolean getNewRecord() {
			return newRecord;
		}

		/**
		 * @return the reversal
		 */
		public Boolean getReversal() {
			return reversal;
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
		}

		public JuryUpdate(Object origin, int i, Boolean[] juryMemberDecision2, int jurySize) {
			super(origin);
			this.collective = null;
			this.juryMemberUpdated = i;
			this.juryMemberDecision = juryMemberDecision2;
		}

		/**
		 * @return the collective
		 */
		public Boolean getCollective() {
			return collective;
		}

		/**
		 * @return the juryMemberDecision
		 */
		public Boolean[] getJuryMemberDecision() {
			return juryMemberDecision;
		}

		/**
		 * @return the juryMemberUpdated
		 */
		public Integer getJuryMemberUpdated() {
			return juryMemberUpdated;
		}

		/**
		 * @return the jurySize
		 */
		public int getJurySize() {
			return jurySize;
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
		 * @param nextAthlete     the next athlete that will lift (cannot be the same as
		 *                        athlete)
		 * @param previousAthlete the last athlete to have lifted (can be the same as
		 *                        athlete)
		 * @param changingAthlete the athlete who triggered the lifting update
		 * @param liftingOrder    the lifting order
		 * @param displayOrder    the display order
		 * @param timeAllowed     the time allowed
		 * @param displayToggle   if true, just update display according to lifting
		 *                        order.
		 * @param origin          the origin
		 * @param newWeight       newly requested weight, null if no change from
		 *                        previous
		 */
		public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete,
		        Athlete changingAthlete, List<Athlete> liftingOrder, List<Athlete> displayOrder, Integer timeAllowed,
		        boolean currentDisplayAffected, boolean displayToggle, Object origin, boolean inBreak,
		        Integer newWeight) {
			super(athlete, origin);
			this.setTrace(LoggerUtils.stackTrace());
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
		}

		public Athlete getChangingAthlete() {
			return changingAthlete;
		}

		/**
		 * Gets the display order.
		 *
		 * @return the display order
		 */
		public List<Athlete> getDisplayOrder() {
			return displayOrder;
		}

		/**
		 * Gets the lifting order.
		 *
		 * @return the lifting order
		 */
		public List<Athlete> getLiftingOrder() {
			return liftingOrder;
		}

		public Integer getNewWeight() {
			return newWeight;
		}

		/**
		 * Gets the next athlete.
		 *
		 * @return the next athlete
		 */
		public Athlete getNextAthlete() {
			return nextAthlete;
		}

		/**
		 * Gets the previous athlete.
		 *
		 * @return the previous athlete
		 */
		public Athlete getPreviousAthlete() {
			return previousAthlete;
		}

		/**
		 * Gets the time allowed.
		 *
		 * @return the timeAllowed
		 */
		public Integer getTimeAllowed() {
			return timeAllowed;
		}

		/**
		 * @return true if the current event requires to stop the timer
		 */
		public boolean isCurrentDisplayAffected() {
			return currentDisplayAffected;
		}

		public boolean isDisplayToggle() {
			return displayToggle;
		}

		public boolean isInBreak() {
			return inBreak;
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
			this.setFopEventString(fopEventString);
			this.setLevel(level);
			this.setInfos(infos);
			this.setMsDuration(msDuration);
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
			return fopEventString;
		}

		public String[] getInfos() {
			return infos;
		}

		public Level getLevel() {
			return level;
		}

		public Integer getMsDuration() {
			return msDuration;
		}

		public String getNotificationString() {
			return notificationString;
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
		public SetTime(Integer timeRemaining, Object origin) {
			super(origin);
			this.timeRemaining = timeRemaining;
		}

		/**
		 * Gets the time remaining.
		 *
		 * @return the time remaining
		 */
		public Integer getTimeRemaining() {
			return timeRemaining;
		}

	}

	public static class StartLifting extends UIEvent {
		private Group group;

		public StartLifting(Group group, Object object) {
			super(object);
			this.setGroup(group);
		}

		public Group getGroup() {
			return group;
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

		/**
		 * Instantiates a new start time.
		 *
		 * @param timeRemaining the time remaining
		 * @param origin        the origin
		 * @param serverSound
		 */
		public StartTime(Integer timeRemaining, Object origin, boolean serverSound) {
			super(origin);
			this.timeRemaining = timeRemaining;
			this.serverSound = serverSound;
		}

		public StartTime(Integer timeRemaining, Object origin, boolean serverSound, String stackTrace) {
			this(timeRemaining, origin, serverSound);
		}

		/**
		 * Gets the time remaining.
		 *
		 * @return the time remaining
		 */
		public Integer getTimeRemaining() {
			return timeRemaining;
		}

		public boolean isServerSound() {
			return serverSound;
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
		}

		/**
		 * Gets the time remaining.
		 *
		 * @return the time remaining
		 */
		public Integer getTimeRemaining() {
			return timeRemaining;
		}
	}

	static public class SummonRef extends UIEvent {

		public int ref;

		public SummonRef(int refNum, boolean b, Object origin) {
			// ref 1..3 ; 4 is technical controller.
			super(origin);
			this.ref = refNum;
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
		}

		public Group getGroup() {
			return group;
		}

		public FOPState getState() {
			return state;
		}

		public void setGroup(Group group) {
			this.group = group;
		}

		public void setState(FOPState state) {
			this.state = state;
		}
	}

	public static class VideoRefresh extends UIEvent {
		private Group group;
		private Category category;

		public VideoRefresh(Object origin, Group g, Category c) {
			super(origin);
			this.setGroup(g);
			this.setCategory(c);
		}

		public Group getGroup() {
			return group;
		}

		public void setGroup(Group g) {
			this.group = g;
		}

		public Category getCategory() {
			return category;
		}

		public void setCategory(Category c) {
			this.category = c;
		}
	}

	static public class WakeUpRef extends UIEvent {

		public boolean on;
		public int ref;

		public WakeUpRef(int lastRef, boolean b, Object origin) {
			super(origin);
			this.ref = lastRef;
			this.on = b;
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
		return athlete;
	}

	/**
	 * Gets the origin.
	 *
	 * @return the originating object
	 */
	public Object getOrigin() {
		return origin;
	}

	public String getTrace() {
		return trace;
	}

	public void setAthlete(Athlete athlete) {
		this.athlete = athlete;
	}

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	protected void setTrace(String stackTrace) {
		this.trace = stackTrace;
	}

}
