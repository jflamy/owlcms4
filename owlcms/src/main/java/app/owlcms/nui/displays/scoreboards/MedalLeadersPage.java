/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.scoreboards;

import com.vaadin.flow.router.Route;

import app.owlcms.access.Access;
import app.owlcms.access.RequiresAccess;
import app.owlcms.access.TokenAccessible;
import app.owlcms.displays.scoreboard.ResultsMedals;

/**
 * Prospective medalists, including categories that are not finished.
 */
@SuppressWarnings("serial")
@RequiresAccess(value = { Access.DISPLAYS, Access.STREAMING }, platformBound = true)
@TokenAccessible
@Route("displays/medalLeaders")

public class MedalLeadersPage extends MedalsPage {

	public MedalLeadersPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	protected void init() {
		super.init();
		((ResultsMedals) getBoard()).setMedalLeaders(true);
	}

}
