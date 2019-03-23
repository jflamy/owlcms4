/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package app.owlcms.utils;

import java.util.Locale;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import ch.qos.logback.classic.Logger;

/**
 * Common base for creating fields that parse text to obtain a value.
 * 
 * Subclasses define a converter and a renderer. This parent class uses a TextField to display and
 * read the corresponding text. Typical use of a subclass of this class is as follows
 * 
 * <pre>
 * LocalDateField f = ((LocalDateField) field);
 * Validator<LocalDate> fv = f.formatValidation(locale);
 * binder.forField(f).withValidator(fv).bind(property);
 * </pre>
 * 
 * In the example, validator fv is the most basic validation -- the content of the field was syntactically
 * legal and was converted successfully into a value.  Additional semantic validations can of course be
 * chained after fv.
 * 
 * @author Jean-François Lamy
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class WrappedTextField<T> extends AbstractCompositeField<TextField, WrappedTextField<T>, T>
		implements HasValidation {

	protected Logger logger;

	protected boolean validFormat;

	public WrappedTextField() {
		this(null, null);
	}

	public WrappedTextField(String label, T defaultValue) {
		super(defaultValue);
		initLoggers();
		setLabel(label);
		getWrappedTextField().addValueChangeListener(
			event -> {
				String presentationValue = event.getValue();
				doConvertToModel(presentationValue, true);
			});
	}

	public WrappedTextField(T defaultValue) {
		super(defaultValue);
	}
	
	/**
	 * @return the wrapped text field.
	 */
	public TextField getWrappedTextField() {
		return getContent();
	}
	
	/**
	 * This method is expected by the CrudUI framework (found by introspection)
	 * 
	 * @param label
	 */
	public void setLabel(String label) {
		getWrappedTextField().setLabel(label);
	}

	abstract public Converter<String, T> getConverter();

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.HasValidation#getErrorMessage()
	 */
	@Override
	public String getErrorMessage() {
		return getWrappedTextField().getErrorMessage();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.HasValidation#isInvalid()
	 */
	@Override
	public boolean isInvalid() {
		return getWrappedTextField().isInvalid();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.HasValidation#setErrorMessage(java.lang.String)
	 */
	@Override
	public void setErrorMessage(String e) {
		getWrappedTextField().setErrorMessage(e);
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.HasValidation#setInvalid(boolean)
	 */
	@Override
	public void setInvalid(boolean invalid) {
		getWrappedTextField().setInvalid(invalid);
	}

	protected void doConvertToModel(String presentationValue, boolean fromClient) {
		Result<T> modelValue = getConverter().convertToModel(presentationValue, new ValueContext(this.getLocale()));
		modelValue.ifOk(v -> {
			super.setModelValue(v, fromClient);
		});
		modelValue.ifError(e -> {
			setInvalid(true);
			setErrorMessage(e);
			logConversionError(e);
		});
	}

	protected abstract void initLoggers();

	protected void logConversionError(String e) {
		logger.error(e);
	}

	/**
	 * Keep parsing result for use during validation.
	 * 
	 * Binder validates converted values, it does not convert again from the field content. So if an
	 * ill-formed syntax is used, the field value is not updated, and the original valid value is still
	 * present. So no error is shown to the user. In order to show the error, we memorize the last
	 * parsing status and we systematically use the {@link #formatValidation(Locale)} validator when
	 * binding a field.
	 * 
	 * @param valid
	 * @param locale
	 */
	protected void setFormatValidationStatus(boolean valid, Locale locale) {
		logger.warn("format valid = {} ",valid);
		this.validFormat = valid;
		this.setInvalid(!valid);
		if (!valid) {
			this.setErrorMessage(invalidFormatErrorMessage(locale));
		}
	}

	protected abstract String invalidFormatErrorMessage(Locale locale);

	@Override
	protected void setPresentationValue(T value) {
		// there is only one TextField inside us, so we can manipulate directly
		if (value == null)
			getWrappedTextField().clear();
		else
			getWrappedTextField()
				.setValue(getConverter().convertToPresentation(value, new ValueContext(UI.getCurrent().getLocale())));
	}

	public Validator<T> formatValidation(Locale locale) {
		return Validator.from(ld -> (this.validFormat), this.invalidFormatErrorMessage(locale));
	}

}