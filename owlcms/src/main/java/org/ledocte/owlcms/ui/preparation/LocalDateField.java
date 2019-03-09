package org.ledocte.owlcms.ui.preparation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.ledocte.owlcms.crudui.Bindable;
import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

/**
 * Conversion and validation methods for Body Weight
 * 
 * {@link OwlcmsCrudFormFactory} binds all instances of {@link Bindable} in this sequence
 *
 * <code><pre>
 * binder.forField(bwField);  // bwField is a subclass of TextField
 * binder.withConverter(bwField.getConverter());
 * binder.withValidator(bwField.getValidator());
 * binder.bind(property);  // property is of type T
 * </pre></code>
 * 
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("serial")
public class LocalDateField extends TextField implements Bindable<LocalDate> {

	private static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	@Override
	public Converter<String, LocalDate> getConverter() {
		Locale locale = UI.getCurrent().getLocale();
		
		return new Converter<String,LocalDate>() {
			
			@Override
			public String convertToPresentation(LocalDate value, ValueContext context) {
				if (value == null) value = LocalDate.now();			
				return ISO_LOCAL_DATE.format(value);
			}

			/**
			 * The full value must be parsed -- by default parsing stops at first error.
			 * 
			 * @see com.vaadin.flow.data.converter.StringToDoubleConverter#convertToModel(java.lang.String, com.vaadin.flow.data.binder.ValueContext)
			 */
			@Override
			public Result<LocalDate> convertToModel(String value, ValueContext context) {
				return tryFormatter(value, context, ISO_LOCAL_DATE);
			}

			protected Result<LocalDate> tryFormatter(String value, ValueContext context, DateTimeFormatter formatter) {
				LocalDate parse;
				try {
					parse = LocalDate.parse(value, ISO_LOCAL_DATE);
					return Result.ok(parse);
				} catch (DateTimeParseException e) {
					return Result.error(this.getErrorMessage(context));
				}
			}

			private String getErrorMessage(ValueContext context) {
				return "Date must be in international format YYYY-MM-DD  (2000-12-31 for "
						+ DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
							.format(LocalDate.of(2000, 12, 31))
						+ " )";
			}
		};
	}

	@Override
	public Validator<LocalDate> getValidator() {
		return Validator.from(ld -> (ld.compareTo(LocalDate.now()) < 0), "cannot be in the future");
	}
}

