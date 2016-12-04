package com.mr_starktastic.sugardays.activity;

import android.Manifest;
import android.animation.LayoutTransition;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.PopupMenu;
import android.text.InputFilter;
import android.text.InputType;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.mr_starktastic.sugardays.BuildConfig;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.data.Day;
import com.mr_starktastic.sugardays.data.Food;
import com.mr_starktastic.sugardays.data.Pill;
import com.mr_starktastic.sugardays.data.Serving;
import com.mr_starktastic.sugardays.data.SugarEntry;
import com.mr_starktastic.sugardays.data.TempBasal;
import com.mr_starktastic.sugardays.fragment.FoodDialogFragment;
import com.mr_starktastic.sugardays.fragment.TempBasalDialogFragment;
import com.mr_starktastic.sugardays.text.InputFilterMax;
import com.mr_starktastic.sugardays.text.SimpleTextWatcher;
import com.mr_starktastic.sugardays.util.NumericTextUtil;
import com.mr_starktastic.sugardays.util.PrefUtil;
import com.mr_starktastic.sugardays.widget.LabeledEditText;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.FastDateFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;

import id.zelory.compressor.Compressor;

/**
 * Activity for adding new entries or editing existing ones
 */
public class EditEntryActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener, MenuItem.OnMenuItemClickListener,
        ResultCallback<PlaceLikelihoodBuffer> {
    /**
     * Request codes
     */
    private static final int REQ_PLACE_PICKER = 1;
    private static final int REQ_TAKE_PHOTO = 2;
    private static final int REQ_CHOOSE_PHOTO = 3;

    /**
     * Permission request codes
     */
    private static final int PERMISSION_REQ_IGNORED = 0;
    private static final int PERMISSION_REQ_PLACE_DETECT = 1;
    private static final int PERMISSION_REQ_PLACE_PICKER = 2;
    private static final int PERMISSION_REQ_CHOOSE_PHOTO = 3;
    private static final int PERMISSION_REQ_CAMERA = 4;

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

    private static FastDateFormat FILE_DATE_FORMAT = FastDateFormat.getInstance("yyyyMMdd_HHmmss");

    /**
     * Member variables
     */
    private GregorianCalendar calendar;
    private boolean isEdit;
    private Day oldDay;
    private int oldEntryIdx;
    private GoogleApiClient googleApiClient;
    private String currentPhotoPath;
    private boolean photoCompressEnabled, bolusPredictEnabled;
    private ArrayList<Food> foods;
    private float carbSum = 0;
    private TempBasal tempBasal;
    private String[] pillNames;
    private ArrayList<Pill> pills;

    /**
     * Views
     */
    private AppCompatSpinner typeSpinner;
    private TextView dateText, timeText;
    private EditText locationEdit;
    private ImageView photoThumbnailView;
    private LabeledEditText bloodSugarEdit;
    private LinearLayout foodEntryContainer;
    private LabeledEditText corrBolusEdit, mealBolusEdit, basalEdit;
    private TextView tempBasalText;
    private Button clearTempBasalButton;
    private LinearLayout pillEntryContainer;
    private EditText notesEdit;

    /**
     * @param file File to extract path from.
     * @return A "file:absolutePath" string for use with ACTION_VIEW intents.
     */
    private static String getFilePath(File file) {
        return "file:" + file.getAbsolutePath();
    }

    private static boolean isPermissionGranted(int requestCode, int[] grantResults) {
        return requestCode > 0 &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_entry);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        final Intent intent = getIntent();

        // Setting the ActionBar
        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close);

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Places.PLACE_DETECTION_API)
                .addOnConnectionFailedListener(this)
                .build();

        // Wiring-up views
        final LinearLayout root = (LinearLayout) findViewById(R.id.scroll_view_content);
        root.getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
        typeSpinner = (AppCompatSpinner) root.findViewById(R.id.entry_type_spinner);
        dateText = (TextView) root.findViewById(R.id.date_text);
        timeText = (TextView) root.findViewById(R.id.time_text);
        locationEdit = (EditText) root.findViewById(R.id.location_edit_text);
        final ImageView placePickerButton =
                (ImageView) root.findViewById(R.id.current_location_button);
        final AppCompatButton changePhotoButton =
                (AppCompatButton) root.findViewById(R.id.photo_button);
        final PopupMenu popupMenu = new PopupMenu(this, changePhotoButton);
        popupMenu.inflate(R.menu.photo_change);
        final Menu changePhotoMenu = popupMenu.getMenu();
        photoThumbnailView = (ImageView) root.findViewById(R.id.thumbnail_photo);
        bloodSugarEdit = (LabeledEditText) root.findViewById(R.id.blood_sugar_edit_text);
        foodEntryContainer = (LinearLayout) root.findViewById(R.id.food_entry_container);
        final View insulinEditContainer = root.findViewById(R.id.insulin_edit_container);
        corrBolusEdit =
                (LabeledEditText) insulinEditContainer.findViewById(R.id.bolus_correction_edit);
        corrBolusEdit.setLabels(
                (TextView) insulinEditContainer.findViewById(R.id.bolus_correction_label));
        mealBolusEdit =
                (LabeledEditText) insulinEditContainer.findViewById(R.id.bolus_meal_edit);
        mealBolusEdit.setLabels(
                (TextView) insulinEditContainer.findViewById(R.id.bolus_meal_label));
        final View basalEditContainer =
                insulinEditContainer.findViewById(R.id.basal_edit_container);
        final View tempBasalEditContainer =
                insulinEditContainer.findViewById(R.id.temp_basal_edit_container);
        pillEntryContainer = (LinearLayout) root.findViewById(R.id.pill_entry_container);
        notesEdit = (EditText) root.findViewById(R.id.notes_edit);

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        loadOldData(intent);

        // SugarEntry type
        typeSpinner.setSelection(intent.getIntExtra(DiaryActivity.EXTRA_TYPE, 0));

        // Date
        dateText.setText(SugarEntry.DATE_FORMAT.format(calendar));
        final DatePickerDialog dateDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        calendar.set(year, month, dayOfMonth);
                        dateText.setText(SugarEntry.DATE_FORMAT.format(calendar));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dateDialog.getDatePicker().setMaxDate(new Date().getTime());
        dateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dateDialog.show();
            }
        });

        // Time
        timeText.setText(SugarEntry.TIME_FORMAT.format(calendar));
        final TimePickerDialog timeDialog = new TimePickerDialog(this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        timeText.setText(SugarEntry.TIME_FORMAT.format(calendar));
                    }
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                DateFormat.is24HourFormat(this));
        timeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeDialog.show();
            }
        });

        // Automatic current location fetch
        if (locationEdit.getText().length() == 0 && savedInstanceState == null &&
                PrefUtil.getAutoLocation(preferences) &&
                checkPermission(PERMISSION_REQ_PLACE_DETECT,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
            googleApiClient.connect();
            Places.PlaceDetectionApi
                    .getCurrentPlace(googleApiClient, null)
                    .setResultCallback(this);
        }

        // Listener for invoking the PlacePicker
        placePickerButton.setOnClickListener(this);

        // Listeners for photo change button (and its menu items)
        changePhotoMenu.getItem(MENU_REMOVE_PHOTO).setOnMenuItemClickListener(this);
        changePhotoMenu.getItem(MENU_TAKE_PHOTO).setOnMenuItemClickListener(this);
        changePhotoMenu.getItem(MENU_CHOOSE_PHOTO).setOnMenuItemClickListener(this);

        changePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        photoCompressEnabled = PrefUtil.getPhotoCompressEnabled(preferences);

        // Blood sugar
        final float hypo, minTargetRng, maxTargetRng, hyper;
        final BloodSugar hypoBG = PrefUtil.getHypo(preferences);
        final BloodSugar[] targetRngBG = PrefUtil.getTargetRange(preferences);
        final BloodSugar hyperBG = PrefUtil.getHyper(preferences);
        final int badBgColor = ContextCompat.getColor(this, R.color.colorBadBloodGlucose);
        final int medBgColor = ContextCompat.getColor(this, R.color.colorIntermediateBloodGlucose);
        final int goodBgColor = ContextCompat.getColor(this, R.color.colorGoodBloodGlucose);
        final int bgUnitIdx = PrefUtil.getBgUnitIdx(preferences);
        final TextView bloodSugarLabel = (TextView) findViewById(R.id.blood_sugar_label);
        bloodSugarEdit.setLabels(bloodSugarLabel);
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

        bolusPredictEnabled = PrefUtil.getBolusPredictSwitch(preferences);
        bloodSugarEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                final float val;

                try {
                    val = Float.parseFloat(s.toString());
                } catch (NumberFormatException e) {
                    return;
                }

                bloodSugarEdit.setTextColor(val <= hypo || val >= hyper ? badBgColor :
                        val >= minTargetRng && val <= maxTargetRng ? goodBgColor : medBgColor);

                if (bolusPredictEnabled) {
                    final float factor = PrefUtil.getCorrectionFactor(preferences);

                    if (factor > 0) {
                        final float corr =
                                (val - PrefUtil.getOptimalBg(preferences).get(bgUnitIdx)) / factor;
                        corrBolusEdit.setText(corr > 0 ?
                                NumericTextUtil.roundToIncrement(
                                        corr, PrefUtil.getInsulinIncrement(preferences)) : null);
                    }
                }
            }
        });

        // Food
        final LayoutTransition foodTransition = foodEntryContainer.getLayoutTransition();
        foodTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
        foodTransition.setAnimateParentHierarchy(false);

        foods = new ArrayList<>();
        final AppCompatButton addFoodButton =
                (AppCompatButton) foodEntryContainer.findViewById(R.id.food_add_button);
        addFoodButton.setOnClickListener(this);

        // Insulin
        switch (PrefUtil.getInsulinTherapy(preferences)) {
            case PrefUtil.THERAPY_PEN:
                tempBasalEditContainer.setVisibility(View.GONE);
                basalEdit = (LabeledEditText) basalEditContainer.findViewById(R.id.basal_edit);
                basalEdit.setLabels((TextView) basalEditContainer.findViewById(R.id.basal_label));
                break;

            case PrefUtil.THERAPY_PUMP:
                basalEditContainer.setVisibility(View.GONE);
                tempBasalText =
                        (TextView) tempBasalEditContainer.findViewById(R.id.temp_basal_text);
                tempBasalText.setOnClickListener(this);
                clearTempBasalButton =
                        (Button) tempBasalEditContainer.findViewById(R.id.temp_basal_clear_button);
                clearTempBasalButton.setOnClickListener(this);
                setTempBasal(tempBasal);
                break;

            case PrefUtil.THERAPY_NO_INSULIN:
                insulinEditContainer.setVisibility(View.GONE);
                break;
        }

        // Pills
        if ((pillNames = PrefUtil.getPills(preferences)) != null) {
            final LayoutTransition pillTransition = pillEntryContainer.getLayoutTransition();
            pillTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
            pillTransition.setAnimateParentHierarchy(false);

            pills = new ArrayList<>();
            final AppCompatButton addPillButton =
                    (AppCompatButton) pillEntryContainer.findViewById(R.id.pill_add_button);
            addPillButton.setOnClickListener(this);
        } else ((View) pillEntryContainer.getParent()).setVisibility(View.GONE);
    }

    private void loadOldData(Intent data) {
        calendar = (GregorianCalendar) data.getSerializableExtra(DiaryActivity.EXTRA_CALENDAR);

        if (isEdit = data.getBooleanExtra(DiaryActivity.EXTRA_IS_EDIT, false)) {
            setTitle(R.string.action_title_edit_entry);
            oldDay = Day.findById(data.getIntExtra(DiaryActivity.EXTRA_DAY_ID, 0));
            assert oldDay != null;
            final SugarEntry entry = oldDay.getEntries()[oldEntryIdx =
                    data.getIntExtra(DiaryActivity.EXTRA_ENTRY_INDEX, 0)];
            // TODO: Load old data here
        } else setTitle(R.string.action_title_add_entry);
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
     * Opens the {@link PlacePicker}.
     */
    private void openPlacePicker() {
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
        locationEdit.setText(!address.isEmpty() ?
                !address.contains(name) ? name + ", " + address : address : name);
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
     * Opens the photo chooser.
     */
    private void openPhotoChooser() {
        startActivityForResult(Intent.createChooser(
                new Intent(Intent.ACTION_GET_CONTENT).setType(PHOTO_CHOOSER_TYPE),
                getString(R.string.choose_photo)),
                REQ_CHOOSE_PHOTO);
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

    /**
     * Modifies a food entry at a given position.
     *
     * @param position Index of the food entry.
     * @param food     New food object for this entry.
     */
    public void setFood(final int position, @NonNull Food food) {
        final View entry;

        if (position == foodEntryContainer.getChildCount() - 1) {
            entry = getLayoutInflater()
                    .inflate(R.layout.food_entry_editable, foodEntryContainer, false);
            foodEntryContainer.addView(entry, position);
            foods.add(position, food);
        } else {
            entry = foodEntryContainer.getChildAt(position);
            final float prevCarbs = foods.set(position, food).getCarbs();

            if (prevCarbs != Food.NO_CARBS)
                carbSum -= prevCarbs;
        }

        entry.findViewById(R.id.food_delete_button).setOnClickListener(this);
        ((TextView) entry.findViewById(R.id.food_name_text)).setText(food.getName());
        final TextView quantityText = ((TextView) entry.findViewById(R.id.quantity_text));
        final Serving[] servings = food.getServings();

        if (servings != null) {
            final Serving chosenServing = food.getChosenServing();
            String servingStr = chosenServing.getDescription();

            if (chosenServing.getCaption() != null)
                servingStr += " - " + chosenServing.getCaption();

            quantityText.setText(String.format("%s \u00D7 %s",
                    NumericTextUtil.trim(food.getQuantity()), servingStr));
        } else quantityText.setVisibility(View.GONE);

        final TextView carbsText = (TextView) entry.findViewById(R.id.carbs_text);
        final float carbs = food.getCarbs();

        if (carbs != Food.NO_CARBS) {
            carbSum += carbs;
            carbsText.setText(NumericTextUtil.trim(carbs) + " " +
                    getString(R.string.grams_of_carb));
        } else carbsText.setVisibility(View.GONE);

        entry.setOnClickListener(this);
        setMealBolus();
    }

    /**
     * Opens a {@link FoodDialogFragment} for the relevant entry.
     *
     * @param index Index of entry.
     * @param food  {@link Food} object representing the entry.
     */
    private void showFoodEntry(int index, Food food) {
        checkPermission(PERMISSION_REQ_IGNORED, Manifest.permission.INTERNET);
        final DialogFragment fragment = new FoodDialogFragment();
        final Bundle args = new Bundle();
        args.putInt(FoodDialogFragment.EXTRA_POSITION, index);
        args.putSerializable(FoodDialogFragment.EXTRA_FOOD, food);
        fragment.setArguments(args);
        fragment.show(getSupportFragmentManager(), null);
    }

    /**
     * If the user has set the bolus prediction feature on,
     * then the bolus is calculated and set in the appropriate {@link EditText}.
     */
    private void setMealBolus() {
        if (bolusPredictEnabled) {
            final SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(this);
            final float ratio = PrefUtil.getCarbToInsulin(preferences);

            if (ratio > 0) {
                final float val = carbSum / ratio;
                mealBolusEdit.setText(val > 0 ?
                        NumericTextUtil.roundToIncrement(
                                val, PrefUtil.getInsulinIncrement(preferences)) : null);
            }
        }
    }

    /**
     * {@link TempBasal} data is set in the appropriate {@link TextView}s.
     *
     * @param tempBasal The data to be set.
     */
    public void setTempBasal(TempBasal tempBasal) {
        if (tempBasal != null) {
            tempBasalText.setText(tempBasal.toString());
            clearTempBasalButton.setVisibility(View.VISIBLE);
        } else {
            tempBasalText.setText(null);
            clearTempBasalButton.setVisibility(View.GONE);
        }

        this.tempBasal = tempBasal;
    }

    /**
     * Adds a {@link Pill} entry to the layout (and to the {@link ArrayList} respectively)
     * and sets all the required listeners for the {@link View} components.
     */
    private void addPillEntry() {
        final View entry =
                getLayoutInflater().inflate(R.layout.pill_entry, pillEntryContainer, false);
        final AppCompatSpinner quantitySpinner =
                (AppCompatSpinner) entry.findViewById(R.id.pill_quantity_spinner);
        final AppCompatSpinner nameSpinner =
                (AppCompatSpinner) entry.findViewById(R.id.pill_name_spinner);
        final ImageButton removeButton = (ImageButton) entry.findViewById(R.id.pill_delete_button);

        final Pill pill = new Pill(
                Float.parseFloat((String) quantitySpinner.getSelectedItem()),
                pillNames[0]);
        pills.add(pill);
        final ArrayAdapter<String> nameAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, pillNames);
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nameSpinner.setAdapter(nameAdapter);

        nameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pill.setName(pillNames[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        quantitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pill.setQuantity(Float.parseFloat((String) quantitySpinner.getSelectedItem()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        removeButton.setOnClickListener(this);

        pillEntryContainer.addView(entry, pillEntryContainer.getChildCount() - 1);
    }

    /**
     * Saves the entry into the database and deletes the previous instance.
     */
    private void saveAndExit() {
        String photoPath = null;

        if (currentPhotoPath != null) {
            final File cacheFile = new File(Uri.parse(currentPhotoPath).getPath());
            final File newFile = new File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    cacheFile.getName());
            // noinspection ResultOfMethodCallIgnored
            cacheFile.renameTo(newFile);
            photoPath = getFilePath(newFile);
        }

        BloodSugar bg = null;
        Float corrBolus = null, mealBolus = null, basal = null;

        try {
            final float val = Float.parseFloat(bloodSugarEdit.getText().toString());
            if (bloodSugarEdit.getInputType() == InputType.TYPE_CLASS_NUMBER)
                bg = new BloodSugar((int) val);
            else bg = new BloodSugar(val);

            corrBolus = Float.valueOf(corrBolusEdit.getText().toString());
            mealBolus = Float.valueOf(mealBolusEdit.getText().toString());
            basal = Float.valueOf(basalEdit.getText().toString());
        } catch (NumberFormatException | NullPointerException ignored) {

        }

        if (isEdit)
            deleteEntry();

        final int dayId = Day.generateId(calendar);
        Day day = Day.findById(dayId);

        if (day == null)
            day = new Day(dayId);

        final ArrayList<SugarEntry> entries = new ArrayList<>(Arrays.asList(day.getEntries()));
        entries.add(new SugarEntry().setType(typeSpinner.getSelectedItemPosition())
                .setTime(calendar.getTime().getTime())
                .setLocation(locationEdit.getText().toString().trim()).setPhotoPath(photoPath)
                .setBloodSugar(bg).setFoods(foods).setCorrBolus(corrBolus).setMealBolus(mealBolus)
                .setBasal(basal).setTempBasal(tempBasal).setPills(pills)
                .setNotes(notesEdit.getText().toString().trim()));
        Collections.sort(entries, SugarEntry.getCompByTime());

        day.setEntries(entries.toArray(new SugarEntry[entries.size()]));
        day.save();

        setResult(RESULT_OK, new Intent().putExtra(DiaryActivity.EXTRA_CALENDAR, calendar));
        finish();
    }

    private void deleteEntry() {
        final ArrayList<SugarEntry> entries = new ArrayList<>(Arrays.asList(oldDay.getEntries()));
        entries.remove(oldEntryIdx);

        if (entries.size() == 0)
            oldDay.delete();
        else oldDay.setEntries(entries.toArray(new SugarEntry[entries.size()]));
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.current_location_button:
                if (checkPermission(PERMISSION_REQ_PLACE_PICKER,
                        Manifest.permission.ACCESS_FINE_LOCATION))
                    openPlacePicker();
                break;

            case R.id.food_add_button:
                showFoodEntry(foodEntryContainer.getChildCount() - 1, null);
                break;

            case R.id.food_delete_button:
                final int foodIdx = foodEntryContainer.indexOfChild((View) view.getParent());
                foodEntryContainer.removeViewAt(foodIdx);
                carbSum -= foods.remove(foodIdx).getCarbs();
                setMealBolus();
                break;

            case R.id.food_entry:
                final int indexToView = foodEntryContainer.indexOfChild(view);
                showFoodEntry(indexToView, foods.get(indexToView));
                break;

            case R.id.temp_basal_text:
                final TempBasalDialogFragment dialogFragment = new TempBasalDialogFragment();
                final Bundle args = new Bundle();
                args.putSerializable(TempBasalDialogFragment.EXTRA_TEMP_BASAL, tempBasal);
                dialogFragment.setArguments(args);
                dialogFragment.show(getSupportFragmentManager(), null);
                break;

            case R.id.temp_basal_clear_button:
                setTempBasal(null);
                break;

            case R.id.pill_add_button:
                addPillEntry();
                break;

            case R.id.pill_delete_button:
                final int pillIdx = pillEntryContainer.indexOfChild((View) view.getParent());
                pillEntryContainer.removeViewAt(pillIdx);
                pills.remove(pillIdx);
                break;
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_remove_photo:
                // noinspection ResultOfMethodCallIgnored
                new File(Uri.parse(currentPhotoPath).getPath()).delete();
                currentPhotoPath = null;
                photoThumbnailView.setImageBitmap(null);
                return true;

            case R.id.menu_take_photo:
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
                    if (!checkPermission(PERMISSION_REQ_CAMERA, Manifest.permission.CAMERA))
                        break;

                    dispatchTakePictureIntent();
                    return true;
                }
                break;

            case R.id.menu_choose_photo:
                if (!checkPermission(PERMISSION_REQ_CHOOSE_PHOTO,
                        Manifest.permission.READ_EXTERNAL_STORAGE))
                    break;

                openPhotoChooser();
                return true;
        }

        return false;
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
        photoThumbnailView.setImageBitmap(
                savedInstanceState.<Bitmap>getParcelable(KEY_INSTANCE_BITMAP));
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

    @SuppressWarnings("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_PLACE_DETECT:
                if (isPermissionGranted(requestCode, grantResults))
                    Places.PlaceDetectionApi
                            .getCurrentPlace(googleApiClient, null)
                            .setResultCallback(this);
                break;

            case PERMISSION_REQ_PLACE_PICKER:
                if (isPermissionGranted(requestCode, grantResults))
                    openPlacePicker();
                break;

            case PERMISSION_REQ_CHOOSE_PHOTO:
                if (isPermissionGranted(requestCode, grantResults))
                    openPhotoChooser();
                break;

            case PERMISSION_REQ_CAMERA:
                if (isPermissionGranted(requestCode, grantResults))
                    dispatchTakePictureIntent();
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_entry, menu);
        menu.findItem(R.id.action_delete).setEnabled(isEdit);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;

            case R.id.action_save:
                saveAndExit();
                return true;

            case R.id.action_delete:
                new MaterialDialog.Builder(this)
                        .canceledOnTouchOutside(false)
                        .positiveText(R.string.action_delete)
                        .negativeText(android.R.string.cancel)
                        .title(R.string.question_delete_entry)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog,
                                                @NonNull DialogAction which) {
                                deleteEntry();
                                finish();
                            }
                        }).build().show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .canceledOnTouchOutside(false)
                .positiveText(R.string.action_save)
                .neutralText(android.R.string.cancel)
                .negativeText(R.string.action_discard)
                .title(R.string.question_save_changes)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog,
                                        @NonNull DialogAction which) {
                        saveAndExit();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog,
                                        @NonNull DialogAction which) {
                        EditEntryActivity.super.onBackPressed();
                    }
                }).build().show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
        if (likelyPlaces.getCount() > 0)
            setLocationText(likelyPlaces.get(0).getPlace());

        likelyPlaces.release();
        googleApiClient.disconnect();
        googleApiClient = null;
    }
}
