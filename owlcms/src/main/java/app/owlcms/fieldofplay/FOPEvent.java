/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.fieldofplay;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.ui.group.BreakDialog;
import app.owlcms.ui.group.BreakDialog.BreakType;

/**
 * The subclasses of FOPEvent are all the events that can take place on the field of play.
 * 
 * @author owlcms
 */
public class FOPEvent {
	
	/**
	 * When a FOPEvent (for example stopping the clock) is handled, it is often reflected
	 * as a series of UIEvents (for example, all the displays running the clock get told to
	 * stop it).  The user interface that gave the order doesn't want to be notified again,
	 * so we memorize which user interface element created the original order so it can ignore it.
	 */
	protected Object origin;
	FOPEvent (Object origin) {
		this.origin = origin;
	}
	
	public Object getOrigin() {
		return origin;
	}

	/**
	 * The Class AthleteAnnounced.
	 */
	static public class AthleteAnnounced extends FOPEvent {

		public AthleteAnnounced(Object object) {
			super(object);
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

	/**
	 * The Class DownSignal.
	 */
	static public class DownSignal extends FOPEvent {

		public DownSignal(Object origin) {
			super(origin);
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

	/**
	 * Class BreakStarted.
	 */
	static public class BreakStarted extends FOPEvent {

		private BreakType breakType;
		private int breakDuration;

		public BreakStarted(BreakDialog.BreakType breakType, int timeRemaining, Object origin) {
			super(origin);
			this.setBreakType(breakType);
			this.setBreakDuration(timeRemaining);
		}

		public BreakType getBreakType() {
			return breakType;
		}

		public void setBreakType(BreakType breakType) {
			this.breakType = breakType;
		}

		public int getBreakDuration() {
			return breakDuration;
		}

		public void setBreakDuration(int breakDuration) {
			this.breakDuration = breakDuration;
		}
	}
	
	/**
	 * Class BreakPaused.
	 */
	static public class BreakPaused extends FOPEvent {

		public BreakPaused(Object origin) {
			super(origin);
		}
	}

	/**
	 * The Class LiftingOrderUpdated.
	 */
	static public class WeightChange extends FOPEvent {

		private Athlete athlete;

		public WeightChange(Object origin, Athlete a) {
			super(origin);
			this.athlete = a;
		}

		public Athlete getAthlete() {
			return athlete;
		}

	}

	/**
	 * The Class RefereeDecision.
	 */
	static public class RefereeDecision extends FOPEvent {
		
		/** The decision. */
		public Boolean success = null;
		public Boolean ref1;
		public Boolean ref2;
		public Boolean ref3;

		/**
		 * Instantiates a new referee decision.
		 * @param decision the decision
		 * @param ref1 
		 * @param ref2 
		 * @param ref3 
		 */
		public RefereeDecision(Object origin, boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
			super(origin);
			this.success = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
		}

	}

	/**
	 * The Class StartTime.
	 */
	static public class TimeStartedManually extends FOPEvent {

		public TimeStartedManually(Object object) {
			super(object);
		}

	}

	/**
	 * The Class StopTime.
	 */
	static public class TimeStoppedManually extends FOPEvent {

		public TimeStoppedManually(Object object) {
			super(object);
		}

	}
	
	static public class TimeOver extends FOPEvent{

		public TimeOver(Object origin) {
			super(origin);
		}

	}
	

	static public class ForceTime extends FOPEvent {

		public int timeAllowed;

		public ForceTime(int timeAllowed, Object object) {
			super(object);
			this.timeAllowed = timeAllowed;
		}
	}

}
