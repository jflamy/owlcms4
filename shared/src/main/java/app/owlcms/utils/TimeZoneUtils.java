/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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

    // Canonical IANA region prefixes (excludes legacy links like US/, Canada/, Brazil/, etc.)
    private static final List<String> CANONICAL_PREFIXES = List.of(
            "Africa/", "America/", "Antarctica/", "Asia/", "Atlantic/",
            "Australia/", "Europe/", "Indian/", "Pacific/");

    public static List<TimeZone> allTimeZones() {
        long now = System.currentTimeMillis();
        String[] allIds = TimeZone.getAvailableIDs();
        List<TimeZone> tzList = Stream.of(allIds)
                .filter(id -> CANONICAL_PREFIXES.stream().anyMatch(id::startsWith))
                .map(id -> TimeZone.getTimeZone(id))
                .sorted((a, b) -> Integer.compare(a.getOffset(now), b.getOffset(now)))
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
