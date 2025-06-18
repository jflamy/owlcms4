/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.jpa;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
import ch.qos.logback.classic.Logger;

/**
 * THIS CLASS IS BROKEN.
 * The timestamp uses the current time zone.
 * Should be converted to UTC then back so the local time is preserved.
 * 
 * Cannot be changed because of existing dates.
 */
@Converter(autoApply = true)
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

	Logger logger = (Logger) LoggerFactory.getLogger(LocalDateAttributeConverter.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.persistence.AttributeConverter#convertToDatabaseColumn(java.lang. Object)
	 */
	@Override
	public Date convertToDatabaseColumn(LocalDate locDate) {
		if (locDate == null) return null;
		if (Config.getCurrent().isLocalDateTimeUtcNormalized()) {
			// Store as UTC midnight
			long epochMilli = locDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
			Date date = new Date(epochMilli);
			//logger.debug("stored millis {} as Date {}", epochMilli, date);
			return date;
		} else {
			// Store as system default
			return Date.valueOf(locDate);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see javax.persistence.AttributeConverter#convertToEntityAttribute(java.lang. Object)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public LocalDate convertToEntityAttribute(Date sqlDate) {
		if (sqlDate == null) return null;
		if (Config.getCurrent().isLocalDateTimeUtcNormalized()) {
			// Read as UTC midnight
			// Always use fallback: convert millis to Instant, then to LocalDate
			long millis = sqlDate.getTime();
			
			java.time.Instant instant = java.time.Instant.ofEpochMilli(millis);
			LocalDate localDate = instant.atZone(ZoneOffset.UTC).toLocalDate();
			//logger.debug("local received date {} {}", millis, localDate);
			return localDate;
		} else {
			// Legacy logic (including H2 workaround)
			if (this.logger.isDebugEnabled()) {
				Calendar cal = Calendar.getInstance();
				int timezoneOffset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET)) / (60 * 1000);
				this.logger.debug("sqlDate {} realOffset {} TZ={} sqlDateOffset {}", sqlDate, timezoneOffset,
				        ZoneId.systemDefault(), sqlDate.getTimezoneOffset());
			}

			LocalDate local;
			String prop = (String) JPAService.getFactory().getProperties().get("JPA_JDBC_URL");
			if (sqlDate.getTimezoneOffset() >= 360 && prop != null && prop.contains("h2:")) {
				local = sqlDate.toLocalDate().plus(1, ChronoUnit.DAYS);
				this.logger.debug("sqlDate fixed {} to {}", sqlDate.toLocalDate(), local);
			} else {
				local = sqlDate.toLocalDate();
			}
			return local;
		}
	}
}