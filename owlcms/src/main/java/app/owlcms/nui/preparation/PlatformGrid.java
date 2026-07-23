/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0 ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import com.vaadin.flow.component.grid.Grid;

import app.owlcms.data.platform.Platform;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.crudui.OwlcmsCrudGrid;
import app.owlcms.nui.crudui.OwlcmsGridLayout;

@SuppressWarnings("serial")
final class PlatformGrid extends OwlcmsCrudGrid<Platform> {
	PlatformGrid(Class<Platform> domainType, OwlcmsGridLayout crudLayout,
	        OwlcmsCrudFormFactory<Platform> owlcmsCrudFormFactory, Grid<Platform> grid) {
		super(domainType, crudLayout, owlcmsCrudFormFactory, grid);
	}

	void updateButtonClicked(Platform platform) {
		this.grid.asSingleSelect().setValue(platform);
		super.updateButtonClicked();
	}
}