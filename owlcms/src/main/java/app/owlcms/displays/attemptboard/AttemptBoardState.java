/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import tools.jackson.databind.node.ObjectNode;

import app.owlcms.utils.JsonUtils;

/** Immutable state published to the attempt board as one property. */
final class AttemptBoardState {

	private final String athleteImg;
	private final String attempt;
	private final String breakType;
	private final String category;
	private final String competitionName;
	// dormant: published for a future passive decision display, ignored by AttemptBoard.js
	private final boolean decisionVisible;
	private final String firstName;
	private final String firstNameSizeOverride;
	private final String lastName;
	private final String mode;
	private final String nameSizeOverride;
	private final boolean recordAttempt;
	private final boolean recordBroken;
	private final String recordMessage;
	private final int recordMessageSpeed;
	private final long sequence;
	private final int startNumber;
	private final String teamFlagImg;
	private final String teamName;
	private final String weight;

	private AttemptBoardState(Builder builder) {
		this.athleteImg = builder.athleteImg;
		this.attempt = builder.attempt;
		this.breakType = builder.breakType;
		this.category = builder.category;
		this.competitionName = builder.competitionName;
		this.decisionVisible = builder.decisionVisible;
		this.firstName = builder.firstName;
		this.firstNameSizeOverride = builder.firstNameSizeOverride;
		this.lastName = builder.lastName;
		this.mode = builder.mode;
		this.nameSizeOverride = builder.nameSizeOverride;
		this.recordAttempt = builder.recordAttempt;
		this.recordBroken = builder.recordBroken;
		this.recordMessage = builder.recordMessage;
		this.recordMessageSpeed = builder.recordMessageSpeed;
		this.sequence = builder.sequence;
		this.startNumber = builder.startNumber;
		this.teamFlagImg = builder.teamFlagImg;
		this.teamName = builder.teamName;
		this.weight = builder.weight;
	}

	static Builder builder(long sequence, String mode) {
		return new Builder(sequence, mode);
	}

	/** New builder pre-filled with this state's fields, for record-only updates. */
	Builder copy(long sequence) {
		return new Builder(sequence, this.mode)
				.athleteImg(this.athleteImg)
				.attempt(this.attempt)
				.breakType(this.breakType)
				.category(this.category)
				.competitionName(this.competitionName)
				.decisionVisible(this.decisionVisible)
				.firstName(this.firstName)
				.firstNameSizeOverride(this.firstNameSizeOverride)
				.lastName(this.lastName)
				.nameSizeOverride(this.nameSizeOverride)
				.recordAttempt(this.recordAttempt)
				.recordBroken(this.recordBroken)
				.recordMessage(this.recordMessage)
				.recordMessageSpeed(this.recordMessageSpeed)
				.startNumber(this.startNumber)
				.teamFlagImg(this.teamFlagImg)
				.teamName(this.teamName)
				.weight(this.weight);
	}

	long getSequence() {
		return this.sequence;
	}

	ObjectNode toJson() {
		ObjectNode state = JsonUtils.object();
		state.put("athleteImg", this.athleteImg);
		state.put("attempt", this.attempt);
		state.put("breakType", this.breakType);
		state.put("category", this.category);
		state.put("competitionName", this.competitionName);
		state.put("decisionVisible", this.decisionVisible);
		state.put("firstName", this.firstName);
		state.put("firstNameSizeOverride", this.firstNameSizeOverride);
		state.put("lastName", this.lastName);
		state.put("mode", this.mode);
		state.put("nameSizeOverride", this.nameSizeOverride);
		state.put("recordAttempt", this.recordAttempt);
		state.put("recordBroken", this.recordBroken);
		state.put("recordMessage", this.recordMessage);
		state.put("recordMessageSpeed", this.recordMessageSpeed);
		state.put("sequence", this.sequence);
		state.put("startNumber", this.startNumber);
		state.put("teamFlagImg", this.teamFlagImg);
		state.put("teamName", this.teamName);
		state.put("weight", this.weight);
		return state;
	}

	static final class Builder {

		private String athleteImg = "";
		private String attempt = "";
		private String breakType = "";
		private String category = "";
		private String competitionName = "";
		private boolean decisionVisible;
		private String firstName = "";
		private String firstNameSizeOverride = "";
		private String lastName = "";
		private final String mode;
		private String nameSizeOverride = "";
		private boolean recordAttempt;
		private boolean recordBroken;
		private String recordMessage = "";
		private int recordMessageSpeed;
		private final long sequence;
		private int startNumber;
		private String teamFlagImg = "";
		private String teamName = "";
		private String weight = "";

		private Builder(long sequence, String mode) {
			this.sequence = sequence;
			this.mode = valueOrEmpty(mode);
		}

		Builder athleteImg(String athleteImg) {
			this.athleteImg = valueOrEmpty(athleteImg);
			return this;
		}

		Builder attempt(String attempt) {
			this.attempt = valueOrEmpty(attempt);
			return this;
		}

		Builder breakType(String breakType) {
			this.breakType = valueOrEmpty(breakType);
			return this;
		}

		AttemptBoardState build() {
			return new AttemptBoardState(this);
		}

		Builder category(String category) {
			this.category = valueOrEmpty(category);
			return this;
		}

		Builder competitionName(String competitionName) {
			this.competitionName = valueOrEmpty(competitionName);
			return this;
		}

		Builder decisionVisible(boolean decisionVisible) {
			this.decisionVisible = decisionVisible;
			return this;
		}

		Builder firstName(String firstName) {
			this.firstName = valueOrEmpty(firstName);
			return this;
		}

		Builder firstNameSizeOverride(String firstNameSizeOverride) {
			this.firstNameSizeOverride = valueOrEmpty(firstNameSizeOverride);
			return this;
		}

		Builder lastName(String lastName) {
			this.lastName = valueOrEmpty(lastName);
			return this;
		}

		Builder nameSizeOverride(String nameSizeOverride) {
			this.nameSizeOverride = valueOrEmpty(nameSizeOverride);
			return this;
		}

		Builder recordAttempt(boolean recordAttempt) {
			this.recordAttempt = recordAttempt;
			return this;
		}

		Builder recordBroken(boolean recordBroken) {
			this.recordBroken = recordBroken;
			return this;
		}

		Builder recordMessage(String recordMessage) {
			this.recordMessage = valueOrEmpty(recordMessage);
			return this;
		}

		Builder recordMessageSpeed(int recordMessageSpeed) {
			this.recordMessageSpeed = recordMessageSpeed;
			return this;
		}

		Builder startNumber(int startNumber) {
			this.startNumber = startNumber;
			return this;
		}

		Builder teamFlagImg(String teamFlagImg) {
			this.teamFlagImg = valueOrEmpty(teamFlagImg);
			return this;
		}

		Builder teamName(String teamName) {
			this.teamName = valueOrEmpty(teamName);
			return this;
		}

		Builder weight(String weight) {
			this.weight = valueOrEmpty(weight);
			return this;
		}

		private static String valueOrEmpty(String value) {
			return value != null ? value : "";
		}
	}
}