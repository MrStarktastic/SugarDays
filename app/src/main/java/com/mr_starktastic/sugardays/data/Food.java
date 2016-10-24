package com.mr_starktastic.sugardays.data;

import android.os.AsyncTask;

import com.mr_starktastic.sugardays.internet.FatSecretRequestBuilder;
import com.mr_starktastic.sugardays.internet.FetchAndParseJSONTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class Food {
    public static final FetchAndParseJSONTask.JSONParser<ArrayList<Food>> JSON_SEARCH_PARSER =
            new FetchAndParseJSONTask.JSONParser<ArrayList<Food>>() {
                private static final String JSON_ROOT_NAME = "foods";
                private static final String JSON_ARRAY_NAME = "food";

                @Override
                public ArrayList<Food> parseJSON(String json) throws JSONException {
                    final JSONArray foods = new JSONObject(json)
                            .getJSONObject(JSON_ROOT_NAME)
                            .getJSONArray(JSON_ARRAY_NAME);
                    final ArrayList<Food> results = new ArrayList<>();

                    for (int i = 0; i < foods.length(); ++i)
                        results.add(new Food(foods.getJSONObject(i)));

                    return results;
                }
            };

    public static final FetchAndParseJSONTask.JSONParser<Food> JSON_ID_PARSER =
            new FetchAndParseJSONTask.JSONParser<Food>() {
                private static final String JSON_FOOD_ID = "food_id";
                private static final String JSON_ID_KEY = "value";

                @Override
                public Food parseJSON(String json) throws JSONException {
                    try {
                        final Serving[] servings = new FetchAndParseJSONTask<>(
                                FatSecretRequestBuilder.getInstance().buildFoodGetUrl(
                                        new JSONObject(json).getJSONObject(JSON_FOOD_ID)
                                                .getString(JSON_ID_KEY)), Serving.JSON_PARSER)
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();

                        if (servings == null)
                            return new Food(null, null);

                        return new Food(Serving.JSON_PARSER.toString(), servings);
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
            };

    private static final String JSON_FOOD_ID_KEY = "food_id";
    private static final String JSON_FOOD_NAME_KEY = "food_name";
    private static final String JSON_FOOD_BRAND_NAME_KEY = "brand_name";

    private transient long id;
    private String fullName;
    private Serving[] servings;

    private Food(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getLong(JSON_FOOD_ID_KEY);
        final String name = jsonObject.getString(JSON_FOOD_NAME_KEY);

        try {
            final String brandName = jsonObject.getString(JSON_FOOD_BRAND_NAME_KEY);
            fullName = name + " - " + brandName;
        } catch (JSONException e) {
            fullName = name;
        }
    }

    private Food(String name, Serving[] servings) {
        fullName = name;
        this.servings = servings;
    }

    public long getId() {
        return id;
    }

    public Serving[] getServings() {
        return servings;
    }

    public void setServings(Serving[] servings) {
        this.servings = servings;
    }

    @Override
    public String toString() {
        return fullName;
    }
}
