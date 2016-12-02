package com.mr_starktastic.sugardays.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.data.Day;
import com.mr_starktastic.sugardays.data.SugarEntry;
import com.mr_starktastic.sugardays.util.NumericTextUtil;
import com.mr_starktastic.sugardays.util.PrefUtil;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ViewEntryActivity extends AppCompatActivity implements View.OnClickListener {
    private SugarEntry entry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entry);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        final Intent intent = getIntent();
        // noinspection ConstantConditions
        entry = Day.findById(intent.getIntExtra(DiaryActivity.EXTRA_DAY_ID, 0))
                .getEntries()[intent.getIntExtra(DiaryActivity.EXTRA_ENTRY_INDEX, 0)];
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final FloatingActionButton editFab = (FloatingActionButton) findViewById(R.id.edit_button);
        final CollapsingToolbarLayout collapsingToolbarLayout =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);
        final Toolbar toolbar = (Toolbar) collapsingToolbarLayout.findViewById(R.id.toolbar);
        final ImageView photoView =
                (ImageView) collapsingToolbarLayout.findViewById(R.id.photo_view);
        final LinearLayout scrollViewContent =
                (LinearLayout) findViewById(R.id.scroll_view_content);
        final ImageView dateTimeIcon =
                (ImageView) scrollViewContent.findViewById(R.id.date_time_icon);
        final TextView dateText = (TextView) scrollViewContent.findViewById(R.id.date_text);
        final TextView timeText = (TextView) scrollViewContent.findViewById(R.id.time_text);
        final ImageView locationIcon =
                (ImageView) scrollViewContent.findViewById(R.id.location_icon);
        final TextView locationText = (TextView) scrollViewContent.findViewById(R.id.location_text);
        final ImageView bloodSugarIcon =
                (ImageView) scrollViewContent.findViewById(R.id.blood_sugar_icon);
        final TextView bloodSugarText =
                (TextView) scrollViewContent.findViewById(R.id.blood_sugar_text);

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close);

        editFab.setOnClickListener(this);

        final String photoPath = entry.getPhotoPath();
        int primaryColor = entry.getPrimaryColor();

        if (primaryColor == 0)
            primaryColor = ContextCompat.getColor(this, R.color.colorPrimary);
        if (photoPath != null) {
            ActivityCompat.postponeEnterTransition(this);

            Picasso.with(this).load(entry.getPhotoPath()).noFade().fit().centerCrop()
                    .into(photoView, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                            new Callback() {
                                private void startPostponedEnterTransition() {
                                    ActivityCompat.startPostponedEnterTransition(
                                            ViewEntryActivity.this);
                                }

                                @Override
                                public void onSuccess() {
                                    startPostponedEnterTransition();
                                }

                                @Override
                                public void onError() {
                                    startPostponedEnterTransition();
                                }
                            } : null);
        }

        collapsingToolbarLayout.setContentScrimColor(primaryColor);
        collapsingToolbarLayout.setStatusBarScrimColor(primaryColor);
        dateTimeIcon.setColorFilter(primaryColor);
        locationIcon.setColorFilter(primaryColor);
        bloodSugarIcon.setColorFilter(primaryColor);

        collapsingToolbarLayout.setTitle(
                getResources().getStringArray(R.array.entry_types)[entry.getType()]);

        final long time = entry.getTime();
        dateText.setText(SugarEntry.DATE_FORMAT.format(time));
        timeText.setText(SugarEntry.TIME_FORMAT.format(time));

        final String location = entry.getLocation();

        if (location != null) {
            locationText.setText(location);
            locationText.setOnClickListener(this);
        } else ((View) locationIcon.getParent()).setVisibility(View.GONE);

        final BloodSugar bloodSugar = entry.getBloodSugar();

        if (bloodSugar != null) {
            final int bgUnitIdx = PrefUtil.getBgUnitIdx(preferences);
            final float val = bloodSugar.get(bgUnitIdx);
            final BloodSugar optimalBg = PrefUtil.getOptimalBg(preferences);
            final String deviationStr;

            if (optimalBg != null) {
                final float deviationVal = val - optimalBg.get(bgUnitIdx);
                deviationStr = "(" + (deviationVal >= 0 ? "+" : "") +
                        NumericTextUtil.trim(deviationVal) + ")";
            } else deviationStr = "";

            bloodSugarText.setText(NumericTextUtil.trim(val) + " " +
                    getResources().getStringArray(R.array.pref_bgUnits_entries)[bgUnitIdx] + " " +
                    deviationStr);

            final int bColor = ContextCompat.getColor(this, R.color.colorBadBloodGlucose);
            final int mColor = ContextCompat.getColor(this, R.color.colorIntermediateBloodGlucose);
            final int gColor = ContextCompat.getColor(this, R.color.colorGoodBloodGlucose);
            final float hypo = PrefUtil.getHypo(preferences).get(bgUnitIdx);
            final BloodSugar[] targetRngBg = PrefUtil.getTargetRange(preferences);
            final float minTarget = targetRngBg[0].get(bgUnitIdx);
            final float maxTarget = targetRngBg[1].get(bgUnitIdx);
            final float hyper = PrefUtil.getHyper(preferences).get(bgUnitIdx);

            bloodSugarText.setTextColor(val <= hypo || val >= hyper ? bColor :
                    val >= minTarget && val <= maxTarget ? gColor : mColor);
        } else ((View) bloodSugarText.getParent()).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.location_text:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=" + Uri.encode(entry.getLocation())))
                        .setPackage("com.google.android.apps.maps"));
                break;

            case R.id.edit_button:
                // TODO: Launch EditEntryActivity
                break;
        }
    }
}
