package com.mr_starktastic.sugardays.activity;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.gson.Gson;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.preference.PrefKeys;

import java.util.ArrayList;

public class PillManagerActivity extends AppCompatActivity {
    private static final int LAYOUT_TRANSITION_DURATION = 100;
    private static final InputFilter[] INPUT_FILTERS = {
            (source, start, end, dest, dstart, dend) -> {
                for (int i = start; i < end; ++i)
                    if (source.charAt(i) == ',')
                        return "";

                return null;
            }};

    private LinearLayout pillItemContainer;
    private boolean isLoadingData;

    /**
     * Listeners for the clear {@link android.widget.ImageButton}
     * and {@link EditText} which holds the name of a pill.
     */
    private View.OnClickListener onClearListener = v -> {
        final View item = (View) v.getParent();
        if (!TextUtils.isEmpty(((EditText) item.findViewById(R.id.pill_name_edit)).getText()) &&
                pillItemContainer.getChildCount() > 1)
            pillItemContainer.removeView(item);
    };
    private TextWatcher textWatcher = new TextWatcher() {
        private boolean wasEmpty;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            wasEmpty = s.length() == 0;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!isLoadingData)
                if (s.length() == 0 && pillItemContainer.getChildCount() > 1)
                    pillItemContainer.removeViewAt(indexOfEmptyEditText());
                else if (wasEmpty)
                    addPillEntry();
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();

        if (actionBar != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pillItemContainer = new LinearLayout(this);
        pillItemContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        pillItemContainer.setOrientation(LinearLayout.VERTICAL);
        final LayoutTransition transition = new LayoutTransition();
        transition.setDuration(LAYOUT_TRANSITION_DURATION);
        pillItemContainer.setLayoutTransition(new LayoutTransition());

        scrollView.addView(pillItemContainer);
        setContentView(scrollView);

        final String[] pillNames = new Gson().fromJson(PreferenceManager
                .getDefaultSharedPreferences(this).getString(PrefKeys.PILLS, null), String[].class);

        if (isLoadingData = (pillNames != null && pillNames.length > 0)) {
            for (String name : pillNames)
                addPill(name);

            isLoadingData = false;
        }

        addPillEntry(); // Empty entry in the end of the ScrollView
    }

    @Override
    @SuppressLint("CommitPrefEdits")
    protected void onPause() {
        final int childCount = pillItemContainer.getChildCount();
        ArrayList<String> pillNames = new ArrayList<>();

        for (int i = 0; i < childCount; ++i) {
            final String text = ((EditText) pillItemContainer.getChildAt(i)
                    .findViewById(R.id.pill_name_edit)).getText().toString().trim();

            if (!TextUtils.isEmpty(text))
                pillNames.add(text);
        }

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PrefKeys.PILLS,
                new Gson().toJson(!pillNames.isEmpty() ? pillNames.toArray() : null)).commit();

        super.onPause();
    }

    @SuppressLint("InflateParams")
    private EditText addPillEntry() {
        final View pillItem =
                getLayoutInflater().inflate(R.layout.preference_pill_item, null, false);
        pillItem.findViewById(R.id.clear_button).setOnClickListener(onClearListener);
        final EditText pillNameEdit = (EditText) pillItem.findViewById(R.id.pill_name_edit);
        pillNameEdit.addTextChangedListener(textWatcher);
        pillNameEdit.setFilters(INPUT_FILTERS);
        pillItemContainer.addView(pillItem);

        return pillNameEdit;
    }

    private void addPill(String pillName) {
        addPillEntry().setText(pillName);
    }

    private int indexOfEmptyEditText() {
        // The end of the container is most likely where an empty EditText will be found
        for (int i = pillItemContainer.getChildCount() - 1; i >= 0; --i)
            if (TextUtils.isEmpty(((EditText) pillItemContainer.getChildAt(i)
                    .findViewById(R.id.pill_name_edit)).getText()))
                return i;

        return -1; // Not supposed to happen
    }
}
