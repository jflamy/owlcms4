/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.ui.preparation;

import java.io.IOException;
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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.components.NavigationPage;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.records.RecordRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.JXLSRegistration;
import app.owlcms.spreadsheet.JXLSRegistrationExport;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.DownloadButtonFactory;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class PreparationNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "preparation", layout = OwlcmsRouterLayout.class)
public class PreparationNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(PreparationNavigationContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private Group currentGroup;

    /**
     * Instantiates a new preparation navigation content.
     */
    public PreparationNavigationContent() {

        Button competition = openInNewTabNoParam(CompetitionContent.class, getTranslation("CompetitionInformation"));
        Button config = openInNewTabNoParam(ConfigContent.class, getTranslation("Config.Title"));
        Button ageGroups = openInNewTabNoParam(AgeGroupContent.class, getTranslation("DefineAgeGroups"));
        Button groups = openInNewTabNoParam(GroupContent.class, getTranslation("DefineGroups"));
        Button platforms = openInNewTabNoParam(PlatformContent.class, getTranslation("DefineFOP"));
        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(competition, config, ageGroups, platforms, groups);
        doGroup(getTranslation("PreCompetitionSetup"), grid1, this);

        Div downloadDiv = DownloadButtonFactory.createDynamicXLSDownloadButton("registration",
                getTranslation("DownloadRegistrationTemplate"), new JXLSRegistration(UI.getCurrent()));
        Optional<Component> content = downloadDiv.getChildren().findFirst();
        content.ifPresent(c -> ((Button) c).setWidth("93%"));
        downloadDiv.setWidthFull();
        Button upload = new Button(getTranslation("UploadRegistrations"), new Icon(VaadinIcon.UPLOAD_ALT),
                buttonClickEvent -> new RegistrationFileUploadDialog().open());
        Div exportDiv = DownloadButtonFactory.createDynamicXLSDownloadButton("exportRegistration",
                getTranslation("ExportRegistrationData"), new JXLSRegistrationExport(UI.getCurrent()));
        Optional<Component> exportDivButton = exportDiv.getChildren().findFirst();
        exportDivButton.ifPresent(c -> ((Button) c).setWidth("93%"));
        exportDiv.setWidthFull();
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(downloadDiv, upload, exportDiv);
        doGroup(getTranslation("Registration"), grid2, this);

        Button athletes = openInNewTabNoParam(RegistrationContent.class, getTranslation("EditAthletes"));
        Button teams = openInNewTabNoParam(TeamSelectionContent.class, getTranslation(TeamSelectionContent.TITLE));
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(athletes, teams);
        doGroup(getTranslation("EditAthletes_Groups"), grid3, this);
        
        if (Config.getCurrent().featureSwitch("preCompDocs", true)) {
            FlexibleGridLayout grid6;
            Button documents = openInNewTabNoParam(DocsContent.class, getTranslation(DocsContent.PRECOMP_DOCS_TITLE));
            grid6 = HomeNavigationContent.navigationGrid(documents);
            doGroup(getTranslation(DocsContent.PRECOMP_DOCS_TITLE), grid6, this);
        }

        Button uploadJson = new Button(getTranslation("ExportDatabase.UploadJson"), new Icon(VaadinIcon.UPLOAD_ALT),
                buttonClickEvent -> new JsonUploadDialog(UI.getCurrent()).open());
        Div exportJsonDiv = DownloadButtonFactory.createDynamicJsonDownloadButton("owlcmsDatabase",
                getTranslation("ExportDatabase.DownloadJson"));
        Optional<Component> exportJsonButton = exportJsonDiv.getChildren().findFirst();
        exportJsonButton.ifPresent(c -> ((Button) c).setWidth("93%"));
        exportJsonDiv.setWidthFull();
        FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(exportJsonDiv, uploadJson/* , clearDatabase */);
        doGroup(getTranslation("ExportDatabase.ExportImport"), grid4, this);
        
        Button clearNewRecords = new Button(getTranslation("Preparation.ClearNewRecords"),
                buttonClickEvent -> {
                    try {
                        RecordRepository.clearNewRecords();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        clearNewRecords.getElement().setProperty("title", Translator.translate("Preparation.ClearNewRecordsExplanation"));
        FlexibleGridLayout grid5 = HomeNavigationContent.navigationGrid(clearNewRecords);
        doGroup(getTranslation("Preparation.Records"), grid5, this);

        DebugUtils.gc();
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("OWLCMS_Preparation");
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.ui.shared.QueryParameterReader#isIgnoreGroupFromURL()
     */
    @Override
    public boolean isIgnoreGroupFromURL() {
        return true;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    /**
     * Parse the http query parameters
     *
     * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
     *
     * @param event     Vaadin navigation event
     * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query
     *                  parameters instead.
     *
     * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
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
            currentGroup = GroupRepository.findByName(groupName);
        } else {
            currentGroup = null;
        }
        if (currentGroup != null) {
            params.put("group", Arrays.asList(URLUtils.urlEncode(currentGroup.getName())));
        } else {
            params.remove("group");
        }
        params.remove("fop");

        // change the URL to reflect group
        event.getUI().getPage().getHistory().replaceState(null,
                new Location(getLocation().getPath(), new QueryParameters(params)));
    }

    @Override
    protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
        return null;
    }

    @Override
    protected String getTitle() {
        return getTranslation("PrepareCompetition");
    }

}
