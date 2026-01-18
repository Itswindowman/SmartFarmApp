package com.example.smartfarmapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

// --- EXPLANATION ---
// This is FarmRepo
// This is a "service" class. In this context, it doesn't mean an official Android Service,
// but rather a helper class dedicated to a single purpose: communicating with your Supabase backend.
// This is a great practice called "Separation of Concerns." It keeps your complex networking
// code separate from your UI code (like MainFragment), making your app much easier to manage.



public class SupabaseService {

    private static final String SUPABASE_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Farm";

    // This is your public API key.
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    // --- MEMBER VARIABLES ---
    // These are tools the service will use to perform its work.

    // OkHttp is a powerful and popular third-party library for making HTTP requests (i.e., network calls).
    private final OkHttpClient client = new OkHttpClient();

    // Gson is a library from Google that converts JSON data (which is what web APIs send)
    // into Java objects (like your `Farm` class) and vice-versa. This is called "deserialization".
    private final Gson gson = new Gson();

    // --- THREADING HELPER ---
    // This is the key to solving Android's main threading rule. A Handler attached to the
    // "Main Looper" can take a piece of code and ensure it runs on the main UI thread,
    // even if it was started from a background thread. This prevents your app from crashing.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- THE CALLBACK INTERFACE ---
    // An interface is like a contract. We are defining a contract that says: "Anyone who wants
    // to use `fetchFarms` must provide an object that has two methods: `onSuccess` and `onFailure`."
    // This allows the `MainFragment` to give the service code to run once the network request is complete.
    public interface FarmCallback {
        void onSuccess(List<Farm> farms); // This will be called on success.
        void onFailure(Exception e);     // This will be called on failure.
    }

    // --- THE MAIN METHOD ---
    // This is the public method that your UI code (e.g., MainFragment) will call.
    // Now accepts a farmId to filter the results.
    public void fetchFarms(int farmId, FarmCallback callback) {
        // Build the URL to filter by the 'id' column.
        // The query "?id=eq.{farmId}" tells Supabase to only return rows where the id column equals our farmId.
        String urlWithFilter = SUPABASE_URL + "?id=eq." + farmId;
        Log.d("SupabaseService", "fetchFarms called with URL: " + urlWithFilter);

        Request request = new Request.Builder()
                .url(urlWithFilter) // Use the new URL with the filter
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("SupabaseService", "Farm fetch failed: " + e.getMessage());
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
                    Log.d("SupabaseService", "Farm JSON response: " + json);

                    Type listType = new TypeToken<List<Farm>>() {}.getType();
                    final List<Farm> farms = gson.fromJson(json, listType);
                    Log.d("SupabaseService", "Parsed farms: " + farms);

                    // Deliver the result (which should be a list containing just one farm)
                    mainHandler.post(() -> callback.onSuccess(farms));

                } catch (Exception e) {
                    Log.e("SupabaseService", "Farm fetch exception: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }

}
