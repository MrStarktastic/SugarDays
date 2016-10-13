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

import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * An alternative to the planned {@link android.support.v7.preference.PreferenceScreen}
 * due to the fact that it's easier to handle dynamic views in an {@link AppCompatActivity}.
 */
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
    private LinkedList<EditText> editTexts;

    /**
     * Listeners for the clear {@link android.widget.ImageButton}
     * and {@link EditText} which holds the name of a pill.
     */
    private View.OnClickListener onClearListener = v -> {
        final int index = pillItemContainer.indexOfChild((View) v.getParent());

        if (!TextUtils.isEmpty(editTexts.get(index).getText()) && editTexts.size() > 1) {
            editTexts.remove(index);
            pillItemContainer.removeViewAt(index);
        }
    };
    private TextWatcher textWatcher = new TextWatcher() {
        private boolean wasEmpty;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            wasEmpty = s.length() == 0;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0 && editTexts.size() > 1)
                removeItemWithEmptyEditText();
            else if (wasEmpty)
                addPillEntry(null);
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

        editTexts = new LinkedList<>();
        final String[] pillNames = new Gson().fromJson(PreferenceManager
                .getDefaultSharedPreferences(this).getString(PrefKeys.PILLS, null), String[].class);

        if (pillNames != null && pillNames.length > 0)
            for (String name : pillNames)
                addPillEntry(name);

        addPillEntry(null); // Empty entry in the end of the ScrollView
    }

    @Override
    @SuppressLint("CommitPrefEdits")
    protected void onPause() {
        final LinkedHashSet<String> set = new LinkedHashSet<>();

        for (EditText e : editTexts) {
            final String text = e.getText().toString().trim();
            final String prohibitedText = getString(R.string.pref_pills_default_summary);

            if (!TextUtils.isEmpty(text) && !text.equals(prohibitedText))
                set.add(text);
        }

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString(PrefKeys.PILLS,
                new Gson().toJson(set.size() != 0 ? set.toArray() : null)).commit();

        super.onPause();
    }

    @SuppressLint("InflateParams")
    private void addPillEntry(String pillName) {
        final View pillItem =
                getLayoutInflater().inflate(R.layout.preference_pill_item, null, false);
        pillItem.findViewById(R.id.clear_button).setOnClickListener(onClearListener);
        final EditText pillNameEdit = (EditText) pillItem.findViewById(R.id.pill_name_edit);

        if (pillName != null)
            pillNameEdit.setText(pillName);

        pillNameEdit.addTextChangedListener(textWatcher);
        pillNameEdit.setFilters(INPUT_FILTERS);
        editTexts.add(pillNameEdit);
        pillItemContainer.addView(pillItem);
    }

    private void removeItemWithEmptyEditText() {
        // The end of the pillItem is most likely where an empty EditText will be found
        for (int i = editTexts.size() - 1; i >= 0; --i) {
            final EditText pillNameEdit = editTexts.get(i);

            if (TextUtils.isEmpty(pillNameEdit.getText())) {
                editTexts.remove(i);
                pillItemContainer.removeViewAt(i);

                return;
            }
        }
    }
}
