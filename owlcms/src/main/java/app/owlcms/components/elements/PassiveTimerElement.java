/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

import tools.jackson.databind.node.ObjectNode;

import app.owlcms.utils.JsonUtils;

/**
 * Parent-driven countdown display with no timer or event-bus ownership.
 */
@SuppressWarnings("serial")
@Tag("passive-timer-element")
@JsModule("./components/PassiveTimerElement.js")
public class PassiveTimerElement extends LitTemplate {

	private double initialWarningThresholdSeconds = -1.0D;
	private double finalWarningThresholdSeconds = -1.0D;
	private String initialWarningSoundUrl = "../local/sounds/initialWarning.mp3";
	private String finalWarningSoundUrl = "../local/sounds/finalWarning.mp3";
	private String timeOverSoundUrl = "../local/sounds/timeOver.mp3";
	private boolean silent = true;
	private long commandSequence;
	private long settingsSequence;

	public void applyState(boolean running, Integer milliseconds) {
		applyState(running, milliseconds, System.currentTimeMillis());
	}

	public void applyState(boolean running, Integer milliseconds, long issuedAtMillis) {
		if (running) {
			start(milliseconds, issuedAtMillis);
		} else {
			pause(milliseconds);
		}
	}

	public void display(Integer milliseconds) {
		setTimerCommand("display", milliseconds, System.currentTimeMillis());
	}

	public void pause(Integer milliseconds) {
		setTimerCommand("pause", milliseconds, System.currentTimeMillis());
	}

	public void setSilent(boolean silent) {
		this.silent = silent;
		syncSettings();
	}

	public void setSoundUrls(String initialWarningSoundUrl, String finalWarningSoundUrl,
			String timeOverSoundUrl) {
		this.initialWarningSoundUrl = initialWarningSoundUrl;
		this.finalWarningSoundUrl = finalWarningSoundUrl;
		this.timeOverSoundUrl = timeOverSoundUrl;
		syncSettings();
	}

	public void setWarningThresholds(double initialWarningThresholdSeconds,
			double finalWarningThresholdSeconds) {
		this.initialWarningThresholdSeconds = initialWarningThresholdSeconds;
		this.finalWarningThresholdSeconds = finalWarningThresholdSeconds;
		syncSettings();
	}

	public void start(Integer milliseconds) {
		start(milliseconds, System.currentTimeMillis());
	}

	public void start(Integer milliseconds, long issuedAtMillis) {
		long payloadIssuedAtMillis = System.currentTimeMillis();
		Integer adjustedMilliseconds = milliseconds;
		if (milliseconds != null && issuedAtMillis > 0L) {
			long serverDelayMillis = Math.max(0L, payloadIssuedAtMillis - issuedAtMillis);
			adjustedMilliseconds = (int) Math.max(0L, milliseconds.longValue() - serverDelayMillis);
		}
		setTimerCommand("start", adjustedMilliseconds, payloadIssuedAtMillis);
	}

	private ObjectNode createSettingsPayload() {
		ObjectNode payload = JsonUtils.object();
		payload.put("silent", this.silent);
		payload.put("initialWarningThresholdSeconds", this.initialWarningThresholdSeconds);
		payload.put("finalWarningThresholdSeconds", this.finalWarningThresholdSeconds);
		payload.put("initialWarningSoundUrl", this.initialWarningSoundUrl);
		payload.put("finalWarningSoundUrl", this.finalWarningSoundUrl);
		payload.put("timeOverSoundUrl", this.timeOverSoundUrl);
		return payload;
	}

	private void setTimerCommand(String command, Integer milliseconds, long issuedAtMillis) {
		ObjectNode payload = createSettingsPayload();
		payload.put("sequence", Long.toString(++this.commandSequence));
		payload.put("command", command);
		payload.put("seconds", milliseconds == null ? 0.0D : milliseconds / 1000.0D);
		payload.put("indefinite", milliseconds == null);
		payload.put("issuedAtMillis", Long.toString(issuedAtMillis));
		getElement().setPropertyJson("timerCommandPayload", payload);
	}

	private void syncSettings() {
		ObjectNode payload = createSettingsPayload();
		payload.put("sequence", Long.toString(++this.settingsSequence));
		getElement().setPropertyJson("timerSettingsPayload", payload);
	}
}