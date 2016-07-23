package com.mr_starktastic.sugardays.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.mr_starktastic.sugardays.R;

import java.util.HashSet;
import java.util.Set;

/**
 * SeekBar following Material Design with two movable targets
 * that allow user to select a range of integers
 */
public class RangeSeekBar extends View {
    public interface RangeSeekBarListener {
        void onMaxChanged(int newValue);

        void onMinChanged(int newValue);
    }

    /**
     * Converts from dp to pixels
     *
     * @param dp Dimension value in dp
     * @return Dimension value in pixels as an integer
     */
    private static int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics()));
    }

    private static final int HORIZONTAL_PADDING = dpToPx(16);
    private static final int DEFAULT_TOUCH_TARGET_SIZE = dpToPx(40);
    private static final int DEFAULT_UNPRESSED_RADIUS = dpToPx(6);
    private static final int DEFAULT_PRESSED_RADIUS = dpToPx(9);
    private static final int DEFAULT_INSIDE_RANGE_STROKE_WIDTH = dpToPx(2);
    private static final int DEFAULT_OUTSIDE_RANGE_STROKE_WIDTH = dpToPx(2);
    private static final int DEFAULT_MAX = 100;

    private float unpressedRadius;
    private float pressedRadius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int lineStartX;
    private int lineEndX;
    private int lineLength;
    private float minTargetRadius = 0;
    private float maxTargetRadius = 0;
    private int minPosition = 0;
    private int maxPosition = 0;
    private int midY = 0;
    //List of event IDs touching targets
    private Set<Integer> isTouchingMinTarget = new HashSet<>();
    private Set<Integer> isTouchingMaxTarget = new HashSet<>();
    private int min = 0;
    private int max = DEFAULT_MAX;
    private int range;
    private float convertFactor;
    private RangeSeekBarListener rangeSeekBarListener;
    private int targetColor;
    private int insideRangeColor;
    private int outsideRangeColor;
    private float insideRangeLineStrokeWidth;
    private float outsideRangeLineStrokeWidth;
    private ObjectAnimator minAnimator;
    private ObjectAnimator maxAnimator;
    boolean lastTouchedMin;

    private Integer startingMin;
    private Integer startingMax;

    public RangeSeekBar(Context context) {
        super(context);
        init(null);
    }

    public RangeSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public RangeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    /**
     * Initializes this RangeSeekBar's dimensions & style with respect to XML attributes
     *
     * @param attrs XML attributes
     */
    public void init(AttributeSet attrs) {
        final Context context = getContext();
        final TypedArray styledAttrs = context.obtainStyledAttributes(attrs,
                R.styleable.RangeSeekBar, 0, 0);
        targetColor = styledAttrs.getColor(R.styleable.RangeSeekBar_insideRangeLineColor,
                ContextCompat.getColor(context, android.R.color.holo_blue_light));
        insideRangeColor = styledAttrs.getColor(R.styleable.RangeSeekBar_insideRangeLineColor,
                ContextCompat.getColor(context, android.R.color.holo_blue_dark));
        outsideRangeColor = styledAttrs.getColor(R.styleable.RangeSeekBar_outsideRangeLineColor,
                ContextCompat.getColor(context, android.R.color.darker_gray));
        min = styledAttrs.getInt(R.styleable.RangeSeekBar_min, min);
        max = styledAttrs.getInt(R.styleable.RangeSeekBar_max, max);

        unpressedRadius = styledAttrs.getDimension(R.styleable.RangeSeekBar_unpressedTargetRadius,
                DEFAULT_UNPRESSED_RADIUS);
        pressedRadius = styledAttrs.getDimension(R.styleable.RangeSeekBar_pressedTargetRadius,
                DEFAULT_PRESSED_RADIUS);
        insideRangeLineStrokeWidth = styledAttrs.getDimension(
                R.styleable.RangeSeekBar_insideRangeLineStrokeWidth,
                DEFAULT_INSIDE_RANGE_STROKE_WIDTH);
        outsideRangeLineStrokeWidth = styledAttrs.getDimension(
                R.styleable.RangeSeekBar_outsideRangeLineStrokeWidth,
                DEFAULT_OUTSIDE_RANGE_STROKE_WIDTH);

        styledAttrs.recycle();

        minTargetRadius = unpressedRadius;
        maxTargetRadius = unpressedRadius;
        range = max - min;

        minAnimator = getMinTargetAnimator(true);
        maxAnimator = getMaxTargetAnimator(true);
    }

    private ObjectAnimator getMinTargetAnimator(boolean touching) {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(
                this, "minTargetRadius", minTargetRadius,
                touching ? pressedRadius : unpressedRadius);
        anim.addUpdateListener(animation -> invalidate());

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                anim.removeAllListeners();
                super.onAnimationEnd(animation);
            }
        });

        anim.setInterpolator(new AccelerateInterpolator());
        return anim;
    }

    private ObjectAnimator getMaxTargetAnimator(boolean touching) {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(
                this, "maxTargetRadius", maxTargetRadius,
                touching ? pressedRadius : unpressedRadius);
        anim.addUpdateListener(animation -> invalidate());

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                anim.removeAllListeners();
            }
        });

        anim.setInterpolator(new AccelerateInterpolator());
        return anim;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int desiredHeight = 96;

        int width;
        int height = desiredHeight;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(widthSize, widthSize);
        } else {
            width = widthSize;
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = desiredHeight;
        }

        lineLength = (width - HORIZONTAL_PADDING * 2);
        midY = height / 2;
        lineStartX = HORIZONTAL_PADDING;
        lineEndX = lineLength + HORIZONTAL_PADDING;

        calculateConvertFactor();

        setSelectedMin(startingMin != null ? startingMin : min);
        setSelectedMax(startingMax != null ? startingMax : max);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawEntireRangeLine(canvas);
        drawSelectedRangeLine(canvas);
        drawSelectedTargets(canvas);
    }

    private void drawEntireRangeLine(Canvas canvas) {
        paint.setColor(outsideRangeColor);
        paint.setStrokeWidth(outsideRangeLineStrokeWidth);
        canvas.drawLine(lineStartX, midY, lineEndX, midY, paint);
    }

    private void drawSelectedRangeLine(Canvas canvas) {
        paint.setStrokeWidth(insideRangeLineStrokeWidth);
        paint.setColor(insideRangeColor);
        canvas.drawLine(minPosition, midY, maxPosition, midY, paint);
    }

    private void drawSelectedTargets(Canvas canvas) {
        paint.setColor(targetColor);
        canvas.drawCircle(minPosition, midY, minTargetRadius, paint);
        canvas.drawCircle(maxPosition, midY, maxTargetRadius, paint);
    }

    /**
     * Makes a thumb jump to a position (when user clicks outside the range)
     *
     * @param index Pointer index
     * @param event The MotionEvent which triggers the jump
     */
    private void jumpToPosition(int index, MotionEvent event) {
        if (event.getX(index) > maxPosition && event.getX(index) <= lineEndX) {
            maxPosition = (int) event.getX(index);
            invalidate();
            callMaxChangedCallbacks();
        } else if (event.getX(index) < minPosition && event.getX(index) >= lineStartX) {
            minPosition = (int) event.getX(index);
            invalidate();
            callMinChangedCallbacks();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled())
            return false;

        final int actionIndex = event.getActionIndex();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (lastTouchedMin) {
                    if (!checkTouchingMinTarget(actionIndex, event))
                        checkTouchingMaxTarget(actionIndex, event);
                } else if (!checkTouchingMaxTarget(actionIndex, event))
                    checkTouchingMinTarget(actionIndex, event);

                getParent().requestDisallowInterceptTouchEvent(true);
                break;

            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);

            case MotionEvent.ACTION_POINTER_UP:
                isTouchingMinTarget.remove(event.getPointerId(actionIndex));
                isTouchingMaxTarget.remove(event.getPointerId(actionIndex));

                if (isTouchingMinTarget.isEmpty()) {
                    minAnimator.cancel();
                    minAnimator = getMinTargetAnimator(false);
                    minAnimator.start();
                }

                if (isTouchingMaxTarget.isEmpty()) {
                    maxAnimator.cancel();
                    maxAnimator = getMaxTargetAnimator(false);
                    maxAnimator.start();
                }
                break;

            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    if (isTouchingMinTarget.contains(event.getPointerId(i))) {
                        final int touchX = clamp((int) event.getX(i), lineStartX, lineEndX);

                        if (touchX >= maxPosition) {
                            maxPosition = touchX;
                            callMaxChangedCallbacks();
                        }

                        minPosition = touchX;
                        callMinChangedCallbacks();
                    }

                    if (isTouchingMaxTarget.contains(event.getPointerId(i))) {
                        final int touchX = clamp((int) event.getX(i), lineStartX, lineEndX);

                        if (touchX <= minPosition) {
                            minPosition = touchX;
                            callMinChangedCallbacks();
                        }

                        maxPosition = touchX;
                        callMaxChangedCallbacks();
                    }
                }

                invalidate();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                for (int i = 0; i < event.getPointerCount(); i++)
                    if (lastTouchedMin) {
                        if (!checkTouchingMinTarget(i, event)
                                && !checkTouchingMaxTarget(i, event))
                            jumpToPosition(i, event);
                    } else if (!checkTouchingMaxTarget(i, event)
                            && !checkTouchingMinTarget(i, event))
                        jumpToPosition(i, event);

                break;

            case MotionEvent.ACTION_CANCEL:
                isTouchingMinTarget.clear();
                isTouchingMaxTarget.clear();
                break;
        }

        return true;
    }

    /**
     * Checks if given index is touching the min target.  If touching start animation.
     */
    private boolean checkTouchingMinTarget(int index, MotionEvent event) {
        if (isTouchingMinTarget(index, event)) {
            lastTouchedMin = true;
            isTouchingMinTarget.add(event.getPointerId(index));

            if (!minAnimator.isRunning()) {
                minAnimator = getMinTargetAnimator(true);
                minAnimator.start();
            }

            return true;
        }

        return false;
    }

    /**
     * Checks if given index is touching the max target.  If touching starts animation.
     */
    private boolean checkTouchingMaxTarget(int index, MotionEvent event) {
        if (isTouchingMaxTarget(index, event)) {
            lastTouchedMin = false;
            isTouchingMaxTarget.add(event.getPointerId(index));

            if (!maxAnimator.isRunning()) {
                maxAnimator = getMaxTargetAnimator(true);
                maxAnimator.start();
            }

            return true;
        }

        return false;
    }

    private void callMinChangedCallbacks() {
        if (rangeSeekBarListener != null) {
            rangeSeekBarListener.onMinChanged(getSelectedMin());
        }
    }

    private void callMaxChangedCallbacks() {
        if (rangeSeekBarListener != null) {
            rangeSeekBarListener.onMaxChanged(getSelectedMax());
        }
    }

    private boolean isTouchingMinTarget(int pointerIndex, MotionEvent event) {
        return event.getX(pointerIndex) > minPosition - DEFAULT_TOUCH_TARGET_SIZE
                && event.getX(pointerIndex) < minPosition + DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) > midY - DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) < midY + DEFAULT_TOUCH_TARGET_SIZE;
    }

    private boolean isTouchingMaxTarget(int pointerIndex, MotionEvent event) {
        return event.getX(pointerIndex) > maxPosition - DEFAULT_TOUCH_TARGET_SIZE
                && event.getX(pointerIndex) < maxPosition + DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) > midY - DEFAULT_TOUCH_TARGET_SIZE
                && event.getY(pointerIndex) < midY + DEFAULT_TOUCH_TARGET_SIZE;
    }

    private void calculateConvertFactor() {
        convertFactor = ((float) range) / lineLength;
    }

    public int getSelectedMin() {
        return Math.round((minPosition - lineStartX) * convertFactor + min);
    }

    public int getSelectedMax() {
        return Math.round((maxPosition - lineStartX) * convertFactor + min);
    }

    public void setStartingMinMax(int startingMin, int startingMax) {
        this.startingMin = startingMin;
        this.startingMax = startingMax;
    }

    private void setSelectedMin(int selectedMin) {
        minPosition = Math.round(((selectedMin - min) / convertFactor) + lineStartX);
        callMinChangedCallbacks();
    }

    private void setSelectedMax(int selectedMax) {
        maxPosition = Math.round(((selectedMax - min) / convertFactor) + lineStartX);
        callMaxChangedCallbacks();
    }

    public void setRangeSliderListener(RangeSeekBarListener listener) {
        rangeSeekBarListener = listener;
    }

    public RangeSeekBarListener getRangeSliderListener() {
        return rangeSeekBarListener;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
        range = max - min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
        range = max - min;
    }

    /**
     * Resets selected values to MIN and MAX.
     */
    public void reset() {
        minPosition = lineStartX;
        maxPosition = lineEndX;

        if (rangeSeekBarListener != null) {
            rangeSeekBarListener.onMinChanged(getSelectedMin());
            rangeSeekBarListener.onMaxChanged(getSelectedMax());
        }

        invalidate();
    }

    public void setMinTargetRadius(float minTargetRadius) {
        this.minTargetRadius = minTargetRadius;
    }

    public void setMaxTargetRadius(float maxTargetRadius) {
        this.maxTargetRadius = maxTargetRadius;
    }

    /**
     * Keeps Number value inside min/max bounds by returning min or max if outside of
     * bounds.  Otherwise will return the value without altering.
     */
    private <T extends Number> T clamp(@NonNull T value, @NonNull T min, @NonNull T max) {
        final double dVal = value.doubleValue();

        return dVal > max.doubleValue() ? max : dVal < min.doubleValue() ? min : value;
    }
}
