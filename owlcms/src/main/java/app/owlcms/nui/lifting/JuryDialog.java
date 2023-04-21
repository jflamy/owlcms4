/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.FormItem;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.ContentAlignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDialog extends Dialog {
	private static final int CONTROLLER_REFNUM = 4;
	private JuryDeliberationEventType deliberation;
	private String endBreakText;

	private long lastShortcut;
	private Integer liftValue;
	final private Logger logger = (Logger) LoggerFactory.getLogger(JuryDialog.class);
	private Object origin;
	private Athlete reviewedAthlete;
	private Integer reviewedLift;

	{
		logger.setLevel(Level.INFO);
	}

	public JuryDialog(Object origin, Athlete athleteUnderReview, JuryDeliberationEventType deliberation,
	        boolean summonEnabled) {
		this.origin = origin;
		this.deliberation = deliberation;
		this.setCloseOnEsc(false);
		this.setCloseOnOutsideClick(false);
		this.setModal(false);
		this.setDraggable(true);
		logger.info(
		        deliberation == JuryDeliberationEventType.START_DELIBERATION ? "{}{} reviewedAthlete {}" : "{}{}",
		        OwlcmsSession.getFop().getLoggingName(), deliberation, athleteUnderReview);
		this.setReviewedAthlete(athleteUnderReview);
		this.setWidth("50em");
		switch (deliberation) {
		case CALL_REFEREES:
			doSummonReferees(origin);
			addSummonReferees(origin);
			break;
		case START_DELIBERATION:
			doDeliberation(origin, athleteUnderReview, null);
			if (summonEnabled) {
				addSummonReferees(origin);
			}
			break;
		case CHALLENGE:
			doChallenge(origin, athleteUnderReview, null);
			if (summonEnabled) {
				addSummonReferees(origin);
			}
			break;
		case TECHNICAL_PAUSE:
			doTechnicalPause(origin);
			addSummonReferees(origin);
			addCallController();
			break;
		default:
			break;
		}

		doEnd();
	}

	public void doClose(boolean noAction) {
		UI.getCurrent().access(() -> {
			OwlcmsSession.withFop(fop -> {
				fop.fopEventPost(new FOPEvent.StartLifting(this));
			});
			if (noAction) {
				((JuryContent) origin).doSync();
			}
			this.close();

			String message = deliberation == JuryDeliberationEventType.START_DELIBERATION
			        ? "{}end of jury deliberation"
			        : (deliberation == JuryDeliberationEventType.CHALLENGE
			                ? "{}end of challenge"
			                : "{}end jury break");
			logger.info(
			        message,
			        OwlcmsSession.getFop().getLoggingName());

			this.close();
		});
	}

	private void addCallController() {
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
	}

	private void addSummonReferees(Object origin2) {
		FormLayout layout3 = new FormLayout();

		FormItem callController;
		Button one = new Button("1", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(1);
			});
		});
		styleRefereeButton(one, true);

		Button two = new Button("2", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(2);
			});
		});
		styleRefereeButton(two, false);

		Button three = new Button("3", (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(3);
			});
		});
		styleRefereeButton(three, false);
		Shortcuts.addShortcutListener(this, () -> summonReferee(1), Key.KEY_H);
		Shortcuts.addShortcutListener(this, () -> summonReferee(2), Key.KEY_I);
		Shortcuts.addShortcutListener(this, () -> summonReferee(3), Key.KEY_J);
		Shortcuts.addShortcutListener(this, () -> summonReferee(0), Key.KEY_K);

		Button all = new Button(Translator.translate("JuryDialog.AllReferees"), (e) -> {
			OwlcmsSession.withFop(fop -> {
				this.summonReferee(0);
			});
		});
		styleRefereeButton(all, false);

		FlexLayout selection = new FlexLayout(one, two, three, all);
		selection.setWidth("30em");
		selection.setAlignContent(ContentAlignment.STRETCH);

		Label lab = new Label(Translator.translate("JuryDialog.SummonRefereesLabel"));
		HorizontalLayout label = new HorizontalLayout(lab);
		label.setPadding(false);
		label.setMargin(false);
		label.setAlignItems(Alignment.STRETCH);
		callController = layout3.addFormItem(selection, Translator.translate("JuryDialog.SummonRefereesLabel"));
		fixItemFormat(layout3, callController);
		this.add(layout3);
		this.setWidth("50em");
	}

	private void doBadLift(Athlete athleteUnderReview, FieldOfPlay fop) {
		if (shortcutTooSoon()) {
			return;
		}
		fop.fopEventPost(new FOPEvent.JuryDecision(athleteUnderReview, this, false));
		UI.getCurrent().access(() -> {
			if (origin != null) {
				((JuryContent) origin).decisionNotification.close();
			}
		});
		doClose(false);
	}

	private void doCallController(FieldOfPlay fop) {
		if (shortcutTooSoon()) {
			return;
		}
		this.summonReferee(CONTROLLER_REFNUM);
		return;
	}

	private void doDeliberation(Object origin, Athlete athleteUnderReview, Object newParam) {
		// stop competition
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop == null) {
			return;
		}
		if (!(fop.getState() != FOPState.BREAK && fop.getBreakType() == BreakType.JURY)) {
			// not already in a jury break, force one.
			fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
		}
		this.setDraggable(true);

		doDebate(athleteUnderReview);
	}

	private void doChallenge(Object origin, Athlete athleteUnderReview, Object newParam) {
		// stop competition
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop == null) {
			return;
		}
		if (!(fop.getState() != FOPState.BREAK && fop.getBreakType() == BreakType.CHALLENGE)) {
			// not already in a jury break, force one.
			fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.CHALLENGE, CountdownType.INDEFINITE, 0, null, true, this));
		}
		this.setDraggable(true);

		doDebate(athleteUnderReview);
	}

	private void doDebate(Athlete athleteUnderReview) {
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
		endBreakText = Translator.translate("JuryDialog.ResumeCompetition");

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
			if (getReviewedAthlete() != null) {
				reviewedLift = getReviewedAthlete().getAttemptsDone();
				liftValue = getReviewedAthlete().getActualLift(reviewedLift);
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
				String weightText = liftValue != null ? Translator.translate("Kg", Math.abs(liftValue)) : "";
				String athleteText = getReviewedAthlete().getFullId();
				this.setHeaderTitle(athleteText + " \u2003 " + weightText + " \u2013 " + status);

//				H3 status1 = new H3(status);
//				H3 weight = new H3(weightText);
//				H3 athlete = new H3(athleteText);
//				status1.getStyle().set("margin-top", "0");
//				athlete.getStyle().set("margin-top", "0");
//				status1.getStyle().set("margin-top", "0");
//				HorizontalLayout header = new HorizontalLayout(
//				        athlete,
//				        weight,
//				        status1);
//				header.setFlexGrow(1, athlete);
//				header.setWidth("90%");
//				header.setPadding(true);
//				header.setMargin(false);
//				this.getHeader().add(header);
			} else {
				this.setHeaderTitle(Translator.translate("JuryDialog.NoCurrentAthlete"));
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
		this.getFooter().add(footer);

		this.addDialogCloseActionListener((e) -> {
			if (shortcutTooSoon()) {
				return;
			}
			doClose(true);
		});
	}

	private void doGoodLift(Athlete athleteUnderReview, FieldOfPlay fop) {
		if (shortcutTooSoon()) {
			return;
		}

		fop.fopEventPost(new FOPEvent.JuryDecision(athleteUnderReview, this, true));
		doClose(false);
	}

	private void doSummonReferees(Object origin2) {
		// jury calls referees
		endBreakText = Translator.translate("JuryDialog.ResumeCompetition");
		this.addAttachListener((e) -> {
			this.setHeaderTitle(Translator.translate("JuryDialog.CALL_REFEREES"));
		});
	}

	private void doTechnicalPause(Object origin) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop == null) {
			return;
		}
		fop.fopEventPost(
		        new FOPEvent.BreakStarted(BreakType.TECHNICAL, CountdownType.INDEFINITE, 0, null, true, this));
		endBreakText = Translator.translate("JuryDialog.ResumeCompetition");

		this.addAttachListener((e) -> {
			this.setHeaderTitle(Translator.translate("JuryDialog.TECHNICAL_PAUSE"));
		});
	}

	private void fixItemFormat(FormLayout layout, FormItem f) {
		layout.setWidthFull();
		layout.setColspan(f, 1);
		f.getElement().getStyle().set("--vaadin-form-item-label-width", "15em");
	}

	private Athlete getReviewedAthlete() {
		return reviewedAthlete;
	}

	private void setReviewedAthlete(Athlete reviewedAthlete) {
		this.reviewedAthlete = reviewedAthlete;
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

	private void styleRefereeButton(Button button, boolean first) {
		final String buttonStyle = "margin-left: 1em";
		final String refereeStyle = "contrast";
		button.setWidth("4em");
		button.getElement().setAttribute("theme", refereeStyle);
		if (!first) {
			button.getElement().setAttribute("style", buttonStyle);
		}
	}

	private void summonReferee(int i) {
		// stop competition and summon referee
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop == null) {
			return;
		}
//        if (fop.getState() != FOPState.BREAK) {
//            fop.fopEventPost(new FOPEvent.BreakStarted(BreakType.JURY, CountdownType.INDEFINITE, 0, null, true, this));
//        }
		fop.fopEventPost(new FOPEvent.SummonReferee(this.origin, i));

	}
}
