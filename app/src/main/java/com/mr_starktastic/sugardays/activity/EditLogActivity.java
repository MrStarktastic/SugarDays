package com.mr_starktastic.sugardays.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.mr_starktastic.sugardays.R;

import java.util.GregorianCalendar;

public class EditLogActivity extends AppCompatActivity {
    private GregorianCalendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_log);
        calendar = (GregorianCalendar) getIntent().getSerializableExtra(DiaryActivity.EXTRA_DATE);
    }
}
