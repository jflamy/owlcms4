/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.config;

public enum FeatureSwitchSection {
	USER_INTERFACE_OVERRIDE("FeatureSwitchSection.UserInterfaceOverride"),
	GENERAL_OPTIONS("FeatureSwitchSection.GeneralOptions"),
	SPECIALTY_FEATURES("FeatureSwitchSection.SpecialtyFeatures"),
	CURRENT_FEATURES("FeatureSwitchSection.CurrentFeatures"),
	OBSOLETE("FeatureSwitchSection.Obsolete"),
	USE_AT_YOUR_OWN_RISK("FeatureSwitchSection.UseAtYourOwnRisk"),
	INTERNAL("FeatureSwitchSection.Internal");

	private final String translationKey;

	FeatureSwitchSection(String translationKey) {
		this.translationKey = translationKey;
	}

	public String getTranslationKey() {
		return this.translationKey;
	}
}
