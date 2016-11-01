package com.mr_starktastic.sugardays.fragment;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.InputFilter;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.activity.EditLogActivity;
import com.mr_starktastic.sugardays.data.TempBasal;
import com.mr_starktastic.sugardays.text.InputFilterMax;
import com.mr_starktastic.sugardays.text.SimpleTextWatcher;
import com.mr_starktastic.sugardays.widget.LabeledEditText;

public class TempBasalDialogFragment extends AppCompatDialogFragment {
    public static final String EXTRA_TEMP_BASAL = "TEMP_BASAL";

    private static final int MAX_HOURS = 24, MAX_MINUTES = 59;

    private TempBasal tempBasal;

    private MDButton positiveButton;
    private LabeledEditText hoursEdit, minutesEdit, rateEdit;

    public TempBasalDialogFragment() {
        // Empty constructor required
    }

    @Override
    public void setArguments(Bundle args) {
        tempBasal = new TempBasal((TempBasal) args.getSerializable(EXTRA_TEMP_BASAL));
        super.setArguments(args);
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(getString(R.string.temp_basal_dialog_title))
                .customView(R.layout.fragment_temp_basal_dialog, true)
                .canceledOnTouchOutside(false)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog,
                                        @NonNull DialogAction which) {
                        ((EditLogActivity) getActivity()).setTempBasal(tempBasal);
                        dismiss();
                    }
                }).build();

        (positiveButton = dialog.getActionButton(DialogAction.POSITIVE)).setEnabled(false);
        final View root = dialog.getCustomView();
        assert root != null;

        final TextView durationDelimText = (TextView) root.findViewById(R.id.duration_delim_text);
        final TextView durationLabel = (TextView) root.findViewById(R.id.duration_label);
        final TextView rateLabel = (TextView) root.findViewById(R.id.rate_label);
        (hoursEdit = (LabeledEditText) root.findViewById(R.id.hours_edit))
                .setFilters(new InputFilter[]{new InputFilterMax(MAX_HOURS)});
        (minutesEdit = (LabeledEditText) root.findViewById(R.id.minutes_edit))
                .setFilters(new InputFilter[]{new InputFilterMax(MAX_MINUTES)});
        rateEdit = (LabeledEditText) root.findViewById(R.id.rate_edit);

        assert tempBasal != null;
        hoursEdit.setText(Integer.toString(tempBasal.getHours()));
        minutesEdit.setText(Integer.toString(tempBasal.getMinutes()));
        rateEdit.setText(Integer.toString(tempBasal.getRate()));

        hoursEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                final String str = s.toString();
                final int hours;

                try {
                    hours = Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    positiveButton.setEnabled(false);
                    return;
                }

                tempBasal.setHours(hours);
                positiveButton.setEnabled(minutesEdit.getText().length() > 0 &&
                        !(hours == 0 && tempBasal.getMinutes() == 0) &&
                        rateEdit.getText().length() > 0 && !tempBasal.isSameAsOld());
            }
        });

        minutesEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                final String str = s.toString();
                final int minutes;

                try {
                    minutes = Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    positiveButton.setEnabled(false);
                    return;
                }

                tempBasal.setMinutes(minutes);
                positiveButton.setEnabled(hoursEdit.getText().length() > 0 &&
                        !(tempBasal.getHours() == 0 && minutes == 0) &&
                        rateEdit.getText().length() > 0 && !tempBasal.isSameAsOld());
            }
        });

        rateEdit.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s) {
                final String str = s.toString();
                final int rate;

                try {
                    rate = Integer.parseInt(str);
                } catch (NumberFormatException e) {
                    positiveButton.setEnabled(false);
                    return;
                }

                tempBasal.setRate(rate);
                positiveButton.setEnabled(hoursEdit.getText().length() > 0 &&
                        minutesEdit.getText().length() > 0 &&
                        !(tempBasal.getHours() == 0 && tempBasal.getMinutes() == 0) &&
                        !tempBasal.isSameAsOld());
            }
        });

        hoursEdit.setLabels(durationDelimText, durationLabel);
        minutesEdit.setLabels(durationDelimText, durationLabel);
        rateEdit.setLabels(rateLabel);

        return dialog;
    }
}
