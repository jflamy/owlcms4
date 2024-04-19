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
 * The subclasses of FOPEvent are all the events that can take place on the field of play.
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
			return this.breakType;
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
			return this.timeRemaining;
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
			return getBreakType() == other.getBreakType() && this.countdownType == other.countdownType
			        && Objects.equals(this.targetTime, other.targetTime)
			        && Objects.equals(this.timeRemaining, other.timeRemaining) && Objects.equals(this.wait, other.wait);
		}

		public BreakType getBreakType() {
			return this.breakType;
		}

		public CountdownType getCountdownType() {
			return this.countdownType;
		}

		public LocalDateTime getTargetTime() {
			return this.targetTime;
		}

		public Integer getTimeRemaining() {
			return this.timeRemaining;
		}

		public Boolean getWait() {
			return this.wait;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
			        + Objects.hash(getBreakType(), this.countdownType, this.targetTime, this.timeRemaining, this.wait);
			return result;
		}

		public boolean isIndefinite() {
			if (this.countdownType != null) {
				return this.countdownType == CountdownType.INDEFINITE;
			} else {
				return getBreakType() == BreakType.JURY
				        || getBreakType() == BreakType.CHALLENGE
				        || getBreakType() == BreakType.TECHNICAL
				        || getBreakType() == BreakType.GROUP_DONE;
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
			return "BreakStarted [breakType=" + getBreakType() + ", countdownType=" + this.countdownType
			        + ", timeRemaining="
			        + this.timeRemaining + ", targetTime=" + this.targetTime + ", wait=" + this.wait + "]";
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
			return this.ceremonyType;
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
			return this.ceremony == other.ceremony && Objects.equals(this.ceremonyCategory, other.ceremonyCategory)
			        && Objects.equals(this.ceremonyGroup, other.ceremonyGroup);
		}

		public CeremonyType getCeremony() {
			return this.ceremony;
		}

		public Category getCeremonyCategory() {
			return this.ceremonyCategory;
		}

		public Group getCeremonyGroup() {
			return this.ceremonyGroup;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(this.ceremony, this.ceremonyCategory, this.ceremonyGroup);
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
			return "CeremonyStarted [ceremony=" + this.ceremony + ", ceremonyGroup=" + this.ceremonyGroup
			        + ", ceremonyCategory="
			        + this.ceremonyCategory + "]";
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
	 * No subclassing relationship with {@link ExplicitDecision} because of different @Subscribe requirements
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
			return Objects.equals(this.ref1, other.ref1) && Objects.equals(this.ref1Time, other.ref1Time)
			        && Objects.equals(this.ref2, other.ref2) && Objects.equals(this.ref2Time, other.ref2Time)
			        && Objects.equals(this.ref3, other.ref3) && Objects.equals(this.ref3Time, other.ref3Time);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result
			        + Objects.hash(this.ref1, this.ref1Time, this.ref2, this.ref2Time, this.ref3, this.ref3Time);
			return result;
		}

		public boolean isImmediate() {
			return this.immediate;
		}

		public void trace() {
			trace(this.ref1, this.ref2, this.ref3, this.immediate);
		}

		private void trace(Boolean ref1, Boolean ref2, Boolean ref3, boolean immediate) {
			this.logger.trace("decision full update {} {} {} {}", ref1, ref2, ref3, LoggerUtils.whereFrom(2));
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

		private boolean decision;
		private int refIndex;

		public DecisionUpdate(Object origin, int refIndex, boolean decision) {
			super(origin);
			this.setRefIndex(refIndex);
			this.setDecision(decision);
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
			return this.isDecision() == other.isDecision() && this.getRefIndex() == other.getRefIndex();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(this.isDecision(), this.getRefIndex());
			return result;
		}

		@Override
		public String toString() {
			return "[decision=" + this.isDecision() + ", refIndex=" + this.getRefIndex() + "]";
		}

		public int getRefIndex() {
			return refIndex;
		}

		public void setRefIndex(int refIndex) {
			this.refIndex = refIndex;
		}

		public boolean isDecision() {
			return decision;
		}

		public void setDecision(boolean decision) {
			this.decision = decision;
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
			return Objects.equals(this.ref1, other.ref1) && Objects.equals(this.ref2, other.ref2)
			        && Objects.equals(this.ref3, other.ref3) && Objects.equals(this.success, other.success);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(this.ref1, this.ref2, this.ref3, this.success);
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
			return this.timeAllowed == other.timeAllowed;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(this.timeAllowed);
			return result;
		}
	}

	static public class JuryDecision extends FOPEvent {
		/** The decision. true = good lift */
		public Boolean success = null;
		/** if true, the decision comes from the jury box, not from the announcer having been told */
		private boolean juryButton;

		public JuryDecision(Athlete athlete, Object origin, boolean decision, boolean juryButton) {
			super(athlete, origin);
			this.logger.trace("jury decision for {}", athlete);
			this.success = decision;
			this.juryButton = juryButton;
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
			return this.juryButton == other.juryButton && Objects.equals(this.success, other.success);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(this.juryButton, this.success);
			return result;
		}

		public boolean isJuryButton() {
			return this.juryButton;
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

		private int refNumber;

		public SummonReferee(Object origin, int refNumber) {
			super(origin);
			this.setRefNumber(refNumber);
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
			return getRefNumber() == other.getRefNumber();
		}

		public int getRefNumber() {
			return this.refNumber;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(getRefNumber());
			return result;
		}

		public void setRefNumber(int refNumber) {
			this.refNumber = refNumber;
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
			return Objects.equals(this.group, other.group);
		}

		public Group getGroup() {
			return this.group;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + Objects.hash(this.group);
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
			return this.resultChange;
		}

		public void setResultChange(boolean resultChange) {
			this.resultChange = resultChange;
		}

	}

	protected Athlete athlete;
	/**
	 * When a FOPEvent (for example stopping the clock) is handled, it is often reflected as a series of UIEvents (for
	 * example, all the displays running the clock get told to stop it). The user interface that gave the order doesn't
	 * want to be notified again, so we memorize which user interface element created the original order so it can
	 * ignore it.
	 */
	protected Object origin;
	final Logger logger = (Logger) LoggerFactory.getLogger(FOPEvent.class);
	private FieldOfPlay fop;
	private String stackTrace;
	private long timestamp;

	public FOPEvent(Athlete athlete, Object origin) {
		this.fop = OwlcmsSession.getFop();
		// if (this.fop == null) {
		// logger.error("no fop {}",LoggerUtils.stackTrace());
		// }
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
		return Objects.equals(this.athlete, other.athlete) && Objects.equals(this.origin, other.origin);
	}

	public Athlete getAthlete() {
		return this.athlete;
	}

	/**
	 * @return the fop
	 */
	public FieldOfPlay getFop() {
		return this.fop;
	}

	public Object getOrigin() {
		return this.origin;
	}

	public String getStackTrace() {
		return this.stackTrace;
	}

	@Override
	public int hashCode() {
		// by default, events are always different, unless they override hashcode.
		// this is because some events such as WeightChange do not currently carry their
		// value,
		// so they come out as always duplicates unless we put a time stamp.
		return Objects.hash(this.athlete, this.origin, this.timestamp, this.getClass());
	}

	public void setAthlete(Athlete athlete) {
		this.athlete = athlete;
	}

	void setFop(FieldOfPlay fieldOfPlay) {
		this.fop = fieldOfPlay;
	}

}
