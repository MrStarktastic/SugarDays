package com.mr_starktastic.sugardays;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Main activity where the user can navigate between days and view his logs
 */
public class DiaryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        AppBarLayout.OnOffsetChangedListener, OnMonthChangedListener, OnDateSelectedListener,
        ViewPager.OnPageChangeListener, DayPageFragment.OnFragmentInteractionListener {
    /**
     * Used to determine which view caused the date change in order to update the date on the other
     * view while avoiding loops
     */
    private enum DateChangeCause {
        CALENDAR_VIEW, VIEW_PAGER
    }

    /**
     * Decorate today's DayView from the calendarView by making the text big and bold
     */
    private class TodayDecorator implements DayViewDecorator {
        private static final float SPAN_SIZE_PROPORTION = 1.5f;

        private CalendarDay date;

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


    /**
     * Adapter for the ViewPager
     */
    private class DayAdapter extends FragmentStatePagerAdapter {
        public DayAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return DayPageFragment.newInstance("Test", Integer.toString(position));
        }

        @Override
        public int getCount() {
            return DAYS_COUNT;
        }
    }

    /**
     * Constants
     */
    private static final int DAYS_IN_WEEK = 7, DAYS_COUNT = 400 * 365, TODAY_IDX = DAYS_COUNT / 2;
    private static final int ARROW_END_ANGLE = -180;
    private static final long CALENDAR_RESIZE_ANIM_DURATION = 200;
    private static final CalendarDay TODAY_CAL = CalendarDay.today();
    private static final long TODAY_TIME = TODAY_CAL.getDate().getTime();

    /**
     * Date formats for setting title & subtitle texts
     */
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final SimpleDateFormat DAY_MONTH_FORMAT = new SimpleDateFormat(
            "EEE, MMM d", DEFAULT_LOCALE);
    private static final SimpleDateFormat DAY_NUMBER_FORMAT = new SimpleDateFormat(
            "d", DEFAULT_LOCALE);
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat(
            "MMMM", DEFAULT_LOCALE);
    private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat(
            "y", DEFAULT_LOCALE);

    private static DrawerLayout drawerLayout;
    private static AppBarLayout appBar;
    private static MaterialCalendarView calendarView;
    private static TextView title, subtitle;
    private static ImageView dropDownArrow;
    private static ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        // Root layout
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ((NavigationView) drawerLayout.findViewById(R.id.nav_view)).
                setNavigationItemSelectedListener(this);

        appBar = (AppBarLayout) drawerLayout.findViewById(R.id.app_bar);
        appBar.addOnOffsetChangedListener(this);
        final Toolbar toolbar = (Toolbar) appBar.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // The layout which unveils the calendar view on click and contains the title & subtitle
        final ViewGroup dropDownLayout = (ViewGroup) toolbar.findViewById(R.id.dropdown_layout);
        final LayoutTransition dropDownTransition = dropDownLayout.getLayoutTransition();
        dropDownTransition.setAnimateParentHierarchy(false);
        dropDownTransition.enableTransitionType(LayoutTransition.CHANGING);
        // Displays the date
        title = (TextView) dropDownLayout.findViewById(R.id.toolbar_title);
        /*
        Indicates whether the shown day is today, displays the year number if not this year
        or is simply not shown (gone)
         */
        subtitle = (TextView) dropDownLayout.findViewById(R.id.toolbar_subtitle);
        // Indicates that the layout can be clicked for a dropdown
        dropDownArrow = (ImageView) dropDownLayout.findViewById(R.id.dropdown_arrow);

        calendarView = (MaterialCalendarView) appBar.findViewById(R.id.calendar_view);
        calendarView.setTopbarVisible(false);
        // Properly sets the width of the calendar view to fill the container
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        calendarView.setTileWidth((displayMetrics.widthPixels - getResources()
                .getDimensionPixelSize(R.dimen.small_margin)) / DAYS_IN_WEEK);
        // Sets proper height
        setCalendarHeight(calcCalendarHeight(TODAY_CAL.getCalendar()));
        // Set decorator to highlight today's view
        calendarView.addDecorator(new TodayDecorator());

        calendarView.setOnMonthChangedListener(this);
        calendarView.setOnDateChangedListener(this);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(v ->
                Snackbar.make(v, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show());

        dropDownLayout.setOnClickListener(v -> appBar.setExpanded(isCalendarHidden()));

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        pager = (ViewPager) drawerLayout.findViewById(R.id.diary_pager);
        pager.setAdapter(new DayAdapter(getSupportFragmentManager()));
        pager.addOnPageChangeListener(this);

        goToDay(TODAY_CAL, null);
    }

    /**
     * Resizes the calendarView when the amount of rows (weeks) is changed
     * Build-in implementation of dynamic height isn't satisfying
     * Animates iff the calendarView is shown
     *
     * @param date The date whose month will help to configure the height
     */
    private void animateCalendarHeight(CalendarDay date) {
        final int newHeight = calcCalendarHeight(date.getCalendar());

        if (isCalendarHidden()) { // No need to animate
            setCalendarHeight(newHeight);

            if (newHeight != calendarView.getHeight())
                appBar.setExpanded(false, false); // Avoids layout quirks
        } else {
            final int oldHeight = calendarView.getHeight();

            if (oldHeight != newHeight) {
                final ValueAnimator animator = ValueAnimator.ofInt(oldHeight, newHeight);
                animator.addUpdateListener(valueAnimator ->
                        setCalendarHeight((int) valueAnimator.getAnimatedValue()));
                animator.setDuration(CALENDAR_RESIZE_ANIM_DURATION).start();
            }
        }
    }

    /**
     * Sets the calendarView's height by modifying its LayoutParams
     *
     * @param height The desired height to set
     */
    private void setCalendarHeight(int height) {
        final ViewGroup.LayoutParams params = calendarView.getLayoutParams();
        params.height = height;
        calendarView.setLayoutParams(params);
    }

    /**
     * Calculates the expected calendarView height
     * by the amount of weeks (rows) in the current month
     *
     * @param date Currently displayed date
     * @return The calculated height dimension in pixels
     */
    private int calcCalendarHeight(Calendar date) {
        // Adds 1 because the day labels header is also considered a tile
        return (date.getActualMaximum(Calendar.WEEK_OF_MONTH) + 1) * calendarView.getTileHeight();
    }

    /**
     * Calculates the amount of days between one date to another
     *
     * @param time1 First date (as time)
     * @param time2 Second date (as time)
     * @return Calculation result
     */
    private int calcDaysDiff(long time1, long time2) {
        return (int) TimeUnit.DAYS.convert(time1 - time2, TimeUnit.MILLISECONDS);
    }

    /**
     * Calculates the index of the page associated with the given date
     *
     * @param date The day date to calculate the index for
     * @return Index of wanted page
     */
    private int getPageIndexFromDate(CalendarDay date) {
        return TODAY_IDX - calcDaysDiff(TODAY_TIME, date.getDate().getTime());
    }

    /**
     * @return False if the calendar view is dropped down, true otherwise
     */
    private boolean isCalendarHidden() {
        return dropDownArrow.getRotation() == 0;
    }

    /**
     * Navigates to the desired day on both CalendarView & ViewPager
     *
     * @param day The day's date
     */
    private void goToDay(CalendarDay day, DateChangeCause cause) {
        if (isCalendarHidden())
            setFullDateText(day);
        else setCondensedDateText(day);

        if (cause != null)
            switch (cause) {
                case VIEW_PAGER:
                    goToDayOnCalendar(day);
                    break;

                case CALENDAR_VIEW:
                    goToDayOnPager(day);
                    break;
            }
        else {
            goToDayOnCalendar(day);
            goToDayOnPager(day);
        }
    }

    /**
     * Navigates to the desired day on CalendarView
     * Temporary removes listeners which might cause a loop and adds them back
     *
     * @param day The day's date
     */
    private void goToDayOnCalendar(CalendarDay day) {
        animateCalendarHeight(day);

        calendarView.setOnMonthChangedListener(null);
        calendarView.setSelectedDate(day);
        calendarView.setCurrentDate(day, !isCalendarHidden());
        calendarView.setOnMonthChangedListener(this);
    }

    /**
     * Navigates to the desired day on ViewPager
     * Temporary removes listeners which might cause a loop and adds them back
     *
     * @param day The day's date
     */
    private void goToDayOnPager(CalendarDay day) {
        pager.removeOnPageChangeListener(this);
        pager.setCurrentItem(getPageIndexFromDate(day));
        pager.addOnPageChangeListener(this);
    }

    /**
     * Used when the calendar is shown
     * Sets the title & subtitle texts with the full date
     *
     * @param date Date to set as text
     */
    private void setFullDateText(CalendarDay date) {
        final Date d = date.getDate();
        title.setText(DAY_MONTH_FORMAT.format(d));

        if (TODAY_CAL.equals(date)) {
            subtitle.setText(R.string.today);
            subtitle.setVisibility(View.VISIBLE);
        } else if (TODAY_CAL.getYear() != date.getYear()) {
            subtitle.setText(YEAR_FORMAT.format(d));
            subtitle.setVisibility(View.VISIBLE);
        } else subtitle.setVisibility(View.GONE);
    }

    /**
     * Used when the calendar is hidden
     * Sets the title & subtitle texts with the condensed date (without the day)
     *
     * @param date Date to set as text
     */
    private void setCondensedDateText(CalendarDay date) {
        final Date d = date.getDate();
        title.setText(MONTH_FORMAT.format(d));

        if (TODAY_CAL.getYear() != date.getYear()) {
            subtitle.setText(YEAR_FORMAT.format(d));
            subtitle.setVisibility(View.VISIBLE);
        } else subtitle.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.diary, menu);
        final View todayIconView = getLayoutInflater().inflate(R.layout.ic_today_num, null);

        // Sets today's number
        ((TextView) todayIconView.findViewById(R.id.today_num_text))
                .setText(DAY_NUMBER_FORMAT.format(TODAY_CAL.getDate()));

        // Converts the layout into a bitmap for use as an icon
        todayIconView.setDrawingCacheEnabled(true);
        todayIconView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        todayIconView.layout(0, 0,
                todayIconView.getMeasuredWidth(), todayIconView.getMeasuredHeight());
        todayIconView.buildDrawingCache(true);
        final Bitmap bitmap = Bitmap.createBitmap(todayIconView.getDrawingCache());
        todayIconView.setDrawingCacheEnabled(false);
        todayIconView.setVisibility(View.GONE);

        // Sets today's icon
        menu.findItem(R.id.action_today).setIcon(new BitmapDrawable(getResources(), bitmap));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_today:
                goToDay(TODAY_CAL, null);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        }

        drawerLayout.closeDrawer(GravityCompat.START);

        return false;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        final float r = ARROW_END_ANGLE * (1 + (float) verticalOffset / calendarView.getHeight());

        if (dropDownArrow.getRotation() == r)
            return;

        dropDownArrow.setRotation(r); // Animates the dropdown arrow

        if (r == 0)
            setFullDateText(calendarView.getSelectedDate());
        else if (r == ARROW_END_ANGLE)
            setCondensedDateText(calendarView.getSelectedDate());
    }

    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
        animateCalendarHeight(date);

        if (date.getMonth() != widget.getSelectedDate().getMonth()) {
            widget.setSelectedDate(date);
            goToDay(date, DateChangeCause.CALENDAR_VIEW);
        }
    }

    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date,
                               boolean selected) {
        goToDay(date, DateChangeCause.CALENDAR_VIEW);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        final Calendar tempCal = Calendar.getInstance();
        tempCal.add(Calendar.DATE, position - TODAY_IDX);
        goToDay(CalendarDay.from(tempCal), DateChangeCause.VIEW_PAGER);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
