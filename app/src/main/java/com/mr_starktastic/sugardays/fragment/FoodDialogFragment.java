package com.mr_starktastic.sugardays.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatSpinner;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.activity.BarcodeScanActivity;
import com.mr_starktastic.sugardays.data.Food;
import com.mr_starktastic.sugardays.data.Serving;
import com.mr_starktastic.sugardays.internet.FatSecretRequestBuilder;
import com.mr_starktastic.sugardays.internet.FetchAndParseJSONTask;
import com.mr_starktastic.sugardays.util.NumericTextUtil;
import com.mr_starktastic.sugardays.widget.FoodAutoCompleteAdapter;
import com.mr_starktastic.sugardays.widget.LoadingAutoCompleteTextView;

import java.util.concurrent.ExecutionException;

public class FoodDialogFragment extends AppCompatDialogFragment
        implements MaterialDialog.SingleButtonCallback {
    public static final int REQ_SCAN_BARCODE = 4;

    private static final int PERMISSION_REQ_CAMERA = 5;

    private static final int AUTOCOMPLETE_THRESHOLD = 2;

    private FatSecretRequestBuilder requestBuilder;
    private Food food;

    private LoadingAutoCompleteTextView foodNameEdit;
    private View quantityContainer;
    private EditText quantityEdit;
    private AppCompatSpinner servingSpinner;
    private EditText carbsEdit;

    public FoodDialogFragment() {
        requestBuilder = FatSecretRequestBuilder.getInstance();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.add_food)
                .customView(R.layout.fragment_food_dialog, true)
                .canceledOnTouchOutside(false).autoDismiss(false)
                .onAny(this)
                .neutralText(R.string.scan_barcode)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .build();

        final View root = dialog.getCustomView();
        assert root != null;

        foodNameEdit = (LoadingAutoCompleteTextView) root
                .findViewById(R.id.food_name_auto_complete_text);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET) ==
                PackageManager.PERMISSION_GRANTED) {
            foodNameEdit.setProgressBar(
                    (ProgressBar) root.findViewById(R.id.food_loading_indicator));
            foodNameEdit.setThreshold(AUTOCOMPLETE_THRESHOLD);
            foodNameEdit.setAdapter(new FoodAutoCompleteAdapter(context));
            foodNameEdit.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    try {
                        food = (Food) foodNameEdit.getAdapter().getItem(position);
                        final Serving[] servings = new FetchAndParseJSONTask<>(
                                requestBuilder.buildFoodGetUrl(id), Serving.JSON_PARSER)
                                .execute().get();
                        food.setServings(servings);
                        servingSpinner.setAdapter(new ServingAdapter(context, servings));
                        quantityContainer.setVisibility(View.VISIBLE);
                    } catch (InterruptedException | ExecutionException e) {
                        food = null;
                        e.printStackTrace();
                    }
                }
            });

            quantityContainer = root.findViewById(R.id.quantity_container);
            quantityEdit = (EditText) quantityContainer.findViewById(R.id.quantity_edit);
            servingSpinner = (AppCompatSpinner) quantityContainer
                    .findViewById(R.id.serving_spinner);
            carbsEdit = (EditText) root.findViewById(R.id.carbs_edit);

            servingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    final Serving serving = food.getServings()[position];
                    quantityEdit.setText(NumericTextUtil.trimNumber(serving.getDefaultQuantity()));
                    carbsEdit.setText(NumericTextUtil.trimNumber(serving.getCarbs()));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }

        // noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED |
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return dialog;
    }

    private void setFoodName(String name) {
        final FoodAutoCompleteAdapter adapter =
                (FoodAutoCompleteAdapter) foodNameEdit.getAdapter();
        foodNameEdit.setAdapter(null);
        foodNameEdit.setText(name);
        foodNameEdit.setAdapter(adapter);
    }

    @Override
    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
        switch (which) {
            case NEUTRAL:
                if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA},
                            PERMISSION_REQ_CAMERA);
                    return;
                }

                startActivityForResult(new Intent(getContext(), BarcodeScanActivity.class),
                        REQ_SCAN_BARCODE);
                break;

            case POSITIVE:
                dismiss();
                break;

            case NEGATIVE:
                dismiss();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQ_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    startActivityForResult(new Intent(getContext(), BarcodeScanActivity.class),
                            REQ_SCAN_BARCODE);
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_SCAN_BARCODE:
                if (data != null) {
                    try {
                        food = new FetchAndParseJSONTask<>(requestBuilder.buildFindIdForBarcodeUrl(
                                data.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE_VALUE)),
                                Food.JSON_ID_PARSER).execute().get();

                        if (food != null) {
                            if (food.toString() == null) {
                                Toast.makeText(getContext(),
                                        getString(R.string.barcode_food_not_found),
                                        Toast.LENGTH_SHORT).show();
                                throw new IllegalArgumentException("Bad barcode");
                            }

                            setFoodName(food.toString());
                            servingSpinner.setAdapter(
                                    new ServingAdapter(getContext(), food.getServings()));
                            quantityContainer.setVisibility(View.VISIBLE);
                        }
                    } catch (ExecutionException | InterruptedException |
                            IllegalArgumentException e) {
                        food = null;
                        e.printStackTrace();
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private static class ServingAdapter extends ArrayAdapter<Serving> {
        private ServingAdapter(Context context, Serving[] servings) {
            super(context, android.R.layout.simple_spinner_item, servings);
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                convertView = ((LayoutInflater) getContext()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                        .inflate(R.layout.spinner_dropdown_two_lines, parent, false);
                holder = new ViewHolder();
                holder.textView1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.textView2 = (TextView) convertView.findViewById(android.R.id.text2);
                convertView.setTag(holder);
            } else holder = (ViewHolder) convertView.getTag();

            final Serving serving = getItem(position);

            if (serving != null) {
                holder.textView1.setText(serving.getDescription());
                final String caption = serving.getCaption();

                if (caption != null) {
                    holder.textView2.setText(caption);
                    holder.textView2.setVisibility(View.VISIBLE);
                } else holder.textView2.setVisibility(View.GONE);
            }

            return convertView;
        }

        private static class ViewHolder {
            private TextView textView1, textView2;
        }
    }
}
