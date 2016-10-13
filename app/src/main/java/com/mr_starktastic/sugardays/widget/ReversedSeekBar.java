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

    public Number getSelectedMinValue() {
        return getSelectedValue(getNormMin());
    }

    @Override
    public void setSelectedMinValue(float value) {
        super.setSelectedMaxValue(maxValue - value + minValue);
    }

    @Override
    public Number getSelectedMaxValue() {
        return getSelectedValue(getNormMin());
    }

    @Override
    public void setSelectedMaxValue(float value) {
        super.setSelectedMinValue(value);
    }

    @Override
    public double getNormMin() {
        return DEFAULT_NORM_MAX_VAL - normMaxVal;
    }

    @Override
    public void setNormalizedMaxValue(double value) {
        if (lowerBoundSeekBar == null ||
                DEFAULT_NORM_MAX_VAL - value > lowerBoundSeekBar.getNormMax())
            normMaxVal = Math.max(0d, Math.min(100d, value));
        else normMaxVal = DEFAULT_NORM_MAX_VAL - lowerBoundSeekBar.getNormMax();
    }

    @Override
    public void notifyValueChange() {
        if (onValueChangeListener != null)
            onValueChangeListener.valueChanged(getSelectedMinValue(), maxValue);
    }

    @Override
    public String toString(Number minValue, Number maxValue) {
        return super.toString(maxValue, minValue);
    }
}
