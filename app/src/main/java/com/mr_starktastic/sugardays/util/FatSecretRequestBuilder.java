package com.mr_starktastic.sugardays.util;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Base64;

import org.apache.commons.lang3.RandomStringUtils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * This singleton class helps in building "search" & "get" requests for the FatSecret REST API.
 */
@SuppressWarnings("SpellCheckingInspection")
public class FatSecretRequestBuilder {
    private static final byte[] SECRET_KEY_BYTES = "310e56e441724afb933cf2c2cce50a83&".getBytes();
    private static final String FOODS_SEARCH_PRE_SIGNATURE =
            "format=json&max_results=10&method=foods.search&oauth_consumer_key=572148ea582941e5b6d2559084fd86dc&oauth_nonce=";
    private static final String POST_SIGNATURE_1 =
            "&oauth_signature_method=HMAC-SHA1&oauth_timestamp=";
    private static final String FOODS_SEARCH_POST_SIGNATURE_2 =
            "&oauth_version=1.0&search_expression=";
    private static final String FOOD_GET_PRE_SIGN_1 = "food_id=";
    private static final String FOOD_GET_PRE_SIGN_2 =
            "&format=json&method=food.get&oauth_consumer_key=572148ea582941e5b6d2559084fd86dc&oauth_nonce=";
    private static final String POST_SIGNATURE_2 = "&oauth_version=1.0";
    private static final String FOOD_BARCODE_PRE_SIGN_1 = "barcode=";
    private static final String FOOD_BARCODE_PRE_SIGN_2 =
            "&format=json&method=food.find_id_for_barcode&oauth_consumer_key=572148ea582941e5b6d2559084fd86dc&oauth_nonce=";

    private static final String OAUTH_SIGNATURE_METHOD = "HmacSHA1";
    private static final String OAUTH_SIGNATURE_KEY = "&oauth_signature=";
    private static final String ENCODED_URL_PREFIX =
            "GET&http%3A%2F%2Fplatform.fatsecret.com%2Frest%2Fserver.api&";
    private static final String URL_PREFIX = "http://platform.fatsecret.com/rest/server.api?";

    private static FatSecretRequestBuilder instance;
    private static Mac mac;

    private FatSecretRequestBuilder() {
        try {
            (mac = Mac.getInstance(OAUTH_SIGNATURE_METHOD)).init(
                    new SecretKeySpec(SECRET_KEY_BYTES, OAUTH_SIGNATURE_METHOD));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public static FatSecretRequestBuilder getInstance() {
        if (instance != null)
            return instance;

        return instance = new FatSecretRequestBuilder();
    }

    private static String nonce() {
        return RandomStringUtils.randomAlphanumeric(new Random().nextInt(9) + 2);
    }

    private static String timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }

    private static String encode(@NonNull String url) {
        return Uri.encode(url)
                .replace("!", "%21")
                .replace("*", "%2A")
                .replace("(", "%28")
                .replace(")", "%29");
    }

    private static String sign(String params) {
        final String signature = encode(new String(Base64.encode(
                mac.doFinal((ENCODED_URL_PREFIX + encode(params)).getBytes()),
                Base64.DEFAULT)));

        return signature.substring(0, signature.length() - 3); // Gets rid of "%0A" at the end
    }

    private static String buildUrl(String preSign, String postSign) {
        return URL_PREFIX + preSign + OAUTH_SIGNATURE_KEY + sign(preSign + postSign) + postSign;
    }

    public String buildFoodSearchUrl(String query) {
        return buildUrl(FOODS_SEARCH_PRE_SIGNATURE + nonce(),
                POST_SIGNATURE_1 + timestamp() + FOODS_SEARCH_POST_SIGNATURE_2 + encode(query));
    }

    public String buildFoodGetUrl(String id) {
        return buildUrl(FOOD_GET_PRE_SIGN_1 + id + FOOD_GET_PRE_SIGN_2 + nonce(),
                POST_SIGNATURE_1 + timestamp() + POST_SIGNATURE_2);
    }

    public String buildFindIdForBarcodeUrl(String barcode) {
        return buildUrl(FOOD_BARCODE_PRE_SIGN_1 + barcode + FOOD_BARCODE_PRE_SIGN_2 + nonce(),
                POST_SIGNATURE_1 + timestamp() + POST_SIGNATURE_2);
    }

    public String buildFoodGetUrl(long id) {
        return buildFoodGetUrl(Long.toString(id));
    }
}
