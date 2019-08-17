/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.referee.RefContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class LiftingNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting", layout = OwlcmsRouterLayout.class)
public class LiftingNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(LiftingNavigationContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    /**
     * Competition Group Navigation
     */
    public LiftingNavigationContent() {
        logger.trace("LiftingNavigationContent constructor start");

        
        Button weighIn = new Button(getTranslation("WeighIn_StartNumbers"),
                buttonClickEvent -> UI.getCurrent().navigate(WeighinContent.class));
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(weighIn);
        doGroup(getTranslation("WeighIn"), grid3, this);

        Button announcer = openInNewTab(AnnouncerContent.class, getTranslation("Announcer"));
        announcer.setAutofocus(true);

        Button marshall = openInNewTab(MarshallContent.class, getTranslation("Marshall"));
        Button timekeeper = openInNewTab(TimekeeperContent.class, getTranslation("Timekeeper"));
        Button technical = openInNewTab(TCContent.class, getTranslation("TechnicalController"));

        VerticalLayout intro = new VerticalLayout();
        addP(intro, getTranslation("AnnouncerSelectsGroup") +
                getTranslation("ChangesGroupEverywhere") +
                getTranslation("AnnouncerEtc"));
        intro.getStyle().set("margin-bottom", "0");
        
        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(announcer, marshall, timekeeper, technical);
        doGroup(getTranslation("Scoreboard.LiftingOrder"), intro, grid1, this);

        Button referee = openInNewTab(RefContent.class, getTranslation("Referee_Mobile_Device"));
        Button jury = openInNewTab(JuryContent.class, getTranslation("Jury_Console"));
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(referee, jury);
        doGroup(getTranslation("Referees_Jury"), grid2, this);


        logger.trace("LiftingNavigationContent constructor stop");
    }

    /*
     * (non-Javadoc)
     * 
     * @see app.owlcms.ui.home.BaseNavigationContent#createTopBarFopField(java.lang.
     * String, java.lang.String)
     */
    @Override
    protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
        Label fopLabel = new Label(label);
        formatLabel(fopLabel);

        ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
        OwlcmsSession.withFop((fop) -> {
            fopSelect.setValue(fop);
        });
        fopSelect.addValueChangeListener(e -> {
            OwlcmsSession.setFop(e.getValue());
        });

        HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
        fopField.setAlignItems(Alignment.CENTER);
        return fopField;
    }

    @Override
    protected String getTitle() {
        return getTranslation("RunLiftingGroup");
    }

    @Override
    public String getPageTitle() {
        return getTranslation("OWLCMS_Lifting");
    }
}
