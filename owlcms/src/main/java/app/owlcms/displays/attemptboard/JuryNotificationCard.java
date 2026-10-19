/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
public class JuryNotificationCard extends Div {

	public JuryNotificationCard(String status, String decision, Athlete athlete, String attempt, boolean newRecord,
	        String notificationClass) {
		this(status, decision, athlete, attempt, newRecord, notificationClass, null);
	}

	/**
	 * @param rule the technical rule for a No Lift (seven-person Adjudication Panel), null if not shown
	 */
	public JuryNotificationCard(String status, String decision, Athlete athlete, String attempt, boolean newRecord,
	        String notificationClass, String rule) {
		boolean success = "successNotification".equals(notificationClass);

		addClassName("jury-notification-card");
		addClassName(success ? "jury-notification-card-success" : "jury-notification-card-error");

		Div meta = new Div();
		meta.addClassName("jury-notification-meta");
		if (status != null && !status.isBlank()) {
			Div statusDiv = new Div();
			statusDiv.addClassName("jury-notification-status");
			statusDiv.setText(status);
			meta.add(statusDiv);
		}
		if (newRecord) {
			meta.add(createJuryNotificationChip(Translator.translate("Scoreboard.NewRecord"), success, false, true));
		}

		Icon decisionIcon = success
		        ? VaadinIcon.CHECK_CIRCLE.create()
		        : VaadinIcon.CLOSE_CIRCLE.create();
		decisionIcon.addClassName("jury-notification-icon");

		Div decisionText = new Div();
		decisionText.addClassName("jury-notification-decision-text");
		decisionText.setText(decision);

		Div decisionDiv = new Div();
		decisionDiv.addClassName("jury-notification-decision");
		decisionDiv.add(decisionIcon, decisionText);

		Div athleteDiv = new Div();
		athleteDiv.addClassName("jury-notification-athlete");
		athleteDiv.setText(athlete != null ? athlete.getFullName() : "");

		Div attemptDiv = new Div();
		attemptDiv.addClassName("jury-notification-attempt");
		attemptDiv.add(new Html("<div>" + (attempt != null ? attempt : "") + "</div>"));

		add(meta, decisionDiv, athleteDiv, attemptDiv);
		if (rule != null && !rule.isBlank()) {
			Div ruleDiv = new Div();
			ruleDiv.addClassName("jury-notification-rule");
			ruleDiv.setText(rule);
			add(ruleDiv);
		}
	}

	private Div createJuryNotificationChip(String text, boolean success, boolean label, boolean record) {
		Div chip = new Div();
		chip.addClassName("jury-notification-chip");
		chip.addClassName(success ? "jury-notification-chip-success" : "jury-notification-chip-error");
		if (label) {
			chip.addClassName("jury-notification-chip-label");
		}
		if (record) {
			chip.addClassName("jury-notification-chip-record");
		}
		chip.setText(text);
		return chip;
	}
}