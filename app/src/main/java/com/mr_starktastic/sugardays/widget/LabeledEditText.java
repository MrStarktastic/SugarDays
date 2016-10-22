package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LabeledEditText extends EditText {
    private int focusedColor, unfocusedColor;

    public LabeledEditText(Context context) {
        super(context);
    }

    public LabeledEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LabeledEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLabels(final TextView... labels) {
        focusedColor = ContextCompat.getColor(getContext(), android.R.color.primary_text_light);
        unfocusedColor = getHintTextColors().getDefaultColor();

        setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                final int color = hasFocus ? focusedColor : unfocusedColor;

                for (TextView l : labels)
                    l.setTextColor(color);
            }
        });
    }
}
