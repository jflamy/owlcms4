/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Duration field with conversion, validation and rendering.
 *
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("serial")
public class DurationField extends WrappedTextField<Duration> implements HasValidation {

	private static DurationField helper = new DurationField();
	private static final String HHMMSS_FORMAT = "HH:mm:ss";

	private static final DateTimeFormatter HHMMSS_FORMATTER = DateTimeFormatter.ofPattern(HHMMSS_FORMAT);

	private static final String MMSS_FORMAT = "mm:ss";
	private static final DateTimeFormatter MMSS_FORMATTER = DateTimeFormatter.ofPattern(MMSS_FORMAT);

	public static <S> Renderer<S> getRenderer(ValueProvider<String, S> v, Locale locale) {
		return new TextRenderer<>(
		        d -> helper.getConverter().convertToPresentation((Duration) d, new ValueContext(locale)));
	}

	Logger overrideLogger = (Logger) LoggerFactory.getLogger(DurationField.class);

	Level overrideLoggerLevel = Level.INFO;

	public DurationField() {
		getWrappedTextField().setValueChangeMode(ValueChangeMode.ON_BLUR);
		getWrappedTextField().setWidth("9ch");
	}

	@Override
	public Converter<String, Duration> getConverter() {
		return new Converter<>() {

			@Override
			public Result<Duration> convertToModel(String string, ValueContext context) {
				// There is no "human-readable" format for inputting minutes:seconds durations
				// Duration parsing requires special ISO syntax, and LocalTime parsing requires
				// hours
				// This routine forces a legal LocalTime out of minutes or minutes+seconds and
				// converts to a duration.
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				if (string.length() <= 2) {
					// last two digits, need zeros at front
					String string2 = "00" + string;
					return doParse("00:" + string2.substring(string2.length() - 2) + ":00", locale, HHMMSS_FORMATTER);
				} else if (string.length() <= 5) {
					// assume minutes seconds
					// last 5 characters, need zeros at front
					String string2 = "00" + string;
					return doParse("00:" + string2.substring(string2.length() - 5), locale, HHMMSS_FORMATTER);
				} else {
					// assume hours
					return doParse(string, locale, HHMMSS_FORMATTER);
				}
			}

			@Override
			public String convertToPresentation(Duration value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				if (!value.minusHours(1L).isNegative()) {
					// over 1h
					return (value != null ? HHMMSS_FORMATTER.withLocale(locale).format(getValueAsLocalTime()) : "");
				} else {
					// show seconds for readability
					return (value != null ? MMSS_FORMATTER.withLocale(locale).format(getValueAsLocalTime()) : "");
				}
			}
		};
	}

	@Override
	public String toString() {
		return getConverter().convertToPresentation(getValue(), new ValueContext(getLocale()));
	}

	private Result<Duration> doParse(String string, Locale locale, DateTimeFormatter formatter) {
		LocalTime parsedTime;
		try {
			if ((string == null || string.trim().isEmpty()) && !this.isRequired()) {
				// field is not required, accept empty content
				setFormatValidationStatus(true, locale);
				return Result.ok(null);
			}
			parsedTime = LocalTime.parse(string, formatter);
			setFormatValidationStatus(true, locale);
			Duration between = Duration.between(LocalTime.MIN, parsedTime);
			getLogger().debug("parsed duration = {}", between);
			return Result.ok(between);
		} catch (DateTimeParseException e) {
//            getLogger().error(e.getLocalizedMessage());
			setFormatValidationStatus(false, locale);
			return Result.error(invalidFormatErrorMessage(locale));
		}
	}

	protected LocalTime getValueAsLocalTime() {
		return LocalTime.MIN.plus(getValue());
	}

	@Override
	protected void initLoggers() {
		overrideLogger = (Logger) LoggerFactory.getLogger(DurationField.class);
		overrideLogger.setLevel(overrideLoggerLevel);
		setLogger(overrideLogger);
	}

	@Override
	protected String invalidFormatErrorMessage(Locale locale) {
		int minute = 10;
		int second = 20;
		LocalTime date = LocalTime.of(0, minute, second);
		return Translator.translate("InvalidDurationFormat", MMSS_FORMAT, MMSS_FORMATTER.format(date), minute, second);
	}

	protected void setValueFromLocalTime(LocalTime hhmmss) {
		LocalTime min = LocalTime.MIN;
		setValue(Duration.between(min, hhmmss));
	}

}
