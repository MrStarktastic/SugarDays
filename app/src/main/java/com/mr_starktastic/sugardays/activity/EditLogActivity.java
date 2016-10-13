package com.mr_starktastic.sugardays.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.mr_starktastic.sugardays.R;

import org.apache.commons.lang3.time.FastDateFormat;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class EditLogActivity extends AppCompatActivity {
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private static FastDateFormat TIME_FORMAT =
            FastDateFormat.getTimeInstance(FastDateFormat.SHORT);
    private static FastDateFormat DATE_FORMAT =
            FastDateFormat.getInstance(FastDateFormat.getInstance("EEE, ").getPattern() +
                    FastDateFormat.getDateInstance(FastDateFormat.MEDIUM).getPattern());

    private GregorianCalendar calendar;

    private AppCompatSpinner typeSpinner;
    private TextView dateText, timeText;
    private EditText locationEdit;
    private ImageButton currentLocationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_log);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        calendar = (GregorianCalendar) getIntent().getSerializableExtra(DiaryActivity.EXTRA_DATE);

        typeSpinner = (AppCompatSpinner) findViewById(R.id.log_type_spinner);
        dateText = (TextView) findViewById(R.id.date_text);
        timeText = (TextView) findViewById(R.id.time_text);
        locationEdit = (EditText) findViewById(R.id.location_edit_text);
        currentLocationButton = (ImageButton) findViewById(R.id.current_location_button);

        dateText.setText(DATE_FORMAT.format(calendar));
        final DatePickerDialog dateDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    dateText.setText(DATE_FORMAT.format(calendar));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dateText.setOnClickListener(v -> dateDialog.show());

        timeText.setText(TIME_FORMAT.format(calendar));
        final TimePickerDialog timeDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendar.set(Calendar.MINUTE, minute);
                    timeText.setText(TIME_FORMAT.format(calendar));
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this));
        timeText.setOnClickListener(v -> timeDialog.show());

        currentLocationButton.setOnClickListener(v -> openPlacePicker());
    }

    private void openPlacePicker() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        final PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException |
                GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    private void setLocationText(Place place) {
        if (!place.getAddress().toString().isEmpty()) {
            if (!place.getAddress().toString().contains(place.getName()))
                locationEdit.setText(place.getName() + ", " + place.getAddress());
            else locationEdit.setText(place.getAddress());
        } else locationEdit.setText(place.getName());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PLACE_PICKER_REQUEST:
                if (resultCode == RESULT_OK)
                    setLocationText(PlacePicker.getPlace(this, data));
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openPlacePicker();
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
