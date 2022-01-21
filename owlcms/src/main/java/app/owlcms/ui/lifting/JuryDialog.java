/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDialog extends EnhancedDialog {
    final private Logger logger = (Logger) LoggerFactory.getLogger(JuryDialog.class);
    {
        logger.setLevel(Level.INFO);
    }

    private Athlete reviewedAthlete;
    private Integer reviewedLift;
    private Integer liftValue;
    private Object origin;
    private long lastShortcut;
    private boolean deliberation;
    private String endBreakText;

    /**
     * Used by the announcer -- tries to guess what type of break is pertinent based on field of play.
     *
     * @param origin             the origin
     * @param athleteUnderReview
     * @param deliberation
     */
    public JuryDialog(Object origin, Athlete athleteUnderReview, boolean deliberation) {
        this.origin = origin;
        this.deliberation = deliberation;
        this.setCloseOnEsc(false);
        logger.info(deliberation ? "{}jury deliberation reviewedAthlete {}" : "{}start jury technical pause",
                OwlcmsSession.getFop().getLoggingName(), athleteUnderReview);
        this.reviewedAthlete = athleteUnderReview;

        if (deliberation) {
            doDeliberation(origin, athleteUnderReview);
        } else {
            doTechnicalPause(origin);
        }

        doCallController();
        doEnd();
    }

    public void doClose(boolean noAction) {
        UI.getCurrent().access(() -> {
            JuryNotification event = new UIEvent.JuryNotification(reviewedAthlete, origin,
                    deliberation ? JuryDeliberationEventType.END_DELIBERATION
                            : JuryDeliberationEventType.END_TECHNICAL_PAUSE,
                    null);
            OwlcmsSession.getFop().getUiEventBus().post(event);
            if (noAction) {
                ((JuryContent) origin).doSync();
            }
            this.close();

            logger.info(deliberation ? "{}end of jury deliberation" : "{}end jury technical pause",
                    OwlcmsSession.getFop().getLoggingName());

            this.close();
        });
    }

    private void doBadLift(Athlete athleteUnderReview, FieldOfPlay fop) {
        if (shortcutTooSoon()) {
            return;
        }
        fop.fopEventPost(new FOPEvent.JuryDecision(athleteUnderReview, this, false));
        UI.getCurrent().access(() -> {
            ((JuryContent) origin).decisionNotification.close();
        });
        doClose(false);
    }

    private void doCallController() {
        FormLayout layout3 = new FormLayout();
        FormItem callController;
        Button callControllerButton = new Button(Translator.translate("JuryDialog.CallController"),
                (e) -> doCallController(OwlcmsSession.getFop()));
        Shortcuts.addShortcutListener(this, () -> doCallController(OwlcmsSession.getFop()),
                Key.KEY_C);
        callController = layout3.addFormItem(callControllerButton,
                Translator.translate("JuryDialog.CallControllerLabel"));
        fixItemFormat(layout3, callController);
        this.add(layout3);
        this.setWidth("50em");
    }

    private void doCallController(FieldOfPlay fop) {
        if (shortcutTooSoon()) {
            return;
        }
        JuryNotification event = new UIEvent.JuryNotification(null, origin,
                JuryDeliberationEventType.CALL_TECHNICAL_CONTROLLER, null);
        OwlcmsSession.getFop().getUiEventBus().post(event);
        return;
    }

    private void doDeliberation(Object origin, Athlete athleteUnderReview) {
        // stop competition
        OwlcmsSession.getFop()
                .fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, this));
        JuryNotification event = new UIEvent.JuryNotification(athleteUnderReview, origin,
                JuryDeliberationEventType.START_DELIBERATION, null);
        OwlcmsSession.getFop().getUiEventBus().post(event);

        Button goodLift = new Button(IronIcons.DONE.create(),
                (e) -> doGoodLift(athleteUnderReview, OwlcmsSession.getFop()));
        Shortcuts.addShortcutListener(this, () -> doGoodLift(athleteUnderReview, OwlcmsSession.getFop()), Key.KEY_G);

        Button badLift = new Button(IronIcons.CLOSE.create(),
                (e) -> doBadLift(athleteUnderReview, OwlcmsSession.getFop()));
        Shortcuts.addShortcutListener(this, () -> doBadLift(athleteUnderReview, OwlcmsSession.getFop()), Key.KEY_B);

        goodLift.getElement().setAttribute("theme", "primary success icon");
        goodLift.setWidth("8em");
        badLift.getElement().setAttribute("theme", "primary error icon");
        badLift.setWidth("8em");
        endBreakText = Translator.translate("JuryDialog.EndDeliberation");

        // workaround for unpredictable behaviour of FormLayout
        FormLayout layoutGreen = new FormLayout();
        FormLayout layoutRed = new FormLayout();
        FormItem red, green;

        Label redLabel = new Label(Translator.translate("JuryDialog.BadLiftLabel"));
        red = layoutGreen.addFormItem(badLift, redLabel);
        fixItemFormat(layoutGreen, red);
        Label greenLabel = new Label(Translator.translate("JuryDialog.GoodLiftLabel"));
        green = layoutRed.addFormItem(goodLift, greenLabel);
        fixItemFormat(layoutRed, green);

        this.add(layoutGreen, layoutRed);

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
    }

    private void doEnd() {

        Button endBreak = new Button(endBreakText, (e) -> doClose(true));
        Shortcuts.addShortcutListener(this, () -> {
            if (shortcutTooSoon()) {
                return;
            }
            doClose(true);
        }, Key.ESCAPE);
        FlexLayout footer = new FlexLayout(endBreak);
        endBreak.getElement().setAttribute("theme", "primary");
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        this.addToFooter(footer);

        this.addDialogCloseActionListener((e) -> {
            if (shortcutTooSoon()) {
                return;
            }
            doClose(false);
        });
    }

    private void doGoodLift(Athlete athleteUnderReview, FieldOfPlay fop) {
        if (shortcutTooSoon()) {
            return;
        }

        fop.fopEventPost(new FOPEvent.JuryDecision(athleteUnderReview, this, true));
        doClose(false);
    }

    private void doTechnicalPause(Object origin) {
        // technical pause from Jury
        OwlcmsSession.getFop()
                .fopEventPost(new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, this));
        JuryNotification event = new UIEvent.JuryNotification(null, origin,
                JuryDeliberationEventType.TECHNICAL_PAUSE, null);
        OwlcmsSession.getFop().getUiEventBus().post(event);
        endBreakText = Translator.translate("JuryDialog.EndTechnicalPause");

        this.addAttachListener((e) -> {
            if (reviewedAthlete != null) {
                reviewedLift = reviewedAthlete.getAttemptsDone();
                liftValue = reviewedAthlete.getActualLift(reviewedLift);
                String status;
                if (liftValue != null) {
                    status = (liftValue > 0 ? Translator.translate("JuryDialog.RefGoodLift")
                            : Translator.translate("JuryDialog.RefBadLift"));
                } else {
                    status = Translator.translate("JuryDialog.NotLiftedYet");
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
                this.setHeader(Translator.translate("JuryDialog.TechnicalPause"));
            }
        });
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
            // logger.trace("long enough {} {}", delta, LoggerUtils.whereFrom());
            lastShortcut = now;
            return false;
        } else {
            // logger.trace("too soon {} {}", delta, LoggerUtils.whereFrom());
            return true;
        }
    }
}
