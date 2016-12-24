package com.mr_starktastic.sugardays.activity;

import android.animation.LayoutTransition;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
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
import android.view.Window;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.Day;
import com.mr_starktastic.sugardays.fragment.DayPageFragment;
import com.mr_starktastic.sugardays.receiver.NotificationPublisher;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Main activity where the user can navigate between days and view his entries
 */
public class DiaryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener,
        AppBarLayout.OnOffsetChangedListener, OnMonthChangedListener, OnDateSelectedListener,
        ViewPager.OnPageChangeListener, DayPageFragment.OnLogCardSelectedListener {
    /**
     * Extra keys for {@link Intent}s
     */
    public static final String EXTRA_IS_EDIT = "EDIT";
    public static final String EXTRA_CALENDAR = "DATE_TIME";
    public static final String EXTRA_DAY_ID = "DAY_KEY";
    public static final String EXTRA_ENTRY_INDEX = "LOG_INDEX";
    public static final String EXTRA_TYPE = "TYPE";

    /**
     * Request codes
     */
    public static final int REQ_ENTRY = 1;
    public static final int REQ_SETTINGS_CHANGE = 2;

    /**
     * Constants
     */
    private static final Calendar TODAY_CAL = CalendarDay.today().getCalendar(),
            MIN_CAL = CalendarDay.from(1900, 1, 1).getCalendar();
    private static final int TODAY_IDX = Day.daysBetween(TODAY_CAL, MIN_CAL);
    private static final int ARROW_START_ANGLE = 0, ARROW_END_ANGLE = -180;
    private static final long CALENDAR_RESIZE_ANIM_DURATION = 200;
    private static final int[] notificationDelays = {
            900000 /* 15min */,
            1800000 /* 30min */,
            3600000 /* 1h */,
            7200000 /* 2h */,
            10800000 /* 3h */};

    /**
     * Date formats for setting title & subtitle texts
     */
    private static final FastDateFormat DAY_MONTH_FORMAT = FastDateFormat.getInstance("EEE, MMM d");
    private static final FastDateFormat DAY_NUMBER_FORMAT = FastDateFormat.getInstance("d");
    private static final FastDateFormat MONTH_FORMAT = FastDateFormat.getInstance("MMMM");
    private static final FastDateFormat YEAR_FORMAT = FastDateFormat.getInstance("y");

    /**
     * GUI related variables
     */
    private DrawerLayout drawerLayout;
    private AppBarLayout appBar;
    private MaterialCalendarView calendarView;
    private final ValueAnimator.AnimatorUpdateListener calendarResizeAnimListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    setCalendarHeight((int) valueAnimator.getAnimatedValue());
                }
            };
    private int calBottomPad;
    private int calTileHeight;
    private TextView title, subtitle;
    private ImageView dropDownArrow;
    private ViewPager pager;

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
        dropDownLayout.setOnClickListener(this);

        // Displays the date
        title = (TextView) dropDownLayout.findViewById(R.id.toolbar_title);
        /*
        Indicates whether the shown day is today, displays the year number if not this year
        or is simply not shown (gone)
         */
        subtitle = (TextView) dropDownLayout.findViewById(R.id.toolbar_subtitle);

        // Small arrow which indicates that the layout can be clicked for a dropdown
        dropDownArrow = (ImageView) dropDownLayout.findViewById(R.id.dropdown_arrow);

        calendarView = (MaterialCalendarView) appBar.findViewById(R.id.calendar_view);

        // Sets the min (Jan 1st, 1900) & max (today) dates
        calendarView.state().edit().setMinimumDate(MIN_CAL).setMaximumDate(TODAY_CAL).commit();

        // The date string will be shown in the toolbar instead
        calendarView.setTopbarVisible(false);

        // Properly sets the width of each tile to fill the app bar
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        calendarView.setTileWidth((int) (displayMetrics.widthPixels / 7f + 0.5f));

        // Sets proper height according to the current month's rows
        final Resources resources = getResources();
        calBottomPad = resources.getDimensionPixelSize(R.dimen.tiny_margin);
        calTileHeight = resources.getDimensionPixelSize(R.dimen.calendar_view_tile_height);
        setCalendarHeight(calcCalendarHeight(TODAY_CAL));

        calendarView.setOnMonthChangedListener(this);
        calendarView.setOnDateChangedListener(this);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        /*
        The ViewPager will display each page as a day with its entries.
        Its onPageSelected is the master listener method behind the date navigation mechanism.
         */
        pager = (ViewPager) drawerLayout.findViewById(R.id.diary_pager);
        pager.setAdapter(new DayAdapter(getSupportFragmentManager()));
        pager.addOnPageChangeListener(this);
        pager.setCurrentItem(TODAY_IDX);

        /*
        Logic for temporary "welcome screen".
         */
        final SharedPreferences preferences = getPreferences(MODE_PRIVATE);

        if (preferences.getBoolean("FIRST_USE", true)) {
            drawerLayout.openDrawer(GravityCompat.START);
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.welcome))
                    .content(getString(R.string.welcome_content))
                    .canceledOnTouchOutside(false)
                    .positiveText(android.R.string.ok)
                    .show();
            preferences.edit().putBoolean("FIRST_USE", false).apply();
        }
    }

    private void addLog() {
        final CalendarDay cal = calendarView.getSelectedDate();
        final Calendar currTime = Calendar.getInstance();

        startActivityForResult(
                new Intent(this, EditEntryActivity.class)
                        .putExtra(EXTRA_IS_EDIT, false)
                        .putExtra(EXTRA_CALENDAR,
                                new GregorianCalendar(cal.getYear(), cal.getMonth(), cal.getDay(),
                                        currTime.get(Calendar.HOUR_OF_DAY),
                                        currTime.get(Calendar.MINUTE))), REQ_ENTRY);
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
        return TODAY_IDX - Day.daysBetween(TODAY_CAL, date.getCalendar());
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
        switch (item.getItemId()) {
            case R.id.action_today:
                pager.setCurrentItem(TODAY_IDX);
                break;

            case R.id.action_reminder:
                new MaterialDialog.Builder(this)
                        .customView(R.layout.dialog_reminder, false)
                        .positiveText(android.R.string.ok)
                        .negativeText(android.R.string.cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog,
                                                @NonNull DialogAction which) {
                                // noinspection ConstantConditions
                                scheduleNotification(getNotification(),
                                        notificationDelays[(((Spinner) dialog.getCustomView()
                                                .findViewById(R.id.reminder_time_spinner))
                                                .getSelectedItemPosition())]);
                            }
                        }).build().show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class),
                        REQ_SETTINGS_CHANGE);
                break;

            case R.id.nav_feedback:
                final StringBuilder footnoteBuilder = new StringBuilder();

                try {
                    footnoteBuilder
                            .append("App version: ")
                            .append(getPackageManager()
                                    .getPackageInfo(getPackageName(), 0).versionName)
                            .append('\n');
                } catch (PackageManager.NameNotFoundException ignored) {

                }

                final String footnote = footnoteBuilder
                        .append("OS: Android ")
                        .append(Build.VERSION.RELEASE)
                        .append(", API ")
                        .append(Build.VERSION.SDK_INT)
                        .append('\n')
                        .append("Device: ")
                        .append(Build.MANUFACTURER)
                        .append(' ')
                        .append(Build.MODEL)
                        .append('\n').toString();

                final Intent intent = new Intent(Intent.ACTION_SENDTO)
                        .setData(Uri.parse("mailto:"))
                        .putExtra(Intent.EXTRA_EMAIL, new String[]{"sugardays.app@gmail.com"})
                        .putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                        .putExtra(Intent.EXTRA_TEXT,
                                "Hello developer of SugarDays,\n\n" + footnote);

                if (intent.resolveActivity(getPackageManager()) != null)
                    startActivity(intent);
                break;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_ENTRY:
                pager.getAdapter().notifyDataSetChanged();

                if (resultCode == RESULT_OK && data.hasExtra(EXTRA_CALENDAR))
                    pager.setCurrentItem(getPageIndexFromDate(CalendarDay.from(
                            (Calendar) data.getSerializableExtra(EXTRA_CALENDAR))));
                break;

            case REQ_SETTINGS_CHANGE:
                pager.getAdapter().notifyDataSetChanged();
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
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
        if (date.getMonth() != widget.getSelectedDate().getMonth()) {
            // Then selected date becomes the first day of the month and pager must be updated
            calendarView.setSelectedDate(date);
            pager.setCurrentItem(getPageIndexFromDate(date));
        }

        // Animates the height of calendarView if necessary
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
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab:
                addLog();
                break;

            case R.id.dropdown_layout:
                appBar.setExpanded(isCalendarHidden());
                break;
        }
    }

    @Override
    public void onEntryCardSelected(int dayId, int entryIndex, View sharedView) {
        final Bundle sceneTransition;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            // noinspection unchecked
            sceneTransition = ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                    /*Pair.create(sharedView, sharedView.getTransitionName()),*/
                    Pair.create(findViewById(android.R.id.navigationBarBackground),
                            Window.NAVIGATION_BAR_BACKGROUND_TRANSITION_NAME)).toBundle();
        else sceneTransition = null;

        startActivityForResult(new Intent(this, ViewEntryActivity.class)
                .putExtra(EXTRA_DAY_ID, dayId)
                .putExtra(EXTRA_ENTRY_INDEX, entryIndex), REQ_ENTRY, sceneTransition);
    }

    private void scheduleNotification(Notification notification, int delay) {
        ((AlarmManager) getSystemService(Context.ALARM_SERVICE))
                .set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delay,
                        PendingIntent.getBroadcast(this, 0,
                                new Intent(this, NotificationPublisher.class)
                                        .putExtra(NotificationPublisher.NOTIFICATION_ID, 1)
                                        .putExtra(NotificationPublisher.NOTIFICATION, notification),
                                PendingIntent.FLAG_UPDATE_CURRENT));
    }

    private Notification getNotification() {
        final int color = ContextCompat.getColor(this, R.color.colorAccent);

        return new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.notification_reminder_title))
                .setContentText(getString(R.string.notification_reminder_content))
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(color)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{250, 1000})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setLights(color, 3000, 3000)
                .setContentIntent(TaskStackBuilder.create(this)
                        .addParentStack(EditEntryActivity.class)
                        .addNextIntent(new Intent(this, EditEntryActivity.class))
                        .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
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
            return DayPageFragment.newInstance(positionToDayId(position));
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        private int positionToDayId(int position) {
            final Calendar tempCal = (Calendar) TODAY_CAL.clone();
            tempCal.add(Calendar.DATE, position - TODAY_IDX);

            return Day.generateId(tempCal);
        }
    }
}
