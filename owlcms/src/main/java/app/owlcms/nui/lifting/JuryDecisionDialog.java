/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.JuryRejectionReasons;
import app.owlcms.uievents.UIEvent;

/**
 * The Speaker's jury dialog. While the jury reviews a lift it shows "Waiting for decision" with Good Lift / No Lift /
 * End jury break, so that the Speaker can enter a decision relayed by the Technical Controller. Once the jury's
 * decision has been applied, it shows the decision and the rule for the Speaker to announce, with a Close button.
 */
@SuppressWarnings("serial")
public class JuryDecisionDialog extends Dialog {
	
	private Button goodLiftButton;
	private Button noLiftButton;
	private Button resumeButton;
	private Athlete athlete;
	private Object origin;
	private Runnable onClose;
	private boolean sent;
	private final boolean decisionShown;
	
	public JuryDecisionDialog(UIEvent.JuryNotification e, JuryDeliberationEventType juryDecision, Runnable onClose) {
		this.origin = e.getOrigin();
		this.onClose = onClose;
		this.decisionShown = juryDecision != null;
		
		// Get athlete - either from event or from current FOP state
		this.athlete = e.getAthlete();
		if (this.athlete == null && OwlcmsSession.getFop() != null) {
			this.athlete = OwlcmsSession.getFop().getAthleteUnderReview();
		}
		
		String statusKey = e.getReversalStatusKey();
		String reversalText = statusKey != null ? Translator.translate(statusKey) : "";
		
		// Build athlete description first - needed for all cases
		String athleteDescription;
		if (this.athlete != null) {
			int previousAttemptNo = this.athlete.getAttemptsDone() - 1;
			String attemptType = (previousAttemptNo < 3) ? Translator.translate("Snatch") : Translator.translate("Clean_and_Jerk");
			int attemptNumber = previousAttemptNo % 3 + 1;
			athleteDescription = this.athlete.getFullName() + " - " + attemptType + " " + attemptNumber;
		} else {
			athleteDescription = "";
		}
		
		// Build header and message based on whether jury made decision
		String headerText;
		String messageText;
		String messageStyle = "";
		
	if (juryDecision == JuryDeliberationEventType.GOOD_LIFT) {
		headerText = Translator.translate("Announcer.JuryDecisionTitle");
		messageText = athleteDescription;
		messageStyle = "color: green; font-size: x-large; font-weight: bold; margin-top: 0.5em;";
	} else if (juryDecision == JuryDeliberationEventType.BAD_LIFT) {
		headerText = Translator.translate("Announcer.JuryDecisionTitle");
		messageText = athleteDescription;
		messageStyle = "color: red; font-size: x-large; font-weight: bold; margin-top: 0.5em;";
	} else {
			// START_DELIBERATION or CHALLENGE - no jury decision yet, only hand signals
			JuryDeliberationEventType et = e.getDeliberationEventType();
			if (et == JuryDeliberationEventType.CHALLENGE) {
				headerText = Translator.translate("JuryNotification.CHALLENGE");
			} else if (e.isPanelReview()) {
				headerText = Translator.translate("JuryNotification.PanelReview");
			} else {
				headerText = Translator.translate("JuryNotification.START_DELIBERATION");
			}
			
			messageText = athleteDescription;
			messageStyle = "font-size: large; font-weight: bold";
		}
		
		// Create dialog layout
		VerticalLayout content = new VerticalLayout();
		content.setPadding(true);
		content.setSpacing(true);
		
		// Add header
		Div header = new Div();
		header.setText(headerText);
		header.getStyle().set("font-size", "x-large");
		header.getStyle().set("font-weight", "bold");
		header.getStyle().set("margin-bottom", "1em");
		
	// Add message
	Div message = new Div();
	String juryDecisionDisplay = "";
	String waitingStatus = "";
	
	String explanationText;
	if (juryDecision == JuryDeliberationEventType.GOOD_LIFT) {
		juryDecisionDisplay = "<div style='color: green; font-size: xx-large; font-weight: bold; text-align: center; margin: 0.5em 0;'>" 
			+ Translator.translate("JuryDialog.GoodLiftLabel").toUpperCase() + " " + reversalText + "</div>";
		explanationText = Translator.translate("Announcer.AnnounceGoodLift");
	} else if (juryDecision == JuryDeliberationEventType.BAD_LIFT) {
		juryDecisionDisplay = "<div style='color: red; font-size: xx-large; font-weight: bold; text-align: center; margin: 0.5em 0;'>" 
			+ Translator.translate("JuryDialog.BadLiftLabel").toUpperCase() + " " + reversalText + "</div>";
		String reason = JuryRejectionReasons.label(e.getReasonCode());
		explanationText = Translator.translate("Announcer.AnnounceJuryDecision");
		if (reason != null) {
			juryDecisionDisplay += "<div style='color: red; font-size: xx-large; font-weight: bold; text-align: center; margin: 0.5em 0;'>"
				+ reason + "</div>";
		}
	} else {
		// Show waiting status when jury is deliberating
		waitingStatus = "<div style='color: #666; font-size: x-large; font-style: italic; text-align: center; margin: 0.5em 0;'>" 
			+ Translator.translate("Announcer.WaitingForJuryDecision") + "</div>";
		explanationText = Translator.translate("Announcer.WaitingForJuryExplanation");
	}
	if (e.isChairEntered() && juryDecision != null) {
		// seven-person Adjudication Panel: the decision did not come from the panel vote
		juryDecisionDisplay += "<div style='font-size: x-large; font-weight: bold; text-align: center; margin: 0.5em 0;'>"
			+ Translator.translate("JuryNotification.ChairEnteredDecision") + "</div>";
	}
	
	message.add(new Html(
	        """
	        <div>
	            <div style="%s">%s</div>
	            %s%s
	            <br/>
	            <div style="font-size: x-large;">%s</div>
	        </div>
	        """.formatted(messageStyle, messageText, juryDecisionDisplay, waitingStatus,
	                explanationText)));
	
	content.add(header, message);		// Create footer with buttons
		HorizontalLayout footer = new HorizontalLayout();
		footer.setPadding(true);
		footer.setSpacing(true);
		footer.setWidthFull();
		footer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
		
		// Create button container
		HorizontalLayout buttonContainer = new HorizontalLayout();
		buttonContainer.setSpacing(true);
		
		if (juryDecision != null) {
			// the jury's decision has already been applied and displayed: the Speaker announces it
			this.resumeButton = new Button(Translator.translate("Close"));
			this.resumeButton.getElement().setAttribute("theme", "primary");
			this.resumeButton.addClickListener(c -> close());
			buttonContainer.add(this.resumeButton);
		} else {
			// Waiting for jury decision - show Good Lift / No Lift buttons and Resume button
			this.goodLiftButton = new Button(Translator.translate("JuryDialog.GoodLiftLabel"), new Icon(VaadinIcon.CHECK));
			this.goodLiftButton.setWidth("150px");
			this.goodLiftButton.getElement().setAttribute("theme", "primary success");
			this.goodLiftButton.addClickListener(c -> sendOnce(() -> {
				if (this.athlete != null) {
					OwlcmsSession.getFop().fopEventPost(new FOPEvent.JuryDecision(this.athlete, this.origin, true, false));
				}
			}));
			
			this.noLiftButton = new Button(Translator.translate("JuryDialog.BadLiftLabel"), new Icon(VaadinIcon.CLOSE));
			this.noLiftButton.setWidth("150px");
			this.noLiftButton.getElement().setAttribute("theme", "primary error");
			this.noLiftButton.addClickListener(c -> sendOnce(() -> {
				if (this.athlete != null) {
					OwlcmsSession.getFop().fopEventPost(new FOPEvent.JuryDecision(this.athlete, this.origin, false, false));
				}
			}));
			
			this.resumeButton = new Button(Translator.translate("JuryNotification.END_JURY_BREAK"));
			this.resumeButton.getElement().setAttribute("theme", "secondary");
			this.resumeButton.addClickListener(c -> sendOnce(() -> {
				OwlcmsSession.getFop().fopEventPost(new FOPEvent.StartLifting(this.origin));
			}));
			
			buttonContainer.add(this.goodLiftButton, this.noLiftButton, this.resumeButton);
		}
		this.resumeButton.addClickShortcut(Key.ENTER).listenOn(this);
		
		footer.add(buttonContainer);
		
		// Assemble dialog
		VerticalLayout dialogLayout = new VerticalLayout(content, footer);
		dialogLayout.setPadding(false);
		dialogLayout.setSpacing(false);
		
		add(dialogLayout);
		setCloseOnEsc(false);
		setCloseOnOutsideClick(false);
		setWidth("600px");
	}
	
	/** @return true when the dialog shows a decision already applied, false while it waits for the decision */
	public boolean isDecisionShown() {
		return this.decisionShown;
	}

	@Override
	public void close() {
		super.close();
		if (this.onClose != null) {
			this.onClose.run();
		}
	}

	/**
	 * A focused button with an Enter click shortcut, or a double-click, delivers the click twice; only the first
	 * one is acted upon.
	 */
	private void sendOnce(Runnable action) {
		if (this.sent) {
			return;
		}
		this.sent = true;
		action.run();
		close();
	}
}
