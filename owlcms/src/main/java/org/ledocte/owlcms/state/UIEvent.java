/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.state;

import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;

import com.vaadin.flow.component.UI;

/**
 * The subclasses of FOPEvent are all the events that can take place on the
 * field of play.
 * 
 * @author owlcms
 */
public class UIEvent {

	private Athlete athlete;

	private UI originatingUI;
	
	private UIEvent(UI originatingUI) {
		this.originatingUI = originatingUI;
	}

	private UIEvent(Athlete athlete, UI originatingUI) {
		this(originatingUI);
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
		public DecisionReset(UI originatingUI) {super(originatingUI);}
	}

	/**
	 * Class DownSignal.
	 */
	static public class DownSignal extends UIEvent {
		public DownSignal(UI originatingUI) {super(originatingUI);}
	}

	/**
	 * Class IntermissionDone.
	 */
	static public class IntermissionDone extends UIEvent {

		public IntermissionDone(Athlete athlete, UI originatingUI) {
			super(athlete, originatingUI);
		}

	}

	/**
	 * Class IntermissionStarted.
	 */
	static public class IntermissionStarted extends UIEvent {

		public IntermissionStarted(Athlete athlete, UI originatingUI) {
			super(athlete, originatingUI);
		}

	}

	
	/**
	 * Class LiftingOrderUpdated.
	 */
	static public class LiftingOrderUpdated extends UIEvent {

		private Athlete nextAthlete;
		private Athlete previousAthlete;
		private Integer timeAllowed;
		private List<Athlete> athletes;

		public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete, List<Athlete> athletes, Integer timeAllowed, UI originatingUI) {
			super(athlete, originatingUI);
			this.nextAthlete = nextAthlete;
			this.previousAthlete = previousAthlete;
			this.timeAllowed = timeAllowed;
			this.athletes = athletes;
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

		public List<Athlete> getAthletes() {
			return athletes;
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
		public RefereeDecision(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, UI originatingUI) {
			super(originatingUI);
			this.decision = decision;
			this.ref1 = ref1;
			this.ref2 = ref2;
			this.ref3 = ref3;
		}

	}

	static public class SetTime extends UIEvent {

		private Integer timeRemaining;

		public SetTime(Integer timeRemaining, UI originatingUI) {
			super(originatingUI);
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

		public StartTime(Integer timeRemaining, UI originatingUI) {
			super(originatingUI);
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

		public StopTime(int timeRemaining, UI originatingUI) {
			super(originatingUI);
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
	 * @return the originatingUI
	 */
	public UI getOriginatingUI() {
		return originatingUI;
	}

}
