package com.mr_starktastic.sugardays.util;

public class NumericTextUtil {
    public static String trim(float number) {
        final String str = Float.toString(number);
        return !str.endsWith(".0") ? str : str.substring(0, str.length() - 2);
    }

    public static String trim(String number) {
        return trim(Float.parseFloat(number));
    }

    /**
     * Rounds a non-integer number to a decimal increment.
     * For instance, an increment of 0.25 and the number 7.2 -> rounded to 7.25.
     *
     * @param num       Given non-integer number to round.
     * @param increment A decimal increment between 1 (inclusive) and 0 (exclusive).
     * @return Rounded number as a string.
     */
    public static String roundToIncrement(float num, float increment) {
        return trim(Math.round(num / increment) * increment);
    }
}
