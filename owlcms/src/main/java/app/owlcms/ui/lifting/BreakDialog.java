/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.lifting;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.timepicker.TimePicker;

import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class BreakDialog extends Dialog {
	public enum CountdownType {
		DURATION, TARGET
	}

	final private Logger logger = (Logger) LoggerFactory.getLogger(BreakDialog.class);;
	{
		logger.setLevel(Level.INFO);
	};

	private Button breakStart = null;
	private Button breakPause = null;
	private Button breakEnd = null;
	private Object origin;
	private Label minutes;
	private HorizontalLayout timer;

	RadioButtonGroup<CountdownType> ct;
	RadioButtonGroup<BreakType> bt;
	NumberField nf = new NumberField();
	TimePicker tp = new TimePicker();
	DatePicker dp = new DatePicker();

	BreakDialog(Object origin) {
		this.setOrigin(origin);
		Dialog dialog = this;

		configureDuration();
		HorizontalLayout buttons = configureButtons(dialog);
		configureTimerDisplay();
		assembleDialog(dialog, buttons);
		syncWithFop();
		ct.setValue(CountdownType.DURATION);
		nf.setValue(10.0D);
	}

	public ComponentEventListener<ClickEvent<Button>> pauseBreak() {
		return (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.BreakPaused(this.getOrigin())));
			breakStart.setEnabled(true);
			breakPause.setEnabled(false);
			breakEnd.setEnabled(true);
		};
	}

	public ComponentEventListener<ClickEvent<Button>> startBreak() {
		return (e) -> {
			OwlcmsSession.withFop(fop -> {
				long breakDuration = setBreakTimeRemaining(ct.getValue(), nf, tp, dp);
				fop.getFopEventBus()
					.post(new FOPEvent.BreakStarted(bt.getValue(), (int) breakDuration, this.getOrigin()));
			});
			e.getSource().setEnabled(false);
			breakStart.setEnabled(false);
			breakPause.setEnabled(true);
			breakEnd.setEnabled(true);
			timer.getStyle().set("visibility", "visible"); //$NON-NLS-1$ //$NON-NLS-2$
		};
	}

	public ComponentEventListener<ClickEvent<Button>> stopBreak(Dialog dialog) {
		return (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.StartLifting(this.getOrigin())));
			breakStart.setEnabled(true);
			breakPause.setEnabled(false);
			breakEnd.setEnabled(false);
			dialog.close();
		};
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		ct.addValueChangeListener(e -> {
			CountdownType cType = e.getValue();
			setValues(cType, nf, tp, dp);
			if (cType != CountdownType.TARGET) {
				switchToDuration(cType, nf, tp, dp);
			} else {
				switchToTarget(cType, nf, tp, dp);
			}
		});
	}

	private void assembleDialog(Dialog dialog, HorizontalLayout buttons) {
		dialog.add(bt);
		dialog.add(new Hr());
		dialog.add(ct);
		dialog.add(new Hr());
		dialog.add(timer);
		timer.getStyle().set("visibility", "hidden"); //$NON-NLS-1$ //$NON-NLS-2$
		dialog.add(buttons);
	}

	private void computeRoundedTargetValues(TimePicker tp, DatePicker dp) {
		int timeStep = 30;
		tp.setStep(Duration.ofMinutes(timeStep));
		LocalTime nowTime = LocalTime.now();
		int nowMin = nowTime.getMinute();
		int nowHr = nowTime.getHour();
		int previousStepMin = (nowMin / timeStep) * timeStep; // between 0 and 50
		int nextStepMin = (previousStepMin + timeStep) % 60;
		logger.trace("previousStepMin = {} nextStepMin = {}", previousStepMin, nextStepMin); //$NON-NLS-1$
		int nextHr = (nextStepMin == 0 ? nowHr + 1 : nowHr);
		LocalDate nextDate = LocalDate.now();
		if (nextHr >= 24) {
			nextDate.plusDays(1);
			nextHr = nextHr % 24;
		}
		dp.setValue(nextDate);
		tp.setValue(LocalTime.of(nextHr, nextStepMin));
	}

	private HorizontalLayout configureButtons(Dialog dialog) {
		breakStart = new Button(AvIcons.PLAY_ARROW.create(), startBreak());
		breakStart.getElement().setAttribute("theme", "primary"); //$NON-NLS-1$ //$NON-NLS-2$
		breakStart.getElement().setAttribute("title", getTranslation("BreakDialog.4")); //$NON-NLS-1$ //$NON-NLS-2$

		breakPause = new Button(AvIcons.PAUSE.create(), pauseBreak());
		breakPause.getElement().setAttribute("theme", "primary"); //$NON-NLS-1$ //$NON-NLS-2$
		breakPause.getElement().setAttribute("title", getTranslation("BreakDialog.3")); //$NON-NLS-1$ //$NON-NLS-2$

		breakEnd = new Button(AvIcons.STOP.create(), stopBreak(dialog));
		breakEnd.getElement().setAttribute("theme", "primary"); //$NON-NLS-1$ //$NON-NLS-2$
		breakEnd.getElement().setAttribute("title", getTranslation("BreakDialog.0")); //$NON-NLS-1$ //$NON-NLS-2$

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.add(breakStart, breakPause, breakEnd);
		buttons.setWidth("100%"); //$NON-NLS-1$
		buttons.setJustifyContentMode(JustifyContentMode.AROUND);
		return buttons;
	}

	private void configureDuration() {
		bt = new RadioButtonGroup<BreakType>();
		bt.setItems(BreakType.values());
		bt.setLabel(getTranslation("BreakDialog.1")); //$NON-NLS-1$

		ct = new RadioButtonGroup<CountdownType>();
		ct.setItems(CountdownType.values());
		ct.setLabel(getTranslation("BreakDialog.2")); //$NON-NLS-1$

		nf.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.DURATION, nf, tp, dp));
		Locale locale = new Locale("en", "SE"); // ISO 8601 style dates and time //$NON-NLS-1$ //$NON-NLS-2$
		tp.setLocale(locale);
		tp.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET, nf, tp, dp));
		dp.setLocale(locale);
		tp.addValueChangeListener(e -> setBreakTimeRemaining(CountdownType.TARGET, nf, tp, dp));
		minutes = new Label("minutes"); //$NON-NLS-1$

		ct.addComponents(CountdownType.DURATION, nf, new Label(" "), minutes, new Div()); //$NON-NLS-1$
		ct.addComponents(CountdownType.TARGET, dp, new Label(" "), tp); //$NON-NLS-1$
	}

	private void configureTimerDisplay() {
		Div countdown = new Div(new BreakTimerElement());
		countdown.getStyle().set("font-size", "x-large"); //$NON-NLS-1$ //$NON-NLS-2$
		countdown.getStyle().set("font-weight", "bold"); //$NON-NLS-1$ //$NON-NLS-2$
		timer = new HorizontalLayout(countdown);
		timer.setWidth("100%"); //$NON-NLS-1$
		timer.setJustifyContentMode(JustifyContentMode.CENTER);
		timer.getStyle().set("margin-top", "0px"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Object getOrigin() {
		return origin;
	}

	private long setBreakTimeRemaining(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
		LocalDateTime target;
		if (cType == CountdownType.DURATION) {
			Double value = nf.getValue();
			target = LocalDateTime.now().plusMinutes(value != null ? value.intValue() : 0);
		} else {
			LocalDate date = dp.getValue();
			LocalTime time = tp.getValue();
			target = LocalDateTime.of(date, time);
		}
		long timeRemaining = LocalDateTime.now().until(target, ChronoUnit.MILLIS);
		OwlcmsSession.withFop(fop -> fop.getBreakTimer().setTimeRemaining((int) timeRemaining));
		return timeRemaining;
	}

	private void setOrigin(Object origin) {
		this.origin = origin;
	}

	private void setValues(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
		LocalDateTime now = LocalDateTime.now();
		if (cType == CountdownType.DURATION) {
			Double value = nf.getValue();
			LocalDateTime target = now.plusMinutes(value != null ? value.intValue() : 0);
			dp.setValue(target.toLocalDate());
			tp.setValue(target.toLocalTime());
		} else {
			computeRoundedTargetValues(tp, dp);
		}
	}

	private void switchToDuration(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
		nf.setEnabled(true);
		minutes.setEnabled(true);
		dp.setEnabled(false);
		tp.setEnabled(false);
		nf.focus();
		nf.setAutoselect(true);
	}

	private void switchToTarget(CountdownType cType, NumberField nf, TimePicker tp, DatePicker dp) {
		nf.setEnabled(false);
		minutes.setEnabled(false);
		dp.setEnabled(true);
		tp.setEnabled(true);
		tp.focus();
	}

	private void syncWithFop() {
		OwlcmsSession.withFop(fop -> {
			switch (fop.getState()) {
			case BREAK:
				bt.setValue(BreakType.FIRST_CJ);
				break;
			case INACTIVE:
				bt.setValue(BreakType.INTRODUCTION);
				break;
			default:
				bt.setValue(BreakType.FIRST_SNATCH);
				break;
			}
		});
	}

}
