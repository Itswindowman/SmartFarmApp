package com.example.smartfarmapp;

import android.os.Handler;
import android.os.Looper;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

// --- EXPLANATION ---
// This is a "service" class. In this context, it doesn't mean an official Android Service,
// but rather a helper class dedicated to a single purpose: communicating with your Supabase backend.
// This is a great practice called "Separation of Concerns." It keeps your complex networking
// code separate from your UI code (like MainFragment), making your app much easier to manage.



public class SupabaseService {

    private static final String SUPABASE_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Farm?select=*";

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
    public void fetchFarms(FarmCallback callback) {

        // --- BUILDING THE REQUEST ---
        // Here, we use OkHttp to construct the network request.
        Request request = new Request.Builder()
                .url(SUPABASE_URL) // Set the URL to connect to.
                // HTTP Headers are extra pieces of information sent with the request.
                // Supabase requires these specific headers for authentication and to know you want JSON data.
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build(); // Build the final, immutable Request object.

        // --- EXECUTING THE REQUEST ASYNCHRONOUSLY ---
        // `client.newCall(request)` creates a call ready to be executed.
        // `.enqueue(...)` tells OkHttp to run this call in the BACKGROUND. It will not freeze the app.
        // We provide a `Callback` object to handle the response when it comes back.
        client.newCall(request).enqueue(new Callback() {

            // This OkHttp callback method is triggered if the request fails at a low level
            // (e.g., no internet connection).
            @Override
            public void onFailure(Call call, IOException e) {
                // We are on a BACKGROUND thread here. We use our `mainHandler` to switch
                // to the MAIN thread before calling the `onFailure` method from our own FarmCallback interface.
                mainHandler.post(() -> callback.onFailure(e));
            }

            // This OkHttp callback method is triggered if the server sends back a response.
            // This response could be a success (like code 200 OK) or a server error (like 404 Not Found).
            @Override
            public void onResponse(Call call, Response response) {
                try {
                    // First, check if the HTTP response code indicates success (usually 200).
                    if (!response.isSuccessful()) {
                        // If not successful, we create our own error and pass it to the failure callback.
                        throw new IOException("Unexpected code " + response);
                    }

                    // Get the body of the response, which contains the JSON data.
                    ResponseBody responseBody = response.body();
                    if (responseBody == null) {
                        // If there is no body for some reason, we consider it a failure.
                        throw new IOException("Response body is null");
                    }

                    // Convert the raw response body into a single JSON string.
                    String json = responseBody.string();

                    // --- DESERIALIZATION WITH GSON ---
                    // This is where the magic happens. We tell Gson to take the `json` string
                    // and convert it into a `List` of `Farm` objects.
                    Type listType = new TypeToken<List<Farm>>() {}.getType();
                    final List<Farm> farms = gson.fromJson(json, listType);

                    // Now that we have our list of Java objects, we use the `mainHandler` again
                    // to switch to the MAIN thread and deliver the successful result.
                    mainHandler.post(() -> callback.onSuccess(farms));

                } catch (Exception e) {
                    // If any part of the `try` block fails (e.g., parsing the JSON, unsuccessful response),
                    // we catch the exception and treat it as a failure.
                    // We switch to the MAIN thread to deliver the error.
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}
