package com.mr_starktastic.sugardays;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.view.View;
import android.view.ViewTreeObserver;

public class SettingsFragment extends PreferenceFragmentCompat {
    private String prefScrKey;
    private ListPreference insulinListPref, bgUnitsListPref;
    private SeekBarPreference hypoPref, targetRangePref, hyperPref;
    private PreferenceScreen bolusPredictPrefScr;
    private CheckBoxPreference savePhotosPref, autoLocationPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String prefScrKey) {
        setPreferencesFromResource(R.xml.preferences, this.prefScrKey = prefScrKey);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (prefScrKey == null) {
            insulinListPref = (ListPreference) findPreference(PrefKeys.INSULIN);
            insulinListPref.setOnPreferenceChangeListener(
                    (preference, newValue) -> setBolusPredictAvailable((String) newValue));

            // TODO: Pills preference stuff goes here

            bgUnitsListPref = (ListPreference) findPreference(PrefKeys.BG_UNITS);

            hypoPref = (SeekBarPreference) findPreference(PrefKeys.HYPO);
            targetRangePref = (SeekBarPreference) findPreference(PrefKeys.TARGET_RANGE);
            hyperPref = (SeekBarPreference) findPreference(PrefKeys.HYPER);
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("deprecation")
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                        view.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    else view.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    hypoPref.setUpperBound(targetRangePref);
                    targetRangePref.setLowerBound(hypoPref);
                    targetRangePref.setUpperBound(hyperPref);
                    hyperPref.setLowerBound(targetRangePref);
                }
            });

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
