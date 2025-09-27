/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.util.EnumMap;
import java.util.Locale;

import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;

/**
 * The Enum Gender.
 */
public enum Gender {
	F, M, I, MF;

	static Gender[] mfValueArray = new Gender[] { F, M };
	static Gender[] mfiValueArray = new Gender[] { F, M, I };
	static Gender[] mfmfValueArray = new Gender[] { F, M, MF};
	static Gender[] mfimfValueArray = new Gender[] { F, M, I, MF};

	public static Gender[] mfValues() {
		if (Competition.getCurrent().isGenderInclusive()) {
			return mfiValueArray;
		}
		return mfValueArray;
	}
	
	public static Gender[] mfmfValues() {
		if (Competition.getCurrent().isGenderInclusive()) {
			return mfimfValueArray;
		}
		return mfmfValueArray;
	}

	// map holding the translated public gender codes per gender
	private static final EnumMap<Gender, String> translatedGenderCodeMap = new EnumMap<>(Gender.class);

	public String asGenderName() {
		switch (this) {
			case F:
				return (Translator.translate("Gender.Women"));
			case I:
				return (Translator.translate("Gender.Inclusive"));
			case M:
				return (Translator.translate("Gender.Men"));
			case MF:
				return (Translator.translate("Gender.Mixed"));
			default:
				throw new IllegalStateException();
		}
	}

	public String asPublicGenderCode() {
		return getTranslatedGenderCode();
	}

	public static void initPublicGenderCodeMapString(Locale locale) {
		// need to use the system-wide language as defined in Config
		// create/populate the map used by getTranslatedGenderCode()
		for (Gender gender : Gender.values()) {
			translatedGenderCodeMap.put(gender,
					Translator.translateExplicitLocale("Gender." + gender.name(), locale));
		}
	}

	public String getTranslatedGenderCode() {
		switch (this) {
			case F:
			case I:
			case M:
			case MF:
				return translatedGenderCodeMap.get(this);
			default:
				throw new IllegalStateException();
		}
	}

	public void setTranslatedGenderCode(String ignored) {
		// do nothing, the translated code is static
	}
}
