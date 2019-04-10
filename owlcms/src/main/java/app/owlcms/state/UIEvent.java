/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.state;

import java.util.List;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;

/**
 * The subclasses of FOPEvent are all the events that can take place on the
 * field of play.
 * 
 * @author owlcms
 */
public class UIEvent {

	private Athlete athlete;

	private Object origin;
	
	private UIEvent(Object origin) {
		this.origin = origin;
	}

	private UIEvent(Athlete athlete, Object origin) {
		this(origin);
		this.athlete = athlete;
	}


	/**
	 * Class AthleteAnnounced.
	 */
	static public class AthleteAnnounced extends UIEvent {
		public AthleteAnnounced(Athlete athlete, UI ui) {
			super(athlete, ui);
		}
	}
	/**
	 * Class DecisionReset.
	 */
	static public class DecisionReset extends UIEvent {
		public DecisionReset(Object origin) {super(origin);}
	}

	/**
	 * Class DownSignal.
	 */
	static public class DownSignal extends UIEvent {
		public DownSignal(Object origin) {super(origin);}
	}

	/**
	 * Class StartLifting.
	 */
	static public class BreakDone extends UIEvent {

		public BreakDone(Object origin) {
			super(origin);
		}

	}

	/**
	 * Class BreakStarted.
	 */
	static public class BreakStarted extends UIEvent {
		private Integer timeRemaining;
		
		public BreakStarted(Integer timeRemaining, Object origin) {
			super(origin);
			this.setTimeRemaining(timeRemaining);
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

		public void setTimeRemaining(Integer timeRemaining) {
			this.timeRemaining = timeRemaining;
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

		public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete, List<Athlete> liftingOrder, List<Athlete> displayOrder, Integer timeAllowed, Object origin) {
			super(athlete, origin);
			this.nextAthlete = nextAthlete;
			this.previousAthlete = previousAthlete;
			this.timeAllowed = timeAllowed;
			this.liftingOrder = liftingOrder;
			this.displayOrder = displayOrder;
		}

		public Athlete getNextAthlete() {
			return nextAthlete;
		}

		public Athlete getPreviousAthlete() {
			return previousAthlete;
		}

		/**
		 * @return the timeAllowed
		 */
		public Integer getTimeAllowed() {
			return timeAllowed;
		}

		public List<Athlete> getLiftingOrder() {
			return liftingOrder;
		}

		public List<Athlete> getDisplayOrder() {
			return displayOrder;
		}

	}
	
	/**
	 * Class RefereeDecision.
	 */
	static public class RefereeDecision extends UIEvent {

		public Boolean decision = null;
		public Boolean ref1;
		public Boolean ref2;
		public Boolean ref3;

		/**
		 * Instantiates a new referee decision.
		 *
		 * @param decision the decision
		 */
		public RefereeDecision(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, Object origin) {
			super(origin);
			this.decision = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
		}

	}

	static public class SetTime extends UIEvent {

		private Integer timeRemaining;

		public SetTime(Integer timeRemaining, Object origin) {
			super(origin);
			this.timeRemaining = timeRemaining;
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

	}

	/**
	 * Class StartTime.
	 */
	static public class StartTime extends UIEvent {

		private Integer timeRemaining;

		public StartTime(Integer timeRemaining, Object origin) {
			super(origin);
			this.timeRemaining = timeRemaining;
		}

		public Integer getTimeRemaining() {
			return timeRemaining;
		}

	}

	/**
	 * Class StopTime.
	 */
	static public class StopTime extends UIEvent {

		private int timeRemaining;

		public StopTime(int timeRemaining, Object origin) {
			super(origin);
			this.timeRemaining = timeRemaining;
		}
		
		public Integer getTimeRemaining() {
			return timeRemaining;
		}
	}


	/**
	 * @return the athlete
	 */
	public Athlete getAthlete() {
		return athlete;
	}

	/**
	 * @return the originating object
	 */
	public Object getOrigin() {
		return origin;
	}

}
