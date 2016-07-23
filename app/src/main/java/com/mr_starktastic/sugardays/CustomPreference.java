package com.mr_starktastic.sugardays;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Preference with a custom layout where the title is at the start, summary is at the end
 * and the widget frame is below them
 */
public class CustomPreference extends Preference {
    private int summaryTextColor;
    private View widget;

    public CustomPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    public CustomPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public CustomPreference(Context context) {
        super(context);
        init(null);
    }

    /**
     * Sets the custom preference layout and obtains the TextView color for the summary
     *
     * @param attrs Set of attributes
     */
    private void init(AttributeSet attrs) {
        setLayoutResource(R.layout.preference_custom);

        if (attrs != null) {
            final TypedArray styledAttrs = getContext().obtainStyledAttributes(attrs,
                    R.styleable.CustomPreference, 0, 0);
            summaryTextColor = styledAttrs
                    .getColor(R.styleable.CustomPreference_summaryTextColor, 0);
            styledAttrs.recycle();
        }
    }

    /**
     * @return Widget view from the widget frame
     */
    public View getWidget() {
        return widget;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        ((TextView) holder.findViewById(android.R.id.summary)).setTextColor(summaryTextColor);
        widget = ((ViewGroup) holder.findViewById(android.R.id.widget_frame)).getChildAt(0);
    }
}
