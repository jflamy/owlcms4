/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.attemptboards;

import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;

import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.displays.DecisionDisplayRules;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
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
