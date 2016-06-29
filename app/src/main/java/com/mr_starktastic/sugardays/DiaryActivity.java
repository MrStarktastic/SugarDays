package com.mr_starktastic.sugardays;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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

import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DiaryActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        AppBarLayout.OnOffsetChangedListener {
    private static final int DAYS_IN_WEEK = 7, ARROW_ANGLE = -180;
    private static final Calendar CALENDAR = Calendar.getInstance();

    private static DrawerLayout drawerLayout;
    private static AppBarLayout appBar;
    private static Toolbar toolbar;
    private static MaterialCalendarView calendarView;
    private static ViewGroup dropDownLayout;
    private static TextView title, subtitle;
    private static ImageView dropDownArrow;
    private static FloatingActionButton fab;

    private static boolean isCalendarShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diary);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        appBar = (AppBarLayout) drawerLayout.findViewById(R.id.app_bar);
        appBar.addOnOffsetChangedListener(this);
        toolbar = (Toolbar) appBar.findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        calendarView = (MaterialCalendarView) appBar.findViewById(R.id.calendar_view);
        calendarView.setDynamicHeightEnabled(true);
        calendarView.setTopbarVisible(false);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        calendarView.setTileWidth((displayMetrics.widthPixels - getResources()
                .getDimensionPixelSize(R.dimen.small_margin)) / DAYS_IN_WEEK);

        dropDownLayout = (ViewGroup) toolbar.findViewById(R.id.dropdown_layout);
        title = (TextView) dropDownLayout.findViewById(R.id.toolbar_title);
        subtitle = (TextView) dropDownLayout.findViewById(R.id.toolbar_subtitle);
        dropDownArrow = (ImageView) dropDownLayout.findViewById(R.id.dropdown_arrow);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d", Locale.getDefault());
        title.setText(dateFormat.format(CALENDAR.getTime()));
        subtitle.setText(R.string.today);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(v ->
                Snackbar.make(v, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show());

        dropDownLayout.setOnClickListener(v -> toggleCalendarVisibility());

        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void toggleCalendarVisibility() {
        if (isCalendarShown) {
            appBar.setExpanded(false, true);
        } else {
            appBar.setExpanded(true, true);
        }
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressLint("InflateParams")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.diary, menu);
        final View todayIconView = getLayoutInflater().inflate(R.layout.ic_today_num, null);

        // Set today's number
        ((TextView) todayIconView.findViewById(R.id.today_num_text))
                .setText(String.valueOf(CALENDAR.get(Calendar.DAY_OF_MONTH)));

        // Convert the layout into a bitmap for use as an icon
        todayIconView.setDrawingCacheEnabled(true);
        todayIconView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        todayIconView.layout(0, 0,
                todayIconView.getMeasuredWidth(), todayIconView.getMeasuredHeight());
        todayIconView.buildDrawingCache(true);
        final Bitmap bitmap = Bitmap.createBitmap(todayIconView.getDrawingCache());
        todayIconView.setDrawingCacheEnabled(false);
        todayIconView.setVisibility(View.GONE);

        // Set today's icon
        menu.findItem(R.id.action_today).setIcon(new BitmapDrawable(getResources(), bitmap));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_today:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        drawerLayout.closeDrawer(GravityCompat.START);

        return false;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        dropDownArrow.setRotation(
                ARROW_ANGLE * (1 + (float) verticalOffset / calendarView.getHeight()));
        isCalendarShown = verticalOffset == 0;
    }
}
