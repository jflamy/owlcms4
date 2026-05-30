/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsContent;
import app.owlcms.nui.shared.OwlcmsLayout;

@SuppressWarnings("serial")
@Route(value = "preparation/championships", layout = OwlcmsLayout.class)
public class ChampionshipsContent extends BaseContent implements OwlcmsContent {
	private OwlcmsLayout routerLayout;

	public ChampionshipsContent() {
		// Recompute default status from stored championship fields before showing
		// the editor, so the user sees an accurate picture.
		ChampionshipRepository.normalizeDefaultTypes();
		ChampionshipRepository.normalizeCompetitionDefaultFlags();
		EditChampionshipsPanel panel = new EditChampionshipsPanel();
		panel.setWidthFull();
		fillH(panel, this);
	}

	@Override
	public FlexLayout createMenuArea() {
		return AgeGroupActionsMenu.build(null);
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("DefineChampionships.Title");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
}