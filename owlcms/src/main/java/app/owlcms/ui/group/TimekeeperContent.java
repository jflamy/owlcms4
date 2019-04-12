/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package app.owlcms.ui.group;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FOPEvent;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "group/timekeeper", layout = AthleteGridLayout.class)
public class TimekeeperContent extends AthleteGridContent {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TimekeeperContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	public TimekeeperContent() {
		super();
		setTopBarTitle("Timekeeper");	
	}
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar()
	 */
	@Override
	protected void createTopBar() {
		super.createTopBar();
		// this hides the back arrow
		getAppLayout().setMenuVisible(false);
	}
	
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {

		Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.TimeStartedManually(this.getOrigin())));
		});
		start.getElement().setAttribute("theme", "primary");
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.TimeStoppedManually(this.getOrigin())));
		});
		stop.getElement().setAttribute("theme", "primary");
		Button _1min = new Button("1:00", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.ForceTime(60000,this.getOrigin())));
		});
		_1min.getElement().setAttribute("theme", "icon");
		_1min.getElement().setAttribute("title", "Reset to 1 min");
		Button _2min = new Button("2:00", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.ForceTime(120000,this.getOrigin())));
		});
		_2min.getElement().setAttribute("theme", "icon");
		_2min.getElement().setAttribute("title", "Reset to 2 min");
		Button breakButton = new Button(IronIcons.ALARM.create(), (e) -> {
			showCountdownDialog();
		});
		breakButton.getElement().setAttribute("theme", "icon");
		breakButton.getElement().setAttribute("title", "Break Timer");
		HorizontalLayout buttons = new HorizontalLayout(
				start,
				stop,
				_1min,
				_2min,
				breakButton);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	enum BreakType {PRESENTATION, FIRST_SNATCH, FIRST_CJ, TECHNICAL};
	enum CountdownType {DURATION, TARGET};
	
	Button start = null;
	Button pause = null;
	Button stop = null;
	
	private void showCountdownDialog() {
		Dialog dialog = new Dialog();
		
		RadioButtonGroup<BreakType> bt = new RadioButtonGroup<TimekeeperContent.BreakType>();
		bt.setItems(BreakType.values());
		bt.setLabel("Break Type");
		
		RadioButtonGroup<CountdownType> ct = new RadioButtonGroup<TimekeeperContent.CountdownType>();
		ct.setItems(CountdownType.values());
		ct.setLabel("Countdown Type");
		NumberField nf = new NumberField();
		TimePicker tp = new TimePicker();
		DatePicker dp = new DatePicker();
		// ISO 8601 style dates and time
		Locale locale = new Locale("en", "SE");
		tp.setLocale(locale);
		dp.setLocale(locale);
		setValues(tp, dp);
		Label minutes = new Label("minutes");
		ct.addComponents(CountdownType.DURATION, nf, new Label(" "), minutes, new Div());
		ct.addComponents(CountdownType.TARGET, dp, new Label(" "), tp);
		ct.addValueChangeListener(e -> {
			if (e.getValue() != CountdownType.TARGET) {
				switchToDuration(nf, tp, dp, minutes);
			} else {
				switchToTarget(nf, tp, dp, minutes);
			}
		});
		
		start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			LocalDateTime target;
			LocalDateTime now = LocalDateTime.now();
			if (ct.getValue() == CountdownType.DURATION) {
				Double value = nf.getValue();
				target = now.plusMinutes(value != null ? value.intValue() : 0);
				dp.setValue(target.toLocalDate());
				tp.setValue(target.toLocalTime());
				switchToTarget(nf, tp, dp, minutes);
			} else {
				LocalDate date = dp.getValue();
				LocalTime time = tp.getValue();
				target = LocalDateTime.of(date, time);
			}
			OwlcmsSession.withFop(fop -> {
				int timeRemaining;
				timeRemaining = (int) now.until(target, ChronoUnit.MILLIS);
				fop.getTimer().setTimeRemaining(timeRemaining);
				fop.getEventBus().post(new FOPEvent.BreakStarted(this.getOrigin()));
			});
			e.getSource().setEnabled(false);
			pause.setEnabled(true);
			stop.setEnabled(true);
			
		});
		start.getElement().setAttribute("theme", "primary");
		pause = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.BreakPaused(this.getOrigin())));
		});
		pause.getElement().setAttribute("theme", "primary");
		Button end = new Button(AvIcons.STOP.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getEventBus()
				.post(new FOPEvent.StartLifting(this.getOrigin())));
		});
		end.getElement().setAttribute("theme", "primary");
		
		dialog.add(bt);
		dialog.add(new Hr());
		dialog.add(ct);
		dialog.open();
		
//		OwlcmsSession.withFop(fop -> fop.getEventBus()
//			.post(new FOPEvent.BreakStarted(10*60*1000,this.getOrigin())));
	}

	public void switchToTarget(NumberField nf, TimePicker tp, DatePicker dp, Label minutes) {
		nf.setEnabled(false);
		minutes.setEnabled(false);
		dp.setEnabled(true);
		tp.setEnabled(true);
		tp.focus();
	}

	public void switchToDuration(NumberField nf, TimePicker tp, DatePicker dp, Label minutes) {
		nf.setEnabled(true);
		minutes.setEnabled(true);
		dp.setEnabled(false);
		tp.setEnabled(false);
		nf.focus();
		nf.setAutoselect(true);
	}

	private Integer computeTimeRemaining() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setValues(TimePicker tp, DatePicker dp) {
		int timeStep = 30;
		tp.setStep(Duration.ofMinutes(timeStep));
		LocalTime nowTime = LocalTime.now();
		int nowMin = nowTime.getMinute();
		int nowHr = nowTime.getHour();
		int previousStepMin = (nowMin / timeStep) * timeStep; // between 0 and 50
		int nextStepMin = (previousStepMin + timeStep) % 60;
		logger.trace("previousStepMin = {} nextStepMin = {}", previousStepMin, nextStepMin);
		int nextHr = (nextStepMin == 0 ? nowHr + 1 : nowHr);
		LocalDate nextDate = LocalDate.now();
		if (nextHr >= 24) {
			nextDate.plusDays(1);
			nextHr = nextHr % 24;
		}
		dp.setValue(nextDate);
		tp.setValue(LocalTime.of(nextHr, nextStepMin));
	}

	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#add(java.lang.Object)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#update(java.lang.Object)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/* (non-Javadoc)
	 * @see org.vaadin.crudui.crud.CrudListener#delete(java.lang.Object)
	 */
	@Override
	public void delete(Athlete Athlete) {;
		// do nothing;
	}
	
}
