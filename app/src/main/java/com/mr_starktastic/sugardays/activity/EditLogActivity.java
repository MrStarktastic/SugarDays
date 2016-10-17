package com.mr_starktastic.sugardays.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.gson.Gson;
import com.mr_starktastic.sugardays.BuildConfig;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.preference.PrefKeys;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import id.zelory.compressor.Compressor;

/**
 * Activity for adding new logs or editing existing ones
 */
public class EditLogActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener {
    /**
     * Request codes
     */
    private static final int REQ_PLACE_PICKER = 1;
    private static final int REQ_TAKE_PHOTO = 2;
    private static final int REQ_CHOOSE_PHOTO = 3;

    /**
     * Permission request codes
     */
    private static final int PERMISSION_REQ_PLACE_DETECT = 1;
    private static final int PERMISSION_REQ_PLACE_PICKER = 2;
    private static final int PERMISSION_REQ_CAMERA = 3;

    /**
     * Action indices from the Camera section
     */
    private static final int MENU_REMOVE_PHOTO = 0, MENU_TAKE_PHOTO = 1, MENU_CHOOSE_PHOTO = 2;

    /**
     * Constants
     */
    private static final String FILE_PROVIDER_AUTH = BuildConfig.APPLICATION_ID + ".provider";
    private static final String PHOTO_CHOOSER_TYPE = "image/*";
    private static final String IMAGE_FILE_SUFFIX = ".jpg";
    private static final int BG_VAL_MGDL_MAX_LENGTH = 3, BG_VAL_MMOLL_MAX_LENGTH = 4;

    /**
     * Instance keys
     */
    private static final String KEY_INSTANCE_PHOTO_PATH = "path", KEY_INSTANCE_BITMAP = "bitmap";

    /**
     * Formats
     */
    private static FastDateFormat TIME_FORMAT =
            FastDateFormat.getTimeInstance(FastDateFormat.SHORT);
    private static FastDateFormat DATE_FORMAT =
            FastDateFormat.getInstance(FastDateFormat.getInstance("EEE, ").getPattern() +
                    FastDateFormat.getDateInstance(FastDateFormat.MEDIUM).getPattern());
    private static FastDateFormat FILE_DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd_HHmmss");

    /**
     * Member variables
     */
    private GregorianCalendar calendar;
    private GoogleApiClient googleApiClient;
    private String currentPhotoPath;
    private boolean photoCompressEnabled;

    /**
     * Views
     */
    private MenuItem saveButton;
    private AppCompatSpinner typeSpinner;
    private TextView dateText, timeText;
    private EditText locationEdit;
    private ImageView photoThumbnailView;
    private EditText bloodSugarEdit;

    /**
     * @param file File to extract path from.
     * @return A "file:absolutePath" string for use with ACTION_VIEW intents.
     */
    private static String getFilePath(File file) {
        return "file:" + file.getAbsolutePath();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_log);

        final Intent intent = getIntent();
        final Gson gson = new Gson();
        final ActionBar actionBar = getSupportActionBar();
        // Setting the ActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_close);
            // TODO: Title should be either "Add log" or "Edit log", based on data from intent.
            actionBar.setTitle(getString(R.string.action_title_add_log));
        }

        // Wiring-up views
        typeSpinner = (AppCompatSpinner) findViewById(R.id.log_type_spinner);
        dateText = (TextView) findViewById(R.id.date_text);
        timeText = (TextView) findViewById(R.id.time_text);
        locationEdit = (EditText) findViewById(R.id.location_edit_text);
        final ImageView placePickerButton = (ImageView) findViewById(R.id.current_location_button);
        final AppCompatButton changePhotoButton = (AppCompatButton) findViewById(R.id.photo_button);
        final PopupMenu popupMenu = new PopupMenu(this, changePhotoButton);
        popupMenu.inflate(R.menu.photo_change);
        final Menu changePhotoMenu = popupMenu.getMenu();
        photoThumbnailView = (ImageView) findViewById(R.id.thumbnail_photo);
        bloodSugarEdit = (EditText) findViewById(R.id.blood_sugar_edit_text);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Log type
        typeSpinner.setSelection(intent.getIntExtra(DiaryActivity.EXTRA_TYPE, 0));

        // Date
        calendar = (GregorianCalendar) intent.getSerializableExtra(DiaryActivity.EXTRA_DATE);
        dateText.setText(DATE_FORMAT.format(calendar));
        final DatePickerDialog dateDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    dateText.setText(DATE_FORMAT.format(calendar));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dateDialog.getDatePicker().setMaxDate(new Date().getTime());
        dateText.setOnClickListener(v -> dateDialog.show());

        // Time
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

        // Automatic current location fetch
        if (savedInstanceState == null &&
                preferences.getBoolean(PrefKeys.AUTO_LOCATION, false)) {
            googleApiClient = new GoogleApiClient
                    .Builder(this)
                    .addApi(Places.PLACE_DETECTION_API)
                    .enableAutoManage(this, this)
                    .build();

            getCurrentLocation();
        }

        // Listener for invoking the PlacePicker
        placePickerButton.setOnClickListener(v -> openPlacePicker());

        // Listeners for photo change button (and its menu items)
        final PackageManager packageManager = getPackageManager();

        changePhotoMenu.getItem(MENU_REMOVE_PHOTO).setOnMenuItemClickListener(item -> {
            // noinspection ResultOfMethodCallIgnored
            new File(Uri.parse(currentPhotoPath).getPath()).delete();
            currentPhotoPath = null;
            photoThumbnailView.setImageBitmap(null);

            return true;
        });

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA))
            changePhotoMenu.getItem(MENU_TAKE_PHOTO).setOnMenuItemClickListener(item -> {
                if (!checkPermission(PERMISSION_REQ_CAMERA, Manifest.permission.CAMERA))
                    return false;

                dispatchTakePictureIntent();
                return true;
            });

        changePhotoMenu.getItem(MENU_CHOOSE_PHOTO).setOnMenuItemClickListener(item -> {
            startActivityForResult(Intent.createChooser(
                    new Intent(Intent.ACTION_GET_CONTENT).setType(PHOTO_CHOOSER_TYPE),
                    item.getTitle()), REQ_CHOOSE_PHOTO);

            return true;
        });

        changePhotoButton.setOnClickListener(v -> {
            final MenuItem removeMenuItem = changePhotoMenu.getItem(MENU_REMOVE_PHOTO);

            if (currentPhotoPath != null) {
                if (!removeMenuItem.isVisible()) {
                    removeMenuItem.setVisible(true);
                    changePhotoMenu.getItem(MENU_TAKE_PHOTO).setTitle(R.string.take_new_photo);
                    changePhotoMenu.getItem(MENU_CHOOSE_PHOTO).setTitle(R.string.choose_new_photo);
                }
            } else if (removeMenuItem.isVisible()) {
                removeMenuItem.setVisible(false);
                changePhotoMenu.getItem(MENU_TAKE_PHOTO).setTitle(R.string.take_photo);
                changePhotoMenu.getItem(MENU_CHOOSE_PHOTO).setTitle(R.string.choose_photo);
            }

            popupMenu.show();
        });

        photoCompressEnabled = preferences.getBoolean(PrefKeys.COMPRESS_PHOTOS, false);

        // Blood sugar
        final float hypo, minTargetRng, maxTargetRng, hyper;
        final BloodSugar hypoBG = gson.fromJson(preferences.getString(PrefKeys.HYPO,
                gson.toJson(BloodSugar.DEFAULT_HYPO)), BloodSugar.class);
        final BloodSugar[] targetRngBG = gson.fromJson(preferences.getString(PrefKeys.TARGET_RANGE,
                gson.toJson(BloodSugar.DEFAULT_TARGET_RNG)), BloodSugar[].class);
        final BloodSugar hyperBG = gson.fromJson(preferences.getString(PrefKeys.HYPER,
                gson.toJson(BloodSugar.DEFAULT_HYPER)), BloodSugar.class);
        final int badBgColor = ContextCompat.getColor(this, R.color.colorBadBloodGlucose);
        final int medBgColor = ContextCompat.getColor(this, R.color.colorIntermediateBloodGlucose);
        final int goodBgColor = ContextCompat.getColor(this, R.color.colorGoodBloodGlucose);
        final int bgUnitIdx = Integer.parseInt(preferences.getString(PrefKeys.BG_UNITS, "0"));
        final TextView bloodSugarLabel = (TextView) findViewById(R.id.blood_sugar_label);
        bloodSugarLabel.setText(
                getResources().getStringArray(R.array.pref_bgUnits_entries)[bgUnitIdx]);

        if (bgUnitIdx == BloodSugar.MGDL_IDX) {
            hypo = hypoBG.getMgdl();
            minTargetRng = targetRngBG[0].getMgdl();
            maxTargetRng = targetRngBG[1].getMgdl();
            hyper = hyperBG.getMgdl();

            bloodSugarEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
            bloodSugarEdit.setFilters(new InputFilter[]{
                    new InputFilterMax(BloodSugar.getMaxValues().getMgdl()),
                    new InputFilter.LengthFilter(BG_VAL_MGDL_MAX_LENGTH)
            });
        } else {
            hypo = hypoBG.getMmoll();
            minTargetRng = targetRngBG[0].getMmoll();
            maxTargetRng = targetRngBG[1].getMmoll();
            hyper = hyperBG.getMmoll();

            bloodSugarEdit.setInputType(
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            bloodSugarEdit.setFilters(new InputFilter[]{
                    new InputFilterMax(BloodSugar.getMaxValues().getMmoll()),
                    new InputFilter.LengthFilter(BG_VAL_MMOLL_MAX_LENGTH)
            });
        }

        bloodSugarEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final float val;

                try {
                    val = Float.parseFloat(s.toString());
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    return;
                }

                bloodSugarEdit.setTextColor(val <= hypo || val >= hyper ? badBgColor :
                        val >= minTargetRng && val <= maxTargetRng ? goodBgColor : medBgColor);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    /**
     * Check if the given permissions were granted for the user.
     * If not, delegates them to be requested.
     *
     * @param requestCode Code to identify denied permissions from
     *                    {@link #onActivityResult(int, int, Intent)}.
     * @param permissions Names of permissions to check.
     * @return True if all permissions are granted, false otherwise (need to ask user).
     */
    private boolean checkPermission(int requestCode, String... permissions) {
        final ArrayList<String> notGranted = new ArrayList<>();

        for (String p : permissions)
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED)
                notGranted.add(p);

        if (!notGranted.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    notGranted.toArray(new String[notGranted.size()]),
                    requestCode);
            return false;
        }

        return true;
    }

    /**
     * Gets the current location by extracting the {@link Place} with the highest likelihood
     * and then sets the appropriate {@link EditText} with the place's description.
     */
    private void getCurrentLocation() {
        if (!checkPermission(PERMISSION_REQ_PLACE_DETECT, Manifest.permission.ACCESS_FINE_LOCATION))
            return;

        final PendingResult<PlaceLikelihoodBuffer> result =
                Places.PlaceDetectionApi.getCurrentPlace(googleApiClient, null);
        result.setResultCallback(likelyPlaces -> {
            if (likelyPlaces.getCount() != 0)
                setLocationText(likelyPlaces.get(0).getPlace());

            likelyPlaces.release();
            googleApiClient.disconnect();
        });
    }

    /**
     * Opens the {@link PlacePicker}.
     */
    private void openPlacePicker() {
        if (!checkPermission(PERMISSION_REQ_PLACE_PICKER, Manifest.permission.ACCESS_FINE_LOCATION))
            return;

        final PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), REQ_PLACE_PICKER);
        } catch (GooglePlayServicesRepairableException |
                GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the location {@link EditText} with the description of a given {@link Place}.
     * The description might be the the place's name, address or a combination of both.
     *
     * @param place Was given to set the text with.
     */
    private void setLocationText(Place place) {
        final String address = place.getAddress().toString(), name = place.getName().toString();
        locationEdit.setText(!address.isEmpty() ? !address.contains(name) ?
                name + ", " + address : address : name);
    }

    /**
     * Creates a file to hold a photo.
     *
     * @return The created file.
     * @throws IOException if the file wasn't created successfully.
     */
    private File createImageFile() throws IOException {
        final File imageFile = File.createTempFile(
                "JPEG_" + FILE_DATE_FORMAT.format(new Date()) + "_",
                IMAGE_FILE_SUFFIX,
                getExternalCacheDir());
        currentPhotoPath = getFilePath(imageFile);

        return imageFile;

    }

    /**
     * Fires-up the intent to take a picture with the camera
     * (at this point, the device is guaranteed to have a camera).
     */
    private void dispatchTakePictureIntent() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File imageFile;

            try {
                imageFile = createImageFile();
            } catch (IOException ex) {
                return;
            }

            // Continue only if the File was successfully created
            startActivityForResult(intent.putExtra(MediaStore.EXTRA_OUTPUT,
                    FileProvider.getUriForFile(this, FILE_PROVIDER_AUTH, imageFile)),
                    REQ_TAKE_PHOTO);
        }
    }

    /**
     * Sets the photo to be viewed in this activity as a thumbnail and compresses if needed.
     *
     * @param imageFile The file containing the photo.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    private void setPhoto(File imageFile) {
        if (photoCompressEnabled) {
            currentPhotoPath = getFilePath(new Compressor.Builder(this)
                    .setDestinationDirectoryPath(getExternalCacheDir().getPath())
                    .setQuality(95)
                    .setMaxWidth(1920)
                    .setMaxHeight(1080)
                    .build()
                    .compressToFile(imageFile));
            imageFile.delete();
        }

        Picasso.with(this).load(currentPhotoPath).fit().centerCrop().into(photoThumbnailView);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_INSTANCE_PHOTO_PATH, currentPhotoPath);

        final Drawable thumbnail = photoThumbnailView.getDrawable();

        if (thumbnail != null)
            outState.putParcelable(KEY_INSTANCE_BITMAP, ((BitmapDrawable) thumbnail).getBitmap());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        currentPhotoPath = savedInstanceState.getString(KEY_INSTANCE_PHOTO_PATH);
        photoThumbnailView.setImageBitmap(savedInstanceState.getParcelable(KEY_INSTANCE_BITMAP));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_PLACE_PICKER:
                if (resultCode == RESULT_OK)
                    setLocationText(PlacePicker.getPlace(this, data));
                break;

            case REQ_TAKE_PHOTO:
                final File origFile = new File(Uri.parse(currentPhotoPath).getPath());

                if (resultCode == RESULT_OK)
                    setPhoto(origFile);
                else {
                    origFile.delete();
                    currentPhotoPath = null;

                    // Retrieves previous file
                    final File extCacheDir = getExternalCacheDir();

                    if (extCacheDir != null && extCacheDir.exists()) {
                        final File[] files = extCacheDir.listFiles();

                        if (files != null && files.length > 0)
                            currentPhotoPath = getFilePath(files[0]);
                    }
                }
                break;

            case REQ_CHOOSE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try (final InputStream inputStream =
                                 getContentResolver().openInputStream(data.getData())) {
                        if (inputStream != null) {
                            // Deletes previous file
                            if (currentPhotoPath != null)
                                new File(Uri.parse(currentPhotoPath).getPath()).delete();

                            final File chosenFile = createImageFile();
                            FileUtils.copyInputStreamToFile(inputStream, chosenFile);
                            setPhoto(chosenFile);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            switch (requestCode) {
                case PERMISSION_REQ_PLACE_DETECT:
                    getCurrentLocation();
                    break;

                case PERMISSION_REQ_PLACE_PICKER:
                    openPlacePicker();
                    break;

                case PERMISSION_REQ_CAMERA:
                    dispatchTakePictureIntent();
                    break;

                default:
                    break;
            }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_log, menu);
        (saveButton = menu.findItem(R.id.action_save)).setEnabled(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_save:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    private class InputFilterMax implements InputFilter {
        private float max;

        private InputFilterMax(float max) {
            this.max = max;
        }

        @Override
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            try {
                if (Float.parseFloat(dest.toString() + source.toString()) <= max)
                    return null;
            } catch (NumberFormatException ignored) {
            }
            return "";
        }
    }
}