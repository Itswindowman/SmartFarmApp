package com.example.smartfarmapp;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

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

/**
 * This is a "Repository" class. Its only job is to handle all data operations (fetch, add, update)
 * for the `Vegetationtbl` table in your Supabase database.
 * This is a very important design pattern because it separates your database logic from your UI logic,
 * making your app much cleaner and easier to maintain.
 */
public class VegetationRepo {

    // A constant holding the specific URL for your 'Vegetationtbl' table.
    private static final String VEGETATION_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Vegetationtbl";
    // Your public Supabase key. It's safe to include in the app as it only allows data access based on your Row Level Security policies.
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    // OkHttp is a powerful library for making network requests.
    private final OkHttpClient client = new OkHttpClient();
    // A Handler tied to the main UI thread. This is crucial for sending results back to the UI safely.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Gson is a library to convert Java objects into JSON strings (serialization) and vice-versa (deserialization).
     * --- IMPORTANT FIX EXPLANATION ---
     * Your database columns are quoted as camelCase (e.g., "dayTempMin").
     * This means the Supabase API sends and expects JSON with camelCase keys (e.g., {"dayTempMin": ...}).
     * By using `new GsonBuilder().create()`, we are telling Gson to use its default behavior,
     * which is to match Java field names (dayTempMin) directly to JSON keys (dayTempMin). This is now correct for your schema.
     */
    private final Gson gson = new GsonBuilder().create();

    /**
     * --- EXPLANATION: Callback Interfaces ---
     * These interfaces are like contracts. They define methods that another class (like MainFragment)
     * must implement. This allows the repository to "call back" to the MainFragment when a network
     * operation is finished, either with the data (onSuccess) or with an error (onFailure).
     */
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

    /**
     * Sends a new vegetation record to the Supabase database.
     * This uses an HTTP POST request.
     */
    public void addVegetation(Vegetation vegetation, AddVegetationCallback callback) {
        // 1. Serialize the Java `Vegetation` object into a JSON string.
        String jsonBody = gson.toJson(vegetation);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // 2. Build the network request.
        Request request = new Request.Builder()
                .url(VEGETATION_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal") // We don't need Supabase to send the new record back, which saves bandwidth.
                .post(body) // Specify this is a POST request.
                .build();

        // 3. Execute the request in the background.
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // The request failed (e.g., no internet). Switch to the main thread to deliver the error.
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // The server responded. Check if it was a success.
                if (response.isSuccessful()) {
                    // Success! Switch to the main thread to call the onSuccess callback.
                    mainHandler.post(callback::onSuccess);
                } else {
                    // The server responded with an error code. Deliver the error on the main thread.
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                }
                response.close(); // Always close the response to free up resources.
            }
        });
    }

    /**
     * Updates an existing vegetation record in the database.
     * This uses an HTTP PATCH request.
     */
    public void updateVegetation(Vegetation vegetation, UpdateVegetationCallback callback) {
        // To update a specific row, we add a query parameter to the URL, like: `?id=eq.123`
        String url = VEGETATION_URL + "?id=eq." + vegetation.getId();
        String jsonBody = gson.toJson(vegetation);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body) // Specify this is a PATCH request for updating.
                .build();

        client.newCall(request).enqueue(new Callback() {
             @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
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
     * Fetches all vegetation records from the database.
     * This uses an HTTP GET request.
     */
    public void fetchVegetations(FetchVegetationsCallback callback) {
        // The `?select=*` query parameter tells Supabase to return all columns for all rows.
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
                    // 1. Get the raw JSON string from the response.
                    String json = responseBody.string();
                    // 2. Define the type of data we expect (a List of Vegetation objects).
                    Type listType = new TypeToken<List<Vegetation>>() {}.getType();
                    // 3. Use Gson to deserialize the JSON string into a list of Java objects.
                    final List<Vegetation> vegetations = gson.fromJson(json, listType);
                    // 4. Deliver the successful result on the main thread.
                    mainHandler.post(() -> callback.onSuccess(vegetations));
                } catch (Exception e) {
                    // If parsing fails, deliver an error.
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}
