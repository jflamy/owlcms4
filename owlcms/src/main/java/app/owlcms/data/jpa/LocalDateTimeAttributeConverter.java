/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/*
 * THIS CLASS IS BROKEN.
 * The timestamp uses the current time zone.
 * Should be converted to UTC then back so the local time is preserved.
 * 
 * Cannot be changed because of existing dates.
 */
@Converter(autoApply = true)
public class LocalDateTimeAttributeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang. Object)
	 */
	@Override
	public Timestamp convertToDatabaseColumn(LocalDateTime locDateTime) {
		return (locDateTime == null ? null : Timestamp.valueOf(locDateTime));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang. Object)
	 */
	@Override
	public LocalDateTime convertToEntityAttribute(Timestamp sqlTimestamp) {
		return (sqlTimestamp == null ? null : sqlTimestamp.toLocalDateTime());
	}
}
