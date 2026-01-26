/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.fields;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.customfield.CustomField;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.timepicker.TimePicker;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class LocalDateTimePicker extends CustomField<LocalDateTime> {
	
	static Logger logger = (Logger) LoggerFactory.getLogger(LocalDateTimePicker.class);

	// override time format. Sorted list of old-style countries. Canada does both,
	// so excluded.
	public final static String[] AM_PM_COUNTRIES = { "AU", "GB", "IN", "NZ", "PH", "US", "ZA" };

	public static Locale fixAM_PM(Locale l) {
		if ((l.getLanguage() != null && l.getLanguage().contentEquals("en"))) {
			String country = l.getCountry();
			if (l != null && Arrays.binarySearch(AM_PM_COUNTRIES, country) < 0) {
				// not an AM-PM country, international format en_SE seems to work best for 24h.
				return (new Locale("en", "SE"));
			}
		} else {
			return (new Locale("en", "SE"));
		}
		return l;
	}
	
	private final DatePicker datePicker = new DatePicker();
	private final TimePicker timePicker = new TimePicker();

	public LocalDateTimePicker() {
		this.timePicker.getStyle().set("margin-left", "1em");
		Locale l = OwlcmsSession.getLocale();
		Locale fixAM_PM = fixAM_PM(l);
		this.datePicker.setLocale(fixAM_PM);
		this.timePicker.setLocale(fixAM_PM);
		add(this.datePicker, this.timePicker);
	}

	/**
	 * @see com.vaadin.flow.component.AbstractField#getValue()
	 */
	@Override
	public LocalDateTime getValue() {
		return generateModelValue();
	}

	@Override
	protected LocalDateTime generateModelValue() {
		final LocalDate date = this.datePicker.getValue();
		final LocalTime time = this.timePicker.getValue();
		//logger.debug("getValue: date {} time {} locale {}",date,time, this.timePicker.getLocale());
		return date != null && time != null ? LocalDateTime.of(date, time) : null;
	}

	@Override
	protected void setPresentationValue(LocalDateTime newPresentationValue) {
		//logger.debug("setPresentationValue {} locale {}",newPresentationValue, this.timePicker.getLocale());
		this.datePicker.setValue(newPresentationValue != null ? newPresentationValue.toLocalDate() : null);
		this.timePicker.setValue(newPresentationValue != null ? newPresentationValue.toLocalTime() : null);
	}

}