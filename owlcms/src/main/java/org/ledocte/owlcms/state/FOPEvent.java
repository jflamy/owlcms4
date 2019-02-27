/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.state;

import com.vaadin.flow.component.UI;

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
	 * The Class IntermissionDone.
	 */
	static public class IntermissionDone extends FOPEvent {

		public IntermissionDone(UI originatingUI) {
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
	static public class LiftingOrderUpdated extends FOPEvent {

		public LiftingOrderUpdated(UI originatingUI) {
			super(originatingUI);
		}

	}

	/**
	 * The Class RefereeDecision.
	 */
	static public class RefereeDecision extends FOPEvent {
		
		/** The success. */
		public Boolean success = null;

		/**
		 * Instantiates a new referee decision.
		 *
		 * @param success the success
		 */
		public RefereeDecision(UI originatingUI, boolean success) {
			super(originatingUI);
			this.success = success;
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

}
