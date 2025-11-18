/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import com.vaadin.flow.component.Html;
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
import app.owlcms.uievents.UIEvent;

/**
 * Custom dialog for jury decisions with proper footer containing Good Lift and No Lift buttons.
 */
@SuppressWarnings("serial")
public class JuryDecisionDialog extends Dialog {
	
	private Button goodLiftButton;
	private Button noLiftButton;
	private Button resumeButton;
	private Button announceButton;
	private Athlete athlete;
	private Object origin;
	private Runnable onClose;
	
	public JuryDecisionDialog(UIEvent.JuryNotification e, JuryDeliberationEventType juryDecision, Runnable onClose) {
		this.origin = e.getOrigin();
		this.onClose = onClose;
		
		// Get athlete - either from event or from current FOP state
		this.athlete = e.getAthlete();
		if (this.athlete == null && OwlcmsSession.getFop() != null) {
			this.athlete = OwlcmsSession.getFop().getAthleteUnderReview();
		}
		
		String reversalText = "";
		if (e.getReversal() != null) {
			reversalText = e.getReversal() ? Translator.translate("JuryNotification.Reversal")
			        : Translator.translate("JuryNotification.Confirmed");
		}
		
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
		explanationText = Translator.translate("Announcer.JuryDecisionExplanation");
	} else if (juryDecision == JuryDeliberationEventType.BAD_LIFT) {
		juryDecisionDisplay = "<div style='color: red; font-size: xx-large; font-weight: bold; text-align: center; margin: 0.5em 0;'>" 
			+ Translator.translate("JuryDialog.BadLiftLabel").toUpperCase() + " " + reversalText + "</div>";
		explanationText = Translator.translate("Announcer.JuryDecisionExplanation");
	} else {
		// Show waiting status when jury is deliberating
		waitingStatus = "<div style='color: #666; font-size: x-large; font-style: italic; text-align: center; margin: 0.5em 0;'>" 
			+ Translator.translate("Announcer.WaitingForJuryDecision") + "</div>";
		explanationText = Translator.translate("Announcer.WaitingForJuryExplanation");
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
			// Jury has made a decision - only show Resume button with primary theme
			this.resumeButton = new Button(Translator.translate("JuryNotification.END_JURY_BREAK"));
			this.resumeButton.getElement().setAttribute("theme", "primary");
			this.resumeButton.addClickListener(c -> {
				OwlcmsSession.getFop().fopEventPost(new FOPEvent.StartLifting(this.origin));
				close();
			});
			buttonContainer.add(this.resumeButton);
		} else {
			// Waiting for jury decision - show Good Lift / No Lift buttons and Resume button
			this.goodLiftButton = new Button(Translator.translate("JuryDialog.GoodLiftLabel"), new Icon(VaadinIcon.CHECK));
			this.goodLiftButton.setWidth("150px");
			this.goodLiftButton.getElement().setAttribute("theme", "primary success");
			this.goodLiftButton.addClickListener(c -> {
				if (this.athlete != null) {
					OwlcmsSession.getFop().fopEventPost(new FOPEvent.JuryDecision(this.athlete, this.origin, true, false));
				}
				close();
			});
			
			this.noLiftButton = new Button(Translator.translate("JuryDialog.BadLiftLabel"), new Icon(VaadinIcon.CLOSE));
			this.noLiftButton.setWidth("150px");
			this.noLiftButton.getElement().setAttribute("theme", "primary error");
			this.noLiftButton.addClickListener(c -> {
				if (this.athlete != null) {
					OwlcmsSession.getFop().fopEventPost(new FOPEvent.JuryDecision(this.athlete, this.origin, false, false));
				}
				close();
			});
			
			this.resumeButton = new Button(Translator.translate("JuryNotification.END_JURY_BREAK"));
			this.resumeButton.getElement().setAttribute("theme", "secondary");
			this.resumeButton.addClickListener(c -> {
				OwlcmsSession.getFop().fopEventPost(new FOPEvent.StartLifting(this.origin));
				close();
			});
			
			buttonContainer.add(this.goodLiftButton, this.noLiftButton, this.resumeButton);
		}
		
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
	
	@Override
	public void close() {
		super.close();
		if (this.onClose != null) {
			this.onClose.run();
		}
	}
}
