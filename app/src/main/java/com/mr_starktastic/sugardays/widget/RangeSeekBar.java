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

/**
 * Widget that lets users select a minimum and maximum value on a given numerical range.
 * The range value types can be one of Long, Double, Integer, Float, Short, Byte or BigDecimal.
 * Resembles the stock SeekBar widget.
 */
public class RangeSeekBar extends View {
    private static final int INVALID_POINTER_ID = 255;
    private static final float NO_STEP = -1f;
    private static final double DEFAULT_NORM_MIN_VAL = 0d, DEFAULT_NORM_MAX_VAL = 100d;
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
    private OnRangeSeekBarChangeListener onValueChangeListener;
    private float absoluteMinValue, absoluteMaxValue, minValue, maxValue, steps;
    private boolean singleThumb;
    private int activePointerId = INVALID_POINTER_ID;
    private int dataType;
    private int barColor, barHighlightColor, thumbColor;
    private float barPadding, barHeight, thumbUnpressedSize, thumbPressedSize;
    private float leftThumbSize, rightThumbSize;
    private final ValueAnimator.AnimatorUpdateListener leftThumbAnimUpdateListener = animation -> {
        leftThumbSize = (float) animation.getAnimatedValue();
        invalidate();
    }, rightThumbAnimUpdateListener = animation -> {
        rightThumbSize = (float) animation.getAnimatedValue();
        invalidate();
    };
    private double normMinVal = DEFAULT_NORM_MIN_VAL, normMaxVal = DEFAULT_NORM_MAX_VAL;
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

        final TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar);

        try {
            singleThumb = array.getBoolean(R.styleable.RangeSeekBar_singleThumb, false);
            steps = array.getFloat(R.styleable.RangeSeekBar_steps, NO_STEP);
            dataType = array.getInt(R.styleable.RangeSeekBar_dataType, DataType.INTEGER);
        } finally {
            array.recycle();
        }

        init();
    }

    private void init() {
        absoluteMaxValue = maxValue;
        absoluteMinValue = minValue;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rect = new RectF(0, (thumbPressedSize - barHeight) / 2,
                getWidth(), (thumbPressedSize + barHeight) / 2);

        pressedThumb = null;

        leftThumbSize = rightThumbSize = thumbUnpressedSize;
        leftThumbAnimator = getThumbAnimator(true, leftThumbSize, leftThumbAnimUpdateListener);
        rightThumbAnimator = getThumbAnimator(true, rightThumbSize, rightThumbAnimUpdateListener);
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
        this.absoluteMinValue = minValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
        this.absoluteMaxValue = maxValue;
    }

    public void setSingleThumb(boolean singleThumb) {
        this.singleThumb = singleThumb;
    }

    public void setSteps(float steps) {
        this.steps = steps;
    }

    public void setDataType(int dataType) {
        this.dataType = dataType;
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        onValueChangeListener = listener;
        notifyValueChange();
    }

    private Number getSelectedValue(double normVal) {
        if (steps > 0 && steps <= absoluteMaxValue / 2) {
            final float stp = steps / (absoluteMaxValue - absoluteMinValue) * 100;
            final double mod = normVal % stp;
            normVal -= mod > stp / 2 ? mod - stp : mod;
        } else if (steps != NO_STEP)
            throw new IllegalStateException("Steps out of range: " + steps);

        return formatValue(normalizedToValue(normVal));
    }

    public Number getSelectedMinValue() {
        return getSelectedValue(normMinVal);
    }

    public Number getSelectedMaxValue() {
        return getSelectedValue(normMaxVal);
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

    private void setNormalizedMinValue(double value) {
        normMinVal = Math.max(0d, Math.min(100d, Math.min(value, normMaxVal)));
        invalidate();
    }

    private void setNormalizedMaxValue(double value) {
        normMaxVal = Math.max(0d, Math.min(100d, Math.max(value, normMinVal)));
        invalidate();
    }

    private double normalizedToValue(double normalized) {
        return normalized / 100 * (maxValue - minValue) + minValue;
    }

    private void attemptClaimDrag() {
        if (getParent() != null)
            getParent().requestDisallowInterceptTouchEvent(true);
    }

    private <T extends Number> Number formatValue(T value) throws IllegalArgumentException {
        final Double v = (Double) value;

        switch (dataType) {
            case DataType.LONG:
                return v.longValue();

            case DataType.DOUBLE:
                return v;

            case DataType.INTEGER:
                return Math.round(v);

            case DataType.FLOAT:
                return v.floatValue();

            case DataType.SHORT:
                return v.shortValue();

            case DataType.BYTE:
                return v.byteValue();

            default:
                throw new IllegalArgumentException(
                        "Number class '" + value.getClass().getName() + "' is not supported");
        }
    }

    private void notifyValueChange() {
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

    protected enum Thumb {MIN, MAX}

    @FunctionalInterface
    public interface OnRangeSeekBarChangeListener {
        void valueChanged(Number minValue, Number maxValue);
    }

    public static final class DataType {
        static final int LONG = 0, DOUBLE = 1, INTEGER = 2, FLOAT = 3, SHORT = 4, BYTE = 5;
    }
}
