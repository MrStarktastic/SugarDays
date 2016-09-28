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
/*
    @Override
    public Number getSelectedMinValue() {
        return normalizedToValue(1 - normalizedMaxValue);
    }*/
}
