package com.mr_starktastic.sugardays;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;

public class SettingsFragment extends PreferenceFragmentCompat {
    private ListPreference insulinListPref, bgUnitsListPref;
    private SeekBarPreference hypoPref, targetRangePref, hyperPref;
    private PreferenceScreen bolusPredictPrefScr;
    private CheckBoxPreference savePhotosPref, autoLocationPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String prefScrKey) {
        setPreferencesFromResource(R.xml.preferences, prefScrKey);

        if (prefScrKey == null) {
            insulinListPref = (ListPreference) findPreference(PrefKeys.INSULIN);
            insulinListPref.setOnPreferenceChangeListener(
                    (preference, newValue) -> setBolusPredictAvailable((String) newValue));

            // TODO Pills preference stuff goes here

            bgUnitsListPref = (ListPreference) findPreference(PrefKeys.BG_UNITS);

            hypoPref = (SeekBarPreference) findPreference(PrefKeys.HYPO);
            targetRangePref = (SeekBarPreference) findPreference(PrefKeys.TARGET_RANGE);
            hyperPref = (SeekBarPreference) findPreference(PrefKeys.HYPER);

            bolusPredictPrefScr = (PreferenceScreen) findPreference(PrefKeys.SCR_BOLUS_PREDICT);

            savePhotosPref = (CheckBoxPreference) findPreference(PrefKeys.SAVE_PHOTOS);
            autoLocationPref = (CheckBoxPreference) findPreference(PrefKeys.AUTO_LOCATION);

            setBolusPredictAvailable(insulinListPref.getValue());
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        if (actionBar != null)
            actionBar.setTitle(getPreferenceScreen().getTitle());

        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Disables the "Bolus prediction" preference when selected "No insulin" as the value of
     * "Insulin therapy", enables it otherwise
     *
     * @param newValue The new insulin therapy selected value
     * @return always true
     */
    private boolean setBolusPredictAvailable(String newValue) {
        final boolean enabled = Integer.parseInt(newValue) != 0;
        bolusPredictPrefScr.setEnabled(enabled);

        if (enabled)
            bolusPredictPrefScr.setSummary(getContext().getString(R.string.off));

        return true;
    }
}
