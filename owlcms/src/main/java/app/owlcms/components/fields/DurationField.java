/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components.fields;

import java.time.Duration;
import java.time.LocalDateTime;
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

    @Override
    protected void initLoggers() {
        logger = (Logger) LoggerFactory.getLogger(DurationField.class);
        logger.setLevel(Level.INFO);
    }

    private static final String HHMMSS_FORMAT = "HH:mm:ss";
    private final static DateTimeFormatter HHMMSS_FORMATTER = DateTimeFormatter.ofPattern(HHMMSS_FORMAT);

    private static final String MMSS_FORMAT = "mm:ss";
    private final static DateTimeFormatter MMSS_FORMATTER = DateTimeFormatter.ofPattern(MMSS_FORMAT);

    private static final String MM_FORMAT = "mm";
    private final static DateTimeFormatter MM_FORMATTER  = DateTimeFormatter.ofPattern(MM_FORMAT);


    @Override
    public Converter<String, Duration> getConverter() {
        return new Converter<String, Duration>() {

            @Override
            public String convertToPresentation(Duration value, ValueContext context) {
                Locale locale = context.getLocale().orElse(Locale.ENGLISH);
                if (!value.minusHours(1L).isNegative()) {
                    // over 1h
                    return (value != null ? HHMMSS_FORMATTER.withLocale(locale).format(getValueAsLocalTime()) : "");
                } else {
                    // always show seconds for readability
                    return (value != null ? MMSS_FORMATTER.withLocale(locale).format(getValueAsLocalTime()) : "");
                }

            }

            @Override
            public Result<Duration> convertToModel(String string, ValueContext context) {
                Locale locale = context.getLocale().orElse(Locale.ENGLISH);
                if (string.length() <= 2) {
                    // assume minutes
                    return doParse(string, locale, MM_FORMATTER);
                } else if (string.length() <= 5) {
                    // assume minutes seconds
                    return doParse(string, locale, MMSS_FORMATTER);
                } else {
                    // assume hours
                    return doParse(string, locale, HHMMSS_FORMATTER);
                }

            }
        };
    }

    protected LocalTime getValueAsLocalTime() {
        return LocalTime.MIN.plus(getValue());
    }

    protected void setValueFromLocalDateTime(LocalDateTime hhmmss) {
        LocalDateTime min = LocalDateTime.MIN;
        setValue(Duration.between(min, hhmmss));
    }

    @Override
    public String toString() {
        return getConverter().convertToPresentation(getValue(), new ValueContext(getLocale()));
    }

    public <S> Renderer<S> getRenderer(ValueProvider<String, S> v, Locale locale) {
        return new TextRenderer<S>(
                d -> getConverter().convertToPresentation((Duration) d, new ValueContext(getLocale())));
    }

    private Result<Duration> doParse(String string, Locale locale, DateTimeFormatter formatter) {
        LocalTime parsedTime;
        try {
            if ((string == null || string.trim().isEmpty()) && !this.isRequired()) {
                // field is not required, accept empty content
                setFormatValidationStatus(true, locale);
                return Result.ok(null);
            }
            parsedTime = LocalTime.parse(string,formatter.withLocale(locale));
            setFormatValidationStatus(true, locale);
            Duration between = Duration.between(LocalTime.MIN, parsedTime);
            logger.warn("duration = {}", between);
            return Result.ok(between);
        } catch (DateTimeParseException e) {
            logger.error(e.getLocalizedMessage() + " *** " + e.getParsedString());
            setFormatValidationStatus(false, locale);
            return Result.error(invalidFormatErrorMessage(locale));
        }
    }

    @Override
    protected String invalidFormatErrorMessage(Locale locale) {
        int minute = 10;
        int second = 20;
        LocalDateTime date = LocalDateTime.of(2000, 12, 30, 22, minute, second);
        return Translator.translate("InvalidDurationFormat", MMSS_FORMAT, MMSS_FORMATTER.format(date), minute, second);
    }

}
