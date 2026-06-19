/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.sql.Date;
import java.time.LocalDate;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Persists {@link LocalDate} values as civil (calendar) dates.
 *
 * A {@code LocalDate} such as a birth date or a competition date is a calendar
 * date, not an instant in time. It must be stored and read back verbatim, with
 * no timezone conversion, so that January 1st is January 1st regardless of the
 * server timezone (Moscow or Hawaii). UTC normalization applies only to
 * {@code LocalDateTime} (moments in time), never to {@code LocalDate}.
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

	@Override
	public Date convertToDatabaseColumn(LocalDate locDate) {
		return locDate == null ? null : Date.valueOf(locDate);
	}

	@Override
	public LocalDate convertToEntityAttribute(Date sqlDate) {
		return sqlDate == null ? null : sqlDate.toLocalDate();
	}
}