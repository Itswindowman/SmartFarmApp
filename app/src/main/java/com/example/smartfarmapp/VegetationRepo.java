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
 * --- REPOSITORY PATTERN EXPLANATION ---
 * This class is a "Repository". Think of it as a manager for your `Vegetationtbl` data.
 * The MainFragment (your UI) shouldn't know HOW to get data from the internet.
 * It just asks this Repository: "Hey, give me the list of vegetations" or "Please save this new plant."
 * This keeps your code clean and organized, separating data logic from UI logic.
 */
public class VegetationRepo {

    // The web address (URL) of your specific table in Supabase.
    private static final String VEGETATION_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Vegetationtbl";
    // Your public Supabase key. It acts like a password that lets your app talk to your Supabase project.
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    // OkHttp is a powerful library for making network requests (sending and receiving data over the internet).
    private final OkHttpClient client = new OkHttpClient();
    
    // A Handler allows us to send tasks to the "Main Thread" (the UI thread).
    // Network requests happen on a background thread so the app doesn't freeze.
    // But we can only update the screen from the Main Thread. This handler bridges that gap.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Gson is a library that translates between Java Objects and JSON text.
    // JSON is the language of web APIs. Java Objects are what your code speaks.
    // Your database uses camelCase column names, so we use the default Gson configuration.
    private final Gson gson = new GsonBuilder().create();

    // --- CALLBACK INTERFACES ---
    // These are "contracts" for communication. When MainFragment asks for data, it provides a callback.
    // The Repository does the work in the background, and when it's done, it calls one of these methods
    // to tell MainFragment the result.
    
    public interface FetchVegetationsCallback {
        void onSuccess(List<Vegetation> vegetations); // Called when we successfully got the list.
        void onFailure(Exception e); // Called if something went wrong (e.g., no internet).
    }

    public interface AddVegetationCallback {
        void onSuccess(); // Called when the save was successful.
        void onFailure(Exception e); // Called if the save failed.
    }

    public interface UpdateVegetationCallback {
        void onSuccess(); // Called when the update was successful.
        void onFailure(Exception e); // Called if the update failed.
    }

    /**
     * Adds a new vegetation record to the database.
     * Uses an HTTP POST request (POST = Create new data).
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
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body) // This tells the server: "I want to CREATE this data"
                .build();

        // 4. Send the request asynchronously (in the background).
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
     * Uses an HTTP PATCH request (PATCH = Modify existing data).
     */
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
                .patch(body) // This tells the server: "I want to UPDATE this data"
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
     * Uses an HTTP GET request (GET = Retrieve data).
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
