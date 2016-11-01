package com.mr_starktastic.sugardays.text;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    public abstract void onTextChanged(CharSequence s);

    @Override
    public final void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Empty
    }

    @Override
    public final void onTextChanged(CharSequence s, int start, int before, int count) {
        onTextChanged(s);
    }

    @Override
    public final void afterTextChanged(Editable s) {
        // Empty
    }
}
