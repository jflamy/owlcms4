package app.owlcms.nui.displays;

import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public abstract class AbstractDisplayPage extends Div implements DisplayParametersReader {

	protected boolean downSilenced;
	protected String routeParameter;
	protected boolean silenced;
	private Logger logger = (Logger) LoggerFactory.getLogger(AbstractDisplayPage.class);
	private DisplayParameters board = null;
	private boolean darkMode;
	private QueryParameters defaultParameters;
	private Dialog dialog;
	private Timer dialogTimer;
	private Location location;
	private UI locationUI;
	private boolean showInitialDialog;
	private Map<String, List<String>> urlParameterMap;
	private boolean video;

	public AbstractDisplayPage() {
		this.addClickListener(c -> openDialog(getDialog()));
	}

	public void addComponent(Component display) {
		display.addClassName(darkMode ? DisplayParameters.DARK : DisplayParameters.LIGHT);
		this.add(display);
	}

	public DisplayParameters getBoard() {
		return board;
	}

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

	public AbstractDisplayPage getWrapper() {
		return this;
	}

	@Override
	public boolean isDarkMode() {
		return darkMode;
	}

	@Override
	public boolean isDownSilenced() {
		logger.warn("AbstractDisplayPage down silent={}", silenced);
		return downSilenced;
	}

	@Override
	public boolean isShowInitialDialog() {
		return showInitialDialog;
	}

	@Override
	public boolean isSilenced() {
		logger.warn("AbstractDisplayPage timer silent={}", silenced);
		return silenced;
	}

	@Override
	public boolean isSwitchableDisplay() {
		return false;
	}

	@Override
	public boolean isVideo() {
		return video;
	}

	public void setBoard(Component board) {
		this.board = (DisplayParameters) board;
	}

	@Override
	public void setDarkMode(boolean darkMode) {
		this.board.setDarkMode(darkMode);
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
		logger.warn("setting AbstractDisplayPage down silent={}", silent);
		this.board.setDownSilenced(silent);
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
		this.board.setRouteParameter(routeParameter);
		this.routeParameter = routeParameter;
	}

	@Override
	public void setShowInitialDialog(boolean showInitialDialog) {
		this.showInitialDialog = showInitialDialog;
	}

	@Override
	public void setSilenced(boolean silent) {
		logger.warn("setting AbstractDisplayPage timer silent={}", silent);
		this.silenced = silent;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> urlParameterMap) {
		this.urlParameterMap = urlParameterMap;
	}
}
