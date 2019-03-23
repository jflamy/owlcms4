/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.fields;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.data.renderer.NumberRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.ValueProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Double field with conversion, validation and rendering.
 * 
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings("serial")
public class BodyWeightField extends WrappedTextField<Double> {
	
	@Override
	protected void initLoggers() {
		logger = (Logger)LoggerFactory.getLogger(BodyWeightField.class);
		logger.setLevel(Level.DEBUG);
	}
	

	@Override
	public Converter<String, Double> getConverter() {
		return new Converter<String,Double>() {		
			@Override
			public String convertToPresentation(Double value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				return (value != null ? getFormatter(locale).format(value) : "");
			}

			@Override
			public Result<Double> convertToModel(String value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				return doParse(value, locale, getFormatter(locale));
			}
		};
	}

	public static <SOURCE> Renderer<SOURCE> getRenderer(ValueProvider<SOURCE, Number> v, Locale locale) {
		return new NumberRenderer<SOURCE>(v, getFormatter(locale));
	}
	
	@Override
	public String toString() {
		return this.getValue().toString();
	}
	
	@Override
	protected String invalidFormatErrorMessage(Locale locale) {
		return "Please enter a valid number (for example " + getFormatter(locale).format(64.56) + ")";
	}
	
	private Result<Double> doParse(String content, Locale locale, NumberFormat formatter) {
		if ((content == null || content.trim().isEmpty()) && !this.isRequired()) {
			// field is not required, accept empty content
			setFormatValidationStatus(true, locale);
			return Result.ok(null);
		}
		// we ignore the provided formatter, and we try both "," and "." as decimal separator, as per ISO 30-1
		DecimalFormatSymbols dc = new DecimalFormatSymbols(locale);
		char alternateSeparator = (dc.getDecimalSeparator() == '.' ? ',' : '.');

		// first try with locale decimal separator
		Result<Double> r = parseWithSeparator(content, locale, dc.getDecimalSeparator());
		if (!r.isError()) return r;
		// then try with alternate
		return parseWithSeparator(content, locale, alternateSeparator);
	}


	public Result<Double> parseWithSeparator(String content, Locale locale, char alternateSeparator) {
		DecimalFormat formatter2 = new DecimalFormat("0.00");
		formatter2.getDecimalFormatSymbols().setDecimalSeparator(alternateSeparator);
		ParsePosition pp2 = new ParsePosition(0);
		Number parse2 = formatter2.parse(content, pp2);
		Result<Double> r2;
		if (pp2.getIndex() < content.length()-1) {
			String m = invalidFormatErrorMessage(locale);
			setFormatValidationStatus(false, locale);
			r2 = Result.error(m);
		} else {
			setFormatValidationStatus(true, locale);
			r2 = Result.ok(parse2.doubleValue());
		}
		return r2;
	}

	
	private static NumberFormat getFormatter(Locale locale) {
		NumberFormat formatter = new DecimalFormat("0.00");
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setGroupingUsed(false);
		return formatter;
	}

}

