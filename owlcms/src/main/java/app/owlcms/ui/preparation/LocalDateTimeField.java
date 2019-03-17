package app.owlcms.ui.preparation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import app.owlcms.ui.crudui.Bindable;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

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
public class LocalDateTimeField extends AbstractCompositeField<TextField, LocalDateTimeField, LocalDateTime> implements HasValidation {
	// implements Bindable<LocalDateTime>
	
	Logger logger = (Logger)LoggerFactory.getLogger(LocalDateTimeField.class);
	public void initLoggers() {
		logger.setLevel(Level.DEBUG);
	}

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public LocalDateTimeField() {
		this(null, null);
	}

	public LocalDateTimeField(String label, LocalDateTime defaultValue) {
		super(defaultValue);
		initLoggers();
		setLabel(label);
		getWrappedTextField().addValueChangeListener(
			event -> {
				String presentationValue = event.getValue();
				doConvertToModel(presentationValue, true);
			});
	}

	public Converter<String, LocalDateTime> getConverter() {	
		return new Converter<String,LocalDateTime>() {
			
			@Override
			public Result<LocalDateTime> convertToModel(String value, ValueContext context) {
				return parser1(value, context, FORMATTER);
			}

			@Override
			public String convertToPresentation(LocalDateTime value, ValueContext context) {
				if (value == null) return "";			
				return FORMATTER.format(value);
			}

			private String getErrorMessage(ValueContext context) {
				return "Date must be in international format YYYY-MM-DD  (2000-12-31 for "
						+ DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(context.getLocale().orElse(Locale.ENGLISH))
							.format(LocalDate.of(2000, 12, 31))
						+ " )";
			}
			
			private Result<LocalDateTime> parser1(String value, ValueContext context, DateTimeFormatter formatter) {
				LocalDateTime parse;
				try {
					parse = LocalDateTime.parse(value, FORMATTER);
					return Result.ok(parse);
				} catch (DateTimeParseException e) {
					String errorMessage = this.getErrorMessage(context);
					logger.error(errorMessage);
					return Result.error(errorMessage);
				}
			}
		};
	}
	
	public Validator<LocalDateTime> getValidator() {
		return Validator.from(ld -> (ld.compareTo(LocalDateTime.now()) >= 0), "cannot be in the past");
	}

	@Override
	public void setErrorMessage(String e) {
		getWrappedTextField().setErrorMessage(e);
	}
	
	@Override
	public String getErrorMessage() {
		return getWrappedTextField().getErrorMessage();
	}

	@Override
	public void setInvalid(boolean invalid) {
		getWrappedTextField().setInvalid(invalid);
	}

	@Override
	public boolean isInvalid() {
		return getWrappedTextField().isInvalid();
	}

	public void setLabel(String label) {
		getWrappedTextField().setLabel(label);
	}

	protected void doConvertToModel(String presentationValue, boolean fromClient) {
		Result<LocalDateTime> modelValue = getConverter().convertToModel(presentationValue, new ValueContext(this.getLocale()));
		modelValue.ifOk(v -> super.setModelValue(v, fromClient));
		modelValue.ifError(e -> {
			setErrorMessage(e);
			logger.error(e);
		});
	}

	protected TextField getWrappedTextField() {
		return getContent();
	}

	@Override
	protected void setPresentationValue(LocalDateTime value) {
		if (value == null) value = LocalDateTime.now(); // for testing -- this way we can't make mistakes in format
		
		// there is only one TextField inside us, so we can manipulate directly
        if (value == null)
            getWrappedTextField().clear();
        else
            getWrappedTextField().setValue(FORMATTER.format(value));
	}
	
}

