package com.mr_starktastic.sugardays.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.internal.MDButton;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.activity.BarcodeScanActivity;
import com.mr_starktastic.sugardays.activity.EditLogActivity;
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
    public static final String EXTRA_POSITION = "POSITION";
    public static final String EXTRA_FOOD = "FOOD";

    public static final int REQ_SCAN_BARCODE = 11;

    private static final int PERMISSION_REQ_CAMERA = 12;

    private static final int AUTOCOMPLETE_THRESHOLD = 2;
    private FatSecretRequestBuilder requestBuilder;
    private Food food;
    private int titleRes;
    private MDButton positiveButton;
    private LoadingAutoCompleteTextView foodNameEdit;
    private View qtyContainer;
    private TextInputEditText quantityEdit;
    private AppCompatSpinner servingSpinner;
    private TextInputEditText carbsEdit;
    /**
     * This {@link TextWatcher} operates with the assumption that it's never triggered
     * upon an item selection from the autocomplete dropdown.
     */
    private final TextWatcher foodNameWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            qtyContainer.setVisibility(View.GONE);
            final String str = s.toString().trim();
            food.setName(str);
            food.setServings(null);

            if (TextUtils.isEmpty(str)) {
                positiveButton.setEnabled(false);
                carbsEdit.setEnabled(false);
                food.setCarbs(Food.NO_CARBS);
            } else {
                try {
                    food.setCarbs(Float.parseFloat(carbsEdit.getText().toString()));
                } catch (NumberFormatException e) {
                    food.setCarbs(Food.NO_CARBS);
                }

                carbsEdit.setEnabled(true);
                positiveButton.setEnabled(!food.isSameAsOld());
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    public FoodDialogFragment() {
        // Empty constructor required
    }

    @Override
    public void setArguments(@NonNull Bundle args) {
        final Food oldFood = (Food) args.getSerializable(EXTRA_FOOD);
        titleRes = oldFood == null ? R.string.add_food : R.string.edit_food;

        food = new Food(oldFood);
        requestBuilder = FatSecretRequestBuilder.getInstance();

        super.setArguments(args);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(titleRes)
                .customView(R.layout.fragment_food_dialog, true)
                .canceledOnTouchOutside(false).autoDismiss(false)
                .onAny(this)
                .neutralText(R.string.scan_barcode)
                .positiveText(android.R.string.ok)
                .negativeText(android.R.string.cancel)
                .build();

        (positiveButton = dialog.getActionButton(DialogAction.POSITIVE)).setEnabled(false);
        final View root = dialog.getCustomView();
        assert root != null;

        foodNameEdit = (LoadingAutoCompleteTextView) root
                .findViewById(R.id.food_name_auto_complete_text);
        qtyContainer = root.findViewById(R.id.quantity_container);
        quantityEdit = (TextInputEditText) qtyContainer.findViewById(R.id.quantity_edit);
        servingSpinner = (AppCompatSpinner) qtyContainer.findViewById(R.id.serving_spinner);
        carbsEdit = (TextInputEditText) root.findViewById(R.id.carbs_edit);

        loadData();

        foodNameEdit.addTextChangedListener(foodNameWatcher);
        carbsEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int stat, int before, int count) {
                try {
                    food.setCarbs(Float.parseFloat(s.toString()));
                } catch (NumberFormatException e) {
                    food.setCarbs(Food.NO_CARBS);
                }

                positiveButton.setEnabled(!food.isSameAsOld());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

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
                        final AsyncTask<Void, Void, Serving[]> task =
                                new FetchAndParseJSONTask<>(requestBuilder.buildFoodGetUrl(id),
                                        Serving.JSON_PARSER).execute();
                        final Food f = (Food) foodNameEdit.getAdapter().getItem(position);
                        food.setId(f.getId());
                        food.setName(f.getName());
                        final Serving[] servings = task.get();
                        food.setServings(servings);
                        servingSpinner.setAdapter(new ServingAdapter(context, servings));
                        qtyContainer.setVisibility(View.VISIBLE);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });

            quantityEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    final String str = s.toString();
                    final float q;

                    if (!TextUtils.isEmpty(str) && (q = Float.parseFloat(str)) != 0) {
                        food.setChosenServingPosition(servingSpinner.getSelectedItemPosition());
                        food.setQuantity(q);
                        carbsEdit.setText(NumericTextUtil.trimNumber(food.getCarbs()));
                    } else positiveButton.setEnabled(false);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            servingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    food.setChosenServingPosition(position);
                    final Serving serving = food.getChosenServing();
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

    private void loadData() {
        final String name = food.getName();

        if (name != null) {
            foodNameEdit.setText(name);
            final Serving[] servings = food.getServings();

            if (servings != null) {
                quantityEdit.setText(NumericTextUtil.trimNumber(food.getQuantity()));
                servingSpinner.setAdapter(new ServingAdapter(getContext(), servings));
                servingSpinner.setSelection(food.getChosenServingPosition());
            } else qtyContainer.setVisibility(View.GONE);

            final float carbs = food.getCarbs();

            if (carbs != Food.NO_CARBS)
                carbsEdit.setText(NumericTextUtil.trimNumber(carbs));
        } else {
            qtyContainer.setVisibility(View.GONE);
            carbsEdit.setEnabled(false);
        }
    }

    private void obscurelySetFoodName(String name) {
        final FoodAutoCompleteAdapter adapter =
                (FoodAutoCompleteAdapter) foodNameEdit.getAdapter();
        foodNameEdit.setAdapter(null);
        foodNameEdit.removeTextChangedListener(foodNameWatcher);
        food.setName(name);
        foodNameEdit.setText(name);
        foodNameEdit.addTextChangedListener(foodNameWatcher);
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
                ((EditLogActivity) getActivity())
                        .setFood(getArguments().getInt(EXTRA_POSITION), food);
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
                if (data != null)
                    try {
                        final Food f = new FetchAndParseJSONTask<>(
                                requestBuilder.buildFindIdForBarcodeUrl(data.getStringExtra(
                                        BarcodeScanActivity.EXTRA_BARCODE_VALUE)),
                                Food.JSON_ID_PARSER).execute().get();

                        if (f != null) {
                            final String name = f.getName();

                            if (name == null) {
                                Toast.makeText(getContext(),
                                        getString(R.string.barcode_food_not_found),
                                        Toast.LENGTH_SHORT).show();
                                throw new IllegalArgumentException("Bad barcode");
                            }

                            obscurelySetFoodName(name);
                            final Serving[] servings = f.getServings();
                            food.setServings(servings);
                            servingSpinner.setAdapter(
                                    new ServingAdapter(getContext(), servings));
                            qtyContainer.setVisibility(View.VISIBLE);
                            carbsEdit.setEnabled(true);
                        }
                    } catch (ExecutionException | InterruptedException |
                            IllegalArgumentException e) {
                        e.printStackTrace();
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
