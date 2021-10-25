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
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
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
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.JuryNotification;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDialog extends EnhancedDialog {
    final private Logger logger = (Logger) LoggerFactory.getLogger(JuryDialog.class);
    private Athlete reviewedAthlete;
    private Integer reviewedLift;
    private Integer liftValue;
    private Object origin;
    private long lastShortcut;
    {
        logger.setLevel(Level.INFO);
    }

    /**
     * Used by the announcer -- tries to guess what type of break is pertinent based on field of play.
     *
     * @param origin             the origin
     * @param athleteUnderReview
     */
    public JuryDialog(Object origin, Athlete athleteUnderReview) {
        this.origin = origin;
        this.setCloseOnEsc(false);
        logger.debug("reviewedAthlete {}", athleteUnderReview);
        this.reviewedAthlete = athleteUnderReview;

        // stop competition
        OwlcmsSession.getFop().getFopEventBus()
                .post(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, this));

        JuryNotification event = new UIEvent.JuryNotification(athleteUnderReview, origin,
                JuryDeliberationEventType.START_DELIBERATION, null);
        OwlcmsSession.getFop().getUiEventBus().post(event);

        Button endBreak = new Button(Translator.translate("JuryDialog.EndDeliberation"), (e) -> doClose(true));
        Shortcuts.addShortcutListener(this, () -> {
            if (shortcutTooSoon()) {
                return;
            }
            doClose(true);
        }, Key.ESCAPE);

        Button goodLift = new Button(IronIcons.DONE.create(),
                (e) -> doGoodLift(athleteUnderReview, OwlcmsSession.getFop()));
        Shortcuts.addShortcutListener(this, () -> doGoodLift(athleteUnderReview, OwlcmsSession.getFop()), Key.KEY_G);

        Button badLift = new Button(IronIcons.CLOSE.create(),
                (e) -> doBadLift(athleteUnderReview, OwlcmsSession.getFop()));
        Shortcuts.addShortcutListener(this, () -> doBadLift(athleteUnderReview, OwlcmsSession.getFop()), Key.KEY_B);

        Button loadingError = new Button(Translator.translate("JuryDialog.CallController"),
                (e) -> doCallController(athleteUnderReview, OwlcmsSession.getFop()));
        Shortcuts.addShortcutListener(this, () -> doCallController(athleteUnderReview, OwlcmsSession.getFop()),
                Key.KEY_C);
        goodLift.getElement().setAttribute("theme", "primary success icon");
        goodLift.setWidth("8em");
        badLift.getElement().setAttribute("theme", "primary error icon");
        badLift.setWidth("8em");

        // workaround for unpredictable behaviour of FormLayout
        FormLayout layoutGreen = new FormLayout();
        FormLayout layoutRed = new FormLayout();
        FormLayout layout3 = new FormLayout();
        FormItem red, green, loading;
        Label redLabel = new Label(Translator.translate("JuryDialog.BadLiftLabel"));
        red = layoutGreen.addFormItem(badLift, redLabel);
        fixItemFormat(layoutGreen, red);
        Label greenLabel = new Label(Translator.translate("JuryDialog.GoodLiftLabel"));
        green = layoutRed.addFormItem(goodLift, greenLabel);
        fixItemFormat(layoutRed, green);
        loading = layout3.addFormItem(loadingError, Translator.translate("JuryDialog.CallControllerLabel"));
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
                    status = (liftValue > 0 ? Translator.translate("JuryDialog.RefGoodLift")
                            : Translator.translate("JuryDialog.RefBadLift"));
                    redLabel.setText(liftValue < 0 ? Translator.translate("JuryDialog.Accept")
                            : Translator.translate("JuryDialog.Reverse"));
                    greenLabel.setText(liftValue < 0 ? Translator.translate("JuryDialog.Reverse")
                            : Translator.translate("JuryDialog.Accept"));
                    layoutGreen.setEnabled(true);
                    layoutRed.setEnabled(true);
                } else {
                    status = Translator.translate("JuryDialog.NotLiftedYet");
                    redLabel.setText(Translator.translate("JuryDialog.NotLiftedYet"));
                    greenLabel.setText(Translator.translate("JuryDialog.NotLiftedYet"));
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
                this.setHeader(Translator.translate("JuryDialog.NoCurrentAthlete"));
                redLabel.setText(Translator.translate("JuryDialog.NotLiftedYet"));
                greenLabel.setText(Translator.translate("JuryDialog.NotLiftedYet"));
                layoutGreen.setEnabled(false);
                layoutRed.setEnabled(false);
            }
        });

        this.addDialogCloseActionListener((e) -> {
            if (shortcutTooSoon()) {
                return;
            }
            doClose(false);
        });
    }

    public void doClose(boolean noAction) {
        UI.getCurrent().access(() -> {
            if (noAction) {
                JuryNotification event = new UIEvent.JuryNotification(reviewedAthlete, origin,
                        JuryDeliberationEventType.END_DELIBERATION,
                        null);
                OwlcmsSession.getFop().getUiEventBus().post(event);
            }
            this.close();

            logger.info("{}end of jury deliberation", OwlcmsSession.getFop().getLoggingName());
            ((JuryContent) origin).doSync();
            this.close();
        });
    }

    private void doBadLift(Athlete athleteUnderReview, FieldOfPlay fop) {
        if (shortcutTooSoon()) {
            return;
        }
        fop.getFopEventBus().post(new FOPEvent.JuryDecision(athleteUnderReview, this, false));
        doClose(false);
    }

    private void doCallController(Athlete athleteUnderReview, FieldOfPlay fop) {
        if (shortcutTooSoon()) {
            return;
        }
        JuryNotification event = new UIEvent.JuryNotification(athleteUnderReview, origin,
                JuryDeliberationEventType.CALL_TECHNICAL_CONTROLLER, null);
        OwlcmsSession.getFop().getUiEventBus().post(event);
        return;
    }

    private void doGoodLift(Athlete athleteUnderReview, FieldOfPlay fop) {
        if (shortcutTooSoon()) {
            return;
        }
        fop.getFopEventBus().post(new FOPEvent.JuryDecision(athleteUnderReview, this, true));
        doClose(false);
    }

    private void fixItemFormat(FormLayout layout, FormItem f) {
        layout.setWidthFull();
        layout.setColspan(f, 1);
        f.getElement().getStyle().set("--vaadin-form-item-label-width", "15em");
    }

    private synchronized boolean shortcutTooSoon() {
        long now = System.currentTimeMillis();
        long delta = now - lastShortcut;
        if (delta > 350) {
            //logger.trace("long enough {} {}", delta, LoggerUtils.whereFrom());
            lastShortcut = now;
            return false;
        } else {
            //logger.trace("too soon {} {}", delta, LoggerUtils.whereFrom());
            return true;
        }
    }
}
