package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.Food;
import com.mr_starktastic.sugardays.internet.FatSecretRequestBuilder;
import com.mr_starktastic.sugardays.internet.FetchAndParseJSONTask;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class FoodAutoCompleteAdapter extends ArrayAdapter {
    private ArrayList<Food> foods;
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
        return foods.get(position).getId();
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                final FilterResults filterResults = new FilterResults();

                if (!TextUtils.isEmpty(constraint)) {
                    final ArrayList<Food> foodNames;

                    try {
                        foodNames = new FetchAndParseJSONTask<>(
                                requestBuilder.buildFoodSearchUrl(constraint.toString()),
                                Food.JSON_SEARCH_PARSER).execute().get();

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
                    foods = (ArrayList<Food>) results.values;
                    notifyDataSetChanged();
                } else notifyDataSetInvalidated();
            }
        };
    }
}
