package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.mr_starktastic.sugardays.R;

/**
 * Same as {@link SingleSeekBar} except the whole bar is gray
 */
public class PointSeekBar extends SingleSeekBar {
    public PointSeekBar(Context context) {
        super(context);
        setBarHighlightColor(context);
    }

    public PointSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setBarHighlightColor(context);
    }

    public PointSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setBarHighlightColor(context);
    }

    private void setBarHighlightColor(Context context) {
        barHighlightColor = ContextCompat.getColor(context, R.color.colorOutsideRangeSeekBar);
    }
}
