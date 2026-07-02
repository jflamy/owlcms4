/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils.queryparameters;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;

import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.nui.displays.SoundEntries;
import app.owlcms.nui.lifting.MarshallContent;
import ch.qos.logback.classic.Logger;

public interface SoundParameters extends FOPParameters, SoundEntries {

	final Logger logger = (Logger) LoggerFactory.getLogger(SoundParameters.class);
	public static final String SILENT = "silent";
	public static final String SINGLEREF = "singleRef";
	public static final String IMMEDIATE = "immediate";
	public static final String DOWNSILENT = "downSilent";
	public static final String LIVE_LIGHTS = "liveLights";
	public static final String SHOW_DECLARATIONS = "declarations";
	public static final String START_ORDER = "startOrder";
	public static final String CENTER_NOTIFICATIONS = "centerNotifications";

	public default void buildDialog(Component target) {
	}

	public default boolean isCenterNotifications() {
		return false;
	}

	public default boolean isDeclarations() {
		return false;
	}

	public default boolean isDownSilenced() {
		return true;
	}

	public default boolean isLiveLights() {
		return !Config.getCurrent().featureSwitch(FeatureSwitch.NO_LIVE_LIGHTS);
	}

	public default boolean isSilenced() {
		return true;
	}

	public default boolean isSingleReferee() {
		return false;
	}

	public default boolean isStartOrder() {
		if (this instanceof MarshallContent) {
			return true;
		} else {
			return false;
		}
	}

	public default void setCenterNotifications(boolean showLiveLights) {
	}

	public default void setDeclarations(boolean showDeclarations) {
	}

	public default void setDownSilenced(boolean silent) {
	}

	public default void setLiveLights(boolean showLiveLights) {
	}

	public void setSilenced(boolean silent);

	public default void setSingleReferee(boolean b) {
	}

	public default void setStartOrder(boolean useStartOrder) {
	}
}
