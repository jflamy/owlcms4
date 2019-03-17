package app.owlcms.ui.preparation;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.Validator;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.converter.StringToDoubleConverter;
import com.vaadin.flow.data.validator.DoubleRangeValidator;

import app.owlcms.ui.crudui.Bindable;
import app.owlcms.ui.crudui.OwlcmsCrudFormFactory;

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
public class BodyWeightField extends TextField implements Bindable<Double> {

	@Override
	public Converter<String, Double> getConverter() {
		Locale locale = UI.getCurrent().getLocale();
		
		return new StringToDoubleConverter(
				"Please enter a valid number; note that the decimal separator is '" +
						new DecimalFormatSymbols(locale).getDecimalSeparator() + "'") {
			
			/**
			 * We only show two decimals.
			 * 
			 * @see com.vaadin.flow.data.converter.AbstractStringToNumberConverter#convertToPresentation(java.lang.Number, com.vaadin.flow.data.binder.ValueContext)
			 */
			@Override
			public String convertToPresentation(Double value, ValueContext context) {
				NumberFormat formatter = NumberFormat.getNumberInstance(locale);
				formatter.setMaximumFractionDigits(2);
				formatter.setMinimumFractionDigits(2);
				formatter.setGroupingUsed(false);
				if (value == null) return "0.00";
				return formatter.format(value);
			}

			/**
			 * The full value must be parsed -- by default parsing stops at first error.
			 * 
			 * @see com.vaadin.flow.data.converter.StringToDoubleConverter#convertToModel(java.lang.String, com.vaadin.flow.data.binder.ValueContext)
			 */
			@Override
			public Result<Double> convertToModel(String value, ValueContext context) {
				NumberFormat format = NumberFormat.getNumberInstance(locale);
				format.setGroupingUsed(false);
				ParsePosition pp = new ParsePosition(0);
				Number parse = format.parse(value, pp);
				if (pp.getIndex() < value.length()-1) {
					return Result.error(this.getErrorMessage(context));
				} else {
					return Result.ok(parse.doubleValue());
				}
			}
		};
	}

	@Override
	public Validator<?> getValidator() {
		return new DoubleRangeValidator("Weight should be between 0 and 350kg",0.0D,350.0D);
	}
}

