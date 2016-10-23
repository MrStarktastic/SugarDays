package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.FetchJSONTask;
import com.mr_starktastic.sugardays.util.FatSecretRequestBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class FoodAutoCompleteAdapter extends ArrayAdapter {
    private static final String JSON_ROOT_NAME = "foods";
    private static final String JSON_ARRAY_NAME = "food";

    private static final FetchJSONTask.JSONParser<FoodItem> JSON_PARSER =
            new FetchJSONTask.JSONParser<FoodItem>() {
                @Override
                public ArrayList<FoodItem> parseJSON(String json) throws JSONException {
                    final JSONArray foods = new JSONObject(json)
                            .getJSONObject(JSON_ROOT_NAME)
                            .getJSONArray(JSON_ARRAY_NAME);
                    final ArrayList<FoodItem> results = new ArrayList<>();

                    for (int i = 0; i < foods.length(); ++i)
                        results.add(new FoodItem(foods.getJSONObject(i)));

                    return results;
                }
            };

    private ArrayList<FoodItem> foods;
    private FatSecretRequestBuilder requestBuilder;

    public FoodAutoCompleteAdapter(Context context) {
        super(context, R.layout.list_item_multiline, R.id.list_item);
        foods = new ArrayList<>();
        requestBuilder = FatSecretRequestBuilder.getInstance();
    }

    @Override
    public int getCount() {
        return foods.size();
    }

    @Override
    public Object getItem(int position) {
        return foods.get(position);
    }

    @Override
    public long getItemId(int position) {
        return foods.get(position).id;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults filterResults = new FilterResults();

                if (!TextUtils.isEmpty(constraint)) {
                    final ArrayList<FoodItem> foodNames;

                    try {
                        foodNames = new FetchJSONTask<>(
                                requestBuilder.buildFoodSearchUrl(constraint.toString()),
                                JSON_PARSER).execute().get();

                        if (foodNames != null) {
                            filterResults.values = foodNames;
                            filterResults.count = foodNames.size();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }

                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) { // noinspection unchecked
                    foods = (ArrayList<FoodItem>) results.values;
                    notifyDataSetChanged();
                } else notifyDataSetInvalidated();
            }
        };
    }

    private static class FoodItem {
        private static final String JSON_FOOD_ID_KEY = "food_id";
        private static final String JSON_FOOD_NAME_KEY = "food_name";
        private static final String JSON_FOOD_BRAND_NAME_KEY = "brand_name";

        private long id;
        private String name, fullName;

        private FoodItem(JSONObject jsonObject) throws JSONException {
            id = jsonObject.getLong(JSON_FOOD_ID_KEY);
            name = jsonObject.getString(JSON_FOOD_NAME_KEY);

            try {
                final String brandName = jsonObject.getString(JSON_FOOD_BRAND_NAME_KEY);
                fullName = name + " - " + brandName;
            } catch (JSONException e) {
                fullName = name;
            }
        }

        @Override
        public String toString() {
            return fullName;
        }
    }
}
