package app.owlcms.data.records;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class RecordEventSetters {
    private final static Logger logger = (Logger) LoggerFactory.getLogger(RecordEventSetters.class);
    private final static DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final static DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    private final static DateTimeFormatter yFormatter = DateTimeFormatter.ofPattern("yyyy");

    public static void setFederation(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Federation cannot be empty");
        }
        rec.setRecordFederation(value);
    }

    public static void setRecordName(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();
        value = value != null ? value.trim() : value;
        rec.setRecordName(value);
    }

    public static void setAgeGroup(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();
        value = value != null ? value.trim() : value;
        rec.setAgeGrp(value);
    }

    public static void setGender(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();
        value = value != null ? value.trim().toUpperCase() : value;
        rec.setGender(Gender.valueOf(value));
    }

    public static void setAgeLower(RecordEvent rec, Cell cell) {
        rec.setAgeGrpLower(Math.toIntExact(Math.round(cell.getNumericCellValue())));
    }

    public static void setAgeUpper(RecordEvent rec, Cell cell) {
        int value = Math.toIntExact(Math.round(cell.getNumericCellValue()));
        if (value < rec.getAgeGrpLower()) {
            throw new IllegalArgumentException(value + " upper limit on age category should be >= to " + rec.getAgeGrpLower());
        }
        rec.setAgeGrpUpper(value);
    }

    public static void setBwLower(RecordEvent rec, Cell cell) {
        rec.setBwCatLower(Math.toIntExact(Math.round(cell.getNumericCellValue())));
    }

    public static void setBwUpper(RecordEvent rec, Cell cell) {
        if (cell.getCellType() == CellType.STRING) {
            String cellValue = cell.getStringCellValue();
            rec.setBwCatString(cellValue);
            
            if (cellValue.startsWith(">") || cellValue.startsWith("+") || cellValue.endsWith("+")) {
                rec.setBwCatUpper(999);
                rec.setBwCatString(Translator.translate("catAboveFormat",rec.getBwCatLower()));
            } else {
                try {
                    // scar tissue for legacy error.
                    // compensate for excessive validation in Excel preventing entering 999
                    var val = Integer.parseInt(cellValue);
                    if (val >= 199) {
                        rec.setBwCatUpper(999);
                        rec.setBwCatString(Translator.translate("catAboveFormat",rec.getBwCatLower()));
                    } else {
                        rec.setBwCatUpper(val);
                    }
                } catch (NumberFormatException e) {
                    if (cellValue != null && !cellValue.isBlank()) {
                        logger.error("[" + cell.getSheet().getSheetName() + "," + cell.getAddress() + "]");
                    }
                }
            }
            
            if (rec.getBwCatUpper() < rec.getBwCatLower()) {
                throw new IllegalArgumentException(cellValue + " upper limit on bodyweight category should be >= to " + rec.getBwCatLower());
            }
        } else if (cell.getCellType() == CellType.NUMERIC) {
            long cellValue = Math.round(cell.getNumericCellValue());
            rec.setBwCatString(Long.toString(cellValue));
            rec.setBwCatUpper(Math.toIntExact(cellValue));
            
            if (rec.getBwCatUpper() <= rec.getBwCatLower()) {
                throw new IllegalArgumentException(cellValue + " upper limit on bodyweight category should be > to " + rec.getBwCatLower());
            }
        } else {
            throw new IllegalArgumentException("Unexpected cell type " + cell.getCellType() + 
                " at [" + cell.getSheet().getSheetName() + "," + cell.getAddress() + "]");
        }
    }

    public static void setRecordLift(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();

        // accept translated values for lift types, but store the standard value in the record
        if (Translator.translate("Record.SNATCH").equalsIgnoreCase(value)) {
            value = "SNATCH";
        } else if (Translator.translate("Record.CLEANJERK").equalsIgnoreCase(value)) {
            value = "CLEAN_AND_JERK";
        } else if (Translator.translate("Record.TOTAL").equalsIgnoreCase(value)) {
            value = "TOTAL";
        }

        value = value != null ? value.trim() : value;
        rec.setRecordLift(value);
    }

    public static void setRecordValue(RecordEvent rec, Cell cell) {
        rec.setRecordValue(cell.getNumericCellValue());
    }

    public static void setAthleteName(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();
        value = value != null ? value.trim() : value;
        rec.setAthleteName(value);
    }

    public static void setBirthDate(RecordEvent rec, Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            long numericValue = Math.round(cell.getNumericCellValue());
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
            parseDateOrYear(rec, cell.getStringCellValue(), "birth");
        }
    }

    public static void setNation(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();
        value = value != null ? value.trim() : value;
        rec.setNation(value);
    }

    public static void setGroup(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();
        value = value != null ? value.trim() : value;
        rec.setGroupNameString(value);
    }

    public static void setRecordDate(RecordEvent rec, Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            long numericValue = Math.round(cell.getNumericCellValue());
            if (numericValue < 3000) {
                rec.setRecordYear(Math.toIntExact(numericValue));
                logger.debug("number {}", numericValue);
            } else {
                LocalDate epoch = LocalDate.of(1900, 1, 1);
                LocalDate plusDays = epoch.plusDays(numericValue - 2);
                rec.setRecordDate(plusDays);
                logger.debug("plusDays {}", rec.getRecordDateAsString());
            }
        } else if (cell.getStringCellValue() != null && !cell.getStringCellValue().isBlank()) {
            parseDateOrYear(rec, cell.getStringCellValue(), "record");
        }
    }

    public static void setEventLocation(RecordEvent rec, Cell cell) {
        String value = cell.getStringCellValue();
        value = value != null ? value.trim() : value;
        rec.setEventLocation(value);
    }

    public static void setEvent(RecordEvent rec, Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC) {
            rec.setEvent("");
        } else {
            String value = cell.getStringCellValue();
            value = value != null ? value.trim() : value;
            rec.setEvent(value);
        }
    }

    private static void parseDateOrYear(RecordEvent rec, String cellValue, String type) {
        if (cellValue != null && cellValue.isBlank()) {
            return;
        }
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
