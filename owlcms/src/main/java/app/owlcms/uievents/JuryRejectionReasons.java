/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import app.owlcms.i18n.Translator;

/**
 * Reasons given by the jury for its decision: the technical rules for a No Lift, and {@link #STANDS_AS_CALLED}.
 */
public final class JuryRejectionReasons {

	/** reason code to translation key; iteration order is the order shown to the jury */
	public static final Map<Integer, String> REASONS;
	static {
		Map<Integer, String> reasons = new LinkedHashMap<>();
		reasons.put(1, "Jury.incorrect.1");
		reasons.put(2, "Jury.incorrect.2");
		reasons.put(3, "Jury.incorrect.3");
		reasons.put(4, "Jury.incorrect.4");
		reasons.put(5, "Jury.incorrect.5");
		reasons.put(6, "Jury.incorrect.6");
		reasons.put(7, "Jury.incorrect.7");
		reasons.put(8, "Jury.incorrect.8");
		reasons.put(9, "Jury.incorrect.9");
		reasons.put(10, "Jury.incorrect.10");
		reasons.put(11, "Jury.incorrect.11");
		reasons.put(12, "Jury.incorrect.12");
		reasons.put(13, "Jury.incorrect.13");
		reasons.put(14, "Jury.incorrect.14");
		reasons.put(15, "Jury.incorrect.15");
		reasons.put(16, "Jury.incorrect.16");
		reasons.put(17, "Jury.incorrect.17");
		REASONS = Collections.unmodifiableMap(reasons);
	}

	/**
	 * Not a technical rule: a 3-, 4- or 5-member jury's review vote did not reach the jury's rule (unanimity, or a
	 * majority for five jurors with VPT), so the referees' decision stands as called. Never offered in the rule dialog.
	 */
	public static final int STANDS_AS_CALLED = 0;

	private JuryRejectionReasons() {
	}

	/** @return "code - translated reason", the translated text for {@link #STANDS_AS_CALLED}, or null if unknown */
	public static String label(Integer code) {
		if (code != null && code == STANDS_AS_CALLED) {
			return Translator.translate("Jury.StandsAsCalled");
		}
		String key = code != null ? REASONS.get(code) : null;
		return key != null ? code + " - " + Translator.translate(key) : null;
	}
}
