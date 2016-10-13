package com.mr_starktastic.sugardays.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.mr_starktastic.sugardays.R;

import java.text.DecimalFormat;

/**
 * Widget that lets users select a minimum and maximum value on a given numerical range.
 * The range value types can be one of Long, Double, Integer, Float, Short, Byte or BigDecimal.
 * Resembles {@link android.widget.SeekBar}.
 */
public class RangeSeekBar extends View {
    protected static final double DEFAULT_NORM_MIN_VAL = 0d, DEFAULT_NORM_MAX_VAL = 100d;

    private static final int INVALID_POINTER_ID = 255;
    private static final float NO_STEP = -1f;
    private static final int DEFAULT_MEASURE_WIDTH = 200;
    private static final int THUMB_ANIM_DURATION = 125;
    private static final AccelerateInterpolator THUMB_ANIM_INTERP = new AccelerateInterpolator();
    private static final AnimatorListenerAdapter ANIM_LISTENER_ADAPTER =
            new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animation.removeAllListeners();
                    super.onAnimationEnd(animation);
                }
            };

    protected int barColor, barHighlightColor, thumbColor;
    protected DecimalFormat numberFormat;
    protected double normMinVal = DEFAULT_NORM_MIN_VAL, normMaxVal = DEFAULT_NORM_MAX_VAL;
    protected float minValue, maxValue, steps;
    protected OnRangeSeekBarChangeListener onValueChangeListener;
    protected RangeSeekBar lowerBoundSeekBar, upperBoundSeekBar;

    private boolean singleThumb;
    private int activePointerId = INVALID_POINTER_ID;
    private int dataType;
    private float barPadding, barHeight, thumbUnpressedSize, thumbPressedSize;
    private float leftThumbSize, rightThumbSize;
    private final ValueAnimator.AnimatorUpdateListener leftThumbAnimUpdateListener = animation -> {
        leftThumbSize = (float) animation.getAnimatedValue();
        invalidate();
    }, rightThumbAnimUpdateListener = animation -> {
        rightThumbSize = (float) animation.getAnimatedValue();
        invalidate();
    };
    private RectF rect;
    private Paint paint;
    private ValueAnimator leftThumbAnimator, rightThumbAnimator;
    private boolean isDragging;
    private Thumb pressedThumb;

    public RangeSeekBar(Context context) {
        this(context, null, 0);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources res = getResources();
        thumbColor = barHighlightColor = ContextCompat.getColor(context, R.color.colorAccent);
        barColor = ContextCompat.getColor(context, R.color.colorOutsideRangeSeekBar);
        thumbUnpressedSize = res.getDimensionPixelSize(R.dimen.unpressed_size);
        barPadding = (thumbPressedSize = res.getDimensionPixelSize(R.dimen.pressed_size)) / 2;
        barHeight = res.getDimensionPixelSize(R.dimen.bar_height);

        final TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);

        try {
            minValue = arr.getFloat(R.styleable.RangeSeekBar_minValue, 0f);
            maxValue = arr.getFloat(R.styleable.RangeSeekBar_maxValue, 100f);
            singleThumb = arr.getBoolean(R.styleable.RangeSeekBar_singleThumb, false);
            steps = arr.getFloat(R.styleable.RangeSeekBar_steps, NO_STEP);
            dataType = arr.getInt(R.styleable.RangeSeekBar_dataType, DataType.INTEGER);
        } finally {
            arr.recycle();
        }

        init();
    }

    private void init() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF(0, (thumbPressedSize - barHeight) / 2,
                getWidth(), (thumbPressedSize + barHeight) / 2);

        pressedThumb = null;

        leftThumbSize = rightThumbSize = thumbUnpressedSize;
        leftThumbAnimator = getThumbAnimator(true, leftThumbSize, leftThumbAnimUpdateListener);
        rightThumbAnimator = getThumbAnimator(true, rightThumbSize, rightThumbAnimUpdateListener);

        numberFormat = new DecimalFormat();
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
    }

    public void setSingleThumb(boolean singleThumb) {
        this.singleThumb = singleThumb;
    }

    public void setSteps(float steps) {
        this.steps = steps;
    }

    public void setDataType(int dataType, DecimalFormat numberFormat) {
        this.dataType = dataType;
        this.numberFormat = numberFormat;
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        onValueChangeListener = listener;
    }

    protected Number getSelectedValue(double normVal) {
        if (steps > 0 && steps <= maxValue / 2) {
            final float stp = steps / (maxValue - minValue) * 100;
            final double mod = normVal % stp;
            normVal -= mod > stp / 2 ? mod - stp : mod;
        } else if (steps != NO_STEP)
            throw new IllegalStateException("Steps out of range: " + steps);

        return formatValue(normalizedToValue(normVal));
    }

    public Number getSelectedMinValue() {
        return getSelectedValue(normMinVal);
    }

    public void setSelectedMinValue(float value) {
        normMinVal = 100 * (value - minValue) / (maxValue - minValue);
    }

    public Number getSelectedMaxValue() {
        return getSelectedValue(normMaxVal);
    }

    public void setSelectedMaxValue(float value) {
        normMaxVal = 100 * (value - minValue) / (maxValue - minValue);
    }

    private ValueAnimator getThumbAnimator(boolean isTouching, float thumbSize,
                                           ValueAnimator.AnimatorUpdateListener listener) {
        final ValueAnimator anim = ValueAnimator.ofFloat(thumbSize,
                isTouching ? thumbPressedSize : thumbUnpressedSize);
        anim.setInterpolator(THUMB_ANIM_INTERP);
        anim.setDuration(THUMB_ANIM_DURATION);
        anim.addUpdateListener(listener);
        anim.addListener(ANIM_LISTENER_ADAPTER);

        return anim;
    }

    private void setupBar(Canvas canvas) {
        rect.right = getWidth() - (rect.left = barPadding);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(barColor);
        paint.setAntiAlias(true);
        canvas.drawRect(rect, paint);
    }

    private void setupHighlightBar(Canvas canvas) {
        rect.left = normalizedToScreen(normMinVal);
        rect.right = normalizedToScreen(normMaxVal);
        paint.setColor(barHighlightColor);
        canvas.drawRect(rect, paint);
    }

    private void setupThumbs(Canvas canvas) {
        paint.setColor(thumbColor);

        if (!singleThumb)
            canvas.drawCircle(normalizedToScreen(normMinVal), barPadding, leftThumbSize / 2, paint);

        canvas.drawCircle(normalizedToScreen(normMaxVal), barPadding, rightThumbSize / 2, paint);
    }

    private void trackTouchEvent(MotionEvent event, boolean isEnding) {
        final float x = event.getX(event.findPointerIndex(activePointerId));

        if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(screenToNormalized(x));

            if (!isEnding) {
                if (!rightThumbAnimator.isRunning()) {
                    rightThumbAnimator =
                            getThumbAnimator(true, rightThumbSize, rightThumbAnimUpdateListener);
                    rightThumbAnimator.start();
                }
            } else {
                rightThumbAnimator.cancel();
                rightThumbAnimator =
                        getThumbAnimator(false, rightThumbSize, rightThumbAnimUpdateListener);
                rightThumbAnimator.start();
            }
        } else if (!singleThumb && Thumb.MIN.equals(pressedThumb)) {
            setNormalizedMinValue(screenToNormalized(x));

            if (!isEnding) {
                if (!leftThumbAnimator.isRunning()) {
                    leftThumbAnimator =
                            getThumbAnimator(true, leftThumbSize, leftThumbAnimUpdateListener);
                    leftThumbAnimator.start();
                }
            } else {
                leftThumbAnimator.cancel();
                leftThumbAnimator =
                        getThumbAnimator(false, leftThumbSize, leftThumbAnimUpdateListener);
                leftThumbAnimator.start();
            }
        }
    }

    private Thumb evalPressedThumb(float touchX) {
        final boolean minThumbPressed = isInThumbRange(touchX, normMinVal);
        final boolean maxThumbPressed = isInThumbRange(touchX, normMaxVal);

        return minThumbPressed && maxThumbPressed ? // Chooses the thumb with more room to drag
                touchX / getWidth() > 0.5f ? Thumb.MIN : Thumb.MAX :
                minThumbPressed ? Thumb.MIN : maxThumbPressed ? Thumb.MAX : null;
    }

    private boolean isInThumbRange(float x, double normalizedThumbValue) {
        return Math.abs(x - normalizedToScreen(normalizedThumbValue)) <= thumbPressedSize * 2;
    }

    private void onStartTrackingTouch() {
        isDragging = true;
    }

    private void onStopTrackingTouch() {
        isDragging = false;
    }

    private float normalizedToScreen(double normalizedCoord) {
        return (float) (barPadding + normalizedCoord / 100f * (getWidth() - 2 * barPadding));
    }

    private double screenToNormalized(float screenCoord) {
        final int width = getWidth();

        if (width <= thumbPressedSize)
            return 0d; // Prevents division by zero

        return Math.min(
                100d,
                Math.max(0d, (screenCoord - barPadding) / (width - 2 * barPadding) * 100d));
    }

    public double getNormMin() {
        return normMinVal;
    }

    public double getNormMax() {
        return normMaxVal;
    }

    public void setLowerBoundSeekBar(RangeSeekBar lowerBoundSeekBar) {
        this.lowerBoundSeekBar = lowerBoundSeekBar;
    }

    public void setUpperBoundSeekBar(RangeSeekBar upperBoundSeekBar) {
        this.upperBoundSeekBar = upperBoundSeekBar;
    }

    public void setNormalizedMinValue(double value) {
        if (lowerBoundSeekBar == null || value > lowerBoundSeekBar.getNormMax())
            normMinVal = Math.max(0d, Math.min(100d, Math.min(value, normMaxVal)));
        else normMinVal = lowerBoundSeekBar.getNormMax();
    }

    public void setNormalizedMaxValue(double value) {
        if (upperBoundSeekBar == null || value < upperBoundSeekBar.getNormMin())
            normMaxVal = Math.max(0d, Math.min(100d, Math.max(value, normMinVal)));
        else normMaxVal = upperBoundSeekBar.getNormMin();
    }

    private double normalizedToValue(double normalized) {
        return normalized / 100 * (maxValue - minValue) + minValue;
    }

    private void attemptClaimDrag() {
        if (getParent() != null)
            getParent().requestDisallowInterceptTouchEvent(true);
    }

    private <T extends Number> Number formatValue(T value) throws IllegalArgumentException {
        switch (dataType) {
            case DataType.INTEGER:
                return Math.round(value.doubleValue());

            case DataType.FLOAT:
                return value.floatValue();

            case DataType.LONG:
                return value.longValue();

            case DataType.DOUBLE:
                return value;

            case DataType.SHORT:
                return value.shortValue();

            case DataType.BYTE:
                return value.byteValue();

            default:
                throw new IllegalArgumentException(
                        "Number class '" + value.getClass().getName() + "' is not supported");
        }
    }

    public void notifyValueChange() {
        if (onValueChangeListener != null)
            onValueChangeListener.valueChanged(getSelectedMinValue(), getSelectedMaxValue());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        setupBar(canvas);
        setupHighlightBar(canvas);
        setupThumbs(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = DEFAULT_MEASURE_WIDTH, height = (int) thumbPressedSize;

        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec))
            width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec))
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));

        setMeasuredDimension(width, height);
    }

    /**
     * Handles thumb selection and movement. Notifies listener callback on certain events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(event.getPointerCount() - 1);
                pressedThumb = evalPressedThumb(
                        event.getX(event.findPointerIndex(activePointerId)));

                if (pressedThumb == null)
                    return super.onTouchEvent(event);

                setPressed(true);
                invalidate();
                onStartTrackingTouch();
                trackTouchEvent(event, false);
                attemptClaimDrag();
                break;

            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {
                    if (isDragging)
                        trackTouchEvent(event, false);

                    notifyValueChange();
                }
                break;

            case MotionEvent.ACTION_UP:
                trackTouchEvent(event, true);
                onStopTrackingTouch();
                setPressed(false);
                pressedThumb = null;
                invalidate();
                notifyValueChange();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (pressedThumb.equals(Thumb.MIN)) {
                    leftThumbAnimator.cancel();
                    leftThumbAnimator =
                            getThumbAnimator(false, leftThumbSize, leftThumbAnimUpdateListener);
                    leftThumbAnimator.start();
                } else if (pressedThumb.equals(Thumb.MAX)) {
                    rightThumbAnimator.cancel();
                    rightThumbAnimator =
                            getThumbAnimator(false, rightThumbSize, rightThumbAnimUpdateListener);
                    rightThumbAnimator.start();
                }

                if (isDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }

                invalidate();
                break;
        }

        return true;
    }

    public String toString(Number minValue, Number maxValue) {
        return numberFormat.format(minValue) + " - " + numberFormat.format(maxValue);
    }

    protected enum Thumb {MIN, MAX}

    @FunctionalInterface
    public interface OnRangeSeekBarChangeListener {
        void valueChanged(Number minValue, Number maxValue);
    }

    public static final class DataType {
        @SuppressWarnings("all")
        public static final int LONG = 0, DOUBLE = 1, INTEGER = 2, FLOAT = 3, SHORT = 4, BYTE = 5;
    }
}
