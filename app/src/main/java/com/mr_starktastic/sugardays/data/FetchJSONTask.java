package com.mr_starktastic.sugardays.data;

import android.os.AsyncTask;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FetchJSONTask<T> extends AsyncTask<Void, Void, ArrayList<T>> {
    private static final String CHARSET_NAME = "utf-8";
    private static final int BUFF_SIZE = 8;

    private String url;
    private JSONParser<T> parser;

    public FetchJSONTask(String url, JSONParser<T> parser) {
        this.url = url;
        this.parser = parser;
    }

    @Override
    protected ArrayList<T> doInBackground(Void... voids) {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        String jsonStr;

        try {
            (connection = (HttpURLConnection) new URL(url).openConnection()).connect();
            final InputStream inStream = connection.getInputStream();

            if (inStream == null)
                return null;

            reader = new BufferedReader(new InputStreamReader(inStream, CHARSET_NAME), BUFF_SIZE);
            final StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
                stringBuilder.append(line);

            if (stringBuilder.length() == 0)
                return null;

            jsonStr = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (connection != null)
                connection.disconnect();
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        try {
            return parser.parseJSON(jsonStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public interface JSONParser<T> {
        ArrayList<T> parseJSON(String json) throws JSONException;
    }
}
