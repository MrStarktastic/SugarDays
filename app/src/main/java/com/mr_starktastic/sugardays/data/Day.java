package com.mr_starktastic.sugardays.data;

import java.util.ArrayList;
import java.util.Calendar;

public class Day {
    private ArrayList<Log> logs;

    public Day(Day other) {
        logs = other != null ? new ArrayList<>(other.logs) : new ArrayList<Log>();
    }

    /**
     * Calculates the amount of days between one date and another
     *
     * @param cal1 First older date
     * @param cal2 Second date
     * @return Calculation result
     */
    public static int daysBetween(Calendar cal1, Calendar cal2) {
        final int cal2Year = cal2.get(Calendar.YEAR),
                cal2DayOfYear = cal2.get(Calendar.DAY_OF_YEAR);

        if (cal1.get(Calendar.YEAR) == cal2Year)
            return cal1.get(Calendar.DAY_OF_YEAR) - cal2DayOfYear;

        cal1 = (Calendar) cal1.clone();
        cal2 = (Calendar) cal2.clone();
        final int cal1OriginalDayOfYear = cal1.get(Calendar.DAY_OF_YEAR);
        int extraDays = 0;

        while (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) {
            cal1.add(Calendar.YEAR, -1);
            extraDays += cal1.getActualMaximum(Calendar.DAY_OF_YEAR);
        }

        return extraDays - cal2DayOfYear + cal1OriginalDayOfYear;
    }

    public ArrayList<Log> getLogs() {
        return logs;
    }
}
