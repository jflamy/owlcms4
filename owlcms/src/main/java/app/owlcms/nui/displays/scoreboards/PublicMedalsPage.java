/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.scoreboards;

import java.util.Map;
import java.util.TreeMap;

import com.vaadin.flow.router.QueryParameters;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")
@RequiresRole(value = { Role.DISPLAYS, Role.EXTERNAL }, platformBound = true)
@Route("displays/publicMedals")

public class PublicMedalsPage extends MedalsPage {

	public PublicMedalsPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("CeremonyType.MEDALS") + suffix;
	}

	@Override
	protected void init() {
		var board = new ResultsMedals();
		this.setBoard(board);
		this.addComponent(board);

		var initialMap = Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "true",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED, Boolean.toString(FeatureSwitch.SHORT_SCOREBOARD_NAMES.isEffective()));
		var additionalMap = Map.of(
		        SoundParameters.LIVE_LIGHTS, Boolean.toString(!FeatureSwitch.NO_LIVE_LIGHTS.isEffective()),
		        SoundParameters.SHOW_DECLARATIONS, "false",
		        SoundParameters.CENTER_NOTIFICATIONS, Boolean.toString(FeatureSwitch.CENTER_ANNOUNCER_NOTIFICATIONS.isEffective()),
		        SoundParameters.START_ORDER, "false",
		        DisplayParameters.CURRENT_ATTEMPT, "false",
		        DisplayParameters.SHOW_MEDALS, "auto");
		Map<String, String> fullMap = new TreeMap<>();
		fullMap.putAll(initialMap);
		fullMap.putAll(additionalMap);
		setDefaultParameters(QueryParameters.simple(fullMap));
	}

}
