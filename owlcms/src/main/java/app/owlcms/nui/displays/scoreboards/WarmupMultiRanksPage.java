/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.scoreboards;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.scoreboard.ResultsMultiRanks;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/resultsLeadersRanks")

public class WarmupMultiRanksPage extends AbstractResultsDisplayPage {

	Logger logger = (Logger) LoggerFactory.getLogger(WarmupMultiRanksPage.class);

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("ScoreboardMultiRanksTitle") + suffix;
	}
	

	@Override
	protected void init() {
		this.logger = (Logger) LoggerFactory.getLogger(PublicScoreboardPage.class);

		// each subclass must override this routine.
		// otherwise we end up with multiple instances of the Results board.
		ResultsMultiRanks board =  new ResultsMultiRanks();
		this.setBoard(board);

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		var initialMap = Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "true",
		        DisplayParameters.RECORDS, "true",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "false",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")));
		var additionalMap = Map.of(
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch("noLiveLights")),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
		        SoundParameters.START_ORDER, "false",
		        DisplayParameters.CURRENT_ATTEMPT, "true");
		Map<String, String> fullMap = new TreeMap<>();
		fullMap.putAll(initialMap);
		fullMap.putAll(additionalMap);
		setDefaultParameters(QueryParameters.simple(fullMap));
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		DisplayParameters board = (DisplayParameters) this.getBoard();
		board.setFop(this.getFop());
//		board.setLeadersDisplay(true);
//		board.setRecordsDisplay(true);

		this.addComponent((Component) board);
	}
}
