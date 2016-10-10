package com.mr_starktastic.sugardays.preference;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;

import com.mr_starktastic.sugardays.R;

/**
 * A toolbar-like strip {@link Preference} which includes a {@link SwitchCompat}.
 * Designed to be held at the top of the {@link android.support.v7.preference.PreferenceScreen}.
 * Serves as a toggle for the entire screen and the preferences it represents.
 * Contains a {@link android.widget.TextView} which holds an elaborate description for what the
 * screen represents and shows up only when the switch is not checked.
 */
public class SwitchStripPreference extends Preference {
    private SwitchCompat switchCompat;
    private String offSummary, onSummary;
    private OnCheckedChangeListener listener;

    @SuppressWarnings("unused")
    public SwitchStripPreference(Context context) {
        super(context, null, 0);
    }

    @SuppressWarnings("unused")
    public SwitchStripPreference(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    @SuppressWarnings("unused")
    public SwitchStripPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayoutResource(R.layout.preference_switch_strip);
    }

    public void setSummaries(String offSummary, String onSummary) {
        this.onSummary = onSummary;
        this.offSummary = offSummary;
        setSummary(getSharedPreferences().getBoolean(getKey(), false) ? onSummary : offSummary);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        switchCompat = (SwitchCompat) holder.findViewById(R.id.switch_compat);
        switchCompat.setChecked(getSharedPreferences().getBoolean(getKey(), false));

        switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setTitle(R.string.on);
                setSummary(onSummary);
            } else {
                setTitle(R.string.off);
                setSummary(offSummary);
            }
            if (listener != null)
                listener.onCheckedChangeListener(isChecked);

            getSharedPreferences().edit()
                    .putBoolean(getKey(), isChecked)
                    .putString(getPreferenceManager().getPreferenceScreen().getKey(),
                            getTitle().toString())
                    .commit();
        });
    }

    public boolean isChecked() {
        return switchCompat.isChecked();
    }

    public interface OnCheckedChangeListener {
        void onCheckedChangeListener(boolean isChecked);
    }
}
