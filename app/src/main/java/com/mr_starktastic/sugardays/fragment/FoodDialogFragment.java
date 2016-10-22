package com.mr_starktastic.sugardays.fragment;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.AppCompatSpinner;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.FetchJSONTask;
import com.mr_starktastic.sugardays.util.NumericTextUtil;
import com.mr_starktastic.sugardays.widget.FoodAutoCompleteAdapter;
import com.mr_starktastic.sugardays.widget.LoadingAutoCompleteTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FoodDialogFragment extends AppCompatDialogFragment {
    private static final int AUTOCOMPLETE_THRESHOLD = 2;
    private static final String JSON_ROOT_NAME = "food";
    private static final String JSON_SERVING_CONTAINER_NAME = "servings";
    private static final String JSON_SERVING_ARRAY_OR_OBJECT_NAME = "serving";
    private static final FetchJSONTask.JSONParser<Serving> JSON_PARSER =
            new FetchJSONTask.JSONParser<Serving>() {
                @Override
                public ArrayList<Serving> parseJSON(String json) throws JSONException {
                    final JSONObject servingContainer = new JSONObject(json)
                            .getJSONObject(JSON_ROOT_NAME)
                            .getJSONObject(JSON_SERVING_CONTAINER_NAME);
                    final ArrayList<Serving> array = new ArrayList<>();

                    try {
                        final JSONArray servings = servingContainer
                                .getJSONArray(JSON_SERVING_ARRAY_OR_OBJECT_NAME);

                        for (int i = 0; i < servings.length(); ++i)
                            array.add(new Serving(servings.getJSONObject(i)));
                    } catch (JSONException e) { // There's just 1 serving and not a JSONArray
                        Log.d("JSON", json);
                        array.add(new Serving(servingContainer
                                .getJSONObject(JSON_SERVING_ARRAY_OR_OBJECT_NAME)));
                    }

                    return array;
                }
            };

    private LoadingAutoCompleteTextView foodNameEdit;
    private View quantityContainer;
    private EditText quantityEdit;
    private AppCompatSpinner servingSpinner;
    private EditText carbsEdit;

    public FoodDialogFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getContext();
        final MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.add_food)
                .customView(R.layout.dialog_fragment_food, true)
                .canceledOnTouchOutside(false)
                .positiveText(android.R.string.ok)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialogInterface,
                                        @NonNull DialogAction which) {

                    }
                }).negativeText(android.R.string.cancel)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialogInterface,
                                        @NonNull DialogAction which) {

                    }
                }).build();

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
                        servingSpinner.setAdapter(new ServingAdapter(
                                context, new FetchJSONTask<>(
                                ((FoodAutoCompleteAdapter) foodNameEdit.getAdapter())
                                        .buildFoodGetUrl(position), JSON_PARSER).execute().get()));
                        quantityContainer.setVisibility(View.VISIBLE);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            });

            quantityContainer = root.findViewById(R.id.quantity_container);
            quantityEdit = (EditText) quantityContainer.findViewById(R.id.quantity_edit);
            servingSpinner = (AppCompatSpinner) quantityContainer
                    .findViewById(R.id.serving_spinner);
            carbsEdit = (EditText) root.findViewById(R.id.carbs_edit);
        }

        // noinspection ConstantConditions
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return dialog;
    }

    private static class ServingAdapter extends ArrayAdapter<Serving> {
        private ServingAdapter(Context context, List<Serving> servings) {
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
                holder.textView1.setText(serving.description);

                if (serving.caption != null) {
                    holder.textView2.setText(serving.caption);
                    holder.textView2.setVisibility(View.VISIBLE);
                } else holder.textView2.setVisibility(View.GONE);
            }

            return convertView;
        }

        private static class ViewHolder {
            private TextView textView1, textView2;
        }
    }

    private static class Serving {
        private static final String JSON_CARBS_KEY = "carbohydrate";
        private static final String JSON_DESCRIPTION_KEY = "measurement_description";
        private static final String JSON_WEIGHT_KEY = "metric_serving_amount";
        private static final String JSON_WEIGHT_UNIT_KEY = "metric_serving_unit";
        private static final String JSON_DEFAULT_COUNT_KEY = "number_of_units";

        private String description, caption, weight, weightUnit;
        private float carbs, defaultCount;

        private Serving(JSONObject jsonObject) throws JSONException {
            carbs = Float.parseFloat(jsonObject.getString(JSON_CARBS_KEY));
            description = jsonObject.getString(JSON_DESCRIPTION_KEY);
            weight = NumericTextUtil.trimNumber(jsonObject.getString(JSON_WEIGHT_KEY));
            weightUnit = jsonObject.getString(JSON_WEIGHT_UNIT_KEY);
            defaultCount = Float.parseFloat(jsonObject.getString(JSON_DEFAULT_COUNT_KEY));

            if (description.equals(weightUnit))
                description = weight + " " + weightUnit;
            else caption = weight + " " + weightUnit;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
