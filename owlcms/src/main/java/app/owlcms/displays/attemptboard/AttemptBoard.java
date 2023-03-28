/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.components.elements.Plates;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Attempt board.
 */

@SuppressWarnings({ "serial", "deprecation" })
@Tag("attempt-board-template")
@JsModule("./components/AttemptBoard.js")
@JsModule("./components/AudioContext.js")
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/plates.css")
@Route("displays/attemptBoard")

public class AttemptBoard extends PolymerTemplate<TemplateModel> implements DisplayParameters,
        SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle, RequireDisplayLogin {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AttemptBoard.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	public static void doNotification(AttemptBoard attemptBoard, String text, String recordText, String theme,
	        int duration) {
		attemptBoard.doNotification(text, recordText, theme, duration);
	}

	@Id("athleteTimer")
	protected AthleteTimerElement athleteTimer; // created by Flow during template instantiation

	@Id("breakTimer")
	protected BreakTimerElement breakTimer; // created by Flow during template instantiation

	@Id("decisions")
	protected DecisionElement decisions; // created by Flow during template instantiation

	private Dialog dialog;
	private boolean groupDone;
	private boolean initializationNeeded;
	private Location location;
	private UI locationUI;
	private Plates plates;
	private boolean silenced = true;
	private EventBus uiEventBus;
	private Timer dialogTimer;
	private boolean publicFacing;
	private boolean showBarbell;
	protected boolean teamFlags;
	protected boolean athletePictures;

	protected String routeParameter;

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	/**
	 * Instantiates a new attempt board.
	 */
	public AttemptBoard() {
		OwlcmsFactory.waitDBInitialized();
		// logger.debug("*** AttemptBoard new {}", LoggerUtils.whereFrom());
		athleteTimer.setOrigin(this);
		this.getElement().setProperty("kgSymbol", getTranslation("KgSymbol"));
		breakTimer.setParent("attemptBoard");
		checkImages();
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout dialog) {
		DisplayOptions.addSoundEntries(dialog, target, this);
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			BreakType breakType = fop.getBreakType();
			// logger.trace("doBreak({}) bt={} a={}}", e, breakType, fop.getCurAthlete());
			if (breakType == BreakType.GROUP_DONE) {
				doGroupDoneBreak(fop);
				return;
			} else if (breakType == BreakType.JURY) {
				doJuryBreak(fop, breakType);
				return;
			}

			this.getElement().setProperty("lastName", inferGroupName());
			this.getElement().setProperty("firstName", inferMessage(breakType, fop.getCeremonyType(), true));
			this.getElement().setProperty("teamName", "");

			setDisplayedWeight("");

			Athlete a = fop.getCurAthlete();
			if (a != null) {;
				this.getElement().setProperty("category", a.getCategory().getTranslatedName());
				String formattedAttempt = formatAttempt(a);
				this.getElement().setProperty("attempt - ", formattedAttempt);
				Integer nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
				setDisplayedWeight(nextAttemptRequestedWeight > 0 ? nextAttemptRequestedWeight.toString() : "");
				showPlates();
				// logger.trace("showingPlates {}",a.getNextAttemptRequestedWeight());
			} else {
				this.getElement().setProperty("attempt", "");
				setDisplayedWeight("");
			}

			breakTimer.setVisible(!fop.getBreakTimer().isIndefinite());

			uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
			// logger.trace("attemptBoard showWeights ? {}", fop.getCeremonyType());
			this.getElement().callJsFunction("doBreak", fop.getCeremonyType() == null);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
	}

	/**
	 * return dialog, but only on first call.
	 *
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#getDialog()
	 */
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
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Attempt") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public boolean isDarkMode() {
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.ui.shared.QueryParameterReader#isIgnoreGroupFromURL()
	 */
	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	/**
	 * @return the publicFacing
	 */
	public boolean isPublicFacing() {
		return publicFacing;
	}

	/**
	 * @return the showBarbell
	 */
	public boolean isShowBarbell() {
		return showBarbell;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
	 */
	@Override
	public boolean isShowInitialDialog() {
		return this.initializationNeeded;
	}

	@Override
	public boolean isSilenced() {
		return silenced;
	}

	@Override
	public boolean isSilencedByDefault() {
		return true;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setDarkMode(boolean)
	 */
	@Override
	public void setDarkMode(boolean dark) {
		// noop
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

	/**
	 * @param publicFacing the publicFacing to set
	 */
	public void setPublicFacing(boolean publicFacing) {
		this.getElement().setProperty("publicFacing", true);
		this.publicFacing = publicFacing;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}

	/**
	 * @param showBarbell the showBarbell to set
	 */
	public void setShowBarbell(boolean showBarbell) {
		this.getElement().setProperty("showBarbell", true);
		this.showBarbell = showBarbell;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setShowInitialDialog(boolean)
	 */
	@Override
	public void setShowInitialDialog(boolean b) {
		this.initializationNeeded = true;
	}

	@Override
	public void setSilenced(boolean silenced) {
		// logger.debug("{} setSilenced = {} from {}", this.getClass().getSimpleName(),
		// silenced,
		// LoggerUtils.whereFrom());
		this.athleteTimer.setSilenced(silenced);
		this.breakTimer.setSilenced(silenced);
		this.decisions.setSilenced(silenced);
		this.silenced = silenced;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Subscribe
	public void slaveBarbellOrPlatesChanged(UIEvent.BarbellOrPlatesChanged e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> showPlates());
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			spotlightRecords(OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			if (isDone()) {
				doDone(e.getAthlete().getGroup());
			} else {
				this.getElement().callJsFunction("reset");
			}
		});
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to
	 * show down on the slave devices -- the master device is the one where
	 * refereeing buttons are attached.
	 *
	 * @param e
	 */
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		// don't block others
		new Thread(() -> {
			UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
				this.getElement().setProperty("hideBecauseDecision", "hideBecauseDecision");
				this.getElement().callJsFunction("down");
			});
		}).start();
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Group g = e.getGroup();
			doDone(g);
			setDone(true);
		});
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			String text = "";
			String reversalText = "";
			if (e.getReversal() != null) {
				reversalText = e.getReversal() ? Translator.translate("JuryNotification.Reversal")
				        : Translator.translate("JuryNotification.Confirmed");
			}
			String style = "warning";
			int previousAttemptNo;
			switch (e.getDeliberationEventType()) {
			case BAD_LIFT:
				previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
				text = Translator.translate("JuryNotification.BadLift", reversalText,
				        "<br/>" + e.getAthlete().getFullName(),
				        previousAttemptNo % 3 + 1);
				style = "primary error";
				doNotification(this, text, null, style, (int) (2 * FieldOfPlay.DECISION_VISIBLE_DURATION));
				break;
			case GOOD_LIFT:
				previousAttemptNo = e.getAthlete().getAttemptsDone() - 1;
				text = Translator.translate("JuryNotification.GoodLift", reversalText,
				        "<br/>" + e.getAthlete().getFullName(),
				        previousAttemptNo % 3 + 1);
				style = "primary success";
				doNotification(this, text,
				        (e.getNewRecord() ? "<br/>" + Translator.translate("Scoreboard.NewRecord") : ""),
				        style,
				        (int) (2 * FieldOfPlay.DECISION_VISIBLE_DURATION));
				break;
			default:
				break;
			}

		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			FOPState state = fop.getState();
			uiEventLogger.debug("### {} {} isDisplayToggle={}", state, this.getClass().getSimpleName(),
			        e.isDisplayToggle());
			if (state == FOPState.BREAK) {
				if (e.isDisplayToggle()) {
					Athlete a = e.getAthlete();
					doAthleteUpdate(a);
				} else {
					doBreak(e);
				}
				return;
			} else if (state == FOPState.INACTIVE) {
				return;
			} else if (!e.isCurrentDisplayAffected()) {
				// order change does not affect current lifter
				return;
			} else {
				Athlete a = e.getAthlete();
				doAthleteUpdate(a);
			}
		}));
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to
	 * show decisions on the slave devices -- the master device is the one where
	 * refereeing buttons are attached.
	 *
	 * @param e
	 */
	@Subscribe
	public void slaveRefereeDecision(UIEvent.Decision e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		// hide the athleteTimer except if the decision came from this ui.
		// this does not actually display the down signal, it makes it so the decision
		// element can show the down or decision.
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			this.getElement().setProperty("hideBecauseDecision", "hideBecauseDecision");
			this.getElement().callJsFunction("down");
		});
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doNotEmpty();
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		// logger.debug("start lifting");
		if (e.getGroup() == null) {
			doEmpty();
			return;
		}
		doNotEmpty();
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			this.getElement().setProperty("hideBecauseDecision", "");
			this.getElement().setProperty("hideBecauseRecord", "");
			this.getElement().callJsFunction("reset");
		});
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
//        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
//            Athlete a = e.getAthlete();
//            if (a == null) {
//                OwlcmsSession.withFop(fop -> {
//                    List<Athlete> order = fop.getLiftingOrder();
//                    Athlete athlete = order.size() > 0 ? order.get(0) : null;
//                    doAthleteUpdate(athlete);
//                });
//            } else {
//                doAthleteUpdate(a);
//            }
//        });
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(OwlcmsSession.getFop());
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			OwlcmsSession.withFop(fop -> {
				switch (fop.getState()) {
				case INACTIVE:
					doEmpty();
					break;
				case BREAK:
					if (e.getGroup() == null) {
						doEmpty();
					} else {
						doNotEmpty();
						doBreak(e);
					}
					break;
				default:
					doNotEmpty();
					doAthleteUpdate(fop.getCurAthlete());
				}
			});
			// uiEventLogger./**/warn("#### reloading {}", this.getElement().getClass());
			// this.getElement().callJsFunction("reload");
		});
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#updateURLLocation(com.vaadin.flow.component.UI,
	 *      com.vaadin.flow.router.Location, java.lang.String, java.lang.String)
	 */
	@Override
	public void updateURLLocation(UI ui, Location location, String parameter, String mode) {
		TreeMap<String, List<String>> parametersMap = new TreeMap<>(location.getQueryParameters().getParameters());
		updateParam(parametersMap, DARK, null);
		updateParam(parametersMap, parameter, mode);
		FieldOfPlay fop = OwlcmsSession.getFop();
		updateParam(parametersMap, "fop", fop != null ? fop.getName() : null);
		setUrlParameterMap(parametersMap);
		Location location2 = new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(parametersMap)));
		ui.getPage().getHistory().replaceState(null, location2);
		setLocation(location2);
	}

	private void doDone(Group g) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (g != null) {
				this.getElement().setProperty("lastName", getTranslation("Group_number_done", g.toString()));
			} else {
				this.getElement().setProperty("lastName", "");
			}
			// erase record notification if any
			this.getElement().setProperty("recordKind", "recordNotification none");
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("firstName", "");
			setDisplayedWeight("");
			this.getElement().callJsFunction("groupDone");
			hidePlates();
		});
	}

	private void doGroupDoneBreak(FieldOfPlay fop) {
		Group group = fop.getGroup();
		Athlete a = fop.getCurAthlete();
		if (a != null && a.getAttemptsDone() < 6) {
			// the announcer has switched groups, but not started the introduction
			// countdown.
			doEmpty();
		} else {
			doNotEmpty();
			doDone(group);
		}
	}

	private void doJuryBreak(FieldOfPlay fop, BreakType breakType) {
		this.getElement().setProperty("lastName", inferGroupName());
		this.getElement().setProperty("firstName", inferMessage(breakType, fop.getCeremonyType(), true));
		this.getElement().setProperty("teamName", "");
		// hide the weight and plates.
		this.getElement().callJsFunction("doBreak", false);
	}

	private void doNotification(String text, String recordText, String theme, int duration) {
		Notification n = new Notification();
		// Notification theme styling is done in
		// META-INF/resources/frontend/styles/shared-styles.html
		n.getElement().getThemeList().add(theme);

		n.setDuration(duration);
		n.setPosition(Position.TOP_STRETCH);
		Div label = new Div();
		label.getElement().setProperty("innerHTML", text + (recordText != null ? recordText : ""));
		label.getElement().setAttribute("style", "text: align-center");
		label.addClickListener((event) -> n.close());
		label.setWidth("70vw");
		label.getStyle().set("font-size", "7vh");
		n.add(label);

		OwlcmsSession.withFop(fop -> {
//            this.getElement().callJsFunction("reset");
//            syncWithFOP(OwlcmsSession.getFop());
			n.open();
			return;
		});

		return;
	}

	private String formatAttempt(Athlete a) {
		Integer attemptsDone = a.getAttemptsDone();
		int attemptNo = attemptsDone + 1;
	    //logger.debug("attemptNo {}",attemptNo);
		String translation = Translator.translateOrElseNull("AttemptBoard_lift_attempt_number", getLocale());
		if (translation != null) {
			if (attemptNo <= 3) {
				translation = Translator.translate("AttemptBoard_lift_attempt_number", attemptNo, Translator.translate("AttemptBoard_lift.SNATCH"));
			} else {
				translation = Translator.translate("AttemptBoard_lift_attempt_number", attemptNo - 3, Translator.translate("AttemptBoard_lift.CLEANJERK"));
			}		
		} else {
			translation = Translator.translate("AttemptBoard_attempt_number", ((attemptsDone % 3) + 1) );
		}
		return translation;
	}

	private Object getOrigin() {
		return this;
	}

	private void hidePlates() {
		if (plates != null) {
			try {
				this.getElement().removeChild(plates.getElement());
			} catch (IllegalArgumentException e) {
				// ignore
			}
		}
		plates = null;
	}

	private void hideRecordInfo(FieldOfPlay fop) {
		this.getElement().setProperty("recordKind", "recordNotification none");
		this.getElement().setProperty("teamName", fop.getCurAthlete().getTeam());
		this.getElement().setProperty("hideBecauseRecord", "");
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting attempt board", fop.getLoggingName());
			setId("attempt-board-template");
		});
		setTranslationMap();
	}

	private boolean isDone() {
		return this.groupDone;
	}

	private void setDisplayedWeight(String weight) {
		this.getElement().setProperty("weight", weight);
	}

	private void setDone(boolean b) {
		this.groupDone = b;
	}

	private boolean setImgProp(String propertyName, String prefix, String name, String suffix) {
		boolean found;
		try {
			ResourceWalker.getFileOrResourcePath(prefix + name + suffix);
			found = true;
		} catch (FileNotFoundException e) {
			found = false;
		}
		if (found) {
			this.getElement().setProperty(propertyName, "<img src='local/" + prefix + name + suffix + "'></img>");
		} else {
			this.getElement().setProperty(propertyName, "");
		}
		return found;
	}

	private void showPlates() {
		AttemptBoard attemptBoard = this;
		OwlcmsSession.withFop((fop) -> {
			UIEventProcessor.uiAccess(this, uiEventBus, () -> {
				try {
					if (plates != null) {
						attemptBoard.getElement().removeChild(plates.getElement());
					}
					plates = new Plates();
					plates.computeImageArea(fop, false);
					Element platesElement = plates.getElement();
					// tell polymer that the plates belong in the slot named barbell of the template
					platesElement.setAttribute("slot", "barbell");
					platesElement.getStyle().set("font-size", "3.3vh");
					platesElement.getClassList().set("dark", true);
					attemptBoard.getElement().appendChild(platesElement);
				} catch (Throwable t) {
					t.printStackTrace();
				}
			});
		});
	}

	private void spotlightNewRecord() {
		this.getElement().setProperty("recordKind", "recordNotification new");
		this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.NewRecord"));
		this.getElement().setProperty("hideBecauseRecord", "hideBecauseRecord");
	}

	private void spotlightRecordAttempt() {
		this.getElement().setProperty("recordKind", "recordNotification attempt");
		this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.RecordAttempt"));
		this.getElement().setProperty("hideBecauseRecord", "hideBecauseRecord");
	}

	private void spotlightRecords(FieldOfPlay fop) {
		if (fop.getState() == FOPState.INACTIVE || fop.getState() == FOPState.BREAK) {
			hideRecordInfo(fop);
		} else if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
			spotlightNewRecord();
		} else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
			spotlightRecordAttempt();
		} else {
			hideRecordInfo(fop);
		}
	}

	private void syncWithFOP(FieldOfPlay fop) {
		// sync with current status of FOP
		if (fop.getState() == FOPState.INACTIVE && fop.getCeremonyType() == null) {
			doEmpty();
		} else {
			doNotEmpty();
			Athlete curAthlete = fop.getCurAthlete();
			if (fop.getState() == FOPState.BREAK || fop.getState() == FOPState.INACTIVE) {
				// logger.debug("syncwithfop {} {}",fop.getBreakType(), fop.getCeremonyType());
				if (fop.getCeremonyType() != null) {
					doBreak(fop);
				} else if (curAthlete != null && curAthlete.getAttemptsDone() >= 6) {
					doDone(fop.getGroup());
				} else {
					doBreak(fop);
				}
			} else {
				doAthleteUpdate(curAthlete);
				athleteTimer.syncWithFop();
			}
		}
	}

	protected void checkImages() {
		try {
			ResourceWalker.getFileOrResourcePath("pictures");
			athletePictures = true;
		} catch (FileNotFoundException e) {
			athletePictures = false;
		}

		try {
			ResourceWalker.getFileOrResourcePath("flags");
			teamFlags = true;
		} catch (FileNotFoundException e) {
			teamFlags = false;
		}
	}

	protected void doAthleteUpdate(Athlete a) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		FOPState state = fop.getState();
		if (fop.getState() == FOPState.INACTIVE
		        || (state == FOPState.BREAK && fop.getBreakType() == BreakType.GROUP_DONE)) {
			doEmpty();
			return;
		}

		if (a == null) {
			doEmpty();
			return;
		} else if (a.getAttemptsDone() >= 6) {
			doNotEmpty();
			setDone(true);
			return;
		}

		String lastName = a.getLastName();
		this.getElement().setProperty("lastName", lastName.toUpperCase());
		this.getElement().setProperty("firstName", a.getFirstName());
		this.getElement().setProperty("hideBecauseDecision", "");
		this.getElement().setProperty("category", a.getCategory().getTranslatedName());

		String team = a.getTeam();
		this.getElement().setProperty("teamName", team);
		this.getElement().setProperty("teamFlagImg", "");
		if (teamFlags && team != null) {
			boolean done;
			done = setImgProp("teamFlagImg", "flags/", team, ".svg");
			if (!done) {
				done = setImgProp("teamFlagImg", "flags/", team, ".png");
				if (!done) {
					done = setImgProp("teamFlagImg", "flags/", team, ".jpg");
				}
			}
		}

		String membership = a.getMembership();
		this.getElement().setProperty("athleteImg", "");
		if (athletePictures && membership != null) {
			boolean done;
			done = setImgProp("athleteImg", "pictures/", membership, ".jpg");
			if (!done) {
				done = setImgProp("athleteImg", "pictures/", membership, ".jpeg");
			}
			this.getElement().setProperty("WithPicture", done ? "WithPicture" : "");
		}

		spotlightRecords(fop);

		this.getElement().setProperty("startNumber", a.getStartNumber());
		String formattedAttempt = formatAttempt(a);
		this.getElement().setProperty("attempt", formattedAttempt);
		Integer nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
		setDisplayedWeight(nextAttemptRequestedWeight > 0 ? nextAttemptRequestedWeight.toString() : "");
		showPlates();
		this.getElement().callJsFunction("reset");

		setDone(false);
	}

	/**
	 * Restoring the attempt board during a break. The information about how/why the
	 * break was started is unavailable.
	 *
	 * @param fop
	 */
	protected void doBreak(FieldOfPlay fop) {
		// logger.debug("dobreak");
		this.getElement().setProperty("lastName", inferGroupName(fop.getCeremonyType()));
		this.getElement().setProperty("firstName", inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
		this.getElement().setProperty("teamName", "");
		this.getElement().setProperty("attempt", "");
		Athlete a = fop.getCurAthlete();
		if (a != null) {
			this.getElement().setProperty("category", a.getCategory().getTranslatedName());
			String formattedAttempt = formatAttempt(a);
			this.getElement().setProperty("attempt", formattedAttempt);
			Integer nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
			setDisplayedWeight(nextAttemptRequestedWeight > 0 ? nextAttemptRequestedWeight.toString() : "");
			showPlates();
		}

		boolean showWeights = fop.getCeremonyType() == null && fop.getGroup() != null;
		// logger.trace("*** doBreak {} {} {}", showWeights, fop.getCeremonyType(),
		// LoggerUtils.whereFrom());
		this.getElement().callJsFunction("doBreak", showWeights);
		uiEventLogger.debug("$$$ attemptBoard doBreak(fop)");
	}

	protected void doEmpty() {
		hidePlates();
		FieldOfPlay fop2 = OwlcmsSession.getFop();
		if (fop2.getGroup() == null) {
			setDisplayedWeight("");
		}
		boolean inactive = fop2 == null || fop2.getState() == FOPState.INACTIVE;
		// this.getElement().callJsFunction("clear");
		this.getElement().setProperty("inactiveBlockStyle", (inactive ? "display:grid" : "display:none"));
		this.getElement().setProperty("activeGridStyle", (inactive ? "display:none" : "display:grid"));
		this.getElement().setProperty("inactiveClass", (inactive ? "bigTitle" : ""));
		this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
	}

	protected void doNotEmpty() {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			boolean inactive = false;
			this.getElement().setProperty("inactiveBlockStyle", (inactive ? "display:grid" : "display:none"));
			this.getElement().setProperty("activeGridStyle", (inactive ? "display:none" : "display:grid"));
			this.getElement().setProperty("inactiveClass", (inactive ? "bigTitle" : ""));
		});
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			logger.debug("{}onAttach {}", fop.getLoggingName(), fop.getState());
			init();
			ThemeList themeList = UI.getCurrent().getElement().getThemeList();
			themeList.remove(Lumo.LIGHT);
			themeList.add(Lumo.DARK);

			SoundUtils.enableAudioContextNotification(this.getElement());

			syncWithFOP(fop);
			// we send on fopEventBus, listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
			this.getElement().setProperty("video", routeParameter != null ? routeParameter + "/" : "");
		});
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		this.getElement().setPropertyJson("t", translations);
	}
}
