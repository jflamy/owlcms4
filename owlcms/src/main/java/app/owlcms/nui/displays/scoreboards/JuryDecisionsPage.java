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
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.displays.scoreboard.JuryDecisions;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/juryDecisions")
public class JuryDecisionsPage extends AbstractResultsDisplayPage implements BeforeEnterObserver {

    Logger logger = (Logger) LoggerFactory.getLogger(JuryDecisionsPage.class);
    Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
    Map<String, List<String>> urlParameterMap = new HashMap<>();
    
    private boolean keepInitialDecision = true;
    private boolean keepFinalDecision = true;

    @Override
    public void addDialogContent(Component target, VerticalLayout vl) {
        DisplayOptions.addLightingEntries(vl, target, this);
        DisplayOptions.addRule(vl);
        DisplayOptions.addSoundEntries(vl, target, this);
    }

    @Override
    public String getPageTitle() {
        String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
        return Translator.translate("JuryDecisions") + suffix;
    }

    @Override
    public boolean isShowInitialDialog() {
        return false;
    }

    @Override
    protected void init() {
        this.logger = (Logger) LoggerFactory.getLogger(JuryDecisionsPage.class);
        this.uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
        var board = new JuryDecisions(this);
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
            DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")),
            DisplayParameters.CURRENT_ATTEMPT, "false"
        );
        var additionalMap = Map.of(
            SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch("noLiveLights")),
            SoundParameters.SHOW_DECLARATIONS, "false",
            SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
            SoundParameters.START_ORDER, "false",
            DisplayParameters.KEEP_INITIAL_DECISION, "true",
            DisplayParameters.KEEP_FINAL_DECISION, "true",
            DisplayParameters.SHOW_MEDALS, "auto"
        );
        Map<String, String> fullMap = new TreeMap<>();
        fullMap.putAll(initialMap);
        fullMap.putAll(additionalMap);
        setDefaultParameters(QueryParameters.simple(fullMap));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        JuryDecisions board = (JuryDecisions) this.getBoard();
        board.setFop(this.getFop());
        board.setLeadersDisplay(false);
        board.setRecordsDisplay(false);
        board.setKeepInitialDecision(keepInitialDecision);
        board.setKeepFinalDecision(keepFinalDecision);

        this.addComponentAsFirst((Component) board);
        board.getElement().getParent().getStyle().set("height", "100%");
    }

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
		
		List<String> keepInitialParams = params.get(DisplayParameters.KEEP_INITIAL_DECISION);
		if (keepInitialParams != null && !keepInitialParams.isEmpty()) {
			keepInitialDecision = Boolean.parseBoolean(keepInitialParams.get(0));
		}
		
		List<String> keepFinalParams = params.get(DisplayParameters.KEEP_FINAL_DECISION);
		if (keepFinalParams != null && !keepFinalParams.isEmpty()) {
			keepFinalDecision = Boolean.parseBoolean(keepFinalParams.get(0));
		}
	}
    
    
    
}
