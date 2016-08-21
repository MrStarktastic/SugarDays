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
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;

import com.mr_starktastic.sugardays.R;

import java.math.BigDecimal;

/**
 * Widget that lets users select a minimum and maximum value on a given numerical range.
 * The range value types can be one of Long, Double, Integer, Float, Short, Byte or BigDecimal.
 */
public class RangeSeekBar<T extends Number> extends View {
    // Localized constants from MotionEvent for compatibility with API < 8 "Froyo"
    private static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;
    private static final Integer DEFAULT_MINIMUM = 0, DEFAULT_MAXIMUM = 100;
    private static final int THUMB_ANIM_DURATION = 125;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float thumbUnpressedRadius, thumbPressedRadius, minThumbRadius, maxThumbRadius;
    private ValueAnimator minThumbAnimator, maxThumbAnimator;
    private T absoluteMinValue, absoluteMaxValue;
    private NumberType numberType;
    private double absMinValuePrim, absMaxValuePrim;
    private double normalizedMinValue = 0d, normalizedMaxValue = 1d;
    private Thumb pressedThumb = null;
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener<T> listener;
    private float downMotionX;
    private int activePointerId = 255; // Instantiated with an invalid ID
    private int scaledTouchSlop;
    private boolean isDragging;
    private RectF rect;
    private boolean singleThumb;
    private boolean alwaysActive;
    private float internalPad;
    private int activeColor;
    private int defaultColor;

    public RangeSeekBar(Context context) {
        super(context);
        init(context, null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @SuppressWarnings("unchecked")
    private T extractNumValFromAttrs(TypedArray a, int attribute, int defaultValue) {
        final TypedValue tv = a.peekValue(attribute);

        if (tv == null)
            return (T) Integer.valueOf(defaultValue);

        if (tv.type == TypedValue.TYPE_FLOAT)
            return (T) Float.valueOf(a.getFloat(attribute, defaultValue));

        return (T) Integer.valueOf(a.getInteger(attribute, defaultValue));
    }

    private void init(Context c, AttributeSet attrs) {
        final Resources res = getResources();
        int barHeight;

        if (attrs == null) {
            setRangeToDefaultValues();
            alwaysActive = true;
            singleThumb = false;
            internalPad = res.getDimensionPixelSize(R.dimen.normal_margin);
            barHeight = res.getDimensionPixelSize(R.dimen.bar_height);
            activeColor = ContextCompat.getColor(c, R.color.colorAccent);
            defaultColor = ContextCompat.getColor(c, R.color.colorOutsideRangeSeekBar);
            thumbUnpressedRadius = res.getDimensionPixelSize(R.dimen.unpressed_radius);
            thumbPressedRadius = res.getDimensionPixelSize(R.dimen.pressed_radius);
        } else {
            final TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.RangeSeekBar, 0, 0);

            try {
                setRangeValues(
                        extractNumValFromAttrs(a, R.styleable.RangeSeekBar_absoluteMinValue,
                                DEFAULT_MINIMUM),
                        extractNumValFromAttrs(a, R.styleable.RangeSeekBar_absoluteMaxValue,
                                DEFAULT_MAXIMUM)
                );
                alwaysActive = a.getBoolean(R.styleable.RangeSeekBar_alwaysActive, true);
                singleThumb = a.getBoolean(R.styleable.RangeSeekBar_singleThumb, false);
                internalPad = a.getDimensionPixelSize(R.styleable.RangeSeekBar_internalPadding,
                        res.getDimensionPixelSize(R.dimen.normal_margin));
                barHeight = a.getDimensionPixelSize(R.styleable.RangeSeekBar_barHeight,
                        res.getDimensionPixelSize(R.dimen.bar_height));
                activeColor = a.getColor(R.styleable.RangeSeekBar_activeColor,
                        ContextCompat.getColor(c, R.color.colorAccent));
                defaultColor = a.getColor(R.styleable.RangeSeekBar_defaultColor,
                        ContextCompat.getColor(c, R.color.colorOutsideRangeSeekBar));
                thumbUnpressedRadius = a.getDimension(R.styleable.RangeSeekBar_thumbUnpressedRadius,
                        res.getDimensionPixelSize(R.dimen.unpressed_radius));
                thumbPressedRadius = a.getDimension(R.styleable.RangeSeekBar_thumbPressedRadius,
                        res.getDimensionPixelSize(R.dimen.pressed_radius));
            } finally {
                a.recycle();
            }
        }

        minThumbRadius = thumbUnpressedRadius;
        maxThumbRadius = thumbUnpressedRadius;
        minThumbAnimator = getMinThumbAnimator(true);
        maxThumbAnimator = getMaxThumbAnimator(true);

        setValuePrimAndNumberType();

        rect = new RectF(internalPad,
                thumbPressedRadius - barHeight / 2,
                getWidth() - internalPad,
                thumbPressedRadius + barHeight / 2);

        setFocusable(true);
        setFocusableInTouchMode(true);
        scaledTouchSlop = ViewConfiguration.get(c).getScaledTouchSlop();
    }

    protected void setSingleThumb(boolean singleThumb) {
        this.singleThumb = singleThumb;
    }

    public void setRangeValues(T minValue, T maxValue) {
        this.absoluteMinValue = minValue;
        this.absoluteMaxValue = maxValue;
        setValuePrimAndNumberType();
    }

    @SuppressWarnings("unchecked")
    private void setRangeToDefaultValues() {
        this.absoluteMinValue = (T) DEFAULT_MINIMUM;
        this.absoluteMaxValue = (T) DEFAULT_MAXIMUM;
        setValuePrimAndNumberType();
    }

    private void setValuePrimAndNumberType() {
        absMinValuePrim = absoluteMinValue.doubleValue();
        absMaxValuePrim = absoluteMaxValue.doubleValue();
        numberType = NumberType.fromNumber(absoluteMinValue);
    }

    @SuppressWarnings("unused")
    public void resetSelectedValues() {
        setSelectedMinValue(absoluteMinValue);
        setSelectedMaxValue(absoluteMaxValue);
    }

    @SuppressWarnings("unused")
    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    /**
     * Should the widget notify the listener callback while the user is still dragging a thumb?
     * Default is false.
     */
    @SuppressWarnings("unused")
    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    /**
     * Returns the absolute minimum value of the range that has been set at construction time.
     *
     * @return The absolute minimum value of the range.
     */
    public T getAbsoluteMinValue() {
        return absoluteMinValue;
    }

    /**
     * Returns the absolute maximum value of the range that has been set at construction time.
     *
     * @return The absolute maximum value of the range.
     */
    public T getAbsoluteMaxValue() {
        return absoluteMaxValue;
    }

    /**
     * Returns the currently selected min value.
     *
     * @return The currently selected min value.
     */
    public T getSelectedMinValue() {
        return normalizedToValue(normalizedMinValue);
    }

    /**
     * Sets the currently selected minimum value. The widget will be invalidated and redrawn.
     *
     * @param value The Number value to set the minimum value to.
     *              Will be clamped to given absolute minimum/maximum range.
     */
    public void setSelectedMinValue(T value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero when normalizing
        if (absMaxValuePrim == absMinValuePrim)
            setNormalizedMinValue(0d);
        else
            setNormalizedMinValue(valueToNormalized(value));
    }

    /**
     * Returns the currently selected max value.
     *
     * @return The currently selected max value.
     */
    public T getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValue);
    }

    /**
     * Sets the currently selected maximum value. The widget will be invalidated and redrawn.
     *
     * @param value The Number value to set the maximum value to.
     *              Will be clamped to given absolute minimum/maximum range.
     */
    public void setSelectedMaxValue(T value) {
        // in case absoluteMinValue == absoluteMaxValue, avoid division by zero when normalizing.
        if (absMaxValuePrim == absMinValuePrim)
            setNormalizedMaxValue(1d);
        else
            setNormalizedMaxValue(valueToNormalized(value));
    }

    /**
     * Registers given listener callback to notify about changed selected values.
     *
     * @param listener The listener to notify about changed selected values.
     */
    @SuppressWarnings("unused")
    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener<T> listener) {
        this.listener = listener;
    }

    private ValueAnimator getMinThumbAnimator(boolean isTouching) {
        final ValueAnimator anim = ValueAnimator.ofFloat(minThumbRadius,
                isTouching ? thumbPressedRadius : thumbUnpressedRadius);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(THUMB_ANIM_DURATION);

        anim.addUpdateListener(animation -> {
            minThumbRadius = (float) anim.getAnimatedValue();
            invalidate();
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                anim.removeAllListeners();
                super.onAnimationEnd(animation);
            }
        });

        return anim;
    }

    private ValueAnimator getMaxThumbAnimator(boolean isTouching) {
        final ValueAnimator anim = ValueAnimator.ofFloat(maxThumbRadius,
                isTouching ? thumbPressedRadius : thumbUnpressedRadius);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(THUMB_ANIM_DURATION);

        anim.addUpdateListener(animation -> {
            maxThumbRadius = (float) anim.getAnimatedValue();
            invalidate();
        });

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                anim.removeAllListeners();
                super.onAnimationEnd(animation);
            }
        });

        return anim;
    }

    /**
     * Handles thumb selection and movement. Notifies listener callback on certain events.
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!isEnabled())
            return false;

        switch (e.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // Remember where the motion event started
                activePointerId = e.getPointerId(e.getPointerCount() - 1);
                downMotionX = e.getX(e.findPointerIndex(activePointerId));
                pressedThumb = evalPressedThumb(downMotionX);
                break;

            case MotionEvent.ACTION_MOVE:
                if (pressedThumb == null)
                    break;

                if (isDragging)
                    trackTouchEvent(e, false);
                else if (Math.abs(e.getX(e.findPointerIndex(
                        activePointerId)) - downMotionX) > scaledTouchSlop) {
                    // Scroll to follow the motion event
                    setPressed(true);
                    invalidate();
                    onStartTrackingTouch();
                    trackTouchEvent(e, false);
                    attemptClaimDrag();
                }

                if (notifyWhileDragging && listener != null)
                    listener.onRangeSeekBarValuesChanged(this,
                            getSelectedMinValue(), getSelectedMaxValue());

                break;

            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    trackTouchEvent(e, true);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold
                    // should be interpreted as a tap-seek to that location
                    onStartTrackingTouch();
                    trackTouchEvent(e, true);
                    onStopTrackingTouch();
                }

                pressedThumb = null;
                invalidate();

                if (listener != null)
                    listener.onRangeSeekBarValuesChanged(this,
                            getSelectedMinValue(), getSelectedMaxValue());

                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = e.getPointerCount() - 1;
                downMotionX = e.getX(index);
                activePointerId = e.getPointerId(index);
                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(e);
                invalidate();
                break;

            case MotionEvent.ACTION_CANCEL:
                if (Thumb.MIN.equals(pressedThumb)) {
                    minThumbAnimator.cancel();
                    minThumbAnimator = getMinThumbAnimator(false);
                    minThumbAnimator.start();
                } else if (Thumb.MAX.equals(pressedThumb)) {
                    maxThumbAnimator.cancel();
                    maxThumbAnimator = getMaxThumbAnimator(false);
                    maxThumbAnimator.start();
                }

                if (isDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                    invalidate();
                }

                break;
        }

        return true;
    }

    private void onSecondaryPointerUp(MotionEvent e) {
        final int pointerIndex =
                (e.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;

        if (e.getPointerId(pointerIndex) == activePointerId) {
            // This was our active pointer going up. Choose
            // a new active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            downMotionX = e.getX(newPointerIndex);
            activePointerId = e.getPointerId(newPointerIndex);
        }
    }

    private void trackTouchEvent(MotionEvent e, boolean isEnding) {
        final double normX = screenToNormalized(e.getX(e.findPointerIndex(activePointerId)));

        if (!singleThumb && Thumb.MIN.equals(pressedThumb)) {
            setNormalizedMinValue(normX);

            if (!isEnding) {
                if (!minThumbAnimator.isRunning()) {
                    minThumbAnimator = getMinThumbAnimator(true);
                    minThumbAnimator.start();
                }
            } else {
                minThumbAnimator.cancel();
                minThumbAnimator = getMinThumbAnimator(false);
                minThumbAnimator.start();
            }
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(normX);

            if (!isEnding) {
                if (!maxThumbAnimator.isRunning()) {
                    maxThumbAnimator = getMaxThumbAnimator(true);
                    maxThumbAnimator.start();
                }
            } else {
                maxThumbAnimator.cancel();
                maxThumbAnimator = getMaxThumbAnimator(false);
                maxThumbAnimator.start();
            }
        }
    }

    /**
     * Tries to claim the user's drag motion;
     * requests disallowing any ancestors from stealing events in the drag.
     */
    private void attemptClaimDrag() {
        if (getParent() != null)
            getParent().requestDisallowInterceptTouchEvent(true);
    }

    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        isDragging = true;
    }

    /**
     * This is called when the user either releases his touch or the touch is canceled.
     */
    void onStopTrackingTouch() {
        isDragging = false;
    }

    /**
     * Ensures correct size of the widget.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 200, height = (int) thumbPressedRadius * 2;

        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec))
            width = MeasureSpec.getSize(widthMeasureSpec);

        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec))
            height = Math.min(height, MeasureSpec.getSize(heightMeasureSpec));

        setMeasuredDimension(width, height);
    }

    /**
     * Draws the widget on the given canvas.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(defaultColor);
        paint.setAntiAlias(true);

        // draw seek bar background line
        rect.left = internalPad;
        rect.right = getWidth() - internalPad;
        canvas.drawRect(rect, paint);

        final boolean selectedValuesAreDefault =
                getSelectedMinValue().equals(getAbsoluteMinValue()) &&
                        getSelectedMaxValue().equals(getAbsoluteMaxValue());

        final int colorToUseForButtonsAndHighlightedLine = !alwaysActive &&
                selectedValuesAreDefault ?
                defaultColor : // default values
                activeColor;   // non default, filter is active

        // draw seek bar active range line
        rect.left = normalizedToScreen(normalizedMinValue);
        rect.right = normalizedToScreen(normalizedMaxValue);

        paint.setColor(colorToUseForButtonsAndHighlightedLine);
        canvas.drawRect(rect, paint);

        // draw minimum thumb if not a single thumb control
        if (!singleThumb)
            drawThumb(normalizedToScreen(normalizedMinValue), minThumbRadius, canvas);

        drawThumb(normalizedToScreen(normalizedMaxValue), maxThumbRadius, canvas);
    }

    /**
     * Overridden to save instance state when device orientation changes.
     * This method is called automatically if you assign an id to the RangeSeekBar widget using the
     * {@link #setId(int)} method.
     * Other members of this class than the normalized min and max values don't need to be saved.
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        return bundle;
    }

    /**
     * Overridden to restore instance state when device orientation changes.
     * This method is called automatically if you assign an id to the RangeSeekBar widget using the
     * {@link #setId(int)} method.
     */
    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
    }

    /**
     * Draws the "normal" resp. "pressed" thumb image on specified x-coordinate.
     *
     * @param screenX The x-coordinate in screen space where to draw the image.
     * @param radius  The radius of the thumb to draw.
     * @param canvas  The canvas to draw upon.
     */
    private void drawThumb(float screenX, float radius, Canvas canvas) {
        canvas.drawCircle(screenX, thumbPressedRadius, radius, paint);
    }

    /**
     * Decides which (if any) thumb is touched by the given x-coordinate.
     *
     * @param touchX The x-coordinate of a touch event in screen space.
     * @return The pressed thumb or null if none has been touched.
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        final boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue);
        final boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue);

        if (minThumbPressed && maxThumbPressed)
            // if both thumbs are pressed (they lie on top of each other),
            // choose the one with more room to drag. this avoids "stalling" the thumbs in a corner,
            // not being able to drag them apart anymore.
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        else if (minThumbPressed)
            result = Thumb.MIN;
        else if (maxThumbPressed)
            result = Thumb.MAX;

        return result;
    }

    /**
     * Decides if given x-coordinate in screen space needs to be interpreted
     * as "within" the normalized thumb x-coordinate.
     *
     * @param x                    The x-coordinate in screen space to check.
     * @param normalizedThumbValue The normalized x-coordinate of the thumb to check.
     * @return true if x-coordinate is in thumb range, false otherwise.
     */
    private boolean isInThumbRange(float x, double normalizedThumbValue) {
        return Math.abs(x - normalizedToScreen(normalizedThumbValue)) <= thumbPressedRadius * 2;
    }

    /**
     * Sets normalized min value to value so that 0 <= value <= normalized max value <= 1.
     * The View will get invalidated when calling this method.
     *
     * @param value The new normalized min value to set.
     */
    private void setNormalizedMinValue(double value) {
        normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
    }

    /**
     * Sets normalized max value to value so that 0 <= normalized min value <= value <= 1.
     * The View will get invalidated when calling this method.
     *
     * @param value The new normalized max value to set.
     */
    private void setNormalizedMaxValue(double value) {
        normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
    }

    /**
     * Converts a normalized value to a Number object in the value space between
     * absolute minimum and maximum.
     */
    @SuppressWarnings("unchecked")
    private T normalizedToValue(double normalized) {
        return (T) numberType.toNumber(Math.round(
                (absMinValuePrim + normalized * (absMaxValuePrim - absMinValuePrim)) * 100) / 100d);
    }

    /**
     * Converts the given Number value to a normalized double.
     *
     * @param value The Number value to normalize.
     * @return The normalized double.
     */
    private double valueToNormalized(T value) {
        if (absMaxValuePrim == absMinValuePrim)
            // prevent division by zero, simply return 0
            return 0d;

        return (value.doubleValue() - absMinValuePrim) / (absMaxValuePrim - absMinValuePrim);
    }

    /**
     * Converts a normalized value into screen space.
     *
     * @param normalizedCoord The normalized value to convert.
     * @return The converted value in screen space.
     */
    private float normalizedToScreen(double normalizedCoord) {
        return (float) (internalPad + normalizedCoord * (getWidth() - 2 * internalPad));
    }

    /**
     * Converts screen space x-coordinates into normalized values.
     *
     * @param screenCoord The x-coordinate in screen space to convert.
     * @return The normalized value.
     */
    private double screenToNormalized(float screenCoord) {
        final int width = getWidth();

        if (width <= 2 * internalPad)
            // prevent division by zero, simply return 0
            return 0d;

        return Math.min(1d, Math.max(0d, (screenCoord - internalPad) / (width - 2 * internalPad)));
    }

    /**
     * Thumb constants (min and max).
     */
    private enum Thumb {
        MIN, MAX
    }

    /**
     * Utility enumeration used to convert between Numbers and doubles.
     */
    private enum NumberType {
        LONG, DOUBLE, INTEGER, FLOAT, SHORT, BYTE, BIG_DECIMAL;

        public static <E extends Number> NumberType fromNumber(E value)
                throws IllegalArgumentException {
            if (value instanceof Long)
                return LONG;

            if (value instanceof Double)
                return DOUBLE;

            if (value instanceof Integer)
                return INTEGER;

            if (value instanceof Float)
                return FLOAT;

            if (value instanceof Short)
                return SHORT;

            if (value instanceof Byte)
                return BYTE;

            if (value instanceof BigDecimal)
                return BIG_DECIMAL;

            throw new IllegalArgumentException(
                    "Number class '" + value.getClass().getName() + "' is not supported");
        }

        public Number toNumber(double value) {
            switch (this) {
                case LONG:
                    return (long) value;

                case DOUBLE:
                    return value;

                case INTEGER:
                    return (int) value;

                case FLOAT:
                    return (float) value;

                case SHORT:
                    return (short) value;

                case BYTE:
                    return (byte) value;

                case BIG_DECIMAL:
                    return BigDecimal.valueOf(value);
            }

            throw new InstantiationError("Can't convert " + this + " to a Number object");
        }
    }

    /**
     * Callback listener interface to notify about changed range values.
     *
     * @param <T> The Number type the RangeSeekBar has been declared with.
     */
    public interface OnRangeSeekBarChangeListener<T> {
        void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, T minValue, T maxValue);
    }
}