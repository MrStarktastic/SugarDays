package com.mr_starktastic.sugardays.util;

public class NumericTextUtil {
    public static String trimNumber(float number) {
        final String str = Float.toString(number);
        return !str.endsWith(".0") ? str : str.substring(0, str.length() - 2);
    }

    public static String trimNumber(String number) {
        return trimNumber(Float.parseFloat(number));
    }
}
