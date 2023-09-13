package app.owlcms.nui.displays;

import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;

import app.owlcms.apputils.queryparameters.DisplayParameters;

@SuppressWarnings("serial")
public abstract class AbstractDisplayPage extends VerticalLayout implements DisplayParameters {

	private boolean darkMode;
	private Dialog dialog;
	private Timer dialogTimer;
	private Location location;
	private UI locationUI;
	private String routeParameter;
	private boolean showInitialDialog;
	private Map<String, List<String>> urlParameterMap;
	private boolean silenced;

	public AbstractDisplayPage() {
		this.addClickListener(c -> getDialog().open());
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
	public boolean isShowInitialDialog() {
		return showInitialDialog;
	}

	@Override
	public boolean isSilenced() {
		return silenced;
	}

	@Override
	public void openDialog() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setDarkMode(boolean darkMode) {
		this.darkMode = darkMode;
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
		this.silenced = silent;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> urlParameterMap) {
		this.urlParameterMap = urlParameterMap;
	}

}
