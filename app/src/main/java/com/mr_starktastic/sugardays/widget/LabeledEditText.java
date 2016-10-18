package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;

public class LabeledEditText extends EditText {
    public LabeledEditText(Context context) {
        super(context);
    }

    public LabeledEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LabeledEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setLabels(TextView... labels) {
        setOnFocusChangeListener((v, hasFocus) -> {
            final int color = hasFocus ?
                    ContextCompat.getColor(getContext(), android.R.color.primary_text_light) :
                    getHintTextColors().getDefaultColor();

            for (TextView l : labels)
                l.setTextColor(color);
        });
    }
}
