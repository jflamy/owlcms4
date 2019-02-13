package org.ledocte.owlcms.state;

import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;

import com.google.common.eventbus.EventBus;

public class GroupState {

	public GroupState(List<Athlete> athletes) {
		// TODO Auto-generated constructor stub
	}

	public GroupState(EventBus eventBus, FieldOfPlayState fieldOfPlayState) {
		// TODO Auto-generated constructor stub
	}

	public int timeAllowed(Athlete a) {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<Athlete> getLifters() {
		// TODO Auto-generated method stub
		return null;
	}

	public void callLifter(Athlete curLifter) {
		// TODO Auto-generated method stub
		
	}

	public Object getTimer() {
		// TODO Auto-generated method stub
		return null;
	}

	public void pause() {
		// TODO Auto-generated method stub
		
	}

	public List<Athlete> getAttemptOrder() {
		// TODO Auto-generated method stub
		return null;
	}

	public Athlete getPreviousLifter() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTimeRemaining() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void liftDone(Athlete lifter, boolean b) {
		// TODO Auto-generated method stub
		
	}

	public void updateListsForLiftingOrderChange(Athlete lifter, boolean b, boolean c) {
		// TODO Auto-generated method stub
		
	}

}
