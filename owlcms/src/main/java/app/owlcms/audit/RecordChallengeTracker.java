package app.owlcms.audit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.records.RecordEvent;

public class RecordChallengeTracker {
	private Set<String> previous = Set.of();

	public List<RecordEvent> newlyChallenged(Athlete athlete, int attempt, Integer requested,
			List<RecordEvent> challenged) {
		Set<String> current = new HashSet<>();
		List<RecordEvent> result = challenged.stream()
				.filter(record -> current.add(key(athlete, attempt, requested, record)))
				.filter(record -> !this.previous.contains(key(athlete, attempt, requested, record)))
				.toList();
		this.previous = current;
		return result;
	}

	public void clear() {
		this.previous = Set.of();
	}

	private String key(Athlete athlete, int attempt, Integer requested, RecordEvent record) {
		return athlete.getId() + ":" + attempt + ":" + requested + ":" + record.getRecordFederation() + ":"
				+ record.getRecordName() + ":" + record.getAgeGrp() + ":" + record.getBwCatString() + ":"
				+ record.getRecordLift();
	}
}