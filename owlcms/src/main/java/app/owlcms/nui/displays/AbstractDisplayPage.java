/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.group.Group;
import app.owlcms.displays.scoreboard.BaseResults;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public abstract class AbstractDisplayPage extends Div implements DisplayParametersReader, SafeEventBusRegistration {

	private static Logger logger = (Logger) LoggerFactory.getLogger(AbstractDisplayPage.class);

	public static Logger getLogger() {
		return logger;
	}

	public static void setLogger(Logger logger) {
		AbstractDisplayPage.logger = logger;
	}

	protected boolean downSilenced;
	protected String routeParameter;
	protected boolean silenced;
	private boolean darkMode;
	private QueryParameters defaultParameters;
	private Dialog dialog;
	private Timer dialogTimer;
	private Location location;
	private UI locationUI;
	private boolean showInitialDialog;
	private Map<String, List<String>> urlParameterMap;
	private boolean video;
	private boolean currentAttempt;
	private FieldOfPlay fop;
	private Group group;
	private boolean abbreviatedName;
	private Double emFontSize;
	private boolean leadersDisplay;
	private boolean recordsDisplay;
	private boolean publicDisplay;
	private String showMedals = "auto";
	private Double teamWidth;
	private Component board;

	public AbstractDisplayPage() {
		init();
		this.addClickListener(c -> openDialog(getDialog()));
	}

	final public void addComponent(Component display) {
		display.addClassName(this.darkMode ? DisplayParameters.DARK : DisplayParameters.LIGHT);
		this.add(display);
	}

	@Override
	public abstract void addDialogContent(Component page, VerticalLayout vl);

	public void addKeyboardShortcuts() {
	}

	public void doChangeAbbreviated() {
		if (isAbbreviatedName()) {
			updateURLLocation(getLocationUI(), getLocation(), DisplayParameters.ABBREVIATED, "true");
		} else {
			updateURLLocation(getLocationUI(), getLocation(), ABBREVIATED, null);
		}
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.SoundParameters#getDefaultParameters()
	 */
	@Override
	final public QueryParameters getDefaultParameters() {
		if (this.defaultParameters == null) {
			return QueryParameters.fromString(
			        "abb=false&singleRef=false&public=false&records=false&fop=A&dark=false&leaders=false&video=false");
		}
		return this.defaultParameters;
	}

	@Override
	final public Dialog getDialog() {
		return this.dialog;
	}

	@Override
	final public Timer getDialogTimer() {
		return this.dialogTimer;
	}

	@Override
	public Double getEmFontSize() {
		return this.emFontSize;
	}

	@Override
	final public FieldOfPlay getFop() {
		return this.fop;
	}

	@Override
	final public Group getGroup() {
		return this.group;
	}

	@Override
	final public Location getLocation() {
		return this.location;
	}

	@Override
	final public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	final public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public Double getTeamWidth() {
		return this.teamWidth;
	}

	@Override
	final public Map<String, List<String>> getUrlParameterMap() {
		return this.urlParameterMap;
	}

	final public AbstractDisplayPage getWrapper() {
		return this;
	}

	@Override
	public final boolean isAbbreviatedName() {
		return this.abbreviatedName;
	}

	@Override
	public final boolean isCurrentAttempt() {
		return this.currentAttempt;
	}

	@Override
	final public boolean isDarkMode() {
		return this.darkMode;
	}

	@Override
	final public boolean isDownSilenced() {
		return this.downSilenced;
	}

	@Override
	public final boolean isLeadersDisplay() {
		return this.leadersDisplay;
	}

	@Override
	final public boolean isPublicDisplay() {
		return this.publicDisplay;
	}

	@Override
	public final boolean isRecordsDisplay() {
		return this.recordsDisplay;
	}

	@Override
	public boolean isShowInitialDialog() {
		return this.showInitialDialog;
	}

	@Override
	final public boolean isSilenced() {
		return this.silenced;
	}

	@Override
	final public boolean isVideo() {
		return this.video;
	}

	@Override
	public void pushEmSize(Element element) {
		// update the dialog
	}

	@Override
	public void pushTeamWidth(Element element) {
		// update the dialog
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
		((SoundParameters) this.board).setDownSilenced(silent);
		this.downSilenced = silent;
	}

	@Override
	public void setCurrentAttempt(boolean currentAttempt) {
		((DisplayParameters) this.board).setCurrentAttempt(currentAttempt);
		this.currentAttempt = currentAttempt;
	}

	@Override
	public void setEmFontSize(Double emFontSize) {
		// clamp the value to something still visible
		if (emFontSize != null && emFontSize <= 0.1) {
			emFontSize = 0.1D;
		}
		this.emFontSize = emFontSize;
		((DisplayParameters) this.board).setEmFontSize(emFontSize);
		pushEmSize(this.getElement());
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
		// Propagate FOP to the board so it can access it via its own getFop()
		if (this.board instanceof FOPParameters) {
			((FOPParameters) this.board).setFop(fop);
		}
	}

	@Override
	final public void setGroup(Group group) {
		this.group = group;
		if (this.board instanceof FOPParameters) {
			((FOPParameters) this.board).setGroup(group);
		}
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
		((SoundParameters) this.board).setSilenced(silent);
		this.silenced = silent;
	}

	@Override
	public void setTeamWidth(Double tw) {
		if (tw != null && tw <= 0.0) {
			tw = 0.0D;
		}
		((DisplayParameters) this.board).setTeamWidth(tw);
		this.teamWidth = tw;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.ParameterReader#setUrlParameterMap(java.util.Map)
	 */
	@Override
	final public void setUrlParameterMap(Map<String, List<String>> urlParameterMap) {
		this.urlParameterMap = removeDefaultValues(urlParameterMap);
		if (this.board instanceof BaseResults) {
			BaseResults baseResults = (BaseResults) this.board;
			boolean hasGroupInUrl = this.urlParameterMap != null && this.urlParameterMap.containsKey(FOPParameters.GROUP);
			baseResults.setGroupPinnedFromURL(hasGroupInUrl);
		}
		if (this.board instanceof ResultsMedals) {
			ResultsMedals medals = (ResultsMedals) this.board;
			boolean hasCategoryInUrl = false;
			if (this.urlParameterMap != null) {
				List<String> catValues = this.urlParameterMap.get(DisplayParameters.CATEGORY);
				hasCategoryInUrl = catValues != null && !catValues.isEmpty() && catValues.get(0) != null
				        && !catValues.get(0).isBlank();
				if (hasCategoryInUrl) {
					String decoded = URLDecoder.decode(catValues.get(0), StandardCharsets.UTF_8);
					medals.setCategory(CategoryRepository.findByCode(decoded));
				}
			}
			medals.setCategoryPinnedFromURL(hasCategoryInUrl);
		}
	}

	@Override
	final public void setVideo(boolean b) {
		((DisplayParameters) this.board).setVideo(b);
		this.video = b;
	}

	@Override
	public boolean isGroupURLAllowedToMutateFop() {
		return false;
	}

	@Override
	public String isShowMedals() {
		return this.showMedals;
	}

	@Override
	public void setShowMedals(String value) {
		((DisplayParameters) this.board).setShowMedals(value);
		this.showMedals = value;
	}

	protected Component getBoard() {
		return this.board;
	}

	protected abstract void init();

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		FieldOfPlay fop = getFop();
		if (fop == null) {
			logger.error("No FOP provided to {} before attach; aborting registration", this.getClass().getSimpleName());
			return;
		}
		uiEventBusRegister(this, fop);
		if (isShowInitialDialog()) {
			openDialog(getDialog());
		}
		addKeyboardShortcuts();
		pushEmSize(this.getElement());
		pushTeamWidth(this.getElement());
	}
}
