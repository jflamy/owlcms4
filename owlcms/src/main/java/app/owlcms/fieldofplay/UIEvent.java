/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import java.util.List;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;

/**
 * UIEvents are triggered in response to field of play events (FOPEvents).
 * 
 * Each field of play has an associated uiEventBus on which the user interface commands are posted.
 * The various browsers subscribe to UIEvents and react accordingly.
 * 
 * @author owlcms
 */
public class UIEvent {

	/**
	 * Class AthleteAnnounced.
	 */
	static public class AthleteAnnounced extends UIEvent {

		/**
		 * Instantiates a new athlete announced.
		 *
		 * @param athlete the athlete
		 * @param ui      the ui
		 */
		public AthleteAnnounced(Athlete athlete, UI ui) {
			super(athlete, ui);
		}
	}

	/**
	 * Class BreakDone.
	 */
	static public class BreakDone extends UIEvent {

		/**
		 * Instantiates a new break done.
		 *
		 * @param origin the origin
		 */
		public BreakDone(Object origin) {
			super(origin);
		}

	}

	/**
	 * Class BreakPaused.
	 */
	static public class BreakPaused extends UIEvent {

		/**
		 * Instantiates a new break paused.
		 *
		 * @param origin the origin
		 */
		public BreakPaused(Object origin) {
			super(origin);
		}
	}

	/**
	 * Class BreakStarted.
	 */
	static public class BreakStarted extends UIEvent {

		private FOPEvent.BreakStarted event;

		/**
		 * Instantiates a new break started.
		 *
		 * @param origin the origin
		 */
		public BreakStarted(Object origin) {
			super(origin);
		}

		public BreakStarted(FOPEvent.BreakStarted e, Object origin) {
			super(origin);
			this.setEvent(e);
		}

		public FOPEvent.BreakStarted getEvent() {
			return event;
		}

		public void setEvent(FOPEvent.BreakStarted event) {
			this.event = event;
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
		public DecisionReset(Object origin) {
			super(origin);
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

	/**
	 * Class LiftingOrderUpdated.
	 */
	static public class LiftingOrderUpdated extends UIEvent {

		private Athlete nextAthlete;
		private Athlete previousAthlete;
		private Integer timeAllowed;
		private List<Athlete> liftingOrder;
		private List<Athlete> displayOrder;

		/**
		 * Instantiates a new lifting order updated.
		 *
		 * @param athlete         the athlete
		 * @param nextAthlete     the next athlete
		 * @param previousAthlete the previous athlete
		 * @param liftingOrder    the lifting order
		 * @param displayOrder    the display order
		 * @param timeAllowed     the time allowed
		 * @param origin          the origin
		 */
		public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete,
				List<Athlete> liftingOrder, List<Athlete> displayOrder, Integer timeAllowed, Object origin) {
			super(athlete, origin);
			this.nextAthlete = nextAthlete;
			this.previousAthlete = previousAthlete;
			this.timeAllowed = timeAllowed;
			this.liftingOrder = liftingOrder;
			this.displayOrder = displayOrder;
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

	}

	/**
	 * Class RefereeDecision.
	 */
	static public class RefereeDecision extends UIEvent {

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
		public RefereeDecision(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, Object origin) {
			super(origin);
			this.decision = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
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

	/**
	 * Class StartTime.
	 */
	static public class StartTime extends UIEvent {

		private Integer timeRemaining;

		/**
		 * Instantiates a new start time.
		 *
		 * @param timeRemaining the time remaining
		 * @param origin        the origin
		 */
		public StartTime(Integer timeRemaining, Object origin) {
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

}
