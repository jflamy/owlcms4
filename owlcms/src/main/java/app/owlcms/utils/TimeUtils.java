package app.owlcms.utils;

import java.time.LocalDateTime;
import java.time.Month;

public class TimeUtils {

    public static LocalDateTime parseExcelDateTime(String competitionTime) {
        double doubleDays = Double.parseDouble(competitionTime);
        long minutes = (long) (doubleDays * 24 * 60);
        return LocalDateTime
                .of(1899, Month.DECEMBER, 30, 0, 0) // Specify epoch reference date used by *some* versions of Excel.
                                                    // Beware: Some versions use a 1904 epoch reference
                .plusMinutes(minutes);     
    }

}
