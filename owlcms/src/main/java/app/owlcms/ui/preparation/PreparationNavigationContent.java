/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.preparation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.server.StreamResource;

import app.owlcms.components.NavigationPage;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.utils.DebugUtils;
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
        Button ageGroups = openInNewTabNoParam(AgeGroupContent.class, getTranslation("DefineAgeGroups"));
        Button groups = openInNewTabNoParam(GroupContent.class, getTranslation("DefineGroups"));
        Button platforms = openInNewTabNoParam(PlatformContent.class, getTranslation("DefineFOP"));

//        Button categories = new Button(getTranslation("DefineCategories"),
//                buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class));

        StreamResource href = new StreamResource("registration.xls",
                () -> this.getClass().getResourceAsStream("/templates/registration/RegistrationTemplate.xls"));
        Anchor download = new Anchor(href, "");
        Button downloadButton = new Button(getTranslation("DownloadRegistrationTemplate"),
                new Icon(VaadinIcon.DOWNLOAD_ALT));
        downloadButton.setWidth("93%"); // don't ask. this is a kludge.
        download.add(downloadButton);
        download.setWidth("100%");
        Div downloadDiv = new Div(download);
        downloadDiv.setWidthFull();
        Button upload = new Button(getTranslation("UploadRegistrations"), new Icon(VaadinIcon.UPLOAD_ALT),
                buttonClickEvent -> new UploadDialog().open());

        Button athletes = openInNewTabNoParam(RegistrationContent.class, getTranslation("EditAthletes"));

        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(competition, ageGroups, groups, platforms,
                downloadDiv, upload);
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(downloadDiv, upload);
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(athletes);

        doGroup(getTranslation("PreCompetitionSetup"), grid1, this);
        doGroup(getTranslation("Registration"), grid2, this);
        doGroup(getTranslation("EditAthletes_Groups"), grid3, this);

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
     * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
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
            params.put("group", Arrays.asList(currentGroup.getName()));
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
    protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
        return null;
    }

    @Override
    protected String getTitle() {
        return getTranslation("PrepareCompetition");
    }

}
