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
public class SeekBarPreference extends Preference
        implements RangeSeekBar.OnRangeSeekBarChangeListener {
    private int summaryTextColor;

    private TextView summaryText;
    private RangeSeekBar seekBar;

    public SeekBarPreference(Context context) {
        super(context);
        init(context, null);
    }

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Sets the custom preference layout and obtains the TextView color for the summary
     *
     * @param attrs Set of attributes
     */
    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.preference_custom);
        final TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);

        try {
            summaryTextColor = arr.getColor(R.styleable.SeekBarPreference_summaryTextColor, 0);
        } finally {
            arr.recycle();
        }
    }

    public void setLowerBound(SeekBarPreference lowerBound) {
        seekBar.setLowerBoundSeekBar(lowerBound.seekBar);
    }

    public void setUpperBound(SeekBarPreference upperBound) {
        seekBar.setUpperBoundSeekBar(upperBound.seekBar);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        (summaryText = (TextView) holder.findViewById(android.R.id.summary))
                .setTextColor(summaryTextColor);
        (seekBar = (RangeSeekBar) ((ViewGroup) holder.findViewById(android.R.id.widget_frame))
                .getChildAt(0)).setOnRangeSeekBarChangeListener(this);
    }

    @Override
    public void valueChanged(Number minValue, Number maxValue) {
        summaryText.setText(seekBar.toString(minValue, maxValue));
    }
}
