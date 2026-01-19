/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.apputils.queryparameters;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;

import app.owlcms.displays.video.StylesDirSelection;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.AnnouncerContent;
import ch.qos.logback.classic.Logger;

public interface SoundParametersReader extends SoundParameters, FOPParametersReader, StylesDirSelection {

	final Logger logger = (Logger) LoggerFactory.getLogger(SoundParametersReader.class);

	public void addDialogContent(Component page, VerticalLayout vl);

	public Dialog getDialog();

	public default void openDialog(Dialog dialog) {
		if (dialog == null) {
			buildDialog((Component) this);
			dialog = this.getDialog();
		}
		if (dialog == null) {
			return;
		}

		final Dialog nDialog = dialog;
		if (!nDialog.isOpened()) {
			nDialog.open();
			setDialog(nDialog);
			UI ui = UI.getCurrent();
			Timer timer = new Timer();
			timer.schedule(
			        new TimerTask() {
				        @Override
				        public void run() {
					        try {
						        ui.access(() -> {
							        // logger.debug("timer closing {}", dialog);
							        nDialog.close();
						        });
					        } catch (Throwable e) {
						        // ignore.
					        }
				        }
			        }, 8 * 1000L);
			setDialogTimer(timer);
		}
	}

	@Override
	public default Map<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		Map<String, List<String>> params = FOPParametersReader.super.readParams(location, parametersMap);

		// Pass persist=false to avoid saving when reading URL params
		processBooleanParam(params, SILENT, (v) -> switchSoundMode(v, false, false));
		processBooleanParam(params, DOWNSILENT, (v) -> switchDownMode(v, false, false));
		processBooleanParam(params, SINGLEREF, (v) -> switchSingleRefereeMode((Component) this, v, false, false));
		processBooleanParam(params, LIVE_LIGHTS, (v) -> switchLiveLightsMode((Component) this, v, false, false));
		processBooleanParam(params, START_ORDER, (v) -> switchStartOrderMode((Component) this, v, false, false));
		processBooleanParam(params, SHOW_DECLARATIONS, (v) -> switchDeclarationsMode((Component) this, v, false, false));
		processBooleanParam(params, CENTER_NOTIFICATIONS, (v) -> switchCenteringMode((Component) this, v, false, false));

		// immediate is true by default, except if single ref.
		List<String> immParams = params.get(IMMEDIATE);
		boolean imm = true;
		if (immParams != null && !immParams.isEmpty()) {
			if (immParams.get(0).toLowerCase().equalsIgnoreCase("false")) {
				imm = false;
			} else if (immParams.get(0).toLowerCase().equalsIgnoreCase("true")) {
				imm = true;
			}
		} else if (isSingleReferee()) {
			imm = false;
		}
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null) {
			// Determine role name for UI settings storage from the class name
			// AnnouncerContent -> "announcer", MarshallContent -> "marshall", etc.
			String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
			
			// Handle immediate decision mode (announcer-specific)
			if (this instanceof AnnouncerContent) {
				fop.setAnnouncerDecisionImmediate(imm);
				switchImmediateDecisionMode((Component) this, imm, false);
				updateParam(params, IMMEDIATE, imm ? null : "false");
			}
			
			// If URL params are missing, restore from platform UI settings
			// This preserves settings when the page is closed and reopened
			if (params.get(SINGLEREF) == null || params.get(SINGLEREF).isEmpty()) {
				Boolean saved = (Boolean) fop.getPlatform().getUISetting(roleName, SINGLEREF, null);
				if (saved != null) {
					setSingleReferee(saved);
				}
			}
			if (params.get(LIVE_LIGHTS) == null || params.get(LIVE_LIGHTS).isEmpty()) {
				Boolean saved = (Boolean) fop.getPlatform().getUISetting(roleName, LIVE_LIGHTS, null);
				if (saved != null) {
					setLiveLights(saved);
				}
			}
			if (params.get(SHOW_DECLARATIONS) == null || params.get(SHOW_DECLARATIONS).isEmpty()) {
				Boolean saved = (Boolean) fop.getPlatform().getUISetting(roleName, SHOW_DECLARATIONS, null);
				if (saved != null) {
					setDeclarations(saved);
				}
			}
			if (params.get(CENTER_NOTIFICATIONS) == null || params.get(CENTER_NOTIFICATIONS).isEmpty()) {
				Boolean saved = (Boolean) fop.getPlatform().getUISetting(roleName, CENTER_NOTIFICATIONS, null);
				if (saved != null) {
					setCenterNotifications(saved);
				}
			}
			if (params.get(SILENT) == null || params.get(SILENT).isEmpty()) {
				Boolean saved = (Boolean) fop.getPlatform().getUISetting(roleName, SILENT, null);
				if (saved != null) {
					setSilenced(saved);
				}
			}
			if (params.get(DOWNSILENT) == null || params.get(DOWNSILENT).isEmpty()) {
				Boolean saved = (Boolean) fop.getPlatform().getUISetting(roleName, DOWNSILENT, null);
				if (saved != null) {
					setDownSilenced(saved);
				}
			}
			if (params.get(START_ORDER) == null || params.get(START_ORDER).isEmpty()) {
				Boolean saved = (Boolean) fop.getPlatform().getUISetting(roleName, START_ORDER, null);
				if (saved != null) {
					setStartOrder(saved);
				}
			}
			
			// Also update FOP's singleReferee field for announcer (runtime state)
			if (this instanceof AnnouncerContent) {
				fop.setSingleReferee(isSingleReferee());
			}
			// Note: No saves here - settings are only persisted when user changes them via cogwheel menu
		}
		setUrlParameterMap(removeDefaultValues(params));
		return params;
	}

	public void setDialog(Dialog nDialog);

	public void setDialogTimer(Timer timer);

	@Override
	public void setShowInitialDialog(boolean b);

	public default void switchCenteringMode(Component component, boolean centerNotification, boolean updateURL) {
		switchCenteringMode(component, centerNotification, updateURL, true);
	}

	public default void switchCenteringMode(Component component, boolean centerNotification, boolean updateURL, boolean persist) {
		setCenterNotifications(centerNotification);
		// Save to platform settings and persist if requested
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null && persist) {
			String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
			fop.getPlatform().setUISetting(roleName, CENTER_NOTIFICATIONS, centerNotification);
			fop.getPlatform().saveSettings();
		}
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), CENTER_NOTIFICATIONS, centerNotification ? "true" : "false");
		}
	}

	public default void switchDeclarationsMode(Component component, boolean showDeclarations, boolean updateURL) {
		switchDeclarationsMode(component, showDeclarations, updateURL, true);
	}

	public default void switchDeclarationsMode(Component component, boolean showDeclarations, boolean updateURL, boolean persist) {
		setDeclarations(showDeclarations);
		// Save to platform settings and persist if requested
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null && persist) {
			String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
			fop.getPlatform().setUISetting(roleName, SHOW_DECLARATIONS, showDeclarations);
			fop.getPlatform().saveSettings();
		}
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SHOW_DECLARATIONS, showDeclarations ? "true" : "false");
		}
	}

	public default void switchDownMode(boolean silent, boolean updateURL) {
		switchDownMode(silent, updateURL, true);
	}

	public default void switchDownMode(boolean silent, boolean updateURL, boolean persist) {
		setDownSilenced(silent);
		// Save to platform settings and persist if requested
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null && persist) {
			String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
			fop.getPlatform().setUISetting(roleName, DOWNSILENT, silent);
			fop.getPlatform().saveSettings();
		}
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), DOWNSILENT, silent ? "true" : "false");
		}
	}

	public default void switchDownMode(Component target, boolean silent, boolean updateURL) {
		switchDownMode(silent, updateURL, true);
	}

	public default void switchImmediateDecisionMode(Component component, boolean b, boolean updateURL) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop == null) {
			return;
		}
		fop.setAnnouncerDecisionImmediate(b);
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), IMMEDIATE, b ? null : "false");
		}
	}

	public default void switchLiveLightsMode(Component component, boolean liveLights, boolean updateURL) {
		switchLiveLightsMode(component, liveLights, updateURL, true);
	}

	public default void switchLiveLightsMode(Component component, boolean liveLights, boolean updateURL, boolean persist) {
		setLiveLights(liveLights);
		// Save to platform settings and persist if requested
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null && persist) {
			String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
			fop.getPlatform().setUISetting(roleName, LIVE_LIGHTS, liveLights);
			fop.getPlatform().saveSettings();
		}
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), LIVE_LIGHTS, liveLights ? "true" : "false");
		}
	}

	public default void switchSingleRefereeMode(Component component, boolean b, boolean updateURL) {
		switchSingleRefereeMode(component, b, updateURL, true);
	}

	public default void switchSingleRefereeMode(Component component, boolean b, boolean updateURL, boolean persist) {
		setSingleReferee(b);
		// Save to platform settings and persist if requested
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null) {
			if (persist) {
				String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
				fop.getPlatform().setUISetting(roleName, SINGLEREF, b);
				fop.getPlatform().saveSettings();
			}
			// Also update FOP's singleReferee field for announcer
			if (component instanceof AnnouncerContent) {
				fop.setSingleReferee(b);
			}
		}
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SINGLEREF, b ? "true" : "false");
		}
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.SoundParametersReader#switchSoundMode(boolean, boolean)
	 */
	public default void switchSoundMode(boolean silent, boolean updateURL) {
		switchSoundMode(silent, updateURL, true);
	}

	public default void switchSoundMode(boolean silent, boolean updateURL, boolean persist) {
		setSilenced(silent);
		// Save to platform settings and persist if requested
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null && persist) {
			String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
			fop.getPlatform().setUISetting(roleName, SILENT, silent);
			fop.getPlatform().saveSettings();
		}
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SILENT, silent ? "true" : "false");
		}
	}

	@Deprecated
	public default void switchSoundMode(Component target, boolean silent, boolean updateURL) {
		switchSoundMode(silent, updateURL, true);
	}

	public default void switchStartOrderMode(Component component, boolean startOrder, boolean updateURL) {
		switchStartOrderMode(component, startOrder, updateURL, true);
	}

	public default void switchStartOrderMode(Component component, boolean startOrder, boolean updateURL, boolean persist) {
		setStartOrder(startOrder);
		// Save to platform settings and persist if requested
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null && fop.getPlatform() != null && persist) {
			String roleName = this.getClass().getSimpleName().replace("Content", "").toLowerCase();
			fop.getPlatform().setUISetting(roleName, START_ORDER, startOrder);
			fop.getPlatform().saveSettings();
		}
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), START_ORDER, startOrder ? "true" : "false");
		}
	}
}