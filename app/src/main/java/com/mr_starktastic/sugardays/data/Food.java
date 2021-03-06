package com.mr_starktastic.sugardays.data;

import android.os.AsyncTask;

import com.mr_starktastic.sugardays.internet.FetchAndParseJSONTask;
import com.mr_starktastic.sugardays.util.FatSecretRequestBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class Food implements Serializable {
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
    public static final float NO_CARBS = -1;
    private static final String JSON_FOOD_ID_KEY = "food_id";
    private static final String JSON_FOOD_NAME_KEY = "food_name";
    private static final String JSON_FOOD_BRAND_NAME_KEY = "brand_name";
    private transient long id;

    private String name;
    private float quantity;
    private Serving[] servings;
    private int chosenServingPosition;
    private float carbs;

    private Food oldInstance;

    private Food(JSONObject jsonObject) throws JSONException {
        id = jsonObject.getLong(JSON_FOOD_ID_KEY);
        final String name = jsonObject.getString(JSON_FOOD_NAME_KEY);

        try {
            final String brandName = jsonObject.getString(JSON_FOOD_BRAND_NAME_KEY);
            this.name = name + " - " + brandName;
        } catch (JSONException e) {
            this.name = name;
        }
    }

    private Food(String name, Serving[] servings) {
        this.name = name;
        this.servings = servings;
    }

    public Food(Food other) {
        if (other != null) {
            name = other.name;
            quantity = other.quantity;
            servings = other.servings;
            chosenServingPosition = other.chosenServingPosition;
            carbs = other.carbs;
        }

        oldInstance = other;
    }

    public boolean isSameAsOld() {
        return oldInstance != null && name.equals(oldInstance.name) &&
                quantity == oldInstance.quantity &&
                ((servings == null && oldInstance.servings == null) ||
                        (servings != null && oldInstance.servings != null &&
                                getChosenServing().equals(oldInstance.getChosenServing()))) &&
                carbs == oldInstance.carbs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Serving[] getServings() {
        return servings;
    }

    public void setServings(Serving[] servings) {
        this.servings = servings;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity /* Assumes not 0 */) {
        this.quantity = quantity;
    }

    public Serving getChosenServing() {
        return servings != null ? servings[chosenServingPosition] : null;
    }

    public int getChosenServingPosition() {
        return chosenServingPosition;
    }

    public void setChosenServingPosition(int position) {
        chosenServingPosition = position;
    }

    public float getCarbs() {
        return carbs;
    }

    public void setCarbs(float carbs) {
        this.carbs = carbs;
    }

    @Override
    public String toString() {
        return name;
    }
}
