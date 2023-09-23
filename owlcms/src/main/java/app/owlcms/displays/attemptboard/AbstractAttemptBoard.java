/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.attemptboard;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.components.elements.PlatesElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.displays.video.StylesDirSelection;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.HasBoardMode;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
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
//@Tag("attempt-board-template")
//@JsModule("./components/AttemptBoard.js")
//@JsModule("./components/AudioContext.js")
//@JsModule("./components/TimerElement.js")
//@JsModule("./components/DecisionElement.js")
//@CssImport(value = "./styles/shared-styles.css")
//@CssImport(value = "./styles/plates.css")

public abstract class AbstractAttemptBoard extends LitTemplate implements
        DisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle,
        RequireDisplayLogin,
        StylesDirSelection, HasBoardMode {

	protected final static Logger logger = (Logger) LoggerFactory.getLogger(AbstractAttemptBoard.class);
	protected final static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	public static void doNotification(AbstractAttemptBoard attemptBoard, String text, String recordText, String theme,
	        int duration) {
		attemptBoard.doNotification(text, recordText, theme, duration);
	}

	/*
	 * The following 3 items need to be injected in the LitTemplate. Vaadin will create the slots and perform the
	 * injection based on the @Id annotation.
	 */
	@Id("athleteTimer")
	protected AthleteTimerElement athleteTimer; // created by Flow during template instantiation
	@Id("breakTimer")
	protected BreakTimerElement breakTimer; // created by Flow during template instantiation
	@Id("decisions")
	protected DecisionElement decisions; // created by Flow during template instantiation
	protected boolean athletePictures;
	protected String routeParameter;
	protected boolean teamFlags;
	protected EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private boolean silenced;
	private boolean downSilenced;
	private boolean groupDone;
	private PlatesElement plates;
	private boolean publicFacing;
	private boolean showBarbell;
	private boolean video;
	private FieldOfPlay fop;
	private Group group;
	private boolean abbreviatedName;

	/**
	 * Instantiates a new attempt board.
	 */
	public AbstractAttemptBoard() {
		OwlcmsFactory.waitDBInitialized();
		// logger.debug("*** AttemptBoard new {}", LoggerUtils.whereFrom());
		// athleteTimer.setOrigin(this);
		this.getElement().setProperty("kgSymbol", getTranslation("KgSymbol"));
		// breakTimer.setParent("attemptBoard");
		checkImages();
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			BreakType breakType = fop.getBreakType();

			setBoardMode(fop.getState(), breakType, fop.getCeremonyType(), this.getElement());

			// logger.trace("doBreak({}) bt={} a={}}", e, breakType, fop.getCurAthlete());
			if (breakType == BreakType.GROUP_DONE) {
				doGroupDoneBreak(fop);
				return;
			} else if (breakType == BreakType.JURY || breakType == BreakType.CHALLENGE) {
				doJuryBreak(fop, breakType);
				return;
			}

			this.getElement().setProperty("lastName", inferGroupName());
			this.getElement().setProperty("firstName", inferMessage(breakType, fop.getCeremonyType(), true));
			this.getElement().setProperty("teamName", "");

			setDisplayedWeight("");

			Athlete a = fop.getCurAthlete();
			if (a != null) {
				this.getElement().setProperty("category", a.getCategory().getTranslatedName());
				String formattedAttempt = formatAttempt(a);
				this.getElement().setProperty("attempt", formattedAttempt);
				Integer nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
				setDisplayedWeight(nextAttemptRequestedWeight > 0 ? nextAttemptRequestedWeight.toString() : "");
				showPlates();
			} else {
				this.getElement().setProperty("attempt", "");
				setDisplayedWeight("");
			}

			uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
			// logger.trace("attemptBoard showWeights ? {}", fop.getCeremonyType());
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			BreakType breakType = fop.getBreakType();
			setBoardMode(fop.getState(), breakType, fop.getCeremonyType(), this.getElement());
			this.getElement().setProperty("lastName", inferGroupName());
			this.getElement().setProperty("firstName", inferMessage(breakType, fop.getCeremonyType(), true));
			this.getElement().setProperty("teamName", "");
		}));
	}

	public DecisionElement getDecisions() {
		return decisions;
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
	public String getPageTitle() {
		return getTranslation("Attempt") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	final public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public final boolean isAbbreviatedName() {
		return abbreviatedName;
	}

	@Override
	public boolean isDarkMode() {
		return true;
	}

	@Override
	public boolean isDownSilenced() {
		return downSilenced;
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

	@Override
	public boolean isSilenced() {
		return silenced;
	}

	@Override
	public boolean isVideo() {
		return video;
	}

	@Override
	final public void setAbbreviatedName(boolean b) {
		this.abbreviatedName = b;
	}

	@Override
	final public void setDarkMode(boolean dark) {
		// always dark, see #isDarkMode
	}

	@Override
	public void setDownSilenced(boolean downSilenced) {
		this.decisions.setSilenced(downSilenced);
		this.downSilenced = downSilenced;
	}

	@Override
	public void setEmFontSize(Double emFontSize) {
	}

	@Override
	final public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	public void setGroup(Group group) {
		this.group = group;

	}

	@Override
	public void setLeadersDisplay(boolean showLeaders) {
	}

	@Override
	public void setPublicDisplay(boolean publicDisplay) {
	}

	/**
	 * @param publicFacing the publicFacing to set
	 */
	public void setPublicFacing(boolean publicFacing) {
		this.getElement().setProperty("publicFacing", true);
		decisions.setPublicFacing(publicFacing);
		this.publicFacing = publicFacing;
	}

	@Override
	public void setRecordsDisplay(boolean showRecords) {
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
		if (routeParameter != null && routeParameter.contentEquals("video")) {
			setVideo(true);
		}
	}

	/**
	 * @param showBarbell the showBarbell to set
	 */
	public void setShowBarbell(boolean showBarbell) {
		this.getElement().setProperty("showBarbell", true);
		this.showBarbell = showBarbell;
	}

	@Override
	public void setSilenced(boolean silenced) {
		this.athleteTimer.setSilenced(silenced);
		this.silenced = silenced;
	}

	@Override
	public void setTeamWidth(Double tw) {
	}

	@Override
	public void setVideo(boolean b) {
		this.video = b;
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
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			spotlightRecords(OwlcmsSession.getFop(), e.getAthlete());
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to show down on the slave devices -- the
	 * master device is the one where refereeing buttons are attached.
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
				this.getElement().setProperty("decisionVisible", true);
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
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			FOPState state = fop.getState();
			uiEventLogger.debug("### {} {} isDisplayToggle={}", state, this.getClass().getSimpleName(),
			        e.isDisplayToggle());
			if (state == FOPState.DECISION_VISIBLE) {
				// ignore -- decision reset will resync.
			} else if (state == FOPState.BREAK) {
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
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to show decisions on the slave devices --
	 * the master device is the one where refereeing buttons are attached.
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
			this.getElement().setProperty("decisionVisible", true);
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
			this.getElement().setProperty("decisionVisible", false);
			this.getElement().setProperty("recordName", "");
			this.getElement().setProperty("mode", BoardMode.CURRENT_ATHLETE.name());
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
					doInactive(fop, fop.getState());
					break;
				case BREAK:
					if (e.getGroup() == null) {
						doInactive(fop, fop.getState());
					} else {
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

	protected void checkImages() {
		teamFlags = URLUtils.checkFlags();
		athletePictures = URLUtils.checkPictures();
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
		// logger.debug("setting lastName");

		this.getElement().setProperty("lastName", lastName.toUpperCase());
		this.getElement().setProperty("firstName", a.getFirstName());
		this.getElement().setProperty("decisionVisible", false);
		this.getElement().setProperty("category", a.getCategory().getTranslatedName());
		this.getElement().setProperty("athletePictures", athletePictures);

		String team = a.getTeam();
		if (team == null) {
			team = "";
		}
		this.getElement().setProperty("teamName", team);
		this.getElement().setProperty("teamFlagImg", "");
		String teamFileName = URLUtils.sanitizeFilename(team);
		if (teamFlags && !team.isBlank()) {
			boolean done;
			done = URLUtils.setImgProp("teamFlagImg", "flags/", teamFileName, ".svg", this);
			if (!done) {
				done = URLUtils.setImgProp("teamFlagImg", "flags/", teamFileName, ".png", this);
				if (!done) {
					done = URLUtils.setImgProp("teamFlagImg", "flags/", teamFileName, ".jpg", this);
				}
			}
		}

		String membership = a.getMembership();
		this.getElement().setProperty("athleteImg", "");
		if (athletePictures && membership != null) {
			boolean done;
			done = URLUtils.setImgProp("athleteImg", "pictures/", membership, ".jpg", this);
			if (!done) {
				done = URLUtils.setImgProp("athleteImg", "pictures/", membership, ".jpeg", this);
			}
			this.getElement().setProperty("WithPicture", done ? "WithPicture" : "");
		}

		spotlightRecords(fop, a);

		this.getElement().setProperty("startNumber", a.getStartNumber());
		String formattedAttempt = formatAttempt(a);
		this.getElement().setProperty("attempt", formattedAttempt);
		Integer nextAttemptRequestedWeight = a.getNextAttemptRequestedWeight();
		setDisplayedWeight(nextAttemptRequestedWeight > 0 ? nextAttemptRequestedWeight.toString() : "");
		showPlates();
		this.getElement().setProperty("mode", BoardMode.CURRENT_ATHLETE.name());

		setDone(false);
	}

	/**
	 * Restoring the attempt board during a break. The information about how/why the break was started is unavailable.
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

		switch (fop.getBreakType()) {
		case BEFORE_INTRODUCTION:
			this.getElement().setProperty("introCountdownMode", "true");
			break;
		case CHALLENGE:
		case JURY:
		case TECHNICAL:
		case MARSHAL:
			this.getElement().setProperty("waitMode", "true");
			break;
		case FIRST_CJ:
		case FIRST_SNATCH:
			this.getElement().setProperty("liftCountdownMode", "true");
			break;
		case GROUP_DONE:
			this.getElement().setProperty("doneMode", "true");
			break;
		default:
			break;
		}
		uiEventLogger.debug("$$$ attemptBoard doBreak(fop)");
	}

	protected void doEmpty() {
		// logger.debug("****doEmpty");
		FieldOfPlay fop2 = OwlcmsSession.getFop();
		if (fop2.getGroup() == null) {
			setDisplayedWeight("");
		}
		this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setBoardMode(fop2.getState(), fop2.getBreakType(), fop2.getCeremonyType(), this.getElement());
		});
	}

	protected void doNotEmpty() {
		// logger.debug("****doNotEmpty");
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			FieldOfPlay fop2 = OwlcmsSession.getFop();
			UIEventProcessor.uiAccess(this, uiEventBus, () -> {
				setBoardMode(fop2.getState(), fop2.getBreakType(), fop2.getCeremonyType(), this.getElement());
			});
		});
	}

	protected Object getOrigin() {
		return this;
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			logger.debug("{}onAttach {}", fop.getLoggingName(), fop.getState());
			init();
			checkVideo(Config.getCurrent().getParamStylesDir() + "/video/attemptboard.css", this);
			ThemeList themeList = UI.getCurrent().getElement().getThemeList();
			themeList.remove(Lumo.LIGHT);
			themeList.add(Lumo.DARK);

			SoundUtils.enableAudioContextNotification(this.getElement());

			syncWithFOP(fop);
			// we send on fopEventBus, listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
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

	protected void syncWithFOP(FieldOfPlay fop) {
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
				// by the time we get called, possible that connection has been closed.
				Athlete nAthlete = AthleteRepository.findById(curAthlete.getId());
				doAthleteUpdate(nAthlete);
				athleteTimer.syncWithFop();
			}
		}
	}

	private void doDone(Group g) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (g != null) {
				this.getElement().setProperty("lastName", getTranslation("Group_number_done", g.toString()));
			} else {
				this.getElement().setProperty("lastName", "");
			}
			// erase record notification if any
			this.getElement().setProperty("recordName", "");
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("firstName", "");
			setDisplayedWeight("");
			hidePlates();
			setBoardMode(FOPState.BREAK, BreakType.GROUP_DONE, null, this.getElement());
		});
	}

	private void doGroupDoneBreak(FieldOfPlay fop) {
		Group group = fop.getGroup();
		Athlete a = fop.getCurAthlete();
		if (a != null && a.getAttemptsDone() < 6) {
			// the announcer has switched groups, but not started the introduction
			// countdown.
			doInactive(fop, fop.getState());
		} else {
			doDone(group);
		}
	}

	private void doInactive(FieldOfPlay fop, FOPState fopState) {
		setBoardMode(fopState, fopState == FOPState.BREAK ? fop.getBreakType() : null, fop.getCeremonyType(),
		        this.getElement());
		this.getElement().setProperty("lastName", inferGroupName(fop.getCeremonyType()));
		this.getElement().setProperty("firstName", inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
	}

	private void doJuryBreak(FieldOfPlay fop, BreakType breakType) {
		// break mode properties are already set.
		this.getElement().setProperty("lastName", inferGroupName());
		this.getElement().setProperty("firstName", inferMessage(breakType, fop.getCeremonyType(), true));
		this.getElement().setProperty("teamName", "");
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
		// logger.debug("attemptNo {}",attemptNo);
		String translation = Translator.translateOrElseNull("AttemptBoard_lift_attempt_number", getLocale());
		if (translation != null) {
			if (attemptNo <= 3) {
				translation = Translator.translate("AttemptBoard_lift_attempt_number", attemptNo,
				        Translator.translate("AttemptBoard_lift.SNATCH"));
			} else {
				translation = Translator.translate("AttemptBoard_lift_attempt_number", attemptNo - 3,
				        Translator.translate("AttemptBoard_lift.CLEANJERK"));
			}
		} else {
			translation = Translator.translate("AttemptBoard_attempt_number", ((attemptsDone % 3) + 1));
		}
		return translation;
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

	private void hideRecordInfo(Athlete a) {
		this.getElement().setProperty("recordName", "");
		this.getElement().setProperty("teamName", a.getTeam());
		this.getElement().setProperty("hideBecauseRecord", "");
		this.getElement().setProperty("recordAttempt", false);
		this.getElement().setProperty("recordBroken", false);
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting attempt board", fop.getLoggingName());
		});
		setTranslationMap();
	}

	@SuppressWarnings("unused")
	private boolean isDone() {
		return this.groupDone;
	}

	private void setDisplayedWeight(String weight) {
		this.getElement().setProperty("weight", weight);
	}

	private void setDone(boolean b) {
		this.groupDone = b;
	}

	private void showPlates() {
		AbstractAttemptBoard attemptBoard = this;
		OwlcmsSession.withFop((fop) -> {
			UIEventProcessor.uiAccess(this, uiEventBus, () -> {
				try {
					if (plates != null) {
						attemptBoard.getElement().removeChild(plates.getElement());
					}
					plates = new PlatesElement();
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
		this.getElement().setProperty("recordBroken", true);
		this.getElement().setProperty("recordAttempt", false);
		this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.NewRecord"));
	}

	private void spotlightRecordAttempt() {
		this.getElement().setProperty("recordBroken", false);
		this.getElement().setProperty("recordAttempt", true);
		this.getElement().setProperty("recordMessage", Translator.translate("Scoreboard.RecordAttempt"));
	}

	private void spotlightRecords(FieldOfPlay fop, Athlete a) {
		if (fop.getState() == FOPState.INACTIVE || fop.getState() == FOPState.BREAK) {
			hideRecordInfo(a);
		} else if (fop.getNewRecords() != null && !fop.getNewRecords().isEmpty()) {
			spotlightNewRecord();
		} else if (fop.getChallengedRecords() != null && !fop.getChallengedRecords().isEmpty()) {
			spotlightRecordAttempt();
		} else {
			hideRecordInfo(a);
		}
	}

}
