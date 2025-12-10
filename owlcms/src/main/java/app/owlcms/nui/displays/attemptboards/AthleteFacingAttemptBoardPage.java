/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.attemptboards;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.attemptboard.AbstractAttemptBoard;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/athleteFacingAttempt")

public class AthleteFacingAttemptBoardPage extends AbstractAttemptBoardPage {

	Logger logger = (Logger) LoggerFactory.getLogger(AthleteFacingAttemptBoardPage.class);

	public AthleteFacingAttemptBoardPage() {
		// intentionally empty; superclass will invoke init() as required.
	}

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("Decision_AF_") + suffix;
	}

	/**
	 * @see app.owlcms.nui.displays.AbstractDisplayPage#init()
	 */
	@Override
	protected void init() {
		setBoard(new AttemptBoard());

		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		var initialMap = Map.of(
		        SoundParameters.SILENT, "false",
		        SoundParameters.DOWNSILENT, "false",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "false",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED, Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")));
		var additionalMap = Map.of(
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!Config.getCurrent().featureSwitch("noLiveLights")),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(Config.getCurrent().featureSwitch("centerAnnouncerNotifications")),
		        SoundParameters.START_ORDER, "false",
		        DisplayParameters.CURRENT_ATTEMPT, "false");
		Map<String, String> fullMap = new TreeMap<>();
		fullMap.putAll(initialMap);
		fullMap.putAll(additionalMap);
		setDefaultParameters(QueryParameters.simple(fullMap));

		AbstractAttemptBoard board = (AbstractAttemptBoard) getBoard();
		board.getDecisions().setDisplaySize("large");
		board.setPublicFacing(false);
		this.addComponent(board);
	}

}
