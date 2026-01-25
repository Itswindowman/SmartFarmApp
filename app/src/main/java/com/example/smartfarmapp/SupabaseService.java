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

/**
 * --- SERVICE CLASS EXPLANATION ---
 * This is a "service" or "repository" class. Its single, dedicated purpose is to communicate
 * with your Supabase backend to get `Farm` data.
 * This is a fundamental concept called "Separation of Concerns." It keeps your complex networking
 * code separate from your UI code (like `MainFragment`), making your entire application much
 * cleaner, more organized, and easier to debug and manage.
 */
public class SupabaseService {

    // The base URL for your `Farm` table in Supabase.
    private static final String SUPABASE_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Farm";
    // Your public, anonymous API key. This allows your app to securely access the data according to the rules you've set in Supabase.
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    // --- MEMBER VARIABLES (TOOLS) ---
    // These are final, meaning they are initialized once and never changed.

    // OkHttp is a powerful and popular third-party library for making HTTP requests (i.e., network calls).
    private final OkHttpClient client = new OkHttpClient();

    // Gson is a library from Google that converts JSON data (which is what web APIs use)
    // into Java objects (like your `Farm` class) and vice-versa. This process is called serialization and deserialization.
    private final Gson gson = new Gson();

    // A Handler tied to the main thread's Looper. This is crucial for Android. Network operations
    // must run on a background thread to avoid freezing the UI. However, only the main thread can
    // update the UI. This handler acts as a bridge, allowing us to post results from the background thread
    // back to the main thread safely.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // --- CALLBACK INTERFACE ---
    // An interface is a contract. We are defining a contract that says: "Anyone who wants
    // to use `fetchFarms` must provide an object that has two methods: `onSuccess` and `onFailure`."
    // This allows the `MainFragment` to give the service code to run once the network request is complete.
    public interface FarmCallback {
        void onSuccess(List<Farm> farms); // This will be called with the results on a successful fetch.
        void onFailure(Exception e);     // This will be called if any error occurs.
    }

    /**
     * The main public method for this service. It fetches the data for a specific farm.
     * @param UserId The ID of the farm to retrieve data for.
     * @param callback The implementation of the FarmCallback interface, which will handle the result.
     */
    public void fetchFarms(int UserId, FarmCallback callback) {
        // Build the full URL with a query parameter to filter by the 'id' column.
        // The query `?id=eq.{UserID}` tells Supabase: "only return rows where the id column equals farmId".
        String urlWithFilter = SUPABASE_URL + "?UserID=eq." + UserId;
        Log.d("SupabaseService", "fetchFarms called with URL: " + urlWithFilter);

        // Build the HTTP GET request. This includes the URL and necessary headers for authentication.
        Request request = new Request.Builder()
                .url(urlWithFilter)
                .addHeader("apikey", SUPABASE_API_KEY) // The API key is required for every request.
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY) // Standard authorization header.
                .addHeader("Accept", "application/json") // Tells the server we expect a JSON response.
                .build();

        // --- ASYNCHRONOUS EXECUTION ---
        // `enqueue` executes the network call on a background thread. It takes a `Callback` object
        // that will handle the response when it arrives.
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // This method is called if the request fails to execute, for example, due to no network connection.
                Log.e("SupabaseService", "Farm fetch failed: " + e.getMessage());
                // We use the mainHandler to post the failure result back to the main thread.
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) {
                // This method is called when the server provides a response, successful or not.
                try {
                    // Check if the HTTP response code indicates success (e.g., 200 OK).
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }
                    // Get the body of the response.
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        throw new IOException("Response body is null");
                    }
                    // Convert the response body to a plain text JSON string.
                    String json = responseBody.string();
                    Log.d("SupabaseService", "Farm JSON response: " + json);

                    // Use Gson to parse the JSON string into a List of Farm objects.
                    // `TypeToken` is used to tell Gson that we expect a list of a specific type.
                    Type listType = new TypeToken<List<Farm>>() {}.getType();
                    final List<Farm> farms = gson.fromJson(json, listType);
                    Log.d("SupabaseService", "Parsed farms: " + farms);

                    // Post the final list of farms to the onSuccess method of the callback on the main thread.
                    mainHandler.post(() -> callback.onSuccess(farms));

                } catch (Exception e) {
                    // If anything goes wrong during response processing, report a failure.
                    Log.e("SupabaseService", "Farm fetch exception: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}
