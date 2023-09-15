package app.owlcms.apputils.queryparameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.displays.video.VideoCSSOverride;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

public interface ContentParametersReader extends ContentParameters, FOPParametersReader, VideoCSSOverride {

	final Logger logger = (Logger) LoggerFactory.getLogger(ContentParametersReader.class);

	public void addDialogContent(Component page, VerticalLayout vl);

	public Dialog getDialog();

	public default void openDialog(Dialog dialog) {
		logger.warn("openDialog {} {}", dialog, (dialog != null ? dialog.isOpened() : "-"));
		if (dialog == null) {
			buildDialog((Component) this);
			dialog = this.getDialog();
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

	public default void processBooleanParam(HashMap<String, List<String>> params, String paramName,
	        Consumer<Boolean> doer) {
		List<String> paramValues = params.get(paramName);
		logger.warn("param {} values={}", paramName, paramValues);
		boolean value = false;
		if (paramValues == null || paramValues.isEmpty()) {
			QueryParameters dp = getDefaultParameters();
			if (dp != null) {
				List<String> defaultValues = dp.getParameters().get(paramName);
				if (defaultValues != null) {
					logger.warn("param {} DEFAULT values={}", paramName, defaultValues);
					String defaultVal = defaultValues.get(0);
					value = defaultVal.toLowerCase().equals("true");
				}
			}
		} else {
			value = paramValues.get(0).toLowerCase().equals("true");
		}
		doer.accept(value);
		updateParam(params, paramName, value ? "true" : "false");
		logger.warn("updated values for {} {}", paramName, params.get(paramName));
	}

	@Override
	public default HashMap<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		HashMap<String, List<String>> params = FOPParametersReader.super.readParams(location, parametersMap);

		processBooleanParam(params, SILENT, (v) -> switchSoundMode(v, false));
		processBooleanParam(params, DOWNSILENT, (v) -> switchDownMode(v, false));
		processBooleanParam(params, SINGLEREF, (v) -> switchSingleRefereeMode((Component) this, v, false));

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
		if (fop != null) {
			fop.setAnnouncerDecisionImmediate(imm);
			switchImmediateDecisionMode((Component) this, imm, false);
			updateParam(params, IMMEDIATE, imm ? null : "false");
		}
		setUrlParameterMap(params);
		return params;
	}

	public void setDialog(Dialog nDialog);

	public void setDialogTimer(Timer timer);

	@Override
	public void setShowInitialDialog(boolean b);

	public default void switchDownMode(boolean silent, boolean updateURL) {
		logger.warn("switching down sound {}", silent);
		setDownSilenced(silent);

		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), DOWNSILENT, silent ? "true" : "false");
		}
	}

	public default void switchDownMode(Component target, boolean silent, boolean updateURL) {
		switchDownMode(silent, updateURL);
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

	public default void switchSingleRefereeMode(Component component, boolean b, boolean updateURL) {
		setSingleReferee(b);
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SINGLEREF, b ? "true" : "false");
		}
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ContentParametersReader#switchSoundMode(boolean, boolean)
	 */
	public default void switchSoundMode(boolean silent, boolean updateURL) {
		logger.warn("switching timer sound {}", silent);
		setSilenced(silent);
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), SILENT, silent ? "true" : "false");
		}
	}

	@Deprecated
	public default void switchSoundMode(Component target, boolean silent, boolean updateURL) {
		switchSoundMode(silent, updateURL);
	}
}