package com.mr_starktastic.sugardays.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
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

import com.google.gson.Gson;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.preference.PrefKeys;
import com.mr_starktastic.sugardays.preference.SeekBarPreference;
import com.mr_starktastic.sugardays.widget.RangeSeekBar;

import java.text.DecimalFormat;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final BloodSugar[] extremeBgValues = BloodSugar.getDiscreteExtremeValues();
    private static final float[] bgSeekBarSteps = {10, 0.5f};
    private static final int[] bgDataTypes =
            {RangeSeekBar.DataType.INTEGER, RangeSeekBar.DataType.FLOAT};
    private static final Gson gson = new Gson();
    private static final String DEFAULT_HYPO_STR = gson.toJson(BloodSugar.DEFAULT_HYPO);
    private static final String DEFAULT_TARGET_RNG_STR = gson.toJson(BloodSugar.DEFAULT_TARGET_RNG);
    private static final String DEFAULT_HYPER_STR = gson.toJson(BloodSugar.DEFAULT_HYPER);

    private String prefScrKey;
    private ListPreference insulinListPref, bgUnitsListPref;
    private SeekBarPreference hypoPref, targetRangePref, hyperPref;
    private PreferenceScreen bolusPredictPrefScr;
    private CheckBoxPreference savePhotosPref, autoLocationPref;
    private int bgUnitIdx;

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
                    (preference, newValue) -> enableBolusPredict((String) newValue));

            // TODO: Pills preference stuff goes here

            bgUnitsListPref = (ListPreference) findPreference(PrefKeys.BG_UNITS);
            bgUnitsListPref.setOnPreferenceChangeListener((preference, newValue) -> {
                saveBgPrefs();
                bgUnitIdx = Integer.parseInt((String) newValue);
                initBgPrefs();
                return true;
            });

            hypoPref = (SeekBarPreference) findPreference(PrefKeys.HYPO);
            targetRangePref = (SeekBarPreference) findPreference(PrefKeys.TARGET_RANGE);
            hyperPref = (SeekBarPreference) findPreference(PrefKeys.HYPER);

            bolusPredictPrefScr = (PreferenceScreen) findPreference(PrefKeys.SCR_BOLUS_PREDICT);

            savePhotosPref = (CheckBoxPreference) findPreference(PrefKeys.SAVE_PHOTOS);
            autoLocationPref = (CheckBoxPreference) findPreference(PrefKeys.AUTO_LOCATION);

            bindSeekBars(view);
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
     * When pausing, {@link BloodSugar} preferences are saved into {@link SharedPreferences}.
     */
    @Override
    public void onPause() {
        if (prefScrKey == null)
            saveBgPrefs();

        super.onPause();
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
                        bgUnitIdx = Integer.parseInt(bgUnitsListPref.getValue());
                        initBgPrefs();
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
     * Initializes the blood glucose {@link SeekBarPreference}s.
     */
    private void initBgPrefs() {
        final int dataType = bgDataTypes[bgUnitIdx];
        final DecimalFormat format = BloodSugar.getFormat(bgUnitIdx);
        final float steps = bgSeekBarSteps[bgUnitIdx];
        final float minV = extremeBgValues[0].get(bgUnitIdx);
        final float maxV = extremeBgValues[1].get(bgUnitIdx);

        /**
         * Gets saved {@link BloodSugar} values from {@link SharedPreferences}.
         */
        final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        final float hypo = gson.fromJson(
                preferences.getString(PrefKeys.HYPO, DEFAULT_HYPO_STR),
                BloodSugar.class).get(bgUnitIdx);
        final BloodSugar[] targetRange = gson.fromJson(
                preferences.getString(PrefKeys.TARGET_RANGE, DEFAULT_TARGET_RNG_STR),
                BloodSugar[].class);
        final float rngMin = targetRange[0].get(bgUnitIdx), rngMax = targetRange[1].get(bgUnitIdx);
        final float hyper = gson.fromJson(
                preferences.getString(PrefKeys.HYPER, DEFAULT_HYPER_STR),
                BloodSugar.class).get(bgUnitIdx);

        hypoPref.initSeekBar(dataType, format, steps, minV, maxV, minV, hypo);
        targetRangePref.initSeekBar(dataType, format, steps, minV, maxV, rngMin, rngMax);
        hyperPref.initSeekBar(dataType, format, steps, minV, maxV, hyper, minV);
    }

    @SuppressLint("CommitPrefEdits")
    private void saveBgPrefs() {
        final Number hypo = hypoPref.getSelectedMaxValue(), hyper = hyperPref.getSelectedMinValue();
        final Number targetMin = targetRangePref.getSelectedMinValue();
        final Number targetMax = targetRangePref.getSelectedMaxValue();

        final BloodSugar hypoBG;
        final BloodSugar[] targetRangeBG;
        final BloodSugar hyperBG;

        if (bgUnitIdx == BloodSugar.MGDL_IDX) {
            hypoBG = new BloodSugar(hypo.intValue());
            targetRangeBG = new BloodSugar[]{
                    new BloodSugar(targetMin.intValue()),
                    new BloodSugar(targetMax.intValue())};
            hyperBG = new BloodSugar(hyper.intValue());
        } else {
            hypoBG = new BloodSugar(hypo.floatValue());
            targetRangeBG = new BloodSugar[]{
                    new BloodSugar(targetMin.floatValue()),
                    new BloodSugar(targetMax.floatValue())};
            hyperBG = new BloodSugar(hyper.floatValue());
        }

        final String hypoJson = gson.toJson(hypoBG), hyperJson = gson.toJson(hyperBG);
        final String targetRangeJson = gson.toJson(targetRangeBG);
        getPreferenceManager().getSharedPreferences().edit()
                .putString(PrefKeys.HYPO, hypoJson)
                .putString(PrefKeys.TARGET_RANGE, targetRangeJson)
                .putString(PrefKeys.HYPER, hyperJson)
                .commit();
    }
}
