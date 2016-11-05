package com.mr_starktastic.sugardays.data;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.Unique;

import java.util.Calendar;
import java.util.List;

public class Day extends SugarRecord {
    private static final String WHERE_CLAUSE = "d_id = ?";

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    @Unique
    private int dId;
    @Ignore
    private SugarLog[] logs;
    private String logArrayJSON;

    @SuppressWarnings("unused")
    public Day() {
        // Required empty constructor
    }

    public Day(Calendar calendar) {
        this(generateId(calendar));
    }

    public Day(int dId) {
        this.dId = dId;
        logs = new SugarLog[]{};
    }

    public static int generateId(int year, int month, int dayOfMonth) {
        return year * 10000 + month * 100 + dayOfMonth;
    }

    public static int generateId(Calendar calendar) {
        return generateId(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static Day findById(int dId) {
        final List<Day> list = find(Day.class, WHERE_CLAUSE, Integer.toString(dId));
        return !list.isEmpty() ? list.get(0) : null;
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

    @NonNull
    public SugarLog[] getLogs() {
        if (logs == null)
            logs = new Gson().fromJson(logArrayJSON, SugarLog[].class);

        return logs;
    }

    public void setLogs(@NonNull SugarLog[] logs) {
        logArrayJSON = new Gson().toJson(this.logs = logs);
    }
}
