package app.owlcms.utils;


import com.vaadin.flow.component.AbstractCompositeField;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

@SuppressWarnings("serial")
public abstract class WrappedTextField<T> extends AbstractCompositeField<TextField, WrappedTextField<T>, T> implements HasValidation {
	
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
	
	public TextField getWrappedTextField() {
		return getContent();
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

	abstract public Converter<String, T> getConverter();
	
	protected void doConvertToModel(String presentationValue, boolean fromClient) {
		Result<T> modelValue = getConverter().convertToModel(presentationValue, new ValueContext(this.getLocale()));
		modelValue.ifOk(v -> super.setModelValue(v, fromClient));
		modelValue.ifError(e -> {
			setInvalid(true);
			setErrorMessage(e);
			logConversionError(e);
		});
	}
	
	@Override
	protected void setPresentationValue(T value) {
		// there is only one TextField inside us, so we can manipulate directly
        if (value == null)
            getWrappedTextField().clear();
        else
            getWrappedTextField().setValue(getConverter().convertToPresentation(value, new ValueContext(UI.getCurrent().getLocale())));
	}

	protected abstract void logConversionError(String e);

	protected abstract void initLoggers();

}