package app.owlcms.data.records;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.apache.poi.ss.usermodel.CellType;
import app.owlcms.data.athlete.Gender;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;

public class RecordEventSetters {
    private final static Logger logger = (Logger) LoggerFactory.getLogger(RecordEventSetters.class);
    private final static DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final static DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final static DateTimeFormatter yFormatter = DateTimeFormatter.ofPattern("yyyy");
    
    public static void setFederation(RecordEvent rec, String cellValue) {
        String trim = cellValue.trim();
        if (trim.isEmpty()) {
            throw new IllegalArgumentException("Federation cannot be empty");
        }
        rec.setRecordFederation(trim);
    }

    public static void setRecordName(RecordEvent rec, String cellValue) {
        cellValue = cellValue != null ? cellValue.trim() : cellValue;
        rec.setRecordName(cellValue);
    }

    public static void setAgeGroup(RecordEvent rec, String cellValue) {
        cellValue = cellValue != null ? cellValue.trim() : cellValue;
        rec.setAgeGrp(cellValue);
    }

    public static void setGender(RecordEvent rec, String cellValue) {
        cellValue = cellValue != null ? cellValue.trim().toUpperCase() : cellValue;
        rec.setGender(Gender.valueOf(cellValue));
    }

    public static void setAgeLower(RecordEvent rec, double cellValue) {
        rec.setAgeGrpLower(Math.toIntExact(Math.round(cellValue)));
    }

    public static void setAgeUpper(RecordEvent rec, double cellValue) {
        int value = Math.toIntExact(Math.round(cellValue));
        if (value < rec.getAgeGrpLower()) {
            throw new IllegalArgumentException(value + " upper limit on age category should be >= to " + rec.getAgeGrpLower());
        }
        rec.setAgeGrpUpper(value);
    }

    public static void setBwLower(RecordEvent rec, double cellValue) {
        rec.setBwCatLower(Math.toIntExact(Math.round(cellValue)));
    }

    public static void setBwUpper(RecordEvent rec, String cellValue, CellType cellType) {
        if (cellType == CellType.STRING) {
            rec.setBwCatString(cellValue);
            try {
                if (cellValue.startsWith(">") || cellValue.startsWith("+")) {
                    rec.setBwCatUpper(999);
                    rec.setBwCatString(">" + rec.getBwCatLower());
                } else {
                    rec.setBwCatUpper(Integer.parseInt(cellValue));
                }
            } catch (NumberFormatException e) {
                if (cellValue != null && !cellValue.isBlank()) {
                    throw new IllegalArgumentException("Invalid bodyweight upper limit: " + cellValue);
                }
            }
            if (rec.getBwCatUpper() < rec.getBwCatLower()) {
                throw new IllegalArgumentException(cellValue + " upper limit on bodyweight category should be >= to " + rec.getBwCatLower());
            }
        } else {
            long numericValue = Math.round(Double.parseDouble(cellValue));
            rec.setBwCatString(Long.toString(numericValue));
            rec.setBwCatUpper(Math.toIntExact(numericValue));
            if (rec.getBwCatUpper() <= rec.getBwCatLower()) {
                throw new IllegalArgumentException(numericValue + " upper limit on bodyweight category should be > to " + rec.getBwCatLower());
            }
        }
    }

    public static void setRecordLift(RecordEvent rec, String cellValue) {
        cellValue = cellValue != null ? cellValue.trim() : cellValue;
        rec.setRecordLift(cellValue);
    }

    public static void setRecordValue(RecordEvent rec, double cellValue) {
        rec.setRecordValue(cellValue);
    }

    public static void setAthleteName(RecordEvent rec, String cellValue) {
        cellValue = cellValue != null ? cellValue.trim() : cellValue;
        rec.setAthleteName(cellValue);
    }

    public static void setBirthDate(RecordEvent rec, String cellValue, CellType cellType) {
        if (cellType == CellType.NUMERIC) {
            long numericValue = Math.round(Double.parseDouble(cellValue));
            if (numericValue < 3000) {
                rec.setBirthYear(Math.toIntExact(numericValue));
                logger.debug("number {}", numericValue);
            } else {
                LocalDate epoch = LocalDate.of(1900, 1, 1);
                LocalDate plusDays = epoch.plusDays(numericValue - 2);
                rec.setBirthDate(plusDays);
                logger.debug("plusDays {}", rec.getRecordDateAsString());
            }
        } else {
            parseDateOrYear(rec, cellValue, "birth");
        }
    }

    public static void setNation(RecordEvent rec, String cellValue) {
        cellValue = cellValue != null ? cellValue.trim() : cellValue;
        rec.setNation(cellValue);
    }

    public static void setRecordDate(RecordEvent rec, String cellValue, CellType cellType) {
        if (cellType == CellType.NUMERIC) {
            long numericValue = Math.round(Double.parseDouble(cellValue));
            if (numericValue < 3000) {
                rec.setRecordYear(Math.toIntExact(numericValue));
                logger.debug("number {}", numericValue);
            } else {
                LocalDate epoch = LocalDate.of(1900, 1, 1);
                LocalDate plusDays = epoch.plusDays(numericValue - 2);
                rec.setRecordDate(plusDays);
                logger.debug("plusDays {}", rec.getRecordDateAsString());
            }
        } else if (cellValue != null && !cellValue.isBlank()) {
            parseDateOrYear(rec, cellValue, "record");
        }
    }

    public static void setEventLocation(RecordEvent rec, String cellValue) {
        cellValue = cellValue != null ? cellValue.trim() : cellValue;
        rec.setEventLocation(cellValue);
    }

    public static void setEvent(RecordEvent rec, String cellValue, CellType cellType) {
        if (cellType == CellType.NUMERIC) {
            rec.setEvent("");
        } else {
            cellValue = cellValue != null ? cellValue.trim() : cellValue;
            rec.setEvent(cellValue);
        }
    }

    private static void parseDateOrYear(RecordEvent rec, String cellValue, String type) {
        try {
            LocalDate date = LocalDate.parse(cellValue, ymdFormatter);
            if (type.equals("birth")) {
                rec.setBirthDate(date);
            } else {
                rec.setRecordDate(date);
            }
            logger.debug("date {}", date);
        } catch (DateTimeParseException e) {
            try {
                YearMonth date = YearMonth.parse(cellValue, ymFormatter);
                if (type.equals("birth")) {
                    rec.setBirthYear(date.getYear());
                } else {
                    rec.setRecordYear(date.getYear());
                }
                logger.debug("datemonth {}", date.getYear());
            } catch (DateTimeParseException e2) {
                try {
                    Year date = Year.parse(cellValue, yFormatter);
                    if (type.equals("birth")) {
                        rec.setBirthYear(date.getValue());
                    } else {
                        rec.setRecordYear(date.getValue());
                    }
                    logger.debug("year {}", date.getValue());
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException(cellValue + " not in yyyy-MM-dd or yyyy-MM or yyyy date format");
                }
            }
        }
    }
}
