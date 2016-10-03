package com.mr_starktastic.sugardays.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceScreen;
import android.view.MenuItem;

import com.mr_starktastic.sugardays.fragment.SettingsFragment;

public class SettingsActivity extends AppCompatActivity
        implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private static final String KEY = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            final Fragment fragment = new SettingsFragment();
            final String key = getIntent().getStringExtra(KEY);

            if (key != null) {
                Bundle args = new Bundle();
                args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key);
                fragment.setArguments(args);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, fragment, null)
                    .commit();
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat caller, PreferenceScreen pref) {
        startActivity(new Intent(this, SettingsActivity.class).putExtra(KEY, pref.getKey()));
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            onBackPressed();

        return super.onOptionsItemSelected(item);
    }
}
