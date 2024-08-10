/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class DateTimeUtils {

	static Logger logger = (Logger) LoggerFactory.getLogger(DateTimeUtils.class);

	public static Date dateFromLocalDate(LocalDate ld) {
		Instant instant = ld.atStartOfDay(ZoneId.systemDefault()).toInstant();
		Date date = Date.from(instant);
		return date;
	}

	public static Date dateFromLocalDateTime(LocalDateTime ldt) {
		Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
		return Date.from(instant);
	}

	/**
	 * Parse an Excel date, not knowing whether a 4-digit year, an ISO format date, or a native Excel days elapsed
	 * format is present
	 *
	 * @param content something that we try to interpret as a date
	 * @return
	 * @throws Exception
	 */
	public static LocalDate parseExcelDate(String content, Locale locale) throws Exception { // logger.debug("parseExcelDate
	                                                                                         // {}", content);
		try {
			long l = Long.parseLong(content);
			if (l < 3000) {
				// year without month and day typed directly.
				return LocalDate.of((int) l, 1, 1);
			} else {
				// an Excel date in days elapsed since 1900 format
				LocalDateTime ldt = parseExcelFractionalDate(content);
				return ldt.toLocalDate();
			}
		} catch (NumberFormatException e) {
			// not a long
			try {
				LocalDateTime ldt = parseExcelFractionalDate(content);
				LocalDate localDate = ldt.toLocalDate();
				// logger.debug("parsed date {} {}",content,localDate);
				return localDate;
			} catch (NumberFormatException e1) {
				LocalDate parse = DateTimeUtils.parseLocalizedOrISO8601Date(content, locale);
				return parse;
			}
		}
	}

	public static LocalDateTime parseExcelFractionalDate(String competitionTime) throws NumberFormatException {
		// logger.debug("parseExcelDateTime {} {}", competitionTime);
		double doubleDays = Double.parseDouble(competitionTime);
		// logger.debug("parseExcelDateTime {} {}", competitionTime, doubleDays);
		long seconds = (long) (doubleDays * 24 * 60 * 60);
		return LocalDateTime
		        .of(1899, Month.DECEMBER, 30, 0, 0) // Specify epoch reference date used by *some* versions of Excel.
		                                            // Beware: Some versions use a 1904 epoch reference
		        .plusSeconds(seconds);
	}

	public static LocalDate parseLocalizedOrISO8601Date(String content, Locale locale) throws Exception {
		String shortPattern = localizedShortDatePattern(locale);
		DateTimeFormatter shortStyleFormatter = DateTimeFormatter.ofPattern(shortPattern, locale);
		try {
			// try as a local date
			LocalDate parse = LocalDate.parse(content, shortStyleFormatter);
			return parse;
		} catch (DateTimeParseException e1) {
			try {
				// try as a ISO Date
				LocalDate parse = LocalDate.parse(content, DateTimeFormatter.ISO_LOCAL_DATE);
				return parse;
			} catch (DateTimeParseException e2) {
				LocalDate sampleDate = LocalDate.of(1961, 02, 28);
				String message = Translator.translate(
				        "Upload.WrongDateFormat",
				        content,
				        locale.getDisplayName(locale),
				        shortPattern,
				        shortStyleFormatter.format(sampleDate),
				        "yyyy-MM-dd",
				        DateTimeFormatter.ISO_LOCAL_DATE.format(sampleDate));
				throw new Exception(message);
			}
		}
	}

	private static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";
	private final static DateTimeFormatter DATETIME_FORMATTER = new DateTimeFormatterBuilder().parseLenient()
	        .appendPattern(DATETIME_FORMAT).toFormatter();
	private static final String DATETIME_FORMAT_SECONDS = "yyyy-MM-dd HH:mm:ss";
	private final static DateTimeFormatter DATETIME_FORMATTER_SECONDS = new DateTimeFormatterBuilder().parseLenient()
	        .appendPattern(DATETIME_FORMAT_SECONDS).toFormatter();
	// try as a local date
	public static LocalDateTime parseISO8601DateTime(String content) {
		// try as a local date
		LocalDateTime parse;
		try {
			parse = LocalDateTime.parse(content, DATETIME_FORMATTER);
			return parse;
		} catch (Exception e) {
			try {
				parse = LocalDateTime.parse(content, DATETIME_FORMATTER_SECONDS);
				return parse;
			} catch (Exception e2) {
				LoggerUtils.logError(logger, e2);
				throw e;
			}
		}
	}

	public static String localizedShortDatePattern(Locale l) {
		String pattern = ((SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, l)).toPattern();
		// if 2-digit year, force 4 digits.
		pattern = pattern.replaceFirst("\\byy\\b", "yyyy");
		return pattern;
	}
	
    public static double localDateTimeToExcelDate(LocalDateTime localDateTime) {
        // Specify the epoch reference date used by Excel (December 30, 1899)
        LocalDateTime excelEpochReference = LocalDateTime.of(1899, Month.DECEMBER, 30, 0, 0);

        // Calculate the number of days between the two dates
        long daysSinceExcelEpoch = localDateTime.toLocalDate().toEpochDay() - excelEpochReference.toLocalDate().toEpochDay();

        // Calculate the fraction of the day (in minutes)
        double fractionOfDay = (double) localDateTime.toLocalTime().toSecondOfDay() / (24 * 60 * 60);

        // Combine the days and fraction to get the Excel numeric date value
        double excelNumericDate = daysSinceExcelEpoch + fractionOfDay;
        
        return excelNumericDate;

    }
}
