package com.mr_starktastic.sugardays.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.View;
import android.view.ViewTreeObserver;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.activity.PillManagerActivity;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.data.PrefKeys;
import com.mr_starktastic.sugardays.preference.InlineEditTextPreference;
import com.mr_starktastic.sugardays.preference.SeekBarPreference;
import com.mr_starktastic.sugardays.preference.SwitchStripPreference;
import com.mr_starktastic.sugardays.util.PrefUtil;
import com.mr_starktastic.sugardays.widget.RangeSeekBar;

import java.text.DecimalFormat;

import static com.mr_starktastic.sugardays.util.PrefUtil.getTargetRange;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final BloodSugar[] extremeBgValues = BloodSugar.getDiscreteExtremeValues();
    private static final float[] bgSeekBarSteps = {10, 0.5f};
    private static final int[] bgDataTypes =
            {RangeSeekBar.DataType.INTEGER, RangeSeekBar.DataType.FLOAT};

    private int bgUnitIdx;
    private String prefScrKey;
    private ListPreference insulinListPref;
    private Preference pillPref;
    private ListPreference bgUnitListPref;
    private SeekBarPreference hypoPref, targetRangePref, hyperPref;
    private PreferenceScreen bolusPredictPrefScr;
    private SwitchStripPreference bolusPredictSwitchPref;
    private SeekBarPreference optimalPref;
    private InlineEditTextPreference correctionFactorPref, carbToInsulinPref;
    private SwitchPreferenceCompat savePhotosPref, autoLocationPref;

    @Override
    public void onCreatePreferences(Bundle bundle, String prefScrKey) {
        setPreferencesFromResource(R.xml.preferences, this.prefScrKey = prefScrKey);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /**
         * Wires-up all {@link Preference}s corresponding to the current {@link PreferenceScreen}.
         */
        if (prefScrKey == null) {
            insulinListPref = (ListPreference) findPreference(PrefKeys.INSULIN);
            insulinListPref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            return enableBolusPredict((String) newValue);
                        }
                    });

            (pillPref = findPreference(PrefKeys.PILLS)).setOnPreferenceClickListener(
                    new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            startActivity(new Intent(getActivity(), PillManagerActivity.class));
                            return true;
                        }
                    });

            bgUnitListPref = (ListPreference) findPreference(PrefKeys.BG_UNITS);
            bgUnitListPref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            saveBgPrefs();
                            bgUnitIdx = Integer.parseInt((String) newValue);
                            initBgPrefs();
                            return true;
                        }
                    });

            hypoPref = (SeekBarPreference) findPreference(PrefKeys.HYPO);
            targetRangePref = (SeekBarPreference) findPreference(PrefKeys.TARGET_RANGE);
            hyperPref = (SeekBarPreference) findPreference(PrefKeys.HYPER);

            bolusPredictPrefScr = (PreferenceScreen) findPreference(PrefKeys.SCR_BOLUS_PREDICT);

            savePhotosPref = (SwitchPreferenceCompat) findPreference(PrefKeys.COMPRESS_PHOTOS);
            autoLocationPref = (SwitchPreferenceCompat) findPreference(PrefKeys.AUTO_LOCATION);
        } else if (prefScrKey.equals(PrefKeys.SCR_BOLUS_PREDICT)) {
            final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
            final Context context = getContext();

            bolusPredictSwitchPref =
                    (SwitchStripPreference) findPreference(PrefKeys.BOLUS_PREDICT_SWITCH);
            bolusPredictSwitchPref.setTitle(
                    preferences.getString(PrefKeys.SCR_BOLUS_PREDICT, getString(R.string.off)));
            bolusPredictSwitchPref.setSummaries(
                    getString(R.string.pref_bolus_predict_off_summary), null);
            initOptimalPref();
            final String perInsulinUnit = getString(R.string.per_insulin_unit);
            correctionFactorPref = new InlineEditTextPreference(context, getResources()
                    .getStringArray(R.array.pref_bgUnits_entries)[bgUnitIdx] + " " +
                    perInsulinUnit);
            carbToInsulinPref = new InlineEditTextPreference(context,
                    getString(R.string.grams_of_carb) + " " + perInsulinUnit);
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
     * When pausing, all complex {@link Preference}s are saved into {@link SharedPreferences}.
     */
    @SuppressLint("CommitPrefEdits")
    @Override
    public void onPause() {
        final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

        if (prefScrKey == null) {
            saveBgPrefs();

            if (PrefUtil.getBolusPredictSwitch(preferences)) {
                final float minVal = targetRangePref.getSelectedMinValue().floatValue();
                final float val = preferences.getFloat(PrefKeys.OPTIMAL_NORM_BG, 0) / 100 *
                        (targetRangePref.getSelectedMaxValue().floatValue() - minVal);
                preferences.edit().putString(PrefKeys.OPTIMAL_BG, PrefUtil.toJson(
                        new BloodSugar(bgUnitIdx == BloodSugar.MGDL_IDX ? (int) val : val)))
                        .commit();
            }
        } else if (prefScrKey.equals(PrefKeys.SCR_BOLUS_PREDICT))
            if (bolusPredictSwitchPref.isChecked()) {
                final float optimalNorm = optimalPref.getNormalizedMaxValue();
                final float correctionFactor = correctionFactorPref.getValue();
                final float carbToInsulin = carbToInsulinPref.getValue();

                preferences.edit()
                        .putFloat(PrefKeys.OPTIMAL_NORM_BG, optimalNorm)
                        .putString(PrefKeys.OPTIMAL_BG, PrefUtil.toJson(
                                new BloodSugar(bgUnitIdx == BloodSugar.MGDL_IDX ?
                                        optimalPref.getSelectedMaxValue().intValue() :
                                        optimalPref.getSelectedMaxValue().floatValue())))
                        .putFloat(PrefKeys.CORRECTION_FACTOR, correctionFactor)
                        .putFloat(PrefKeys.CARB_TO_INSULIN, carbToInsulin)
                        .commit();
            }

        super.onPause();
    }

    /**
     * When resuming, all complex {@link Preference}s are set to their expected behavior
     * while displaying their saved data.
     */
    @SuppressLint("CommitPrefEdits")
    @Override
    public void onResume() {
        super.onResume();
        final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

        if (prefScrKey == null) {
            initPills();
            bindSeekBars(getView());
            enableBolusPredict(insulinListPref.getValue());
            bolusPredictPrefScr.setSummary(preferences
                    .getString(PrefKeys.SCR_BOLUS_PREDICT, getString(R.string.off)));
        } else if (prefScrKey.equals(PrefKeys.SCR_BOLUS_PREDICT))
            initBolusPredictPrefs();
    }

    /**
     * Binds the 3 blood sugar {@link SeekBarPreference} such that every
     * {@link com.mr_starktastic.sugardays.widget.RangeSeekBar} is bounded by the min value of the
     * next one and vice versa by the max value.
     *
     * @param root The container view of the main {@link PreferenceScreen}
     */
    private void bindSeekBars(final View root) {
        bgUnitIdx = Integer.parseInt(bgUnitListPref.getValue());
        initBgPrefs();

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
                    }
                });
    }

    /**
     * The optimalPref {@link SeekBarPreference} is quite complex so better initialize it separately
     */
    private void initOptimalPref() {
        final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        final Context context = getContext();

        bgUnitIdx = PrefUtil.getBgUnitIdx(preferences);
        // Normalized value is used in order to avoid possible issues w.r.t. the target range
        final float optimalNorm = preferences.getFloat(PrefKeys.OPTIMAL_NORM_BG, -1);
        final BloodSugar[] targetRangeBG = PrefUtil.getTargetRange(preferences);
        final float rngMin = targetRangeBG[0].get(bgUnitIdx);
        final float rngMax = targetRangeBG[1].get(bgUnitIdx);
        final DecimalFormat decimalFormat = BloodSugar.getFormat(bgUnitIdx);

        optimalPref = new SeekBarPreference(context, R.layout.preference_point_seek_bar_widget,
                ContextCompat.getColor(context, R.color.colorGoodBloodGlucose),
                bgDataTypes[bgUnitIdx], decimalFormat, bgSeekBarSteps[bgUnitIdx],
                rngMin, rngMax, rngMin, rngMax);
        optimalPref.setNormalizedMaxValue(optimalNorm == -1 ? 50f : optimalNorm);
        optimalPref.setSummary("%s");
    }

    private void initBolusPredictPrefs() {
        final SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        final PreferenceScreen screen = getPreferenceScreen();

        optimalPref.setTitle(getString(R.string.pref_optimal_bg_title));
        correctionFactorPref.setTitle(getString(R.string.pref_correction_factor_title));
        correctionFactorPref.setValue(PrefUtil.getCorrectionFactor(preferences));
        carbToInsulinPref.setTitle(getString(R.string.pref_carb_to_insulin_title));
        carbToInsulinPref.setValue(PrefUtil.getCarbToInsulin(preferences));

        if (PrefUtil.getBolusPredictSwitch(preferences)) {
            screen.addPreference(optimalPref);
            screen.addPreference(correctionFactorPref);
            screen.addPreference(carbToInsulinPref);
        }

        bolusPredictSwitchPref.setOnCheckedChangeListener(
                new SwitchStripPreference.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChangeListener(boolean isChecked) {
                        if (isChecked) {
                            screen.addPreference(optimalPref);
                            screen.addPreference(correctionFactorPref);
                            screen.addPreference(carbToInsulinPref);
                        } else {
                            screen.removePreference(optimalPref);
                            screen.removePreference(correctionFactorPref);
                            screen.removePreference(carbToInsulinPref);
                        }
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
        bolusPredictPrefScr.setSummary(
                !enabled ? getString(R.string.off) : getPreferenceManager().getSharedPreferences()
                        .getString(PrefKeys.SCR_BOLUS_PREDICT, getString(R.string.off)));

        return true;
    }

    /**
     * Sets the summary of the Pills {@link Preference} to the names of saved pills.
     * Format: "pillName1, pillName2, pillName3".
     */
    private void initPills() {
        final String[] pillNames = PrefUtil.getPills(getPreferenceManager().getSharedPreferences());

        if (pillNames == null)
            pillPref.setSummary(getString(R.string.pref_pills_default_summary));
        else {
            final StringBuilder strBuilder = new StringBuilder(pillNames[0]);

            for (int i = 1; i < pillNames.length; ++i)
                strBuilder.append(", ").append(pillNames[i]);

            pillPref.setSummary(strBuilder.toString());
        }
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
        final float hypo = PrefUtil.getHypo(preferences).get(bgUnitIdx);
        final BloodSugar[] targetRange = getTargetRange(preferences);
        final float rngMin = targetRange[0].get(bgUnitIdx), rngMax = targetRange[1].get(bgUnitIdx);
        final float hyper = PrefUtil.getHyper(preferences).get(bgUnitIdx);

        hypoPref.setAttributes(dataType, format, steps, minV, maxV, minV, hypo);
        targetRangePref.setAttributes(dataType, format, steps, minV, maxV, rngMin, rngMax);
        hyperPref.setAttributes(dataType, format, steps, minV, maxV, hyper, minV);
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

        getPreferenceManager().getSharedPreferences().edit()
                .putString(PrefKeys.HYPO, PrefUtil.toJson(hypoBG))
                .putString(PrefKeys.TARGET_RANGE, PrefUtil.toJson(targetRangeBG))
                .putString(PrefKeys.HYPER, PrefUtil.toJson(hyperBG))
                .commit();
    }
}
