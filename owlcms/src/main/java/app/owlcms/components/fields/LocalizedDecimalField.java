/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

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
public class LocalizedDecimalField extends WrappedTextField<Double> {

	public static <SOURCE> Renderer<SOURCE> getRenderer(ValueProvider<SOURCE, Number> v, Locale locale) {
		return new NumberRenderer<>(v, getFormatter(locale));
	}

	private static NumberFormat getFormatter(Locale locale) {
		NumberFormat formatter = new DecimalFormat("0.00");
		formatter.setMaximumFractionDigits(2);
		formatter.setMinimumFractionDigits(2);
		formatter.setGroupingUsed(false);
		return formatter;
	}

	@SuppressWarnings("unused")
	private Logger logger = (Logger) LoggerFactory.getLogger(LocalizedDecimalField.class);

	@Override
	public void focus() {
		getWrappedTextField().focus();
		getWrappedTextField().setAutoselect(true);
	}

	@Override
	public Converter<String, Double> getConverter() {
		return new Converter<>() {
			@Override
			public Result<Double> convertToModel(String value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				return doParse(value, locale, getFormatter(locale));
			}

			@Override
			public String convertToPresentation(Double value, ValueContext context) {
				Locale locale = context.getLocale().orElse(Locale.ENGLISH);
				return (value != null ? getFormatter(locale).format(value) : "");
			}
		};
	}

	@Override
	public String toString() {
		return this.getValue().toString();
	}

	private Result<Double> doParse(String content, Locale locale, NumberFormat formatter) {
		if ((content == null || content.trim().isEmpty()) && !this.isRequired()) {
			// field is not required, accept empty content
			setFormatValidationStatus(true, locale);
			return Result.ok(null);
		}
		// we ignore the provided formatter, and we try both "," and "." as decimal
		// separator, as per ISO 30-1
		DecimalFormatSymbols dc = new DecimalFormatSymbols(locale);
		char decimalSeparator = dc.getDecimalSeparator();
		char alternateSeparator = (decimalSeparator == '.' ? ',' : '.');

		// first try with locale decimal separator
		Result<Double> r = parseWithSeparator(content, locale, decimalSeparator);
		if (!r.isError()) {
			return r;
		}
		// then try with alternate
		r = parseWithSeparator(content, locale, alternateSeparator);
		if (!r.isError()) {
			return r;
		}
		setFormatValidationStatus(false, locale);
		return r;
	}

	private Result<Double> parseWithSeparator(String content, Locale locale, char separator) {
		DecimalFormat formatter2 = new DecimalFormat("0.00");
		DecimalFormatSymbols symbols = formatter2.getDecimalFormatSymbols();
		symbols.setDecimalSeparator(separator);
		formatter2.setDecimalFormatSymbols(symbols);

		ParsePosition pp2 = new ParsePosition(0);
		Number parse2 = formatter2.parse(content, pp2);
		Result<Double> r2;
		int index = pp2.getIndex();
		if (index < content.length() - 1) {
			String m = invalidFormatErrorMessage(locale);
			r2 = Result.error(m);
		} else {
			setFormatValidationStatus(true, locale);
			r2 = Result.ok(parse2.doubleValue());
		}
		return r2;
	}

	@Override
	protected void initLoggers() {
		setLogger((Logger) LoggerFactory.getLogger(LocalizedDecimalField.class));
		getLogger().setLevel(Level.INFO);
	}

	@Override
	protected String invalidFormatErrorMessage(Locale locale) {
		return "Please enter a valid number (for example " + getFormatter(locale).format(64.56) + ")";
	}

}
