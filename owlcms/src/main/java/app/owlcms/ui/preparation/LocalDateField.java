/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.preparation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;

import app.owlcms.utils.WrappedTextField;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Implement a LocalDate field with converter and validators.
 * 
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("serial")
public class LocalDateField extends WrappedTextField<LocalDate> {
	
	private final Logger logger = (Logger)LoggerFactory.getLogger(LocalDateField.class);
	@Override
	protected void initLoggers() {
		logger.setLevel(Level.DEBUG);
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
	
	public LocalDateField() {
		this(null, null);
	}
	
	public LocalDateField(String label, LocalDate defaultValue) {
		super(defaultValue);
		initLoggers();
		setLabel(label);
		getWrappedTextField().addValueChangeListener(
			event -> {
				String presentationValue = event.getValue();
				doConvertToModel(presentationValue, true);
			});
	}

	@Override
	public Converter<String, LocalDate> getConverter() {
		
		return new Converter<String,LocalDate>() {
			
			@Override
			public String convertToPresentation(LocalDate value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				if (value == null) value = LocalDate.now();
				return FORMATTER.withLocale(locale).format(value);
			}

			@Override
			public Result<LocalDate> convertToModel(String value, ValueContext context) {
				return parser1(value, context, FORMATTER);
			}

			protected Result<LocalDate> parser1(String value, ValueContext context, DateTimeFormatter formatter) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				LocalDate parse;
				try {
					parse = LocalDate.parse(value, FORMATTER);
					return Result.ok(parse);
				} catch (DateTimeParseException e) {
					return Result.error(this.getErrorMessage(locale));
				}
			}

			private String getErrorMessage(Locale locale) {
				LocalDate sampleDate = LocalDate.of(2000, 12, 31);
				return "Date must be in international format YYYY-MM-DD "
						+ "("
						+ FORMATTER.format(sampleDate)
						+ " for "
						+ DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
							.format(sampleDate)
						+ " )";
			}
		};
	}
	
	public Validator<LocalDate> getNotFutureValidator() {
		return Validator.from(ld -> (ld.compareTo(LocalDate.now()) < 0), "cannot be in the future");
	}

	@Override
	protected void logConversionError(String e) {
		logger.error(e);
	}
	
	
	@Override
	public String toString() {
		return FORMATTER.format(getValue());
	}
	
	public static Renderer<LocalDate> getRenderer(ValueProvider<LocalDate, LocalDate> v, Locale locale) {
		return new LocalDateRenderer<LocalDate>(v, FORMATTER);
	}

}

