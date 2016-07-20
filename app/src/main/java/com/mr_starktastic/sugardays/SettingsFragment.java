package com.mr_starktastic.sugardays;

import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
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
    private static EditTextPreference correctionEditPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.preferences);

        insulinListPref = (ListPreference) findPreference(PrefKeys.INSULIN);
        insulinListPref.setOnPreferenceChangeListener(
                (preference, newValue) -> configInsulinDependencies((String) newValue));

        bgUnitsListPref = (ListPreference) findPreference(PrefKeys.BG_UNITS);
        correctionEditPref = (EditTextPreference) findPreference(PrefKeys.CORRECTION);

        configInsulinDependencies(insulinListPref.getValue());
    }

    /**
     * Disables the dependencies of the insulin therapy if set to "No insulin", enables otherwise
     *
     * @param newValue The new insulin therapy selected value
     * @return always true
     */
    private boolean configInsulinDependencies(String newValue) {
        correctionEditPref.setEnabled(Integer.parseInt(newValue) != 0);

        return true;
    }
}
