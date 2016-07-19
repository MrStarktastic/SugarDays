package com.mr_starktastic.sugardays;

import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    /**
     * Blood glucose constants for both measure units (mg/dL & mmol/L)
     */
    private static final int MGPDL_MIN_BG = 10, MGPDL_MAX_BG = 900, MGPDL_INCREMENT = 10;
    private static final float MMOLPL_MIN_BG = 0.6f, MMOLPL_MAX_BG = 49.9f, MMOLPL_INCREMENT = 0.1f;

    /**
     * Preference objects
     */
    private static ListPreference insulinListPref, bgUnitsListPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);

        insulinListPref = (ListPreference) findPreference(PrefKeys.INSULIN);
        bgUnitsListPref = (ListPreference) findPreference(PrefKeys.BG_UNITS);
    }
}
