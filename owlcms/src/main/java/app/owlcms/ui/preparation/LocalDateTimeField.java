package app.owlcms.ui.preparation;

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
import com.vaadin.flow.function.ValueProvider;

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
public class LocalDateTimeField<SOURCE> extends WrappedTextField<LocalDateTime> implements HasValidation {
	

	private Logger logger;
	@Override
	protected void initLoggers() {
		logger = (Logger)LoggerFactory.getLogger(LocalDateTimeField.class);
		logger.setLevel(Level.DEBUG);
	}

	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";
	private final static DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder().parseLenient().appendPattern(DATE_FORMAT).toFormatter();

	@Override
	public Converter<String, LocalDateTime> getConverter() {	
		return new Converter<String,LocalDateTime>() {
			
			@Override
			public String convertToPresentation(LocalDateTime value, ValueContext context) {
				//Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				if (value == null) return "";
				return FORMATTER.format(value);
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
					//errorMessage = "error parsing *"+e.getParsedString()+"* at position "+e.getErrorIndex();
					return Result.error(errorMessage);
				}
			}
			
			private String getErrorMessage(Locale locale) {
				LocalDateTime date = LocalDateTime.of(2000, 11, 29,13,31);
				return "Time must be in international format "
						+ DATE_FORMAT
						+ " ("
						+ FORMATTER.format(date)
						+" for "
						+ DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(locale)
							.format(date)
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
	
	@Override
	public String toString() {
		return FORMATTER.format(getValue());
	}
	
	public static <SOURCE> Renderer<SOURCE> getRenderer(ValueProvider<SOURCE, LocalDateTime> v, Locale locale) {
		return new LocalDateTimeRenderer<SOURCE>(v, FORMATTER);
	}
}

