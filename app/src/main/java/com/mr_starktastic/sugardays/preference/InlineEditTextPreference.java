package com.mr_starktastic.sugardays.preference;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.mr_starktastic.sugardays.R;

/**
 * A {@link Preference} representing an {@link EditText} (without a dialog) and a label next to it.
 * Designed for numeric input.
 */
public class InlineEditTextPreference extends Preference {
    private EditText numberEditText;
    private float value;
    private String label;

    public InlineEditTextPreference(Context context, String label) {
        super(context);
        this.label = label;
        setLayoutResource(R.layout.preference_inline_edit_text);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        numberEditText = (EditText) holder.findViewById(R.id.number_edit);

        if (value != 0) {
            final String str = String.valueOf(value);
            numberEditText.setText(!str.endsWith(".0") ? str : str.substring(0, str.length() - 2));
        }

        final TextView numberEditLabel = (TextView) holder.findViewById(R.id.number_label);
        numberEditLabel.setText(label);
    }

    @Override
    protected void onClick() {
        if (numberEditText.requestFocus())
            ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(numberEditText, 0);
    }

    public float getValue() {
        final String str = numberEditText.getText().toString();
        return !str.isEmpty() && !str.equals(".") ?
                Float.parseFloat(numberEditText.getText().toString()) : 0;
    }

    public void setValue(float value) {
        this.value = value;
    }
}
