package com.mr_starktastic.sugardays;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mr_starktastic.sugardays.widget.RangeSeekBar;

/**
 * Preference with a custom layout where the title is at the start, summary is at the end
 * and the seekBar frame is below them
 */
public class SeekBarPreference extends Preference implements RangeSeekBar.OnRangeSeekBarChangeListener {
    private int summaryTextColor;
    private RangeSeekBar seekBar;

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SeekBarPreference(Context context) {
        super(context);
        init(null);
    }

    /**
     * Sets the custom preference layout and obtains the TextView color for the summary
     *
     * @param attrs Set of attributes
     */
    private void init(AttributeSet attrs) {
        setLayoutResource(R.layout.preference_custom);

        if (attrs != null) {
            final TypedArray styledAttrs = getContext().obtainStyledAttributes(attrs,
                    R.styleable.SeekBarPreference, 0, 0);
            summaryTextColor = styledAttrs
                    .getColor(R.styleable.SeekBarPreference_summaryTextColor, 0);
            styledAttrs.recycle();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ((TextView) holder.findViewById(android.R.id.summary)).setTextColor(summaryTextColor);
        seekBar = (RangeSeekBar) ((ViewGroup) holder.findViewById(android.R.id.widget_frame))
                .getChildAt(0);
        seekBar.setOnRangeSeekBarChangeListener(this);
    }

    @Override
    public void valueChanged(Number minValue, Number maxValue) {
        //Log.d("Min", String.valueOf(minValue));
        //Log.d("Max", String.valueOf(maxValue));
    }
}
