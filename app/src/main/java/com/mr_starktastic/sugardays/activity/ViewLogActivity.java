package com.mr_starktastic.sugardays.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.Day;
import com.mr_starktastic.sugardays.data.SugarLog;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ViewLogActivity extends AppCompatActivity implements View.OnClickListener {
    private SugarLog log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_log);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        ActivityCompat.postponeEnterTransition(this);

        final Intent intent = getIntent();
        // noinspection ConstantConditions
        log = Day.findById(intent.getIntExtra(DiaryActivity.EXTRA_DAY_ID, 0))
                .getLogs()[intent.getIntExtra(DiaryActivity.EXTRA_LOG_INDEX, 0)];

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

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close);

        final String photoPath = log.getPhotoPath();
        int primaryColor = log.getPrimaryColor();

        if (primaryColor == 0)
            primaryColor = ContextCompat.getColor(this, R.color.colorPrimary);
        if (photoPath != null)
            Picasso.with(this).load(log.getPhotoPath()).noFade().fit().centerCrop()
                    .into(photoView, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                            new Callback() {
                                private void startPostponedEnterTransition() {
                                    ActivityCompat.startPostponedEnterTransition(
                                            ViewLogActivity.this);
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
        else ActivityCompat.startPostponedEnterTransition(ViewLogActivity.this);

        collapsingToolbarLayout.setContentScrimColor(primaryColor);
        collapsingToolbarLayout.setStatusBarScrimColor(primaryColor);
        dateTimeIcon.setColorFilter(primaryColor);
        locationIcon.setColorFilter(primaryColor);

        collapsingToolbarLayout.setTitle(
                getResources().getStringArray(R.array.log_types)[log.getType()]);

        final long time = log.getTime();
        dateText.setText(SugarLog.DATE_FORMAT.format(time));
        timeText.setText(SugarLog.TIME_FORMAT.format(time));

        final String location = log.getLocation();

        if (location != null) {
            locationText.setText(location);
            locationText.setOnClickListener(this);
        } else ((View) locationIcon.getParent()).setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.location_text:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=" + Uri.encode(log.getLocation())))
                        .setPackage("com.google.android.apps.maps"));
                break;
        }
    }
}
