package app.owlcms.nui.displays;

import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public abstract class AbstractDisplayPage extends VerticalLayout implements DisplayParameters {
	
	Logger logger = (Logger) LoggerFactory.getLogger(AbstractDisplayPage.class);

	private boolean darkMode;
	private Dialog dialog;
	private Timer dialogTimer;
	private Location location;
	private UI locationUI;
	private String routeParameter;
	private boolean showInitialDialog;
	private boolean silenced;
	private Map<String, List<String>> urlParameterMap;
	private boolean video;
	private boolean downSilenced;
	private QueryParameters defaultParameters;

	public AbstractDisplayPage() {
		this.addClickListener(c -> openDialog(getDialog()));
		this.setSizeFull();
		this.setMargin(false);
		this.setPadding(false);
	}

	public void addComponent(Component display) {
		this.add(display);
	}

	@Override
	public abstract void addDialogContent(Component target, VerticalLayout vl);

	@Override
	public QueryParameters getDefaultParameters() {
		return defaultParameters;
	}

	@Override
	public Dialog getDialog() {
		return dialog;
	}

	@Override
	public Timer getDialogTimer() {
		return dialogTimer;
	}

	@Override
	public Location getLocation() {
		return location;
	}

	@Override
	public UI getLocationUI() {
		return locationUI;
	}

	@Override
	public String getRouteParameter() {
		return routeParameter;
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public boolean isDarkMode() {
		return darkMode;
	}

	@Override
	public boolean isDownSilenced() {
		logger.warn("AbstractDisplayPage down silent={}",silenced);
		return downSilenced;
	}

	@Override
	public boolean isShowInitialDialog() {
		return showInitialDialog;
	}

	@Override
	public boolean isSilenced() {
		logger.warn("AbstractDisplayPage timer silent={}",silenced);
		return silenced;
	}

	@Override
	public boolean isVideo() {
		return video;
	}

	@Override
	public void setDarkMode(boolean darkMode) {
		this.darkMode = darkMode;
	}

	@Override
	public void setDefaultParameters(QueryParameters defaultParameters) {
		logger.warn("setting defaults");
		this.defaultParameters = defaultParameters;
	}

	@Override
	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void setDialogTimer(Timer dialogTimer) {
		this.dialogTimer = dialogTimer;
	}

	@Override
	public void setDownSilenced(boolean silent) {
		logger.warn("setting AbstractDisplayPage down silent={}",silent);
		this.downSilenced = silent;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	@Override
	public void setShowInitialDialog(boolean showInitialDialog) {
		this.showInitialDialog = showInitialDialog;
	}

	@Override
	public void setSilenced(boolean silent) {
		logger.warn("setting AbstractDisplayPage timer silent={}",silent);
		this.silenced = silent;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> urlParameterMap) {
		this.urlParameterMap = urlParameterMap;
	}

	@Override
	public void setVideo(boolean video) {
		this.video = video;
	}

}
