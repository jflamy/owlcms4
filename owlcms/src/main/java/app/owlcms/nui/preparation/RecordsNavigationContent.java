/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route(value = "records", layout = OwlcmsLayout.class)
public class RecordsNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(RecordsNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	public RecordsNavigationContent() {
		Button configureRecords = openInNewTabNoParam(RecordsConfigContent.class,
		        Translator.translate("RecordEvent.RecordsConfigurationTitle"));
		Button editExportRecords = openInNewTabNoParam(RecordContent.class,
		        Translator.translate("RecordEvent.EditExportRecords"));

		FlexibleGridLayout recordsGrid = HomeNavigationContent.navigationGrid(configureRecords, editExportRecords);
		doGroup(Translator.translate("RecordEvent.PageTitle"), recordsGrid, this, true);

		DebugUtils.gc();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("RecordEvent.PageTitle");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("RecordEvent.PageTitle");
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		event.forwardTo(RecordContent.class);
	}

	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		return null;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		setLocation(event.getLocation());
		setLocationUI(event.getUI());
		var params = new java.util.HashMap<>(getLocation().getQueryParameters().getParameters());
		params.remove("fop");
		params.remove("group");
		Location newLocation = new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params)));
		URLUtils.replaceState(event.getUI().getPage().getHistory(), null, newLocation, getLocation());
	}
}