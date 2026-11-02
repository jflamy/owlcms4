/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays.scoreboards;

import com.vaadin.flow.router.Route;

import app.owlcms.access.Permission;
import app.owlcms.access.RequiresPermission;
import app.owlcms.access.TokenAccessible;
import app.owlcms.displays.scoreboard.ResultsMedals;

/**
 * Public-styled prospective medalists, including categories that are not finished.
 */
@SuppressWarnings("serial")
@RequiresPermission(value = { Permission.DISPLAYS, Permission.STREAMING }, platformBound = true)
@TokenAccessible
@Route("displays/publicMedalLeaders")

public class PublicMedalLeadersPage extends PublicMedalsPage {

	public PublicMedalLeadersPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	protected void init() {
		super.init();
		((ResultsMedals) getBoard()).setMedalLeaders(true);
	}

}
