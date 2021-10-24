/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.componentfactory.EnhancedDialog;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDialog extends EnhancedDialog {
    final private Logger logger = (Logger) LoggerFactory.getLogger(JuryDialog.class);
    private Athlete reviewedAthlete;
    private Integer reviewedLift;
    private Integer liftValue;
    private Object origin;
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * Used by the announcer -- tries to guess what type of break is pertinent based on field of play.
     *
     * @param origin             the origin
     * @param athleteUnderReview TODO
     */
    public JuryDialog(Object origin, Athlete athleteUnderReview) {
        this.origin = origin;
        logger.debug("reviewedAthlete {}", athleteUnderReview);
        this.reviewedAthlete = athleteUnderReview;

        // stop competition
        OwlcmsSession.getFop().getFopEventBus()
                .post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, this));

        // TODO warn announcer

        Button endBreak = new Button("JuryDialog.EndDeliberation", (e) -> {
            doClose();
        });
        Button goodLift = new Button(IronIcons.DONE.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.JuryDecision(athleteUnderReview, this, true));
            });
        });
        Button badLift = new Button(IronIcons.CLOSE.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.JuryDecision(athleteUnderReview, this, false));
            });
        });
        Button loadingError = new Button("JuryDialog.LoadingError", (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.JuryDecision(athleteUnderReview, this, false));
            });
        });

        goodLift.getElement().setAttribute("theme", "primary success icon");
        goodLift.setWidth("8em");
        badLift.getElement().setAttribute("theme", "primary error icon");
        badLift.setWidth("8em");

        // workaround for unpredictable behaviour of FormLayout
        FormLayout layoutGreen = new FormLayout();
        FormLayout layoutRed = new FormLayout();
        FormLayout layout3 = new FormLayout();
        FormItem red, green, loading;
        Label redLabel = new Label("JuryDialog.BadLiftLabel");
        red = layoutGreen.addFormItem(badLift, redLabel);
        fixItemFormat(layoutGreen, red);
        Label greenLabel = new Label("JuryDialog.GoodLiftLabel");
        green = layoutRed.addFormItem(goodLift, greenLabel);
        fixItemFormat(layoutRed, green);
        loading = layout3.addFormItem(loadingError, "JuryDialog.LoadingErrorLabel");
        fixItemFormat(layout3, loading);
        this.add(layoutGreen, layoutRed, layout3);
        this.setWidth("50em");

        FlexLayout footer = new FlexLayout(endBreak);
        endBreak.getElement().setAttribute("theme", "primary");
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        this.addToFooter(footer);

        this.addAttachListener((e) -> {
            if (reviewedAthlete != null) {
                reviewedLift = reviewedAthlete.getAttemptsDone();
                liftValue = reviewedAthlete.getActualLift(reviewedLift);
                String status;
                if (liftValue != null) {
                    status = (liftValue < 0 ? "JuryDialog.Denied" : "JuryDialog.Accepted");
                    redLabel.setText(liftValue < 0 ? "JuryDialog.Accept" : "JuryDialog.Reverse");
                    greenLabel.setText(liftValue < 0 ? "JuryDialog.Reverse" : "JuryDialog.Accept");
                    layoutGreen.setEnabled(true);
                    layoutRed.setEnabled(true);
                } else {
                    status = "JuryDialog.NotLiftedYet";
                    redLabel.setText("JuryDialog.NotLiftedYet");
                    greenLabel.setText("JuryDialog.NotLiftedYet");
                    layoutGreen.setEnabled(false);
                    layoutRed.setEnabled(false);
                }
                H3 status1 = new H3(status);
                H3 weight = new H3(liftValue != null ? Translator.translate("Kg", Math.abs(liftValue)) : "");
                H3 athlete = new H3(reviewedAthlete.getFullId());
                HorizontalLayout header = new HorizontalLayout(
                        athlete,
                        weight,
                        status1);
                header.setFlexGrow(1, athlete);
                header.setWidthFull();
                header.setPadding(true);
                this.setHeader(header);
            } else {
                this.setHeader("JuryDialog.NoCurrentAthlete");
                redLabel.setText("JuryDialog.NotLiftedYet");
                greenLabel.setText("JuryDialog.NotLiftedYet");
                layoutGreen.setEnabled(false);
                layoutRed.setEnabled(false);
            }
        });

        this.addDialogCloseActionListener((e) -> {
            doClose();
        });
    }

    private void doClose() {
        OwlcmsSession.withFop(fop -> {
            // TODO inform announcer
            this.close();
            logger.info("{}end of jury deliberation",fop.getLoggingName());
            ((JuryContent) origin).doSync();

        });
        this.removeAll();
        this.close();
    }

    private void fixItemFormat(FormLayout layout, FormItem f) {
        layout.setWidthFull();
        layout.setColspan(f, 1);
        f.getElement().getStyle().set("--vaadin-form-item-label-width", "15em");
    }
}
