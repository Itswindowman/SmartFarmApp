package com.example.smartfarmapp;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class VegetationRepo {

    private static final String VEGETATION_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Vegetationtbl";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    // --- CALLBACK INTERFACES ---
    public interface FetchVegetationsCallback {
        void onSuccess(List<Vegetation> vegetations);
        void onFailure(Exception e);
    }

    public interface AddVegetationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // New callback for the update operation
    public interface UpdateVegetationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Adds a new vegetation record.
     */
    public void addVegetation(Vegetation vegetation, AddVegetationCallback callback) {
        // The ID is handled by Supabase, so we don't send it.
        String jsonBody = gson.toJson(vegetation);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(VEGETATION_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                }
                response.close();
            }
        });
    }

    /**
     * Updates an existing vegetation record in the database.
     * @param vegetation The vegetation object with updated data. It MUST have a valid ID.
     * @param callback   The callback for success or failure.
     */
    public void updateVegetation(Vegetation vegetation, UpdateVegetationCallback callback) {
        // Build the URL to target the specific row, e.g., .../Vegetationtbl?id=eq.123
        String url = VEGETATION_URL + "?id=eq." + vegetation.getId();

        String jsonBody = gson.toJson(vegetation);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // An update operation is an HTTP PATCH request.
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body) // Use .patch() for updates
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                // For PATCH, a 204 No Content response means success.
                if (response.isSuccessful()) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                }
                response.close();
            }
        });
    }

    /**
     * Fetches all vegetation records.
     */
    public void fetchVegetations(FetchVegetationsCallback callback) {
        Request request = new Request.Builder()
                .url(VEGETATION_URL + "?select=*")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        throw new IOException("Response body is null");
                    }
                    String json = responseBody.string();
                    Type listType = new TypeToken<List<Vegetation>>() {}.getType();
                    final List<Vegetation> vegetations = gson.fromJson(json, listType);
                    mainHandler.post(() -> callback.onSuccess(vegetations));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}
