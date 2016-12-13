package com.mr_starktastic.sugardays.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.BloodSugar;
import com.mr_starktastic.sugardays.data.Day;
import com.mr_starktastic.sugardays.data.Food;
import com.mr_starktastic.sugardays.data.Pill;
import com.mr_starktastic.sugardays.data.Serving;
import com.mr_starktastic.sugardays.data.SugarEntry;
import com.mr_starktastic.sugardays.data.TempBasal;
import com.mr_starktastic.sugardays.util.NumericTextUtil;
import com.mr_starktastic.sugardays.util.PrefUtil;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.GregorianCalendar;

public class ViewEntryActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQ_ENTRY_EDIT = 1;

    private int dayId;
    private int entryIdx;
    private SugarEntry entry;

    private CollapsingToolbarLayout collapsingToolbarLayout;

    private ImageView dateTimeIcon;
    private ImageView locationIcon;
    private ImageView bloodSugarIcon;
    private ImageView foodIcon;
    private ImageView syringeIcon;
    private ImageView pillIcon;
    private ImageView notesIcon;

    private ImageView photoView;
    private TextView dateText;
    private TextView timeText;
    private TextView locationText;
    private TextView bloodSugarText;
    private TextView totalCarbsText;
    private TextView corrBolusText;
    private TextView mealBolusText;
    private TextView basalText;
    private TextView tempBasalText;
    private LinearLayout pillEntryContainer;
    private TextView notesText;
    private LinearLayout foodEntryContainer;

    private int hiddenInsulinFields;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_entry);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        final Intent intent = getIntent();
        // noinspection ConstantConditions
        entry = Day.findById(dayId = intent.getIntExtra(DiaryActivity.EXTRA_DAY_ID, 0))
                .getEntries()[entryIdx = intent.getIntExtra(DiaryActivity.EXTRA_ENTRY_INDEX, 0)];
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final FloatingActionButton editFab = (FloatingActionButton) findViewById(R.id.edit_button);
        collapsingToolbarLayout =
                (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_layout);
        final Toolbar toolbar = (Toolbar) collapsingToolbarLayout.findViewById(R.id.toolbar);
        photoView = (ImageView) collapsingToolbarLayout.findViewById(R.id.photo_view);
        final LinearLayout scrollViewContent =
                (LinearLayout) findViewById(R.id.scroll_view_content);
        dateTimeIcon = (ImageView) scrollViewContent.findViewById(R.id.date_time_icon);
        dateText = (TextView) scrollViewContent.findViewById(R.id.date_text);
        timeText = (TextView) scrollViewContent.findViewById(R.id.time_text);
        locationIcon = (ImageView) scrollViewContent.findViewById(R.id.location_icon);
        locationText = (TextView) scrollViewContent.findViewById(R.id.location_text);
        bloodSugarIcon = (ImageView) scrollViewContent.findViewById(R.id.blood_sugar_icon);
        bloodSugarText = (TextView) scrollViewContent.findViewById(R.id.blood_sugar_text);
        foodIcon = (ImageView) scrollViewContent.findViewById(R.id.food_icon);
        foodEntryContainer =
                (LinearLayout) scrollViewContent.findViewById(R.id.food_entry_container);
        totalCarbsText = (TextView) foodEntryContainer.findViewById(R.id.total_carbs_text);
        syringeIcon = (ImageView) scrollViewContent.findViewById(R.id.syringe_icon);
        corrBolusText = (TextView) scrollViewContent.findViewById(R.id.bolus_correction_text);
        mealBolusText = (TextView) scrollViewContent.findViewById(R.id.bolus_meal_text);
        basalText = (TextView) scrollViewContent.findViewById(R.id.basal_text);
        tempBasalText = (TextView) scrollViewContent.findViewById(R.id.temp_basal_text);
        pillIcon = (ImageView) scrollViewContent.findViewById(R.id.pill_icon);
        pillEntryContainer =
                (LinearLayout) scrollViewContent.findViewById(R.id.pill_entry_container);
        notesIcon = (ImageView) scrollViewContent.findViewById(R.id.notes_icon);
        notesText = (TextView) scrollViewContent.findViewById(R.id.notes_text);

        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_close);

        editFab.setOnClickListener(this);

        showData();
    }

    private void showData() {
        final String photoPath = entry.getPhotoPath();

        if (photoPath != null) {
            ActivityCompat.postponeEnterTransition(this);

            final DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            final int width = displayMetrics.widthPixels;

            final Target target = new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    photoView.setImageBitmap(bitmap);
                    final Palette palette = Palette.from(bitmap).generate();
                    setViewsColor(palette.getVibrantColor(ContextCompat.getColor(
                            ViewEntryActivity.this, R.color.colorPrimary)));
                    ActivityCompat.startPostponedEnterTransition(ViewEntryActivity.this);
                }

                @Override
                public void onBitmapFailed(Drawable errorDrawable) {
                    ActivityCompat.startPostponedEnterTransition(ViewEntryActivity.this);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {

                }
            };
            photoView.setTag(target);
            Picasso.with(this).load(entry.getPhotoPath())
                    .resize(width, Math.round(width * 9f / 16f)).centerCrop()
                    .into(target);
        } else setViewsColor(ContextCompat.getColor(ViewEntryActivity.this, R.color.colorPrimary));

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

        final float totalCarbs = entry.getCarbs();

        if (totalCarbs != Food.NO_CARBS)
            totalCarbsText.setText(NumericTextUtil.trim(totalCarbs) + " " +
                    getString(R.string.grams_of_carb));
        else totalCarbsText.setVisibility(View.GONE);

        final Food[] foods = entry.getFoods();

        if (foods != null && foods.length != 0) {
            for (int i = 0; i < foods.length; ++i) {
                final View entryView = getLayoutInflater()
                        .inflate(R.layout.food_entry, foodEntryContainer, false);

                final Food f = foods[i];
                ((TextView) entryView.findViewById(R.id.food_name_text)).setText(f.getName());
                final TextView quantityText = (TextView) entryView.findViewById(R.id.quantity_text);
                final Serving[] servings = f.getServings();

                if (servings != null) {
                    final Serving chosenServing = f.getChosenServing();
                    String servingStr = chosenServing.getDescription();

                    if (chosenServing.getCaption() != null)
                        servingStr += " - " + chosenServing.getCaption();

                    quantityText.setText(String.format("%s \u00D7 %s",
                            NumericTextUtil.trim(f.getQuantity()), servingStr));
                } else quantityText.setVisibility(View.GONE);

                final TextView carbsText = (TextView) entryView.findViewById(R.id.carbs_text);
                final float carbs = f.getCarbs();

                if (carbs != Food.NO_CARBS) {
                    carbsText.setText(NumericTextUtil.trim(carbs) + " " +
                            getString(R.string.grams_of_carb));
                } else carbsText.setVisibility(View.GONE);

                foodEntryContainer.addView(entryView, i);
            }
        } else ((View) foodEntryContainer.getParent()).setVisibility(View.GONE);

        hiddenInsulinFields = 0;
        final Float corrBolus = entry.getCorrBolus();
        final Float mealBolus = entry.getMealBolus();
        final Float basal = entry.getBasal();
        final TempBasal tempBasal = entry.getTempBasal();

        loadInsulinData(corrBolus, corrBolusText, getString(R.string.correction));
        loadInsulinData(mealBolus, mealBolusText, getString(R.string.meal));
        loadInsulinData(basal, basalText, getString(R.string.basal_label));

        if (tempBasal != null)
            tempBasalText.setText(tempBasal.toString() + " " +
                    getString(R.string.temp_basal_label));
        else {
            tempBasalText.setVisibility(View.GONE);
            ++hiddenInsulinFields;
        }

        final ViewGroup insulinDataContainer = (ViewGroup) corrBolusText.getParent();

        if (hiddenInsulinFields == insulinDataContainer.getChildCount())
            ((View) insulinDataContainer.getParent()).setVisibility(View.GONE);

        Pill[] pills = entry.getPills();

        if (pills != null && pills.length != 0)
            for (Pill p : pills) {
                View v = getLayoutInflater()
                        .inflate(R.layout.pill_entry, pillEntryContainer, false);
                ((TextView) v.findViewById(R.id.pill_entry_text)).setText(p.toString());
                pillEntryContainer.addView(v);
            }
        else ((View) pillEntryContainer.getParent()).setVisibility(View.GONE);

        final String notes = entry.getNotes();

        if (notes != null)
            notesText.setText(notes);
        else ((View) notesIcon.getParent()).setVisibility(View.GONE);
    }

    private void setViewsColor(int color) {
        collapsingToolbarLayout.setContentScrimColor(color);
        collapsingToolbarLayout.setStatusBarScrimColor(color);
        dateTimeIcon.setColorFilter(color);
        locationIcon.setColorFilter(color);
        bloodSugarIcon.setColorFilter(color);
        foodIcon.setColorFilter(color);
        syringeIcon.setColorFilter(color);
        pillIcon.setColorFilter(color);
        notesIcon.setColorFilter(color);
    }

    private void loadInsulinData(Float val, TextView textView, String label) {
        if (val != null)
            textView.setText(NumericTextUtil.trim(val) + " " + label);
        else {
            textView.setVisibility(View.GONE);
            ++hiddenInsulinFields;
        }
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
                final GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(entry.getTime());

                startActivityForResult(new Intent(this, EditEntryActivity.class)
                        .putExtra(DiaryActivity.EXTRA_CALENDAR, cal)
                        .putExtra(DiaryActivity.EXTRA_IS_EDIT, true)
                        .putExtra(DiaryActivity.EXTRA_DAY_ID, dayId)
                        .putExtra(DiaryActivity.EXTRA_ENTRY_INDEX, entryIdx), REQ_ENTRY_EDIT);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_ENTRY_EDIT && resultCode == RESULT_OK) {
            final GregorianCalendar cal =
                    (GregorianCalendar) data.getSerializableExtra(DiaryActivity.EXTRA_CALENDAR);
            // noinspection ConstantConditions
            entry = Day.findById(dayId = Day.generateId(cal))
                    .getEntries()[entryIdx = data.getIntExtra(DiaryActivity.EXTRA_ENTRY_INDEX, 0)];
            Toast.makeText(this, Integer.toString(entryIdx), Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK, new Intent()
                    .putExtra(DiaryActivity.EXTRA_CALENDAR, cal)
                    .putExtra(DiaryActivity.EXTRA_ENTRY_INDEX, entryIdx));
            showData();
        }
    }
}
