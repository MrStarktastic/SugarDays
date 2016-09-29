package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ReversedSeekBar extends SingleSeekBar {
    private static final int INVERTED_SCALE_X = -1, SCALE_Y = 1;

    public ReversedSeekBar(Context context) {
        super(context);
    }

    public ReversedSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReversedSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.scale(INVERTED_SCALE_X, SCALE_Y, getWidth() / 2, getHeight() / 2);
        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        event.setLocation(getWidth() - event.getX(), event.getY());
        return super.onTouchEvent(event);
    }

    @Override
    public Number getSelectedMaxValue() {
        return getSelectedValue(getNormMin());
    }

    @Override
    protected double getNormMin() {
        return DEFAULT_NORM_MAX_VAL - normMaxVal;
    }

    @Override
    protected void setNormalizedMaxValue(double val) {
        if (lowerBoundSeekBar == null || DEFAULT_NORM_MAX_VAL - val > lowerBoundSeekBar.normMaxVal)
            normMaxVal = Math.max(0d, Math.min(100d, val));
        else normMaxVal = DEFAULT_NORM_MAX_VAL - lowerBoundSeekBar.normMaxVal;
    }

    @Override
    protected void notifyValueChange() {
        if (onValueChangeListener != null)
            onValueChangeListener.valueChanged(getSelectedMaxValue(), absoluteMaxValue);
    }

    @Override
    public String toString(Number minValue, Number maxValue) {
        return super.toString(maxValue, minValue);
    }
}
