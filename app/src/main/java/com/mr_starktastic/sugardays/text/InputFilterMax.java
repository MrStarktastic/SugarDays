package com.mr_starktastic.sugardays.text;

import android.text.InputFilter;
import android.text.Spanned;

public class InputFilterMax implements InputFilter {
    private float max;

    public InputFilterMax(float max) {
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
