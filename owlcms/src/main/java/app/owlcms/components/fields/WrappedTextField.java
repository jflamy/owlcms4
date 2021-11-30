/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.components.fields;

import java.util.Locale;

import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.value.ValueChangeMode;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

/**
 * Common base for creating fields that parse text to obtain a value.
 *
 * Subclasses define a converter and a renderer. This parent class uses a TextField to display and read the
 * corresponding text. Typical use of a subclass of this class is as follows
 *
 * <pre>
 * LocalDateField f = ((LocalDateField) field);
 * Validator<LocalDate> fv = f.formatValidation(locale);
 * binder.forField(f).withValidator(fv).bind(property);
 * </pre>
 *
 * In the example, validator fv is the most basic validation -- the content of the field was syntactically legal and was
 * converted successfully into a value. Additional semantic validations can of course be chained after fv.
 *
 * @author Jean-François Lamy
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class WrappedTextField<T> extends AbstractCompositeField<ValidationTextField, WrappedTextField<T>, T>
        implements HasValidation {

    private Logger logger;

    protected boolean validFormat;

    // TextField wrappedTextField;

    public WrappedTextField() {
        this(null, null);
    }

    public WrappedTextField(String label, T defaultValue) {
        super(defaultValue);
        initLoggers();
        setLabel(label);
        getWrappedTextField().addValueChangeListener(event -> {
            String presentationValue = event.getValue();
            doConvertToModel(presentationValue, true);
        });
        getWrappedTextField().setValueChangeMode(ValueChangeMode.EAGER);
    }

    public WrappedTextField(T defaultValue) {
        super(defaultValue);
    }

    public void focus() {
        getWrappedTextField().focus();
    }

    public Validator<T> formatValidation(Locale locale) {
        return Validator.from(value -> {
            if (!this.isRequired() && value == null) {
                return true;
            }
            getLogger().debug("format validation {} {}", value, this.validFormat);
            return this.validFormat;
        }, this.invalidFormatErrorMessage(locale));
    }

    abstract public Converter<String, T> getConverter();

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.HasValidation#getErrorMessage()
     */
    @Override
    public String getErrorMessage() {
        return getWrappedTextField().getErrorMessage();
    }

    /**
     * @return the wrapped text field.
     */
    public TextField getWrappedTextField() {
        return getContent();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.HasValidation#isInvalid()
     */
    @Override
    public boolean isInvalid() {
        return getWrappedTextField().isInvalid();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.HasValueAndElement#isRequiredIndicatorVisible()
     */
    public boolean isRequired() {
        return getWrappedTextField().isRequired();
    }

    public void setAutoselect(boolean autoselect) {
        getWrappedTextField().setAutoselect(autoselect);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.HasValidation#setErrorMessage(java.lang.String)
     */
    @Override
    public void setErrorMessage(String e) {
        getWrappedTextField().setErrorMessage(e);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.HasValidation#setInvalid(boolean)
     */
    @Override
    public void setInvalid(boolean invalid) {
        getWrappedTextField().setInvalid(invalid);
    }

    /**
     * This method is expected by the CrudUI framework (found by introspection)
     *
     * @param label
     */
    public void setLabel(String label) {
        getWrappedTextField().setLabel(label);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.HasValueAndElement#setRequiredIndicatorVisible( boolean)
     */
    public void setRequired(boolean required) {
        getWrappedTextField().setRequired(required);
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

    protected Logger getLogger() {
        return logger;
    }

    protected abstract void initLoggers();

    protected abstract String invalidFormatErrorMessage(Locale locale);

    protected void logConversionError(String e) {
        getLogger().debug(e);
    }

    /**
     * Keep parsing result for use during validation.
     *
     * Binder validates converted values, it does not convert again from the field content. So if an ill-formed syntax
     * is used, the field value is not updated, and the original valid value is still present. So no error is shown to
     * the user. In order to show the error, we memorize the last parsing status and we systematically use the
     * {@link #formatValidation(Locale)} validator when binding a field.
     *
     * @param valid
     * @param locale
     */
    protected void setFormatValidationStatus(boolean valid, Locale locale) {
        getLogger().trace("format valid = {} ", valid);
        this.validFormat = valid;
        this.setInvalid(!valid);
        if (!valid) {
            this.setErrorMessage(invalidFormatErrorMessage(locale));
        }
    }

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected void setPresentationValue(T value) {
        // there is only one TextField inside us, so we can manipulate directly
        if (value == null) {
            getWrappedTextField().clear();
        } else {
            getWrappedTextField()
                    .setValue(getConverter().convertToPresentation(value, new ValueContext(OwlcmsSession.getLocale())));
        }
    }

}