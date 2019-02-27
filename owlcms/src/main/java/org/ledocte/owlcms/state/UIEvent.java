/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.state;

import org.ledocte.owlcms.data.athlete.Athlete;

import com.vaadin.flow.component.UI;

/**
 * The subclasses of FOPEvent are all the events that can take place on the
 * field of play.
 * 
 * @author owlcms
 */
public class UIEvent {



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
		public DecisionReset(Athlete athlete, UI ui) {
			super(athlete, ui);
		}
	}

	/**
	 * Class DownSignal.
	 */
	static public class DownSignal extends UIEvent {

		public DownSignal(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

	/**
	 * Class IntermissionDone.
	 */
	static public class IntermissionDone extends UIEvent {

		public IntermissionDone(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

	/**
	 * Class IntermissionStarted.
	 */
	static public class IntermissionStarted extends UIEvent {

		public IntermissionStarted(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

	
	/**
	 * Class LiftingOrderUpdated.
	 */
	static public class LiftingOrderUpdated extends UIEvent {

		private Athlete nextAthlete;
		private Athlete previousAthlete;
		private Integer timeAllowed;

		public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete, Integer timeAllowed, UI ui) {
			super(athlete, ui);
			this.nextAthlete = nextAthlete;
			this.previousAthlete = previousAthlete;
			this.timeAllowed = timeAllowed;
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

	}
	
	/**
	 * Class RefereeDecision.
	 */
	static public class RefereeDecision extends UIEvent {

		public Boolean success = null;

		/**
		 * Instantiates a new referee decision.
		 *
		 * @param success the success
		 */
		public RefereeDecision(Athlete athlete, boolean success, UI ui) {
			super(athlete, ui);
			this.success = success;
		}

	}

	static public class SetTime extends UIEvent {

		private Integer timeRemaining;

		public SetTime(Integer timeRemaining) {
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

		public StartTime(Integer timeRemaining) {
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

		public StopTime() {}

	}

	private Athlete athlete;

	private UI ui;
	
	private UIEvent() {}

	private UIEvent(Athlete athlete, UI ui) {
		this.athlete = athlete;
		this.ui = ui;
	}

	/**
	 * @return the athlete
	 */
	public Athlete getAthlete() {
		return athlete;
	}

	/**
	 * @return the ui
	 */
	public UI getUi() {
		return ui;
	}

}
