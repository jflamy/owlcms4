package app.owlcms.apputils.queryparameters;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public interface DisplayParametersReader extends ContentParametersReader, DisplayParameters {

	final Logger logger = (Logger) LoggerFactory.getLogger(DisplayParametersReader.class);

	@Override
	@SuppressWarnings("unchecked")
	public default void buildDialog(Component page) {
		Dialog dialog = getDialogCreateIfMissing();
		if (dialog == null) {
			return;
		}
		dialog.removeAll();

		dialog.setCloseOnOutsideClick(true);
		dialog.setCloseOnEsc(true);
		dialog.setModal(true);
		dialog.addDialogCloseActionListener(e -> {
			// logger.debug("closeActionListener {}", getDialog());
			getDialog().close();
		});
		dialog.setWidth("75%");

		VerticalLayout vl = new VerticalLayout();
		dialog.add(vl);

		addDialogContent(page, vl);

		Button button = new Button(Translator.translate("Close"), e -> {
			// logger.debug("close button {}", getDialog());
			getDialog().close();
		});
		button.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
		VerticalLayout buttons = new VerticalLayout();
		buttons.add(button);
		buttons.setWidthFull();
		buttons.setAlignSelf(Alignment.END, button);
		buttons.setMargin(false);
		vl.setAlignSelf(Alignment.END, buttons);

		vl.add(new Div());
		vl.add(buttons);
	}

	public default void doNotification(boolean dark) {
		Notification n = new Notification();
		H2 h2 = new H2();
		h2.setText(h2.getTranslation("darkMode." + (dark ? DARK : LIGHT)));
		h2.getStyle().set("margin", "0");
		n.add(h2);
		n.setDuration(3000);
		n.setPosition(Position.MIDDLE);
		if (dark) {
			n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
			h2.getStyle().set("color", "white");
		}
		n.getElement().getStyle().set("font-size", "x-large");
		n.open();
	}

	@Override
	public default Map<String, List<String>> readParams(Location location,
	        Map<String, List<String>> parametersMap) {
		// handle FOP and Group by calling superclass
		Map<String, List<String>> params = ContentParametersReader.super.readParams(location, parametersMap);
		
		processBooleanParam(params, DARK, (v) -> switchLightingMode(v, false));
		processBooleanParam(params, PUBLIC, (v) -> switchSwitchable(v, false));
		processBooleanParam(params, RECORDS, (v) -> switchRecords(v, false));
		processBooleanParam(params, LEADERS, (v) -> switchLeaders(v, false));
		processBooleanParam(params, ABBREVIATED, (v) -> switchAbbreviated(v, true));
		processBooleanParam(params, VIDEO, (v) -> switchVideo(v, true));

		List<String> sizeParams = params.get(FONTSIZE);
		Double emSize;
		try {
			emSize = (sizeParams != null && !sizeParams.isEmpty() ? Double.parseDouble(sizeParams.get(0)) : 0.0D);
			if (emSize > 0.0D) {
				switchEmFontSize(emSize, false);
			} else {
				switchEmFontSize(null, true);
			}
		} catch (NumberFormatException e) {
			switchEmFontSize(null, true);
		}

		List<String> twParams = params.get(TEAMWIDTH);
		Double tWidth;
		try {
			tWidth = (twParams != null && !twParams.isEmpty() ? Double.parseDouble(twParams.get(0)) : 0.0D);
			if (tWidth > 0.0D) {;
				switchTeamWidth(tWidth, false);
			} else {
				switchTeamWidth(null, true);
			}
		} catch (NumberFormatException e) {
			switchTeamWidth(null, true);
		}

		setUrlParameterMap(params);
		return params;
	}
	
	/*
	 * Process query parameters
	 *
	 * Note: what Vaadin calls a parameter is in the REST style, actually part of the URL path. We use the old-style
	 * Query parameters for our purposes.
	 *
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin. flow.router.BeforeEvent,
	 * java.lang.String)
	 */
	@Override
	public default void setParameter(BeforeEvent event, @OptionalParameter String routeParameter) {
		Location location = event.getLocation();
		setLocation(location);
		setLocationUI(event.getUI());
		setRouteParameter(routeParameter);
		ContentParametersReader.super.setParameter(event, routeParameter);
	}

	/**
	 * called by updateURLLocation
	 *
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#storeReturnURL()
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#updateURLLocation(UI, Location, String, String)
	 */
	@Override
	public default void storeReturnURL(Location location) {
		if (isPublicDisplay()) {
			// String trace = LoggerUtils.stackTrace();
			UI.getCurrent().getPage().fetchCurrentURL(url -> {
				String urlNonRelative = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
				String arg1 = urlNonRelative + location.getPathWithQueryParameters();
				if (arg1.contains("%")) {
					try {
						arg1 = URLDecoder.decode(arg1, StandardCharsets.UTF_8.name());
					} catch (UnsupportedEncodingException e) {
					}
				}
				// logger.debug("storing pageURL {} {}", arg1, trace);
				storeInSessionStorage("pageURL", url.toExternalForm());
			});
		}
	}

	default Dialog getDialogCreateIfMissing() {
		if (getDialog() == null) {
			setDialog(new Dialog());
		}
		getDialog().setResizable(true);
		getDialog().setDraggable(true);
		return getDialog();
	}
	
	public default void switchAbbreviated(boolean abbreviated, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), ABBREVIATED, Boolean.toString(abbreviated));
		}
		setAbbreviatedName(abbreviated);
	}

	public default void switchEmFontSize(Double emFontSize, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), FONTSIZE,
			        emFontSize != null ? emFontSize.toString() : null);
		}
		setEmFontSize(emFontSize);
	}

	public default void switchLeaders(boolean showLeaders, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), LEADERS, Boolean.toString(showLeaders));
		}
		setLeadersDisplay(showLeaders);
	}

	public default void switchLightingMode(boolean dark, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), DARK, Boolean.toString(dark));
		}
		// updateURLLocation might need the previous value, so we wait.
		setDarkMode(dark);
	}

	public default void switchRecords(boolean showRecords, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), RECORDS, Boolean.toString(showRecords));
		}
		setRecordsDisplay(showRecords);
	}

	public default void switchSwitchable(boolean switchable, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), PUBLIC, Boolean.toString(switchable));
		}
		setPublicDisplay(switchable);
	}

	public default void switchTeamWidth(Double teamWidth, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), TEAMWIDTH,
			        teamWidth != null ? teamWidth.toString() : null);
		}
		setTeamWidth(teamWidth);
	}
	
	public default void switchVideo(boolean video, boolean updateURL) {
		if (updateURL) {
			updateURLLocation(getLocationUI(), getLocation(), VIDEO, Boolean.toString(video));
		}
		setVideo(video);
	}

	Timer getDialogTimer();

}