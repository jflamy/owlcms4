/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.attemptboards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;

import app.owlcms.access.AccessUi;
import app.owlcms.access.Principal;
import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.displays.DecisionDisplayRules;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.fieldofplay.FOPEvent.DecisionUpdate;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.displays.SoundEntries;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Logger;

/**
 * Wrapper class to wrap a board as navigable page, to store the board display options, and to present an option editing dialog.
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractAttemptBoardPage extends AbstractDisplayPage
        implements SoundEntries, DisplayParametersReader, HasDynamicTitle, SafeEventBusRegistration {

	Logger logger = (Logger) LoggerFactory.getLogger(AbstractAttemptBoardPage.class);
	protected static final String SHOW_JURY_DECISIONS = "showJuryDecisions";
	private boolean showJuryDecisions;
	private final List<ShortcutRegistration> shortcutRegistrations = new ArrayList<>();

	public AbstractAttemptBoardPage() {
		// intentionally empty; subclass will invoke init() as required.
	}

	@Override
	public void addDialogContent(Component page, VerticalLayout vl) {
		addSoundEntries(vl, page, (DisplayParametersReader) page);
		if (getBoard() instanceof AttemptBoard && getJurySize() > 0) {
			Checkbox jury = new Checkbox(Translator.translate("DisplayParameters.ShowJuryDecisions"));
			jury.setValue(this.showJuryDecisions);
			jury.addValueChangeListener(event -> {
				setShowJuryDecisions(event.getValue());
				updateURLLocation(getLocationUI(), getLocation(), SHOW_JURY_DECISIONS,
						Boolean.toString(event.getValue()));
			});
			vl.add(jury);
		}
	}

	@Override
	public void addKeyboardShortcuts() {
		clearShortcutRegistrations();
		registerShortcut(() -> postRefereeDecision(0, true), digitKey(1));
		registerShortcut(() -> postRefereeDecision(0, false), digitKey(2));
		registerShortcut(() -> postRefereeDecision(1, true), digitKey(3));
		registerShortcut(() -> postRefereeDecision(1, false), digitKey(4));
		registerShortcut(() -> postRefereeDecision(2, true), digitKey(5));
		registerShortcut(() -> postRefereeDecision(2, false), digitKey(6));
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		clearShortcutRegistrations();
		super.onDetach(detachEvent);
	}

	@Override
	public Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
		// SHOW_JURY_DECISIONS must not be among the static defaults set in init(), otherwise an
		// explicit URL value equal to the static default would be stripped before we get here.
		Map<String, List<String>> params = super.readParams(location, parametersMap);
		if (getBoard() instanceof AttemptBoard) {
			// the FOP is only known after super.readParams
			setDefaultParamValue(SHOW_JURY_DECISIONS,
			        Boolean.toString(DecisionDisplayRules.showJuryDecisionsOnAttemptBoard(getJurySize(),
			                Config.getCurrent().featureSwitch(FeatureSwitch.JURY_DECISIONS_ON_ATTEMPT_BOARD))));
			if (getJurySize() > 0) {
				processBooleanParam(params, SHOW_JURY_DECISIONS, this::setShowJuryDecisions);
			} else {
				setShowJuryDecisions(false);
			}
		}
		return params;
	}

	private void clearShortcutRegistrations() {
		for (ShortcutRegistration registration : this.shortcutRegistrations) {
			registration.remove();
		}
		this.shortcutRegistrations.clear();
	}

	/**
	 * Key.DIGIT_n only matches the top-row key code. Also match the typed character (any layout with
	 * NumLock on) and the numeric keypad code (NumLock off), as USB keypads are used for decisions.
	 */
	private static Key digitKey(int digit) {
		return Key.of(Integer.toString(digit), "Digit" + digit, "Numpad" + digit);
	}

	private void registerShortcut(Runnable action, Key key) {
		UI currentUi = UI.getCurrent();
		if (currentUi == null || key == Key.UNIDENTIFIED) {
			return;
		}
		this.shortcutRegistrations.add(currentUi.addShortcutListener(action::run, key));
	}

	private void postRefereeDecision(int refIndex, boolean decision) {
		FieldOfPlay fop = getFop();
		if (fop == null) {
			return;
		}
		if (!AccessUi.displayInputAllowed(fop)) {
			Principal principal = OwlcmsSession.getPrincipal();
			this.logger.info("{}ignored decision input from read-only display user={}",
			        FieldOfPlay.getLoggingName(fop), principal != null ? principal.username() : null);
			return;
		}
		fop.fopEventPost(new DecisionUpdate(getBoard(), refIndex, decision));
	}

	private int getJurySize() {
		FieldOfPlay fop = getFop();
		return fop != null ? fop.getJurySize() : Competition.getCurrent().getJurySize();
	}

	private void setShowJuryDecisions(boolean show) {
		this.showJuryDecisions = show;
		if (getBoard() instanceof AttemptBoard board) {
			board.getDecisions().setShowJuryDecisions(show);
		}
	}

}
