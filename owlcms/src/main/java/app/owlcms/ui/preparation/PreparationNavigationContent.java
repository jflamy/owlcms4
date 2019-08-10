/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.preparation;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.components.NavigationPage;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
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

    /**
     * Instantiates a new preparation navigation content.
     */
    public PreparationNavigationContent() {

        Button competition = new Button(getTranslation("CompetitionInformation"),
                buttonClickEvent -> UI.getCurrent().navigate(CompetitionContent.class));
        Button categories = new Button(getTranslation("DefineCategories"),
                buttonClickEvent -> UI.getCurrent().navigate(CategoryContent.class));
        Button groups = new Button(getTranslation("DefineGroups"), buttonClickEvent -> UI.getCurrent().navigate(GroupContent.class));
        Button platforms = new Button(getTranslation("DefineFOP"),
                buttonClickEvent -> UI.getCurrent().navigate(PlatformContent.class));

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
        Button athletes = new Button(getTranslation("EditAthletes"),
                buttonClickEvent -> UI.getCurrent().navigate(RegistrationContent.class));

        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(competition, categories, groups, platforms,
                downloadDiv, upload);
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(downloadDiv, upload);
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(athletes);

        doGroup(getTranslation("PreCompetitionSetup"), grid1, this);
        doGroup(getTranslation("Registration"), grid2, this);
        doGroup(getTranslation("EditAthletes_Groups"), grid3, this);

    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("OWLCMS_Preparation");
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
