/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * The subclasses of FOPEvent are all the events that can take place on the
 * field of play.
 *
 * @author owlcms
 */
public class FOPEvent {

	/**
	 * Class BarbellOrPlatesChanged
	 */
	static public class BarbellOrPlatesChanged extends FOPEvent {

		public BarbellOrPlatesChanged(Object origin) {
			super(origin);
		}
	}

	/**
	 * Class BreakDone.
	 */
	static public class BreakDone extends FOPEvent {

		private BreakType breakType;

		public BreakDone(BreakType bt, Object origin) {
			super(origin);
			this.setBreakType(bt);
		}

		public BreakType getBreakType() {
			return breakType;
		}

		public void setBreakType(BreakType breakType) {
			this.breakType = breakType;
		}
	}

	/**
	 * Class BreakPaused.
	 */
	static public class BreakPaused extends FOPEvent {

		private Integer timeRemaining;

		public BreakPaused(Integer timeRemaining, Object origin) {
			super(origin);
			this.timeRemaining = timeRemaining;
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

		public void setTimeRemaining(Integer timeRemaining) {
			this.timeRemaining = timeRemaining;
		}
	}

	/**
	 * Class BreakStarted.
	 */
	static public class BreakStarted extends FOPEvent {

		private BreakType breakType;

		private CountdownType countdownType;

		private LocalDateTime targetTime;

		private Integer timeRemaining;

		private Boolean wait;

		public BreakStarted(BreakType bType, CountdownType cType, Integer timeRemaining, LocalDateTime targetTime,
		        Boolean wait,
		        Object origin) {
			super(origin);
			this.setBreakType(bType);
			this.setCountdownType(cType);
			this.timeRemaining = timeRemaining;
			this.targetTime = targetTime;
			this.setWait(true);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			BreakStarted other = (BreakStarted) obj;
			return breakType == other.breakType && countdownType == other.countdownType
			        && Objects.equals(targetTime, other.targetTime)
			        && Objects.equals(timeRemaining, other.timeRemaining) && Objects.equals(wait, other.wait);
		}

		public BreakType getBreakType() {
			return breakType;
		}

		public CountdownType getCountdownType() {
			return countdownType;
		}

		public LocalDateTime getTargetTime() {
			return targetTime;
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

		public Boolean getWait() {
			return wait;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(breakType, countdownType, targetTime, timeRemaining, wait);
			return result;
		}

		public boolean isIndefinite() {
			if (countdownType != null) {
				return countdownType == CountdownType.INDEFINITE;
			} else {
				return breakType == BreakType.JURY
						|| breakType == BreakType.CHALLENGE
						|| breakType == BreakType.TECHNICAL
				        || breakType == BreakType.GROUP_DONE;
			}
		}

		public void setBreakType(BreakType breakType) {
			this.breakType = breakType;
		}

		public void setCountdownType(CountdownType countdownType) {
			this.countdownType = countdownType;
		}

		public void setTargetTime(LocalDateTime targetTime) {
			this.targetTime = targetTime;
		}

		public void setTimeRemaining(Integer timeRemaining) {
			this.timeRemaining = timeRemaining;
		}

		public void setWait(Boolean wait) {
			this.wait = wait;
		}

		@Override
		public String toString() {
			return "BreakStarted [breakType=" + breakType + ", countdownType=" + countdownType + ", timeRemaining="
			        + timeRemaining + ", targetTime=" + targetTime + ", wait=" + wait + "]";
		}

	}

	/**
	 * Class BreakDone.
	 */
	static public class CeremonyDone extends FOPEvent {

		private CeremonyType ceremonyType;

		public CeremonyDone(CeremonyType ceremonyType, Object origin) {
			super(origin);
			setCeremonyType(ceremonyType);
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
	static public class CeremonyStarted extends FOPEvent {

		private CeremonyType ceremony;
		private Category ceremonyCategory;
		private Group ceremonyGroup;

		public CeremonyStarted(CeremonyType ceremony, Group ceremonyGroup,
		        Category ceremonyCategory, Object origin) {
			super(origin);
			this.setCeremony(ceremony);
			this.setCeremonyGroup(ceremonyGroup);
			this.setCategoryCeremony(ceremonyCategory);
			// logger.trace("FOPEvent ceremonyGroup = {} st={}", this.getCeremonyGroup(),
			// LoggerUtils.stackTrace());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			CeremonyStarted other = (CeremonyStarted) obj;
			return ceremony == other.ceremony && Objects.equals(ceremonyCategory, other.ceremonyCategory)
			        && Objects.equals(ceremonyGroup, other.ceremonyGroup);
		}

		public CeremonyType getCeremony() {
			return ceremony;
		}

		public Category getCeremonyCategory() {
			return ceremonyCategory;
		}

		public Group getCeremonyGroup() {
			return ceremonyGroup;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(ceremony, ceremonyCategory, ceremonyGroup);
			return result;
		}

		public void setCeremony(CeremonyType ceremony) {
			this.ceremony = ceremony;
		}

		public void setCeremonyGroup(Group ceremonyGroup2) {
			this.ceremonyGroup = ceremonyGroup2;
		}

		@Override
		public String toString() {
			return "CeremonyStarted [ceremony=" + ceremony + ", ceremonyGroup=" + ceremonyGroup + ", ceremonyCategory="
			        + ceremonyCategory + "]";
		}

		private void setCategoryCeremony(Category ceremonyCategory2) {
			this.setCeremonyCategory(ceremonyCategory2);
		}

		private void setCeremonyCategory(Category ceremonyCategory2) {
			this.ceremonyCategory = ceremonyCategory2;
		}
	}

	/**
	 * Report an individual decision.
	 *
	 * No subclassing relationship with {@link ExplicitDecision} because of
	 * different @Subscribe requirements
	 */
	static public class DecisionFullUpdate extends FOPEvent {
		public Boolean ref1;

		public Long ref1Time;
		public Boolean ref2;
		public Long ref2Time;
		public Boolean ref3;
		public Long ref3Time;

		private boolean immediate;

		public DecisionFullUpdate(Object origin, Athlete athlete, Boolean ref1, Boolean ref2, Boolean ref3,
		        Long long1, Long long2, Long long3, boolean immediate) {
			super(athlete, origin);
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
			this.ref1Time = long1;
			this.ref2Time = long2;
			this.ref3Time = long3;
			this.immediate = immediate;
			trace();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			DecisionFullUpdate other = (DecisionFullUpdate) obj;
			return Objects.equals(ref1, other.ref1) && Objects.equals(ref1Time, other.ref1Time)
			        && Objects.equals(ref2, other.ref2) && Objects.equals(ref2Time, other.ref2Time)
			        && Objects.equals(ref3, other.ref3) && Objects.equals(ref3Time, other.ref3Time);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(ref1, ref1Time, ref2, ref2Time, ref3, ref3Time);
			return result;
		}

		public boolean isImmediate() {
			return immediate;
		}

		public void trace() {
			trace(ref1, ref2, ref3, immediate);
		}

		private void trace(Boolean ref1, Boolean ref2, Boolean ref3, boolean immediate) {
			logger.trace("decision full update {} {} {} {}", ref1, ref2, ref3, LoggerUtils.whereFrom(2));
		}

	}

	/**
	 * The Class DecisionReset.
	 */
	static public class DecisionReset extends FOPEvent {

		public DecisionReset(Object object) {
			super(object);
		}

	}

	static public class DecisionUpdate extends FOPEvent {

		public boolean decision;

		public int refIndex;

		public DecisionUpdate(Object origin, int refIndex, boolean decision) {
			super(origin);
			this.refIndex = refIndex;
			this.decision = decision;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			DecisionUpdate other = (DecisionUpdate) obj;
			return decision == other.decision && refIndex == other.refIndex;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(decision, refIndex);
			return result;
		}

		@Override
		public String toString() {
			return "[decision=" + decision + ", refIndex=" + refIndex + "]";
		}

	}

	/**
	 * The Class DownSignal.
	 */
	static public class DownSignal extends FOPEvent {

		public DownSignal(Object origin) {
			super(origin);
		}

	}

	/**
	 * The Class ExplicitDecision.
	 */
	static public class ExplicitDecision extends FOPEvent {

		public Boolean ref1;

		public Boolean ref2;

		public Boolean ref3;
		/** The decision. */
		public Boolean success = null;

		/**
		 * Instantiates a new referee decision.
		 *
		 * @param decision the decision
		 * @param ref1
		 * @param ref2
		 * @param ref3
		 */
		public ExplicitDecision(Athlete athlete, Object origin, boolean decision, Boolean ref1, Boolean ref2,
		        Boolean ref3) {
			super(athlete, origin);
			// logger.debug("explicit decision for {}", athlete);
			this.success = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			ExplicitDecision other = (ExplicitDecision) obj;
			return Objects.equals(ref1, other.ref1) && Objects.equals(ref2, other.ref2)
			        && Objects.equals(ref3, other.ref3) && Objects.equals(success, other.success);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(ref1, ref2, ref3, success);
			return result;
		}
	}

	static public class ForceTime extends FOPEvent {

		public int timeAllowed;

		public ForceTime(int timeAllowed, Object object) {
			super(object);
			this.timeAllowed = timeAllowed;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			ForceTime other = (ForceTime) obj;
			return timeAllowed == other.timeAllowed;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(timeAllowed);
			return result;
		}
	}

	static public class JuryDecision extends FOPEvent {
		/** The decision. */
		public Boolean success = null;

		public JuryDecision(Athlete athlete, Object origin, boolean decision) {
			super(athlete, origin);
			logger.trace("jury decision for {}", athlete);
			this.success = decision;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			JuryDecision other = (JuryDecision) obj;
			return Objects.equals(success, other.success);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(success);
			return result;
		}

	}

	public static class JuryMemberDecisionUpdate extends FOPEvent {

		public boolean decision;
		public int refIndex;

		public JuryMemberDecisionUpdate(Object origin, int refIndex, boolean decision) {
			super(origin);
			this.refIndex = refIndex;
			this.decision = decision;
		}

	}

	/**
	 * The Class StartLifting.
	 */
	static public class StartLifting extends FOPEvent {

		public StartLifting(Object origin) {
			super(origin);
		}

	}

	static public class SummonReferee extends FOPEvent {

		public int refNumber;

		public SummonReferee(Object origin, int refNumber) {
			super(origin);
			this.refNumber = refNumber;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			SummonReferee other = (SummonReferee) obj;
			return refNumber == other.refNumber;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(refNumber);
			return result;
		}

	}

	static public class SwitchGroup extends FOPEvent {

		private Group group;

		public SwitchGroup(Group g, Object origin) {
			super(origin);
			this.group = g;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!super.equals(obj) || (getClass() != obj.getClass())) {
				return false;
			}
			SwitchGroup other = (SwitchGroup) obj;
			return Objects.equals(group, other.group);
		}

		public Group getGroup() {
			return group;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(group);
			return result;
		}

	}

	static public class TimeOver extends FOPEvent {

		public TimeOver(Object origin) {
			super(origin);
		}

	}

	/**
	 * The Class StartTime.
	 */
	static public class TimeStarted extends FOPEvent {

		public TimeStarted(Object object) {
			super(object);
		}

	}

	/**
	 * The Class StopTime.
	 */
	static public class TimeStopped extends FOPEvent {

		public TimeStopped(Object object) {
			super(object);
		}

	}

	// /**
	// * The Class AthleteAnnounced.
	// */
	// static public class AthleteAnnounced extends FOPEvent {
	//
	// public AthleteAnnounced(Object object) {
	// super(object);
	// }
	//
	// }

	/**
	 * Class WeightChange.
	 */
	static public class WeightChange extends FOPEvent {

		private boolean resultChange;

		public WeightChange(Object origin, Athlete a, Boolean resultChange) {
			super(a, origin);
			this.setResultChange(resultChange);
		}

		public Boolean isResultChange() {
			return resultChange;
		}

		public void setResultChange(boolean resultChange) {
			this.resultChange = resultChange;
		}

	}

	protected Athlete athlete;

	/**
	 * When a FOPEvent (for example stopping the clock) is handled, it is often
	 * reflected as a series of UIEvents (for example, all the displays running the
	 * clock get told to stop it). The user interface that gave the order doesn't
	 * want to be notified again, so we memorize which user interface element
	 * created the original order so it can ignore it.
	 */
	protected Object origin;

	final Logger logger = (Logger) LoggerFactory.getLogger(FOPEvent.class);

	private FieldOfPlay fop;

	private String stackTrace;

	private long timestamp;

	public FOPEvent(Athlete athlete, Object origin) {
		this.fop = OwlcmsSession.getFop();
//        if (this.fop == null) {
//            logger.error("no fop  {}",LoggerUtils.stackTrace());
//        }
		this.stackTrace = LoggerUtils.stackTrace();
		this.athlete = athlete;
		this.origin = origin;
		this.timestamp = System.currentTimeMillis();
	}

	FOPEvent(Object origin) {
		this(null, origin);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		FOPEvent other = (FOPEvent) obj;
		return Objects.equals(athlete, other.athlete) && Objects.equals(origin, other.origin);
	}

	public Athlete getAthlete() {
		return athlete;
	}

	/**
	 * @return the fop
	 */
	public FieldOfPlay getFop() {
		return fop;
	}

	public Object getOrigin() {
		return origin;
	}

	public String getStackTrace() {
		return stackTrace;
	}

	@Override
	public int hashCode() {
		// by default, events are always different, unless they override hashcode.
		// this is because some events such as WeightChange do not currently carry their
		// value,
		// so they come out as always duplicates unless we put a time stamp.
		return Objects.hash(athlete, origin, timestamp, this.getClass());
	}

	public void setAthlete(Athlete athlete) {
		this.athlete = athlete;
	}

	void setFop(FieldOfPlay fieldOfPlay) {
		this.fop = fieldOfPlay;
	}

}
