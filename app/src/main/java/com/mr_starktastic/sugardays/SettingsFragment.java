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

import com.mr_starktastic.sugardays.widget.RangeSeekBar;

import java.text.DecimalFormat;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final BloodSugar[] extremeBgValues = BloodSugar.getDiscreteExtremeValues();
    private static final float[] bgSeekBarSteps = new float[]{10, 0.5f};
    private static final int[] bgDataTypes =
            new int[]{RangeSeekBar.DataType.INTEGER, RangeSeekBar.DataType.FLOAT};

    private String prefScrKey;
    private ListPreference insulinListPref, bgUnitsListPref;
    private SeekBarPreference hypoPref, targetRangePref, hyperPref;
    private PreferenceScreen bolusPredictPrefScr;
    private CheckBoxPreference savePhotosPref, autoLocationPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String prefScrKey) {
        setPreferencesFromResource(R.xml.preferences, this.prefScrKey = prefScrKey);
        setRetainInstance(true);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (prefScrKey == null) {
            insulinListPref = (ListPreference) findPreference(PrefKeys.INSULIN);
            insulinListPref.setOnPreferenceChangeListener(
                    (preference, newValue) -> enableBolusPredict((String) newValue));

            // TODO: Pills preference stuff goes here

            bgUnitsListPref = (ListPreference) findPreference(PrefKeys.BG_UNITS);
            bgUnitsListPref.setOnPreferenceChangeListener(
                    (preference, newValue) -> setBgUnit(Integer.parseInt((String) newValue)));

            hypoPref = (SeekBarPreference) findPreference(PrefKeys.HYPO);
            targetRangePref = (SeekBarPreference) findPreference(PrefKeys.TARGET_RANGE);
            hyperPref = (SeekBarPreference) findPreference(PrefKeys.HYPER);
            bindSeekBars(view);

            bolusPredictPrefScr = (PreferenceScreen) findPreference(PrefKeys.SCR_BOLUS_PREDICT);

            savePhotosPref = (CheckBoxPreference) findPreference(PrefKeys.SAVE_PHOTOS);
            autoLocationPref = (CheckBoxPreference) findPreference(PrefKeys.AUTO_LOCATION);

            enableBolusPredict(insulinListPref.getValue());
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
     * Binds the 3 blood sugar {@link SeekBarPreference} such that every
     * {@link com.mr_starktastic.sugardays.widget.RangeSeekBar} is bounded by the min value of the
     * next one and vice versa by the max value.
     *
     * @param root The container view of the main {@link PreferenceScreen}
     */
    private void bindSeekBars(View root) {
        root.getViewTreeObserver()
                .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void onGlobalLayout() {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                            root.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                        else root.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                        hypoPref.setUpperBound(targetRangePref);
                        targetRangePref.setLowerBound(hypoPref);
                        targetRangePref.setUpperBound(hyperPref);
                        hyperPref.setLowerBound(targetRangePref);
                        setBgUnit(Integer.parseInt(bgUnitsListPref.getValue()));
                    }
                });
    }

    /**
     * Disables the "Bolus prediction" preference when selected "No insulin" as the value of
     * "Insulin therapy", enables it otherwise.
     *
     * @param insulinTherapy The new insulin therapy selected value
     * @return always true
     */
    private boolean enableBolusPredict(String insulinTherapy) {
        final boolean enabled = Integer.parseInt(insulinTherapy) != 0;
        bolusPredictPrefScr.setEnabled(enabled);

        if (enabled)
            bolusPredictPrefScr.setSummary(getContext().getString(R.string.off));

        return true;
    }

    /**
     * Sets the chosen blood glucose unit.
     *
     * @param bgUnit The blood glucose unit selected value
     * @return always true
     */
    private boolean setBgUnit(int bgUnit) {
        final int dataType = bgDataTypes[bgUnit];
        final DecimalFormat format = BloodSugar.getFormat(bgUnit);
        final float steps = bgSeekBarSteps[bgUnit];
        final float minV = extremeBgValues[0].get(bgUnit), maxV = extremeBgValues[1].get(bgUnit);

        hypoPref.initSeekBar(dataType, format, steps, minV, maxV);
        targetRangePref.initSeekBar(dataType, format, steps, minV, maxV);
        hyperPref.initSeekBar(dataType, format, steps, minV, maxV);

        return true;
    }
}
