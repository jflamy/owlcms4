package app.owlcms.nui.displays;

import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public abstract class AbstractDisplayPage extends Div implements DisplayParametersReader {

	protected boolean downSilenced;
	protected String routeParameter;
	protected boolean silenced;
	private Logger logger = (Logger) LoggerFactory.getLogger(AbstractDisplayPage.class);
	private boolean darkMode;
	private QueryParameters defaultParameters;
	private Dialog dialog;
	private Timer dialogTimer;
	private Location location;
	private UI locationUI;
	private boolean showInitialDialog;
	private Map<String, List<String>> urlParameterMap;
	private boolean video;
	private FieldOfPlay fop;
	private Group group;
	private boolean abbreviatedName;
	private Double emFontSize;
	private boolean leadersDisplay;
	private boolean recordsDisplay;
	private boolean publicDisplay;
	private Double teamWidth;
	private Component board;

	public AbstractDisplayPage() {
		this.addClickListener(c -> openDialog(getDialog()));
	}

	final public void addComponent(Component display) {
		display.addClassName(darkMode ? DisplayParameters.DARK : DisplayParameters.LIGHT);
		this.add(display);
	}
	
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		openDialog(getDialog());
	}

	@Override
	public abstract void addDialogContent(Component page, VerticalLayout vl);

	public void doChangeAbbreviated() {
		if (isAbbreviatedName()) {
			updateURLLocation(getLocationUI(), getLocation(), DisplayParameters.ABBREVIATED, "true");
		} else {
			updateURLLocation(getLocationUI(), getLocation(), ABBREVIATED, null);
		}
	}

	@Override
	public void pushEmSize() {
		// update the dialog
	}

	@Override
	public void pushTeamWidth() {
		// update the dialog
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ContentParameters#getDefaultParameters()
	 */
	@Override
	final public QueryParameters getDefaultParameters() {
		if (defaultParameters == null) {
			return QueryParameters.fromString("abb=false&singleRef=false&public=false&records=false&fop=A&dark=false&leaders=false&video=false");
		}
		return defaultParameters;
	}

	@Override
	final public Dialog getDialog() {
		return dialog;
	}

	@Override
	final public Timer getDialogTimer() {
		return dialogTimer;
	}

	@Override
	public Double getEmFontSize() {
		return emFontSize;
	}

	@Override
	final public FieldOfPlay getFop() {
		return fop;
	}

	@Override
	final public Group getGroup() {
		return group;
	}

	@Override
	final public Location getLocation() {
		return location;
	}

	@Override
	final public UI getLocationUI() {
		return locationUI;
	}

	@Override
	final public String getRouteParameter() {
		return routeParameter;
	}

	@Override
	public Double getTeamWidth() {
		return teamWidth;
	}

	@Override
	final public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	final public AbstractDisplayPage getWrapper() {
		return this;
	}

	@Override
	public final boolean isAbbreviatedName() {
		return abbreviatedName;
	}

	@Override
	final public boolean isDarkMode() {
		return darkMode;
	}

	@Override
	final public boolean isDownSilenced() {
		return downSilenced;
	}

	@Override
	public final boolean isLeadersDisplay() {
		return leadersDisplay;
	}

	@Override
	final public boolean isPublicDisplay() {
		return publicDisplay;
	}

	@Override
	public final boolean isRecordsDisplay() {
		return recordsDisplay;
	}

	@Override
	final public boolean isShowInitialDialog() {
		return showInitialDialog;
	}

	@Override
	final public boolean isSilenced() {
		return silenced;
	}

	@Override
	final public boolean isVideo() {
		return video;
	}

	@Override
	final public void setAbbreviatedName(boolean b) {
		((DisplayParameters) this.board).setAbbreviatedName(b);
		this.abbreviatedName = b;
	}

	public final void setBoard(Component board) {
		this.board = board;
	}

	@Override
	final public void setDarkMode(boolean darkMode) {
		((DisplayParameters) this.board).setDarkMode(darkMode);
		this.darkMode = darkMode;
	}

	@Override
	final public void setDefaultParameters(QueryParameters defaultParameters) {
		this.defaultParameters = defaultParameters;
	}

	@Override
	final public void setDialog(Dialog dialog) {
		this.dialog = dialog;
	}

	@Override
	final public void setDialogTimer(Timer dialogTimer) {
		this.dialogTimer = dialogTimer;
	}

	@Override
	final public void setDownSilenced(boolean silent) {
		((ContentParameters) this.board).setDownSilenced(silent);
		this.downSilenced = silent;
	}

	@Override
	final public void setEmFontSize(Double emFontSize) {
		logger.warn("**** setEmFontSize={}", emFontSize);
		this.emFontSize = emFontSize;
		((DisplayParameters) this.board).setEmFontSize(emFontSize);
		pushEmSize();
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	final public void setGroup(Group group) {
		this.group = group;
	}

	@Override
	final public void setLeadersDisplay(boolean showLeaders) {
		((DisplayParameters) this.board).setLeadersDisplay(showLeaders);
		this.leadersDisplay = showLeaders;
	}

	@Override
	final public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	final public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	final public void setPublicDisplay(boolean publicDisplay) {
		((DisplayParameters) this.board).setPublicDisplay(publicDisplay);
		this.publicDisplay = publicDisplay;
	}

	@Override
	final public void setRecordsDisplay(boolean showRecords) {
		((DisplayParameters) this.board).setRecordsDisplay(showRecords);
		this.recordsDisplay = showRecords;
	}

	@Override
	final public void setRouteParameter(String routeParameter) {
		((DisplayParameters) this.board).setRouteParameter(routeParameter);
		this.routeParameter = routeParameter;
	}

	@Override
	final public void setShowInitialDialog(boolean showInitialDialog) {
		this.showInitialDialog = showInitialDialog;
	}

	@Override
	final public void setSilenced(boolean silent) {
		((ContentParameters) this.board).setSilenced(silent);
		this.silenced = silent;
	}

	@Override
	final public void setTeamWidth(Double tw) {
		((DisplayParameters) this.board).setTeamWidth(tw);
		this.teamWidth = tw;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#setUrlParameterMap(java.util.Map)
	 */
	@Override
	final public void setUrlParameterMap(Map<String, List<String>> urlParameterMap) {
		this.urlParameterMap = urlParameterMap;
	}

	@Override
	final public void setVideo(boolean b) {
		// FIXME this.board.setVideo(b);
		this.video = b;
	}

	protected Component getBoard() {
		return this.board;
	}

}
