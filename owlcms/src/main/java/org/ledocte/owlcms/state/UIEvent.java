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

	private Athlete athlete;
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

	private UI ui;

	public UIEvent(Athlete athlete, UI ui) {
		this.athlete = athlete;
		this.ui = ui;
	}

	/**
	 * The Class AthleteAnnounced.
	 */
	static public class AthleteAnnounced extends UIEvent {
		public AthleteAnnounced(Athlete athlete, UI ui) {
			super(athlete, ui);
		}
	}

	/**
	 * The Class DecisionReset.
	 */
	static public class DecisionReset extends UIEvent {
		public DecisionReset(Athlete athlete, UI ui) {
			super(athlete, ui);
		}
	}

	/**
	 * The Class DownSignal.
	 */
	static public class DownSignal extends UIEvent {

		public DownSignal(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

	/**
	 * The Class IntermissionDone.
	 */
	static public class IntermissionDone extends UIEvent {

		public IntermissionDone(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

	/**
	 * The Class IntermissionStarted.
	 */
	static public class IntermissionStarted extends UIEvent {

		public IntermissionStarted(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

	/**
	 * The Class LiftingOrderUpdated.
	 */
	static public class LiftingOrderUpdated extends UIEvent {

		private Athlete nextAthlete;
		private Athlete previousAthlete;
		private Integer timeAllowed;

		/**
		 * @return the timeAllowed
		 */
		public Integer getTimeAllowed() {
			return timeAllowed;
		}

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

	}

	/**
	 * The Class RefereeDecision.
	 */
	static public class RefereeDecision extends UIEvent {

		/** The success. */
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

	/**
	 * The Class TimeStartedByTimeKeeper.
	 */
	static public class TimeStartedByTimeKeeper extends UIEvent {

		public TimeStartedByTimeKeeper(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

	/**
	 * The Class TimeStoppedByTimeKeeper.
	 */
	static public class TimeStoppedByTimeKeeper extends UIEvent {

		public TimeStoppedByTimeKeeper(Athlete athlete, UI ui) {
			super(athlete, ui);
		}

	}

}
