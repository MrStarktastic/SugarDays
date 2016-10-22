package com.mr_starktastic.sugardays.widget;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import com.mr_starktastic.sugardays.R;
import com.mr_starktastic.sugardays.data.FetchJSONTask;

import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class FoodAutoCompleteAdapter extends ArrayAdapter {
    private static final FatSecretRequestBuilder REQUEST_BUILDER = new FatSecretRequestBuilder();
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

    public FoodAutoCompleteAdapter(Context context) {
        super(context, R.layout.list_item_multiline, R.id.list_item);
        foods = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return foods.size();
    }

    @Override
    public Object getItem(int i) {
        return foods.get(i);
    }

    public String buildFoodGetUrl(int position) {
        return REQUEST_BUILDER.buildFoodGetUrl(foods.get(position).id);
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
                                REQUEST_BUILDER.buildFoodSearchUrl(constraint.toString()),
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
        private static final String JSON_FOOD_NAME_KEY = "food_name";
        private static final String JSON_FOOD_ID_KEY = "food_id";

        private String name, id;

        private FoodItem(JSONObject jsonObject) throws JSONException {
            name = jsonObject.getString(JSON_FOOD_NAME_KEY);
            id = jsonObject.getString(JSON_FOOD_ID_KEY);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * This class helps in building "search" & "get" requests for the FatSecret REST API.
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static class FatSecretRequestBuilder {
        private static final String FOODS_SEARCH_PRE_SIGNATURE_PREFIX =
                "format=json&max_results=10&method=foods.search&oauth_consumer_key=572148ea582941e5b6d2559084fd86dc&oauth_nonce=";
        private static final String POST_SIGNATURE_1 =
                "&oauth_signature_method=HMAC-SHA1&oauth_timestamp=";
        private static final String FOODS_SEARCH_POST_SIGNATURE_2 =
                "&oauth_version=1.0&search_expression=";
        private static final String FOOD_GET_PRE_SIGNATURE_PREFIX_1 = "food_id=";
        private static final String FOOD_GET_PRE_SIGNATURE_PREFIX_2 =
                "&format=json&method=food.get&oauth_consumer_key=572148ea582941e5b6d2559084fd86dc&oauth_nonce=";
        private static final String FOOD_GET_POST_SIGNATURE_2 = "&oauth_version=1.0";

        private static final String OAUTH_SIGNATURE_METHOD = "HmacSHA1";
        private static final String OAUTH_SIGNATURE_KEY = "&oauth_signature=";
        private static final String ENCODED_URL_PREFIX =
                "GET&http%3A%2F%2Fplatform.fatsecret.com%2Frest%2Fserver.api&";
        private static final String URL_PREFIX = "http://platform.fatsecret.com/rest/server.api?";

        private Mac mac;

        private FatSecretRequestBuilder() {
            try {
                (mac = Mac.getInstance(OAUTH_SIGNATURE_METHOD))
                        .init(new SecretKeySpec(
                                "310e56e441724afb933cf2c2cce50a83&".getBytes(),
                                OAUTH_SIGNATURE_METHOD));
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
            }
        }

        private String nonce() {
            return RandomStringUtils.randomAlphanumeric(new Random().nextInt(9) + 2);
        }

        private String timestamp() {
            return Long.toString(System.currentTimeMillis() / 1000);
        }

        private String encode(@NonNull String url) {
            return Uri.encode(url)
                    .replace("!", "%21")
                    .replace("*", "%2A")
                    .replace("(", "%28")
                    .replace(")", "%29");
        }

        private String sign(String params) {
            final String signature = encode(new String(Base64.encode(
                    mac.doFinal((ENCODED_URL_PREFIX + encode(params)).getBytes()),
                    Base64.DEFAULT)));

            return signature.substring(0, signature.length() - 3); // Gets rid of "%0A" at the end
        }

        private String buildFoodSearchUrl(String query) {
            final String preSignature = FOODS_SEARCH_PRE_SIGNATURE_PREFIX + nonce();
            final String postSignature = POST_SIGNATURE_1 + timestamp() +
                    FOODS_SEARCH_POST_SIGNATURE_2 + encode(query);

            return URL_PREFIX + preSignature + OAUTH_SIGNATURE_KEY +
                    sign(preSignature + postSignature) + postSignature;
        }

        private String buildFoodGetUrl(String id) {
            final String preSignature = FOOD_GET_PRE_SIGNATURE_PREFIX_1 + id +
                    FOOD_GET_PRE_SIGNATURE_PREFIX_2 + nonce();
            final String postSignature = POST_SIGNATURE_1 + timestamp() + FOOD_GET_POST_SIGNATURE_2;

            return URL_PREFIX + preSignature + OAUTH_SIGNATURE_KEY +
                    sign(preSignature + postSignature) + postSignature;
        }

        @SuppressWarnings("unused")
        private String buildFoodGetUrl(long id) {
            return buildFoodGetUrl(Long.toString(id));
        }
    }
}
