package com.mr_starktastic.sugardays.text;

import android.text.Editable;
import android.text.TextWatcher;

public abstract class SimpleTextWatcher implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // Empty
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        onTextChanged(s);
    }

    public abstract void onTextChanged(CharSequence s);

    @Override
    public void afterTextChanged(Editable s) {
        // Empty
    }
}
