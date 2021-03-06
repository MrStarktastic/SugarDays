package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.v7.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class LoadingAutoCompleteTextView extends AppCompatAutoCompleteTextView {
    private static final int DELAY = 250;

    private final Handler handler = new Handler();
    private Runnable run;
    private ProgressBar progressBar;

    // Helps avoid unnecessary filtering on orientation changes
    private boolean filteringAllowed = true;

    public LoadingAutoCompleteTextView(Context context) {
        super(context);
    }

    public LoadingAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoadingAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setProgressBar(ProgressBar progressBar) {
        this.progressBar = progressBar;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        filteringAllowed = false;
    }

    @Override
    protected void performFiltering(final CharSequence text, final int keyCode) {
        handler.removeCallbacks(run);

        if (filteringAllowed)
            handler.postDelayed(run = new Runnable() {
                @Override
                public void run() {
                    LoadingAutoCompleteTextView.super.performFiltering(text, keyCode);

                    if (progressBar != null)
                        progressBar.setVisibility(VISIBLE);
                }
            }, DELAY);
        else filteringAllowed = true;
    }

    @Override
    public void onFilterComplete(int count) {
        if (progressBar != null)
            progressBar.setVisibility(GONE);

        super.onFilterComplete(count);
    }
}
