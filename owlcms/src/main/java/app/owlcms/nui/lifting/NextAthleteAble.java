package app.owlcms.nui.lifting;

import java.util.List;

import app.owlcms.data.athlete.Athlete;

public interface NextAthleteAble {
	
	List<Athlete> getAthletes();
	
	Athlete getNextAthlete(Athlete current);
	
	Athlete getPreviousAthlete(Athlete current);

}
