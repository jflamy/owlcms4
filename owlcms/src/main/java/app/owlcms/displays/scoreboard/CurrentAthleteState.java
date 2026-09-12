/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/** Immutable state published atomically to the current-athlete displays. */
final class CurrentAthleteState {

	private final JsonArray athletes;
	private final JsonArray cjIndicatorClasses;
	private final JsonArray cjIndicators;
	private final JsonArray decisions;
	private final String fullName;
	private final String groupDescription;
	private final String lift;
	private final String mode;
	private final long sequence;
	private final boolean showAthleteClock;
	private final boolean showAttemptResults;
	private final boolean showBreakClock;
	private final boolean showDecisions;
	private final boolean showDetails;
	private final JsonArray snIndicatorClasses;
	private final JsonArray snIndicators;
	private final int startNumber;
	private final String team;
	private final String weight;

	private CurrentAthleteState(Builder builder) {
		this.athletes = copyArray(builder.athletes);
		this.cjIndicatorClasses = copyArray(builder.cjIndicatorClasses);
		this.cjIndicators = copyArray(builder.cjIndicators);
		this.decisions = copyArray(builder.decisions);
		this.fullName = builder.fullName;
		this.groupDescription = builder.groupDescription;
		this.lift = builder.lift;
		this.mode = builder.mode;
		this.sequence = builder.sequence;
		this.showAthleteClock = builder.showAthleteClock;
		this.showAttemptResults = builder.showAttemptResults;
		this.showBreakClock = builder.showBreakClock;
		this.showDecisions = builder.showDecisions;
		this.showDetails = builder.showDetails;
		this.snIndicatorClasses = copyArray(builder.snIndicatorClasses);
		this.snIndicators = copyArray(builder.snIndicators);
		this.startNumber = builder.startNumber;
		this.team = builder.team;
		this.weight = builder.weight;
	}

	static Builder builder(long sequence, String mode) {
		return new Builder(sequence, mode);
	}

	boolean isCurrentAthlete() {
		return "CURRENT_ATHLETE".equals(this.mode);
	}

	CurrentAthleteState withDecision(long sequence) {
		Builder builder = builder(sequence, this.mode)
		        .athletes(this.athletes)
		        .cjIndicatorClasses(this.cjIndicatorClasses)
		        .cjIndicators(this.cjIndicators)
		        .decisions(this.decisions)
		        .fullName(this.fullName)
		        .groupDescription(this.groupDescription)
		        .lift(this.lift)
		        .showAthleteClock(false)
		        .showAttemptResults(this.showAttemptResults)
		        .showBreakClock(false)
		        .showDecisions(true)
		        .showDetails(this.showDetails)
		        .snIndicatorClasses(this.snIndicatorClasses)
		        .snIndicators(this.snIndicators)
		        .startNumber(this.startNumber)
		        .team(this.team);
		builder.weight = this.weight;
		return builder.build();
	}

	JsonObject toJson() {
		JsonObject state = Json.createObject();
		state.put("athletes", copyArray(this.athletes));
		state.put("cjIndicatorClasses", copyArray(this.cjIndicatorClasses));
		state.put("cjIndicators", copyArray(this.cjIndicators));
		state.put("decisions", copyArray(this.decisions));
		state.put("fullName", this.fullName);
		state.put("groupDescription", this.groupDescription);
		state.put("lift", this.lift);
		state.put("mode", this.mode);
		state.put("sequence", this.sequence);
		state.put("showAthleteClock", this.showAthleteClock);
		state.put("showAttemptResults", this.showAttemptResults);
		state.put("showBreakClock", this.showBreakClock);
		state.put("showDecisions", this.showDecisions);
		state.put("showDetails", this.showDetails);
		state.put("snIndicatorClasses", copyArray(this.snIndicatorClasses));
		state.put("snIndicators", copyArray(this.snIndicators));
		state.put("startNumber", this.startNumber);
		state.put("team", this.team);
		state.put("weight", this.weight);
		return state;
	}

	private static JsonArray copyArray(JsonArray array) {
		JsonObject wrapper = Json.createObject();
		wrapper.put("array", array);
		return Json.parse(wrapper.toJson()).getArray("array");
	}

	static final class Builder {

		private JsonArray athletes = Json.createArray();
		private JsonArray cjIndicatorClasses = Json.createArray();
		private JsonArray cjIndicators = Json.createArray();
		private JsonArray decisions = Json.createArray();
		private String fullName = "";
		private String groupDescription = "";
		private String lift = "";
		private final String mode;
		private final long sequence;
		private boolean showAthleteClock;
		private boolean showAttemptResults;
		private boolean showBreakClock;
		private boolean showDecisions;
		private boolean showDetails;
		private JsonArray snIndicatorClasses = Json.createArray();
		private JsonArray snIndicators = Json.createArray();
		private int startNumber;
		private String team = "";
		private String weight = "";

		private Builder(long sequence, String mode) {
			this.sequence = sequence;
			this.mode = valueOrEmpty(mode);
		}

		Builder athletes(JsonArray athletes) {
			this.athletes = athletes != null ? athletes : Json.createArray();
			return this;
		}

		CurrentAthleteState build() {
			return new CurrentAthleteState(this);
		}

		Builder cjIndicatorClasses(JsonArray cjIndicatorClasses) {
			this.cjIndicatorClasses = cjIndicatorClasses != null ? cjIndicatorClasses : Json.createArray();
			return this;
		}

		Builder cjIndicators(JsonArray cjIndicators) {
			this.cjIndicators = cjIndicators != null ? cjIndicators : Json.createArray();
			return this;
		}

		Builder decisions(JsonArray decisions) {
			this.decisions = decisions != null ? decisions : Json.createArray();
			return this;
		}

		Builder fullName(String fullName) {
			this.fullName = valueOrEmpty(fullName);
			return this;
		}

		Builder groupDescription(String groupDescription) {
			this.groupDescription = valueOrEmpty(groupDescription);
			return this;
		}

		Builder lift(String lift) {
			this.lift = valueOrEmpty(lift);
			return this;
		}

		Builder showAthleteClock(boolean showAthleteClock) {
			this.showAthleteClock = showAthleteClock;
			return this;
		}

		Builder showAttemptResults(boolean showAttemptResults) {
			this.showAttemptResults = showAttemptResults;
			return this;
		}

		Builder showBreakClock(boolean showBreakClock) {
			this.showBreakClock = showBreakClock;
			return this;
		}

		Builder showDecisions(boolean showDecisions) {
			this.showDecisions = showDecisions;
			return this;
		}

		Builder showDetails(boolean showDetails) {
			this.showDetails = showDetails;
			return this;
		}

		Builder snIndicatorClasses(JsonArray snIndicatorClasses) {
			this.snIndicatorClasses = snIndicatorClasses != null ? snIndicatorClasses : Json.createArray();
			return this;
		}

		Builder snIndicators(JsonArray snIndicators) {
			this.snIndicators = snIndicators != null ? snIndicators : Json.createArray();
			return this;
		}

		Builder startNumber(Integer startNumber) {
			this.startNumber = startNumber != null ? startNumber : 0;
			return this;
		}

		Builder team(String team) {
			this.team = valueOrEmpty(team);
			return this;
		}

		Builder weight(Integer weight) {
			this.weight = weight != null && weight > 0 ? weight.toString() : "";
			return this;
		}

		private static String valueOrEmpty(String value) {
			return value != null ? value : "";
		}
	}
}