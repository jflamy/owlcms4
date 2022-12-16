/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.lifting;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.referee.RefContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class LiftingNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "nlifting", layout = OwlcmsLayout.class)
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

        Button weighIn = openInNewTabNoParam(WeighinContent.class, getTranslation("WeighIn_StartNumbers"));
        FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(weighIn);
        doGroup(getTranslation("WeighIn"), grid3, this);

        Button announcer = openInNewTab(AnnouncerContent.class, getTranslation("Announcer"));
        announcer.setAutofocus(true);
        Button marshall = openInNewTab(MarshallContent.class, getTranslation("Marshall"));
        Button timekeeper = openInNewTab(TimekeeperContent.class, getTranslation("Timekeeper"));
        Button technical = openInNewTab(TCContent.class, getTranslation("PlatesCollarBarbell"));

        VerticalLayout intro = new VerticalLayout();
        addP(intro, getTranslation("AnnouncerSelectsGroup") + getTranslation("ChangesGroupEverywhere")
                + getTranslation("AnnouncerEtc"));
        intro.getStyle().set("margin-bottom", "0");

        FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(announcer, marshall, timekeeper, technical);
        doGroup(getTranslation("Scoreboard.LiftingOrder"), intro, grid1, this);

        Button referee = openInNewTab(RefContent.class, getTranslation("Referee_Mobile_Device"));
        Button jury = openInNewTab(JuryContent.class, getTranslation("Jury_Console"));
        FlexibleGridLayout grid2 = HomeNavigationContent.navigationGrid(referee, jury);
        doGroup(getTranslation("Referees_Jury"), grid2, this);

        DebugUtils.gc();
        logger.trace("LiftingNavigationContent constructor stop");
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    @Override
    public String getPageTitle() {
        return getTranslation("OWLCMS_Lifting") + OwlcmsSession.getFopNameIfMultiple();
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.nui.home.BaseNavigationContent#createTopBarFopField(java.lang. String, java.lang.String)
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
            updateURLLocation(getLocationUI(), getLocation(), null);
        });

        HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
        fopField.setAlignItems(Alignment.CENTER);
        return fopField;
    }

}
