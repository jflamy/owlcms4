/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.scoreboards;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.scoreboard.NCurrentAthlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/ncurrentathlete")
public class NCurrentAthletePage extends AbstractResultsDisplayPage implements BeforeEnterObserver {

	private static final String DECISION_STICK_MILLIS = "decision";
    private static final int DEFAULT_DECISION_STICK_MILLIS = 5000;

    Logger logger = (Logger) LoggerFactory.getLogger(NCurrentAthletePage.class);
    Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
    Map<String, List<String>> urlParameterMap = new HashMap<>();
    private int decisionStickMillis = DEFAULT_DECISION_STICK_MILLIS;

    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
        DisplayOptions.addRule(vl);
        DisplayOptions.addSoundEntries(vl, target, this);
    }

    @Override
    public String getPageTitle() {
        String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
        return Translator.translate("CurrentAthlete") + suffix;
    }

    @Override
    public boolean isShowInitialDialog() {
        return false;
    }

    @Override
    protected void init() {
        this.logger = (Logger) LoggerFactory.getLogger(NCurrentAthletePage.class);
        this.uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
        var board = new NCurrentAthlete(this);
        this.setBoard(board);

        var initialMap = Map.of(
            SoundParameters.SILENT, "true",
            SoundParameters.DOWNSILENT, "true",
            DisplayParameters.DARK, "true",
            DisplayParameters.LEADERS, "false",
            DisplayParameters.RECORDS, "false",
            DisplayParameters.VIDEO, "false",
            DisplayParameters.PUBLIC, "false",
            SoundParameters.SINGLEREF, "false",
            DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames"))
        );
        var additionalMap = Map.of(
            SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch("noLiveLights")),
            SoundParameters.SHOW_DECLARATIONS, "false",
            SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
            SoundParameters.START_ORDER, "false",
            DisplayParameters.CURRENT_ATTEMPT, "false",
            DisplayParameters.SHOW_MEDALS, "auto"
        );
        Map<String, String> fullMap = new TreeMap<>();
        fullMap.putAll(initialMap);
        fullMap.putAll(additionalMap);
        setDefaultParameters(QueryParameters.simple(fullMap));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        DisplayParameters board = (DisplayParameters) this.getBoard();
        board.setFop(this.getFop());
        board.setLeadersDisplay(true);
        board.setRecordsDisplay(true);
        ((NCurrentAthlete) board).setDecisionStickMillis(this.decisionStickMillis);

        this.addComponentAsFirst((Component) board);
    }

    @Override
    public Map<String, List<String>> readParams(Location location, Map<String, List<String>> parametersMap) {
        Map<String, List<String>> params = super.readParams(location, parametersMap);
        readDecisionStickMillis(params);
        ((NCurrentAthlete) this.getBoard()).setDecisionStickMillis(this.decisionStickMillis);
        return params;
    }

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		if (!Config.getCurrent().featureSwitch("iwfLook")) {
			throw new AccessDeniedException();
		}
        readDecisionStickMillis(event.getLocation().getQueryParameters().getParameters());
	}

    private void readDecisionStickMillis(Map<String, List<String>> params) {
        this.decisionStickMillis = DEFAULT_DECISION_STICK_MILLIS;
        List<String> decisionValues = params.get(DECISION_STICK_MILLIS);
        if (decisionValues == null || decisionValues.isEmpty()) {
            return;
        }
        try {
            this.decisionStickMillis = Math.max(0, Integer.parseInt(decisionValues.get(0)));
        } catch (NumberFormatException ignored) {
            this.decisionStickMillis = DEFAULT_DECISION_STICK_MILLIS;
        }
    }
    
    
    
}
