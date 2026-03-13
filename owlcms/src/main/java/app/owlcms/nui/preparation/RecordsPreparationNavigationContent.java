/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Curtailed Preparation page shown in recordsOnly mode.
 * Only exposes Language and System Settings plus Export/Import Database.
 */
@SuppressWarnings("serial")
@Route(value = "recordsPreparation", layout = OwlcmsLayout.class)
public class RecordsPreparationNavigationContent extends BaseNavigationContent
	implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(RecordsPreparationNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	public RecordsPreparationNavigationContent() {
		Button config = openInNewTabNoParam(ConfigContent.class, Translator.translate("Config.Title"),
		        VaadinIcon.COG.create());

		Button uploadJson = new Button(Translator.translate("ExportDatabase.UploadJson"),
		        new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> new JsonUploadDialog(UI.getCurrent()).open());

		Notification notification2 = new Notification(Translator.translate("LongProcessing"));
		notification2.setPosition(Position.TOP_END);
		Div exportJsonDiv = DownloadButtonFactory.createDynamicJsonDownloadButton("owlcmsDatabase",
		        Translator.translate("ExportDatabase.DownloadJson"), notification2);
		Optional<Component> exportJsonButton = exportJsonDiv.getChildren().findFirst();
		exportJsonButton.ifPresent(c -> ((Button) c).setWidth("100%"));
		exportJsonDiv.setWidthFull();

		// V2 export button (conditional on feature switch)
		Div exportJsonV2Div = null;
		if (Config.getCurrent().featureSwitch("v2Export")) {
			Notification notification3 = new Notification(Translator.translate("LongProcessing"));
			notification3.setPosition(Position.TOP_END);
			exportJsonV2Div = DownloadButtonFactory.createDynamicJsonV2DownloadButton("owlcmsDatabase",
			        Translator.translate("ExportDatabase.DownloadJsonV2"), notification3);
			Optional<Component> exportJsonV2Button = exportJsonV2Div.getChildren().findFirst();
			exportJsonV2Button.ifPresent(c -> ((Button) c).setWidth("100%"));
			exportJsonV2Div.setWidthFull();
		}

		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(config);
		doGroup(Translator.translate("PreCompetitionSetup"), grid1, this, true);

		FlexibleGridLayout grid5;
		if (exportJsonV2Div != null) {
			grid5 = HomeNavigationContent.navigationGrid(exportJsonDiv, exportJsonV2Div, uploadJson);
		} else {
			grid5 = HomeNavigationContent.navigationGrid(exportJsonDiv, uploadJson);
		}
		doGroup(Translator.translate("ExportDatabase.ExportImport"), grid5, this, true);

		DebugUtils.gc();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("Configuration");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Configuration");
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
