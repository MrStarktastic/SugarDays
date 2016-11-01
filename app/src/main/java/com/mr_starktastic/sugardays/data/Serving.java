package com.mr_starktastic.sugardays.data;

import com.mr_starktastic.sugardays.internet.FetchAndParseJSONTask;
import com.mr_starktastic.sugardays.util.NumericTextUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class Serving implements Serializable {
    private static final String JSON_ROOT_NAME = "food";
    private static final String JSON_FOOD_NAME = "food_name";
    private static final String JSON_CONTAINER_NAME = "servings";
    private static final String JSON_ARRAY_OR_OBJECT_NAME = "serving";

    public static final FetchAndParseJSONTask.JSONParser<Serving[]> JSON_PARSER =
            new FetchAndParseJSONTask.JSONParser<Serving[]>() {
                private String foodName;

                @Override
                public Serving[] parseJSON(String json) throws JSONException {
                    final JSONObject root, servingContainer;

                    try {
                        root = new JSONObject(json).getJSONObject(JSON_ROOT_NAME);
                        foodName = root.getString(JSON_FOOD_NAME);
                        servingContainer = root.getJSONObject(JSON_CONTAINER_NAME);
                    } catch (JSONException e) {
                        return null;
                    }

                    try {
                        final JSONArray servings = servingContainer
                                .getJSONArray(JSON_ARRAY_OR_OBJECT_NAME);
                        final Serving[] array = new Serving[servings.length()];

                        for (int i = 0; i < servings.length(); ++i)
                            array[i] = new Serving(servings.getJSONObject(i));

                        return array;
                    } catch (JSONException e) { // There's just 1 serving and not a JSONArray
                        return new Serving[]{new Serving(servingContainer
                                .getJSONObject(JSON_ARRAY_OR_OBJECT_NAME))};
                    }
                }

                /**
                 * @return Name of the food to whom the servings belong.
                 */
                @Override
                public String toString() {
                    return foodName;
                }
            };

    private static final String JSON_CARBS_KEY = "carbohydrate";
    private static final String JSON_MEASUREMENT_DESCRIPTION_KEY = "measurement_description";
    private static final String JSON_WEIGHT_KEY = "metric_serving_amount";
    private static final String JSON_WEIGHT_UNIT_KEY = "metric_serving_unit";
    private static final String JSON_DEFAULT_QUANTITY_KEY = "number_of_units";
    private static final String JSON_SERVING_DESCRIPTION_KEY = "serving_description";

    private String description;
    private String caption;
    private float carbs, defaultQuantity;

    private Serving(JSONObject jsonObject) throws JSONException {
        carbs = Float.parseFloat(jsonObject.getString(JSON_CARBS_KEY));
        description = jsonObject.getString(JSON_MEASUREMENT_DESCRIPTION_KEY);
        defaultQuantity = Float.parseFloat(jsonObject.getString(JSON_DEFAULT_QUANTITY_KEY));
        final String weight, weightUnit;

        try {
            weight = NumericTextUtil.trim(jsonObject.getString(JSON_WEIGHT_KEY));
            weightUnit = jsonObject.getString(JSON_WEIGHT_UNIT_KEY);
        } catch (JSONException e) {
            caption = jsonObject.getString(JSON_SERVING_DESCRIPTION_KEY);
            return;
        }

        if (!description.equals(weightUnit) && !description.contains(weight + weightUnit))
            caption = weight + " " + weightUnit;
    }

    public String getDescription() {
        return description;
    }

    public String getCaption() {
        return caption;
    }

    public float getCarbs() {
        return carbs;
    }

    public float getDefaultQuantity() {
        return defaultQuantity;
    }

    boolean equals(Serving other) {
        return description.equals(other.description) &&
                ((caption == null && other.caption == null) ||
                        (caption != null && other.caption != null && caption.equals(other.caption))) &&
                carbs == other.carbs && defaultQuantity == other.defaultQuantity;
    }

    @Override
    public String toString() {
        return description;
    }
}
