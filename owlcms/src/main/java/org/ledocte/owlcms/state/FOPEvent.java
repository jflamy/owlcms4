package org.ledocte.owlcms.state;

/**
 * The subclasses of FOPEvent are all the events that can take place on the field of play.
 * 
 * @author owlcms
 */
public class FOPEvent {

	static public class AthleteAnnounced extends FOPEvent {

	}

	static public class DecisionReset extends FOPEvent {

	}

	static public class DownSignal extends FOPEvent {

	}

	static public class IntermissionDone extends FOPEvent {

	}

	static public class IntermissionStarted extends FOPEvent {

	}

	static public class LiftingOrderUpdated extends FOPEvent {

	}

	static public class RefereeDecision extends FOPEvent {
		public Boolean success = null;

		public RefereeDecision(boolean success) {
			this.success = success;
		}

	}

	static public class TimeStartedByTimeKeeper extends FOPEvent {

	}

	static public class TimeStoppedByTimeKeeper extends FOPEvent {

	}

}
