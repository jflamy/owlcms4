package org.ledocte.owlcms.state;

public interface FOPEvent {

	public class IntermissionStarted {

	}

	public class IntermissionDone {

	}

	public class DownSignal {

	}

	public class RefereeDecision {
		public Boolean success = null;

		public RefereeDecision(boolean success) {
			this.success = success;
		}

	}

	public class LiftingOrderUpdated {

	}

	public class TimeStartedByTimeKeeper {

	}

	public class TimeStoppedByTimeKeeper {

	}

	public class AthleteAnnounced {

	}

}
