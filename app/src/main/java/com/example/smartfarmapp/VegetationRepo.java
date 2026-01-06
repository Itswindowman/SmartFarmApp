package com.example.smartfarmapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

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

/**
 * This is a "Repository" class. Its only job is to handle all data operations (fetch, add, update)
 * for the `Vegetationtbl` table in your Supabase database.
 */
public class VegetationRepo {

    private static final String VEGETATION_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Vegetationtbl";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * --- IMPORTANT FIX ---
     * Your database columns are quoted as camelCase (e.g., "dayTempMin").
     * This means the Supabase API sends and expects JSON with camelCase keys.
     * By removing the `.setFieldNamingPolicy` line, we tell Gson to use its default behavior,
     * which correctly maps Java camelCase fields to JSON camelCase keys.
     */
    private final Gson gson = new GsonBuilder().create();

    public interface FetchVegetationsCallback {
        void onSuccess(List<Vegetation> vegetations);
        void onFailure(Exception e);
    }

    public interface AddVegetationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface UpdateVegetationCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public void addVegetation(Vegetation vegetation, AddVegetationCallback callback) {
        String jsonBody = gson.toJson(vegetation);
        Log.d("VegetationRepo", "Adding Vegetation - Sending JSON: " + jsonBody);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(VEGETATION_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("VegetationRepo", "Vegetation add failed: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d("VegetationRepo", "Vegetation add success. Code: " + response.code());
                    mainHandler.post(callback::onSuccess);
                } else {
                    String errorBody = "";
                    try (ResponseBody rb = response.body()) {
                        if (rb != null) errorBody = rb.string();
                    } catch (Exception ignored) {}
                    String str =  errorBody; // to use for line 94 for some reason
                    Log.e("VegetationRepo", "Vegetation add failed. Code: " + response.code() + ", Error: " + errorBody);
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response + " body: " + str)));
                }
                response.close();
            }
        });
    }

    public void updateVegetation(Vegetation vegetation, UpdateVegetationCallback callback) {
        String url = VEGETATION_URL + "?id=eq." + vegetation.getId();
        String jsonBody = gson.toJson(vegetation);
        Log.d("VegetationRepo", "Updating Vegetation - Sending JSON: " + jsonBody);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("VegetationRepo", "Vegetation update failed: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d("VegetationRepo", "Vegetation update success. Code: " + response.code());
                    mainHandler.post(callback::onSuccess);
                } else {
                    String errorBody = "";
                    try (ResponseBody rb = response.body()) {
                        if (rb != null) errorBody = rb.string();
                    } catch (Exception ignored) {}
                    Log.e("VegetationRepo", "Vegetation update failed. Code: " + response.code() + ", Error: " + errorBody);
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                }
                response.close();
            }
        });
    }

    public void fetchVegetations(FetchVegetationsCallback callback) {
        Request request = new Request.Builder()
                .url(VEGETATION_URL + "?select=*")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                    return;
                }
                try (ResponseBody responseBody = response.body()){
                    if (responseBody == null) {
                        mainHandler.post(() -> callback.onFailure(new IOException("Response body is null")));
                        return;
                    }
                    String json = responseBody.string();
                    Log.d("VegetationRepo", "Vegetation JSON response: " + json);
                    Type listType = new TypeToken<List<Vegetation>>() {}.getType();
                    final List<Vegetation> vegetations = gson.fromJson(json, listType);
                    Log.d("VegetationRepo", "Parsed vegetations: " + vegetations);
                    mainHandler.post(() -> callback.onSuccess(vegetations));
                } catch (Exception e) {
                    Log.e("VegetationRepo", "Error parsing vegetations", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}
