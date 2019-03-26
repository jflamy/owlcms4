/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.state;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;

/**
 * The subclasses of FOPEvent are all the events that can take place on the field of play.
 * 
 * @author owlcms
 */
public class FOPEvent {
	

	protected UI originatingUI;
	FOPEvent (UI originatingUI) {
		this.originatingUI = originatingUI;
	}
	
	public UI getOriginatingUI() {
		return originatingUI;
	}

	/**
	 * The Class AthleteAnnounced.
	 */
	static public class AthleteAnnounced extends FOPEvent {

		public AthleteAnnounced(UI originatingUI) {
			super(originatingUI);
		}

	}

	/**
	 * The Class DecisionReset.
	 */
	static public class DecisionReset extends FOPEvent {

		public DecisionReset(UI originatingUI) {
			super(originatingUI);
		}

	}

	/**
	 * The Class DownSignal.
	 */
	static public class DownSignal extends FOPEvent {

		public DownSignal(UI originatingUI) {
			super(originatingUI);
		}

	}

	/**
	 * The Class StartLifting.
	 */
	static public class StartLifting extends FOPEvent {

		public StartLifting(UI originatingUI) {
			super(originatingUI);
		}

	}

	/**
	 * The Class IntermissionStarted.
	 */
	static public class IntermissionStarted extends FOPEvent {

		public IntermissionStarted(UI originatingUI) {
			super(originatingUI);
		}

	}

	/**
	 * The Class LiftingOrderUpdated.
	 */
	static public class WeightChange extends FOPEvent {

		private Athlete athlete;

		public WeightChange(UI originatingUI, Athlete a) {
			super(originatingUI);
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
		public RefereeDecision(UI originatingUI, boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
			super(originatingUI);
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

		public TimeStartedManually(UI originatingUI) {
			super(originatingUI);
		}

	}

	/**
	 * The Class StopTime.
	 */
	static public class TimeStoppedManually extends FOPEvent {

		public TimeStoppedManually(UI originatingUI) {
			super(originatingUI);
		}

	}
	
	static public class TimeOver extends FOPEvent{

		public TimeOver(UI originatingUI) {
			super(originatingUI);
		}

	}
	

	static public class ForceTime extends FOPEvent {

		public int timeAllowed;

		public ForceTime(int timeAllowed, UI originatingUI) {
			super(originatingUI);
			this.timeAllowed = timeAllowed;
		}
	}

}
