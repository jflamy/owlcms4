/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.state;

/**
 * The subclasses of FOPEvent are all the events that can take place on the field of play.
 * 
 * @author owlcms
 */
public class FOPEvent {

	/**
	 * The Class AthleteAnnounced.
	 */
	static public class AthleteAnnounced extends FOPEvent {

	}

	/**
	 * The Class DecisionReset.
	 */
	static public class DecisionReset extends FOPEvent {

	}

	/**
	 * The Class DownSignal.
	 */
	static public class DownSignal extends FOPEvent {

	}

	/**
	 * The Class IntermissionDone.
	 */
	static public class IntermissionDone extends FOPEvent {

	}

	/**
	 * The Class IntermissionStarted.
	 */
	static public class IntermissionStarted extends FOPEvent {

	}

	/**
	 * The Class LiftingOrderUpdated.
	 */
	static public class LiftingOrderUpdated extends FOPEvent {

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
		public RefereeDecision(boolean success) {
			this.success = success;
		}

	}

	/**
	 * The Class TimeStartedByTimeKeeper.
	 */
	static public class TimeStartedByTimeKeeper extends FOPEvent {

	}

	/**
	 * The Class TimeStoppedByTimeKeeper.
	 */
	static public class TimeStoppedByTimeKeeper extends FOPEvent {

	}

}
