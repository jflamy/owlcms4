/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.renderer.LocalDateTimeRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * LocalDateTime field with conversion, validation and rendering.
 *
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("serial")
public class LocalDateTimeField extends WrappedTextField<LocalDateTime> implements HasValidation {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

    private final static DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder().parseLenient()
            .appendPattern(DATE_FORMAT).toFormatter();

    public static <SOURCE> Renderer<SOURCE> getRenderer(ValueProvider<SOURCE, LocalDateTime> v, Locale locale) {
        SerializableSupplier<DateTimeFormatter> ss = () -> FORMATTER.withLocale(locale);
        return new LocalDateTimeRenderer<>(v, ss);
    }

    @Override
    public Converter<String, LocalDateTime> getConverter() {
        return new Converter<>() {

            @Override
            public Result<LocalDateTime> convertToModel(String value, ValueContext context) {
                Locale locale = context.getLocale().orElse(Locale.ENGLISH);
                return doParse(value, locale, FORMATTER.withLocale(locale));
            }

            @Override
            public String convertToPresentation(LocalDateTime value, ValueContext context) {
                Locale locale = context.getLocale().orElse(Locale.ENGLISH);
                return (value != null ? FORMATTER.withLocale(locale).format(value) : "");
            }
        };
    }

    public Validator<LocalDateTime> getNotPastValidator() {
        return Validator.from(ld -> (ld.compareTo(LocalDateTime.now()) >= 0), "cannot be in the past");
    }

    @Override
    public String toString() {
        return FORMATTER.format(getValue());
    }

    @Override
    protected void initLoggers() {
        setLogger((Logger) LoggerFactory.getLogger(LocalDateTimeField.class));
        getLogger().setLevel(Level.INFO);
    }

    @Override
    protected String invalidFormatErrorMessage(Locale locale) {
        LocalDateTime date = LocalDateTime.of(2000, 11, 29, 13, 31);
        return "Time must be in international format " + DATE_FORMAT + " (" + FORMATTER.format(date) + " for "
                + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale).format(date) + " )";
    }

    private Result<LocalDateTime> doParse(String content, Locale locale, DateTimeFormatter formatter) {
        LocalDateTime parse;
        try {
            if ((content == null || content.trim().isEmpty()) && !this.isRequired()) {
                // field is not required, accept empty content
                setFormatValidationStatus(true, locale);
                return Result.ok(null);
            }
            parse = LocalDateTime.parse(content, formatter);
            setFormatValidationStatus(true, locale);
            return Result.ok(parse);
        } catch (DateTimeParseException e) {
            setFormatValidationStatus(false, locale);
            return Result.error(invalidFormatErrorMessage(locale));
        }
    }
}
