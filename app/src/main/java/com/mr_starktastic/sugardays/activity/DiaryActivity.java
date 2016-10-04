package com.mr_starktastic.sugardays.activity;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
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

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.fragment.DayPageFragment;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * Main activity where the user can navigate between days and view his logs
 */
public class DiaryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        AppBarLayout.OnOffsetChangedListener, OnMonthChangedListener, OnDateSelectedListener,
        ViewPager.OnPageChangeListener, DayPageFragment.OnFragmentInteractionListener {
    /**
     * Extra keys
     */
    public static final String EXTRA_DATE = "DATE";

    /**
     * Constants
     */
    private static final Calendar TODAY_CAL = CalendarDay.today().getCalendar(),
            MIN_CAL = CalendarDay.from(1900, 1, 1).getCalendar();
    private static final int TODAY_IDX = daysBetween(TODAY_CAL, MIN_CAL),
            ARROW_START_ANGLE = 0, ARROW_END_ANGLE = -180;
    private static final long CALENDAR_RESIZE_ANIM_DURATION = 200;

    /**
     * Date formats for setting title & subtitle texts
     */
    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final SimpleDateFormat DAY_MONTH_FORMAT = new SimpleDateFormat(
            "EEE, MMM d", DEFAULT_LOCALE),
            DAY_NUMBER_FORMAT = new SimpleDateFormat("d", DEFAULT_LOCALE),
            MONTH_FORMAT = new SimpleDateFormat("MMMM", DEFAULT_LOCALE),
            YEAR_FORMAT = new SimpleDateFormat("y", DEFAULT_LOCALE);

    /**
     * GUI related variables
     */
    private DrawerLayout drawerLayout;
    private AppBarLayout appBar;
    private MaterialCalendarView calendarView;
    private final ValueAnimator.AnimatorUpdateListener calendarResizeAnimListener =
            valueAnimator -> setCalendarHeight((int) valueAnimator.getAnimatedValue());
    private int calBottomPad;
    private int calTileHeight;
    private TextView title, subtitle;
    private ImageView dropDownArrow;
    private ViewPager pager;

    /**
     * Calculates the amount of days between one date and another
     *
     * @param cal1 First older date
     * @param cal2 Second date
     * @return Calculation result
     */
    private static int daysBetween(Calendar cal1, Calendar cal2) {
        final int cal2Year = cal2.get(Calendar.YEAR),
                cal2DayOfYear = cal2.get(Calendar.DAY_OF_YEAR);

        if (cal1.get(Calendar.YEAR) == cal2Year)
            return cal1.get(Calendar.DAY_OF_YEAR) - cal2DayOfYear;

        cal1 = (Calendar) cal1.clone();
        cal2 = (Calendar) cal2.clone();
        final int cal1OriginalDayOfYear = cal1.get(Calendar.DAY_OF_YEAR);
        int extraDays = 0;

        while (cal1.get(Calendar.YEAR) > cal2.get(Calendar.YEAR)) {
            cal1.add(Calendar.YEAR, -1);
            extraDays += cal1.getActualMaximum(Calendar.DAY_OF_YEAR);
        }

        return extraDays - cal2DayOfYear + cal1OriginalDayOfYear;
    }

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

        // Sets the min (Jan 1st, 1900) & max (today) dates
        calendarView.state().edit().setMinimumDate(MIN_CAL).setMaximumDate(TODAY_CAL).commit();

        // The date string will be shown in the toolbar instead
        calendarView.setTopbarVisible(false);

        // Properly sets the width of each tile to fill the app bar
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        calendarView.setTileWidth((int) (displayMetrics.widthPixels / 7 + 0.5f));

        // Sets proper height according to the current month's rows
        final Resources resources = getResources();
        calBottomPad = resources.getDimensionPixelSize(R.dimen.tiny_margin);
        calTileHeight = resources.getDimensionPixelSize(R.dimen.calendar_view_tile_height);
        setCalendarHeight(calcCalendarHeight(TODAY_CAL));

        calendarView.setOnMonthChangedListener(this);
        calendarView.setOnDateChangedListener(this);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(v -> addLog());

        dropDownLayout.setOnClickListener(v -> appBar.setExpanded(isCalendarHidden()));

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        /*
        The ViewPager will display each page as a day with its logs.
        Its onPageSelected is the master listener method behind the date navigation mechanism.
         */
        pager = (ViewPager) drawerLayout.findViewById(R.id.diary_pager);
        pager.setAdapter(new DayAdapter(getSupportFragmentManager()));
        pager.addOnPageChangeListener(this);
        pager.setCurrentItem(TODAY_IDX);
    }

    private void addLog() {
        final CalendarDay day = calendarView.getSelectedDate();
        final Calendar currTime = Calendar.getInstance();
        startActivity(new Intent(this, EditLogActivity.class).putExtra(EXTRA_DATE,
                new GregorianCalendar(day.getYear(), day.getMonth(), day.getDay(),
                        currTime.get(Calendar.HOUR_OF_DAY), currTime.get(Calendar.MINUTE))));
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
        return (date.getActualMaximum(Calendar.WEEK_OF_MONTH) + 1) * calTileHeight +
                calBottomPad;
    }

    /**
     * Calculates the index of the page associated with the given date
     *
     * @param date The day date to calculate the index for
     * @return Index of wanted page
     */
    private int getPageIndexFromDate(CalendarDay date) {
        return TODAY_IDX - daysBetween(TODAY_CAL, date.getCalendar());
    }

    /**
     * @return False if the calendar view is dropped down, true otherwise
     */
    private boolean isCalendarHidden() {
        return dropDownArrow.getRotation() == 0;
    }

    /**
     * Used when the calendar is shown
     * Sets the title & subtitle texts with the full date
     *
     * @param date Date to set as text
     */
    @SuppressWarnings("WrongConstant")
    private void setFullDateText(CalendarDay date) {
        final Date d = date.getDate();
        title.setText(DAY_MONTH_FORMAT.format(d));

        if (TODAY_IDX == pager.getCurrentItem()) {
            subtitle.setText(R.string.today);
            subtitle.setVisibility(View.VISIBLE);
        } else if (TODAY_CAL.get(Calendar.YEAR) != date.getYear()) {
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
    @SuppressWarnings("WrongConstant")
    private void setCondensedDateText(CalendarDay date) {
        final Date d = date.getDate();
        title.setText(MONTH_FORMAT.format(d));

        if (TODAY_CAL.get(Calendar.YEAR) != date.getYear()) {
            subtitle.setText(YEAR_FORMAT.format(d));
            subtitle.setVisibility(View.VISIBLE);
        } else subtitle.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.diary, menu);
        final View todayIconView = getLayoutInflater().inflate(R.layout.ic_today_num, null);

        // Sets today's number
        ((TextView) todayIconView.findViewById(R.id.today_num_text))
                .setText(DAY_NUMBER_FORMAT.format(TODAY_CAL.getTime()));

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
        if (item.getItemId() == R.id.action_today)
            pager.setCurrentItem(TODAY_IDX);

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        final float r = ARROW_END_ANGLE * (1 + (float) verticalOffset / calendarView.getHeight());

        if (dropDownArrow.getRotation() == r)
            return;

        dropDownArrow.setRotation(r); // Animates the dropdown arrow

        if (r == ARROW_START_ANGLE)
            setFullDateText(calendarView.getSelectedDate());
        else if (r == ARROW_END_ANGLE)
            setCondensedDateText(calendarView.getSelectedDate());
    }

    @Override
    public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
        // First animates the height of calendarView if necessary
        final int newHeight = calcCalendarHeight(date.getCalendar());

        if (isCalendarHidden()) { // Then no need to animate
            if (newHeight != calendarView.getHeight()) {
                setCalendarHeight(newHeight);
                appBar.setExpanded(false, false); // Avoids layout quirks
            }
        } else {
            final int oldHeight = calendarView.getHeight();

            if (oldHeight != newHeight) {
                final ValueAnimator animator = ValueAnimator.ofInt(oldHeight, newHeight);
                animator.addUpdateListener(calendarResizeAnimListener);
                animator.setDuration(CALENDAR_RESIZE_ANIM_DURATION).start();
            }
        }

        if (date.getMonth() != widget.getSelectedDate().getMonth()) {
            // Then selected date becomes the first day of the month and pager must be updated
            widget.setSelectedDate(date);
            pager.setCurrentItem(getPageIndexFromDate(date));
        }
    }

    @Override
    public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date,
                               boolean selected) {
        // This listener is triggered only by the user's explicit selection on the calendarView
        pager.setCurrentItem(getPageIndexFromDate(date));
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        final Calendar tempCal = (Calendar) TODAY_CAL.clone();
        tempCal.add(Calendar.DATE, position - TODAY_IDX);
        final CalendarDay tempCalDay = CalendarDay.from(tempCal);

        if (isCalendarHidden())
            setFullDateText(tempCalDay);
        else setCondensedDateText(tempCalDay);

        // Checks if the calendarView needs to be updated
        if (!tempCalDay.equals(calendarView.getSelectedDate())) {
            calendarView.setSelectedDate(tempCalDay);
            calendarView.setCurrentDate(tempCalDay);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /**
     * Adapter for the ViewPager
     */
    private static class DayAdapter extends FragmentStatePagerAdapter {
        private static final int PAGE_COUNT = TODAY_IDX + 1;

        DayAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return DayPageFragment.newInstance("Test", Integer.toString(position));
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }
    }
}
