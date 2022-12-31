/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TimeZoneUtils {

    public static List<TimeZone> allTimeZones() {
        long now = System.currentTimeMillis();
        String[] allIds = TimeZone.getAvailableIDs();
        List<TimeZone> tzList = Stream.of(allIds).map(id -> TimeZone.getTimeZone(id))
                .sorted((a, b) -> Integer.compare(a.getOffset(now), b.getOffset(now)))
                .filter(s -> s.getID().contains("/") && !s.getID().contains("GMT") && !s.getID().contains("SystemV"))
                .collect(Collectors.toList());
        return tzList;
    }

    public static String getDefault() {
        return TimeZone.getDefault().getID();
    }

    public static String toIdWithOffsetString(TimeZone tz) {
        long now = System.currentTimeMillis();
        long hours = TimeUnit.MILLISECONDS.toHours(tz.getOffset(now));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getOffset(now))
                - TimeUnit.HOURS.toMinutes(hours);
        // avoid -4:-30 issue
        minutes = Math.abs(minutes);

        String result = "";
        if (hours >= 0) {
            result = String.format("%s (UTC+%d:%02d)", tz.getID(), hours, minutes);
        } else {
            result = String.format("%s (UTC%d:%02d)", tz.getID(), hours, minutes);
        }

        return result;
    }

}
