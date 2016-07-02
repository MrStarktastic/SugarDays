package com.mr_starktastic.sugardays;

import android.graphics.Typeface;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

/**
 * Decorate today by making the text big and bold
 */
public class TodayDecorator implements DayViewDecorator {
    /**
     * Constant
     */
    private static final float SPAN_SIZE_PROPORTION = 1.5f;

    /**
     * Member variable
     */
    private CalendarDay date;

    /**
     * Constructor
     */
    public TodayDecorator() {
        date = CalendarDay.today();
    }

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return date != null && day.equals(date);
    }

    @Override
    public void decorate(DayViewFacade view) {
        view.addSpan(new StyleSpan(Typeface.BOLD));
        view.addSpan(new RelativeSizeSpan(SPAN_SIZE_PROPORTION));
    }
}
