package com.mr_starktastic.sugardays.util;

import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.data.PrefKeys;

/**
 * A utility class for fetching {@link android.support.v7.preference.Preference}s.
 */
public class PrefUtil {
    public static final int THERAPY_NO_INSULIN = 0, THERAPY_PEN = 1, THERAPY_PUMP = 2;
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_HYPO_STR = GSON.toJson(BloodSugar.DEFAULT_HYPO);
    private static final String DEFAULT_TARGET_RNG_STR = GSON.toJson(BloodSugar.DEFAULT_TARGET_RNG);
    private static final String DEFAULT_HYPER_STR = GSON.toJson(BloodSugar.DEFAULT_HYPER);
    private static final boolean DEFAULT_BOOLEAN_VALUE = false;
    private static final int DEFAULT_INT_VALUE = 0;

    private static String getBgUnit(SharedPreferences preferences) {
        return preferences.getString(PrefKeys.BG_UNITS, Integer.toString(DEFAULT_INT_VALUE));
    }

    public static int getBgUnitIdx(SharedPreferences preferences) {
        return Integer.parseInt(getBgUnit(preferences));
    }

    public static String[] getPills(SharedPreferences preferences) {
        return GSON.fromJson(preferences.getString(PrefKeys.PILLS, null), String[].class);
    }

    public static BloodSugar getHypo(SharedPreferences preferences) {
        return GSON.fromJson(
                preferences.getString(PrefKeys.HYPO, DEFAULT_HYPO_STR),
                BloodSugar.class);
    }

    public static BloodSugar[] getTargetRange(SharedPreferences preferences) {
        return GSON.fromJson(
                preferences.getString(PrefKeys.TARGET_RANGE, DEFAULT_TARGET_RNG_STR),
                BloodSugar[].class);
    }

    public static BloodSugar getHyper(SharedPreferences preferences) {
        return GSON.fromJson(
                preferences.getString(PrefKeys.HYPER, DEFAULT_HYPER_STR),
                BloodSugar.class);
    }

    public static boolean getBolusPredictSwitch(SharedPreferences preferences) {
        return preferences.getBoolean(PrefKeys.BOLUS_PREDICT_SWITCH, DEFAULT_BOOLEAN_VALUE);
    }

    public static BloodSugar getOptimalBg(SharedPreferences preferences) {
        return GSON.fromJson(
                preferences.getString(PrefKeys.OPTIMAL_BG, null),
                BloodSugar.class);
    }

    public static float getCorrectionFactor(SharedPreferences preferences) {
        return preferences.getFloat(PrefKeys.CORRECTION_FACTOR, DEFAULT_INT_VALUE);
    }

    public static float getCarbToInsulin(SharedPreferences preferences) {
        return preferences.getFloat(PrefKeys.CARB_TO_INSULIN, DEFAULT_INT_VALUE);
    }

    public static float getInsulinIncrement(SharedPreferences preferences) {
        return Float.parseFloat(preferences.getString(PrefKeys.INSULIN_INCREMENT, "1"));
    }

    public static boolean getAutoLocation(SharedPreferences preferences) {
        return preferences.getBoolean(PrefKeys.AUTO_LOCATION, DEFAULT_BOOLEAN_VALUE);
    }

    public static boolean getPhotoCompressEnabled(SharedPreferences preferences) {
        return preferences.getBoolean(PrefKeys.COMPRESS_PHOTOS, DEFAULT_BOOLEAN_VALUE);
    }

    public static int getInsulinTherapy(SharedPreferences preferences) {
        return Integer.parseInt(preferences
                .getString(PrefKeys.INSULIN, Integer.toString(THERAPY_NO_INSULIN)));
    }

    public static String toJson(Object obj) {
        return GSON.toJson(obj);
    }
}
