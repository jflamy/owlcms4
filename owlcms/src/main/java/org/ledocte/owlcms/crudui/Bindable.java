package org.ledocte.owlcms.crudui;

import org.ledocte.owlcms.ui.crudui.OwlcmsCrudFormFactory;

import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.converter.Converter;

/**
 * Defines an extension to TextField that can be bound to CrudUI form creation
 * 
 * Instances of Bindable fields will be automatically converted to type T using the converter
 * and the result of the conversion will be validated with the validator.
 * The converter is also used to format the content of the Bindable field.
 * 
 * {@link OwlcmsCrudFormFactory} processes all instances of {@link Bindable} fields in this sequence
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
 * @param <T>
 */
public interface Bindable<T> {

	public Converter<String, T> getConverter();
	
	public Validator<T> getValidator();

}
