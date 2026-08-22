/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import java.util.Locale;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;

import elemental.json.Json;
import elemental.json.JsonObject;

/** Parent-driven decision-light display with no event-bus ownership. */
@SuppressWarnings("serial")
@Tag("passive-decision-element")
@JsModule("./components/PassiveDecisionElement.js")
public class PassiveDecisionElement extends LitTemplate {

	private long commandSequence;

	public void reset(boolean singleRef) {
		setDecisionPayload("reset", null, null, null, singleRef);
	}

	public void showDecision(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, boolean singleRef) {
		Boolean displayedRef2 = singleRef && ref2 == null ? decision : ref2;
		setDecisionPayload("decision", ref1, displayedRef2, ref3, singleRef);
	}

	public void showLiveDecisions(Boolean ref1, Boolean ref2, Boolean ref3, boolean singleRef) {
		setDecisionPayload("decision", ref1, ref2, ref3, singleRef);
	}

	public void setDisplaySize(String size) {
		String normalized = size == null ? "small" : size.toLowerCase(Locale.ROOT);
		switch (normalized) {
			case "small":
			case "large":
			case "x-large":
				getElement().setProperty("size", normalized);
				break;
			default:
				throw new IllegalArgumentException("Unsupported decision element size: " + size);
		}
	}

	public void setPublicFacing(boolean publicFacing) {
		getElement().setProperty("publicFacing", publicFacing);
	}

	private void setDecisionPayload(String mode, Boolean ref1, Boolean ref2, Boolean ref3, boolean singleRef) {
		JsonObject payload = Json.createObject();
		payload.put("sequence", Long.toString(++this.commandSequence));
		payload.put("mode", mode);
		payload.put("singleRef", singleRef);
		putNullableBoolean(payload, "ref1", ref1);
		putNullableBoolean(payload, "ref2", ref2);
		putNullableBoolean(payload, "ref3", ref3);
		getElement().setPropertyJson("decisionPayload", payload);
	}

	private void putNullableBoolean(JsonObject payload, String key, Boolean value) {
		if (value == null) {
			payload.put(key, Json.createNull());
		} else {
			payload.put(key, value.booleanValue());
		}
	}
}