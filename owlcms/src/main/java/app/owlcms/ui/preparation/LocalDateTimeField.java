package app.owlcms.ui.preparation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import app.owlcms.utils.WrappedTextField;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Conversion and validation methods for LocalDateTime
 * 
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("serial")
public class LocalDateTimeField extends WrappedTextField<LocalDateTime> implements HasValidation {
	
	private Logger logger = (Logger)LoggerFactory.getLogger(LocalDateTimeField.class);
	@Override
	protected void initLoggers() {
		logger.setLevel(Level.DEBUG);
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	@Override
	public Converter<String, LocalDateTime> getConverter() {	
		return new Converter<String,LocalDateTime>() {
			
			@Override
			public String convertToPresentation(LocalDateTime value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				if (value == null) return "";
				return FORMATTER.withLocale(locale).format(value);
			}
			
			@Override
			public Result<LocalDateTime> convertToModel(String value, ValueContext context) {
				return parser1(value, context, FORMATTER);
			}
	
			private Result<LocalDateTime> parser1(String value, ValueContext context, DateTimeFormatter formatter) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				LocalDateTime parse;
				try {
					parse = LocalDateTime.parse(value, FORMATTER);
					return Result.ok(parse);
				} catch (DateTimeParseException e) {
					String errorMessage = this.getErrorMessage(locale);
					return Result.error(errorMessage);
				}
			}
			
			private String getErrorMessage(Locale locale) {
				return "Time must be in international format YYYY-MM-DDThh:dd:ss,mmm  (2000-12-31 for "
						+ DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
							.format(LocalDate.of(2000, 12, 31))
						+ " )";
			}
		};
	}

	public Validator<LocalDateTime> getNotPastValidator() {
		return Validator.from(ld -> (ld.compareTo(LocalDateTime.now()) >= 0), "cannot be in the past");
	}

	@Override
	protected void logConversionError(String e) {
		logger.error(e);
	}
	
}

