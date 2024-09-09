/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.preparation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.DownloadButtonFactory;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.spreadsheet.JXLSRegistrationEmptyExport;
import app.owlcms.spreadsheet.JXLSSBDEExport;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation", layout = OwlcmsLayout.class)
public class PreparationNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(PreparationNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	private Group currentGroup;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	/**
	 * Instantiates a new preparation navigation content.
	 */
	public PreparationNavigationContent() {

		Button competition = openInNewTabNoParam(CompetitionContent.class,
		        Translator.translate("CompetitionInformation"));
		Button config = openInNewTabNoParam(ConfigContent.class, Translator.translate("Config.Title"),
		        VaadinIcon.COG.create());
		Button ageGroups = openInNewTabNoParam(AgeGroupContent.class, Translator.translate("DefineAgeGroups"));
		Button groups = openInNewTabNoParam(SessionContent.class, Translator.translate("DefineGroups"));
		Button platforms = openInNewTabNoParam(PlatformContent.class, Translator.translate("DefineFOP"));
		Button configureRecords = openInNewTabNoParam(RecordsContent.class,
		        Translator.translate("Records.RecordsManagementTitle"));

		var emptyRegistrationWriter = new JXLSRegistrationEmptyExport(UI.getCurrent());
		Notification notification = new Notification(Translator.translate("Processing"));
		notification.setPosition(Position.TOP_END);
		notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
		emptyRegistrationWriter.setDoneCallback((s) -> this.getUI().get().access(() ->  {
			notification.close();
		}));
		Div downloadDiv = DownloadButtonFactory.createDynamicJXLSDownloadButton("Registration", Translator.translate("DownloadRegistrationTemplate"), emptyRegistrationWriter,
		        notification);
		downloadDiv.setWidthFull();

		Button upload = new Button(Translator.translate("UploadRegistrations"), new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> new NRegistrationFileUploadDialog(false).open());
		Button sbdeUpload = new Button(Translator.translate("AdvancedPreparation.Import"),
		        new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> new NRegistrationFileUploadDialog(true).open());
		var registrationWriter = new JXLSSBDEExport(UI.getCurrent());
		Notification notification1 = new Notification(Translator.translate("LongProcessing"));
		notification1.setPosition(Position.TOP_END);
		notification1.addThemeVariants(NotificationVariant.LUMO_WARNING);
		registrationWriter.setDoneCallback((s) -> this.getUI().get().access(() ->  {
			notification1.close();
		}));
		
		Div sbdeDiv = DownloadButtonFactory.createDynamicJXLSDownloadButton("SBDE", Translator.translate("AdvancedPreparation.Export"), registrationWriter,
		        notification1);
		sbdeDiv.setWidthFull();

		Button athletes = openInNewTabNoParam(RegistrationContent.class, Translator.translate("EditAthletes"));
		athletes.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
		Button teams = openInNewTabNoParam(TeamSelectionContent.class,
		        Translator.translate(TeamSelectionContent.TITLE));

		Button documents = openInNewTab(DocumentsContent.class, Translator.translate("Documents.Title"), "documents");
		documents.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

		Button uploadJson = new Button(Translator.translate("ExportDatabase.UploadJson"),
		        new Icon(VaadinIcon.UPLOAD_ALT),
		        buttonClickEvent -> new JsonUploadDialog(UI.getCurrent()).open());
		
		Notification notification2 = new Notification(Translator.translate("LongProcessing"));
		notification2.setPosition(Position.TOP_END);
		notification2.addThemeVariants(NotificationVariant.LUMO_WARNING);
		Div exportJsonDiv = DownloadButtonFactory.createDynamicJsonDownloadButton("owlcmsDatabase",
		        Translator.translate("ExportDatabase.DownloadJson"), notification2);
		Optional<Component> exportJsonButton = exportJsonDiv.getChildren().findFirst();
		exportJsonButton.ifPresent(c -> ((Button) c).setWidth("100%"));
		exportJsonDiv.setWidthFull();

		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(competition, config, ageGroups, groups,
		        platforms,
		        configureRecords);
		doGroup(Translator.translate("PreCompetitionSetup"), grid1, this, true);
		FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(downloadDiv, upload, athletes, teams);
		doGroup(Translator.translate("Registration"), grid2, this, true);
		// FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(athletes, teams, exportDiv);
		// doGroup(Translator.translate("EditAthletes_Groups"), grid3, this);
		FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(documents);
		doGroup(Translator.translate("Documents.Title"), grid4, this, true);
		FlexibleGridLayout grid5 = HomeNavigationContent.navigationGrid(exportJsonDiv, uploadJson);
		doGroup(Translator.translate("ExportDatabase.ExportImport"), grid5, this, true);
		FlexibleGridLayout grid6 = HomeNavigationContent.navigationGrid(sbdeDiv, sbdeUpload);
		doHiddenGroup(Translator.translate("AdvancedPreparation.Title"),
		        new Div(Translator.translate("AdvancedPreparation.Explanation")), grid6, this, true);

		DebugUtils.gc();
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("PrepareCompetition");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("ShortTitle.Preparation");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.nui.shared.QueryParameterReader#isIgnoreGroupFromURL()
	 */
	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	/**
	 * Parse the http query parameters
	 *
	 * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
	 *
	 * @param event     Vaadin navigation event
	 * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query parameters instead.
	 *
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		setLocation(event.getLocation());
		setLocationUI(event.getUI());
		QueryParameters queryParameters = getLocation().getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters(); // immutable
		HashMap<String, List<String>> params = new HashMap<>(parametersMap);

		logger.debug("parsing query parameters");
		List<String> groupNames = params.get("group");
		if (!isIgnoreGroupFromURL() && groupNames != null && !groupNames.isEmpty()) {
			String groupName = groupNames.get(0);
			this.currentGroup = GroupRepository.findByName(groupName);
		} else {
			this.currentGroup = null;
		}
		if (this.currentGroup != null) {
			params.put("group", Arrays.asList(URLUtils.urlEncode(this.currentGroup.getName())));
		} else {
			params.remove("group");
		}
		params.remove("fop");

		// change the URL to reflect group
		event.getUI().getPage().getHistory().replaceState(null,
		        new Location(getLocation().getPath(), new QueryParameters(URLUtils.cleanParams(params))));
	}

	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		return null;
	}
}
