package com.mr_starktastic.sugardays;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Main activity where the user can navigate between days and view his logs
 */
public class DiaryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        AppBarLayout.OnOffsetChangedListener, OnDateSelectedListener {
    /**
     * Constants
     */
    private static final int DAYS_IN_WEEK = 7, ARROW_END_ANGLE = -180;
    private static final long CALENDAR_RESIZE_ANIM_DURATION = 200;
    private static final CalendarDay TODAY_CAL = CalendarDay.today();

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

    /**
     * Member variables
     */
    private static DrawerLayout drawerLayout;
    private static AppBarLayout appBar;
    private static Toolbar toolbar;
    private static MaterialCalendarView calendarView;
    private static ViewGroup dropDownLayout;
    private static TextView title, subtitle;
    private static ImageView dropDownArrow;
    private static FloatingActionButton fab;

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
        toolbar = (Toolbar) appBar.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // The layout which unveils the calendar view on click and contains the title & subtitle
        dropDownLayout = (ViewGroup) toolbar.findViewById(R.id.dropdown_layout);
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

        calendarView.setOnMonthChangedListener((widget, date) -> {
            // The view has to be resized when the amount of rows (weeks) is changed
            // Built-in implementation of dynamic height isn't satisfying
            final int newHeight = calcCalendarHeight(date.getCalendar());

            if (isCalendarHidden()) { // No need to animate
                setCalendarHeight(newHeight);

                if (date.getMonth() != widget.getSelectedDate().getMonth())
                    goToDay(date);

                if (newHeight != widget.getHeight())
                    appBar.setExpanded(false, false); // Avoids layout quirks

                return;
            }

            final int oldHeight = widget.getHeight();

            if (oldHeight != newHeight) {
                final ValueAnimator animator = ValueAnimator.ofInt(oldHeight, newHeight);
                animator.addUpdateListener(valueAnimator ->
                        setCalendarHeight((int) valueAnimator.getAnimatedValue()));
                animator.setDuration(CALENDAR_RESIZE_ANIM_DURATION).start();
            }

            if (date.getMonth() != widget.getSelectedDate().getMonth())
                goToDay(date);
        });

        calendarView.setOnDateChangedListener(this);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(v ->
                Snackbar.make(v, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show());

        dropDownLayout.setOnClickListener(v -> toggleCalendarDropDown());

        calendarView.addDecorator(new TodayDecorator());
        goToDay(TODAY_CAL);

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
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
     * @return False if the calendar view is dropped down, true otherwise
     */
    private boolean isCalendarHidden() {
        return dropDownArrow.getRotation() == 0;
    }

    /**
     * Drops down or raises up the calendarView by expanding/collapsing the AppBar
     */
    private void toggleCalendarDropDown() {
        appBar.setExpanded(isCalendarHidden());
    }

    /**
     * Navigates to the desired day (Main goToDay() method)
     *
     * @param day     The day's date
     * @param animate To use smooth-scroll or not to use
     */
    private void goToDay(CalendarDay day, boolean animate) {
        calendarView.setSelectedDate(day);
        calendarView.setCurrentDate(day, animate);
        // Provoking the listener
        onDateSelected(calendarView, day, true);
    }

    /**
     * Navigates to the desired day
     *
     * @param day The day's date
     */
    private void goToDay(CalendarDay day) {
        goToDay(day, true);
    }

    /**
     * Navigates to the desired day and uses smooth-scroll iff calendar is shown
     *
     * @param day The day's date
     */
    private void observantGoToDay(CalendarDay day) {
        if (isCalendarHidden())
            goToDay(day, false);
        else goToDay(day);
    }

    /**
     * Used when the calendar is shown
     * Sets the title & subtitle texts with the full date
     *
     * @param date Date to set as text
     */
    private void setFullDateText(CalendarDay date) {
        final Date d = date.getDate();
        final String text = DAY_MONTH_FORMAT.format(d);

        if (!title.getText().toString().equals(text)) {
            title.setText(text);

            if (TODAY_CAL.equals(date)) {
                subtitle.setText(R.string.today);
                subtitle.setVisibility(View.VISIBLE);
            } else if (TODAY_CAL.getYear() != date.getYear()) {
                subtitle.setText(YEAR_FORMAT.format(d));
                subtitle.setVisibility(View.VISIBLE);
            } else subtitle.setVisibility(View.GONE);
        }
    }

    /**
     * Used when the calendar is hidden
     * Sets the title & subtitle texts with the condensed date (without the day)
     *
     * @param date Date to set as text
     */
    private void setCondensedDateText(CalendarDay date) {
        final Date d = date.getDate();
        final String text = MONTH_FORMAT.format(d);

        if (!title.getText().toString().equals(text)) {
            title.setText(text);

            if (TODAY_CAL.getYear() != date.getYear()) {
                subtitle.setText(YEAR_FORMAT.format(d));
                subtitle.setVisibility(View.VISIBLE);
            } else subtitle.setVisibility(View.GONE);
        }
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
                observantGoToDay(TODAY_CAL);
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
        dropDownArrow.setRotation(r); // Animates the dropdown arrow

        if (r == 0)
            setFullDateText(calendarView.getSelectedDate());
        else if (r == ARROW_END_ANGLE)
            setCondensedDateText(calendarView.getSelectedDate());
    }

    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date,
                               boolean selected) {
        if (isCalendarHidden())
            setFullDateText(date);
        else setCondensedDateText(date);
    }
}
