package com.example.smartfarmapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class VegetationRepo extends BaseRepo {
    private static final String VEGETATION_URL = SUPABASE_URL + "/rest/v1/Vegetationtbl";

    // --- CALLBACK INTERFACES ---
    public interface FetchVegetationsCallback extends RepoCallBack<List<Vegetation>> {}

    public interface AddVegetationCallback extends RepoCallBack<Void> {}

    public interface UpdateVegetationCallback extends RepoCallBack<Void> {}

    /**
     * Adds a new vegetation record to the database.
     */
    public void addVegetation(Vegetation vegetation, AddVegetationCallback callback) {
        // 1. Convert the Java `Vegetation` object into a JSON string.
        String jsonBody = gson.toJson(vegetation);
        Log.d("VegetationRepo", "Adding Vegetation - Sending JSON: " + jsonBody);

        // 2. Prepare the body of the message we will send.
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // 3. Build the full request with the URL, Headers (for security), and the Body.
        Request request = new Request.Builder()
                .url(VEGETATION_URL)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        // 4. Send the request asynchronously (in the background).
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("VegetationRepo", "Vegetation add failed: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d("VegetationRepo", "Vegetation add success. Code: " + response.code());
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    String errorBodyString = "";
                    try (ResponseBody errorBody = response.body()) {
                        if (errorBody != null) {
                            errorBodyString = errorBody.string();
                        }
                    } catch (IOException e) {
                        Log.e("VegetationRepo", "Error reading error body", e);
                    }
                    Log.e("VegetationRepo", "Vegetation add failed. Code: " + response.code() + ", Error: " + errorBodyString);
                    final String finalErrorBody = errorBodyString;
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response + " body: " + finalErrorBody)));
                }
                response.close();
            }
        });
    }

    /**
     * Updates an existing record.
     */
    public void updateVegetation(Vegetation vegetation, UpdateVegetationCallback callback) {
        String url = VEGETATION_URL + "?id=eq." + vegetation.getId();
        String jsonBody = gson.toJson(vegetation);
        Log.d("VegetationRepo", "Updating Vegetation - Sending JSON: " + jsonBody);

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("VegetationRepo", "Vegetation update failed: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d("VegetationRepo", "Vegetation update success. Code: " + response.code());
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    String errorBodyString = "";
                    try (ResponseBody errorBody = response.body()) {
                        if (errorBody != null) {
                            errorBodyString = errorBody.string();
                        }
                    } catch (IOException e) {
                        Log.e("VegetationRepo", "Error reading error body", e);
                    }
                    Log.e("VegetationRepo", "Vegetation update failed. Code: " + response.code() + ", Error: " + errorBodyString);
                    final String finalErrorBody = errorBodyString;
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response + " body: " + finalErrorBody)));
                }
                response.close();
            }
        });
    }

    /**
     * Gets the list of all vegetations.
     */
    public void fetchVegetations(FetchVegetationsCallback callback) {
        Request request = new Request.Builder()
                .url(VEGETATION_URL + "?select=*")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
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
                try (ResponseBody responseBody = response.body()) {
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