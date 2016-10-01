package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.util.AttributeSet;

/**
 * A single thumb {@link RangeSeekBar} widget.
 */
public class SingleSeekBar extends RangeSeekBar {
    public SingleSeekBar(Context context) {
        super(context);
        setSingleThumb(true);
    }

    public SingleSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSingleThumb(true);
    }

    public SingleSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSingleThumb(true);
    }

    @Override
    public String toString(Number minValue, Number maxValue) {
        return numberFormat.format(maxValue);
    }
}
