/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * The Class LocalDateAttributeConverter.
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

	Logger logger = (Logger) LoggerFactory.getLogger(LocalDateAttributeConverter.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang.
	 * Object)
	 */
	@Override
	public Date convertToDatabaseColumn(LocalDate locDate) {
		return (locDate == null ? null : Date.valueOf(locDate));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang.
	 * Object)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public LocalDate convertToEntityAttribute(Date sqlDate) {
		if (sqlDate == null) {
			return null;
		}

		if (logger.isDebugEnabled()) {
			Calendar cal = Calendar.getInstance();
			int timezoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
			logger.debug("sqlDate {} realOffset {} TZ={} sqlDateOffset {}", sqlDate, timezoneOffset,
			        ZoneId.systemDefault(), sqlDate.getTimezoneOffset());
		}

		LocalDate local;
		String prop = (String) JPAService.getFactory().getProperties().get("JPA_JDBC_URL");
		if (sqlDate.getTimezoneOffset() >= 360 && prop != null && prop.contains("h2:")) {
			// kludge to work around a bug in H2
			local = sqlDate.toLocalDate().plus(1, ChronoUnit.DAYS);
			logger.debug("sqlDate fixed {} to {}", sqlDate.toLocalDate(), local);
		} else {
			// not needed for Postgres
			local = sqlDate.toLocalDate();
		}
		return local;

	}
}