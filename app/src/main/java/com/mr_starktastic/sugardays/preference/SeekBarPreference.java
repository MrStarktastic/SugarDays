package com.mr_starktastic.sugardays.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.widget.RangeSeekBar;

import java.text.DecimalFormat;

/**
 * Preference with a custom layout where the title is at the start, summary is at the end
 * and the seekBar frame is below them.
 */
public class SeekBarPreference extends Preference
        implements RangeSeekBar.OnRangeSeekBarChangeListener {
    private int summaryTextColor;
    private TextView summaryText;
    private int dataType;
    private DecimalFormat format;
    private float steps, min, max, selectedMin, selectedMax;
    private float normMin = -1, normMax = -1;
    private RangeSeekBar seekBar;

    @SuppressWarnings("unused")
    public SeekBarPreference(Context context) {
        super(context);
        init(context, null);
    }

    @SuppressWarnings("unused")
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @SuppressWarnings("unused")
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public SeekBarPreference(Context context, int widgetResId, int summaryTextColor,
                             int dataType, DecimalFormat format, float steps,
                             float min, float max, float selectedMin, float selectedMax) {
        super(context);
        setLayoutResource(R.layout.preference_wide_widget);
        setWidgetLayoutResource(widgetResId);
        setAttributes(dataType, format, steps, min, max, selectedMin, selectedMax);
        this.summaryTextColor = summaryTextColor;
    }

    /**
     * Sets the custom preference layout and obtains the TextView color for the summary
     *
     * @param attrs Set of attributes
     */
    private void init(Context context, AttributeSet attrs) {
        setLayoutResource(R.layout.preference_wide_widget);
        final TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.SeekBarPreference);

        try {
            summaryTextColor = arr.getColor(R.styleable.SeekBarPreference_summaryTextColor, 0);
        } finally {
            arr.recycle();
        }
    }

    public void setAttributes(int dataType, DecimalFormat format, float steps,
                              float min, float max, float selectedMin, float selectedMax) {
        this.dataType = dataType;
        this.format = format;
        this.steps = steps;
        this.min = min;
        this.max = max;
        this.selectedMin = selectedMin;
        this.selectedMax = selectedMax;

        if (seekBar != null)
            initSeekBar();
    }

    public void setLowerBound(SeekBarPreference lowerBound) {
        seekBar.setLowerBoundSeekBar(lowerBound.seekBar);
    }

    public void setUpperBound(SeekBarPreference upperBound) {
        seekBar.setUpperBoundSeekBar(upperBound.seekBar);
    }

    private void initSeekBar() {
        seekBar.setDataType(dataType, format);
        seekBar.setSteps(steps);
        seekBar.setMinValue(min);
        seekBar.setMaxValue(max);

        if (normMin != -1)
            seekBar.setNormalizedMinValue(normMin);
        else if (normMax != -1)
            seekBar.setNormalizedMaxValue(normMax);
        else {
            seekBar.setSelectedMinValue(selectedMin);
            seekBar.setSelectedMaxValue(selectedMax);
        }

        seekBar.invalidate();
        seekBar.notifyValueChange();
    }

    @SuppressWarnings("unused")
    public float getNormalizedMinValue() {
        return (float) seekBar.getNormMin();
    }

    @SuppressWarnings("unused")
    public void setNormalizedMinValue(float normMin) {
        this.normMin = normMin;
    }

    @SuppressWarnings("unused")
    public float getNormalizedMaxValue() {
        return (float) seekBar.getNormMax();
    }

    @SuppressWarnings("unused")
    public void setNormalizedMaxValue(float normMax) {
        this.normMax = normMax;
    }

    public Number getSelectedMinValue() {
        return seekBar.getSelectedMinValue();
    }

    public Number getSelectedMaxValue() {
        return seekBar.getSelectedMaxValue();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        (summaryText = (TextView) holder.findViewById(android.R.id.summary))
                .setTextColor(summaryTextColor);
        (seekBar = (RangeSeekBar) ((ViewGroup) holder.findViewById(android.R.id.widget_frame))
                .getChildAt(0)).setOnRangeSeekBarChangeListener(this);

        initSeekBar();
    }

    @Override
    public void valueChanged(Number minValue, Number maxValue) {
        summaryText.setText(seekBar.toString(minValue, maxValue));
    }
}
