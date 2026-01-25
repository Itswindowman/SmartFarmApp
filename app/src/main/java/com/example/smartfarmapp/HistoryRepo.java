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

/**
 * --- REPOSITORY PATTERN EXPLANATION ---
 * This is the Repository for `History` data. Its job is to manage all data operations
 * for the `FarmHistory` table in your Supabase database.
 * It abstracts the data source from the rest of the app. This means the UI (like MainFragment)
 * doesn't need to know about URLs, API keys, or JSON. It just asks this repository to
 * "add a history entry" or "fetch all history," making the code much cleaner and easier to manage.
 */
public class HistoryRepo {

    // The specific URL for the `FarmHistory` table in your Supabase project.
    private static final String HISTORY_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/FarmHistory";
    // The public, anonymous API key for your Supabase project. This key allows read and write access based on your table's policies.
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    // OkHttp is a powerful library for making network requests (sending and receiving data over the internet).
    private final OkHttpClient client = new OkHttpClient();
    // This handler ensures that results from background network operations are delivered on the main UI thread, which is required for updating the screen.
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // Gson is used to convert Java Objects (like `History`) into JSON strings and vice-versa.
    // The custom date format is crucial to correctly parse the timestamp from Supabase.
    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")
            .create();

    // --- CALLBACK INTERFACES ---
    // These interfaces define a contract for asynchronous communication.
    // The UI calls a method and provides an implementation of the callback.
    // When the network operation finishes, the repository invokes the appropriate callback method.

    public interface AddHistoryCallback {
        void onSuccess(); // Called when an entry is successfully added.
        void onFailure(Exception e); // Called when adding an entry fails.
    }

    public interface FetchHistoryCallback {
        void onSuccess(List<History> historyList); // Called with the list of history entries on success.
        void onFailure(Exception e); // Called when fetching fails.
    }

    /**
     * Adds a new history entry to the `FarmHistory` table in Supabase.
     * This uses an HTTP POST request, which is the standard for creating new data.
     * @param history The `History` object containing the data to be saved.
     * @param callback The callback to notify the caller of success or failure.
     */
    public void addHistory(History history, AddHistoryCallback callback) {
        // Convert the Java `History` object into a JSON string.
        String jsonBody = gson.toJson(history);
        Log.d("HistoryRepo", "Sending to FarmHistory: " + jsonBody);

        // Create the body of the HTTP request.
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // Build the HTTP request, including the URL, headers (for authentication), and the body.
        Request request = new Request.Builder()
                .url(HISTORY_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal") // Tells Supabase not to return the created object in the response body.
                .post(body) // Specifies this as a POST request.
                .build();

        // Execute the request asynchronously (on a background thread).
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // If the network call itself fails (e.g., no internet connection).
                Log.e("HistoryRepo", "Network error adding to FarmHistory", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // If the server responds.
                if (response.isSuccessful()) {
                    // A successful response (like HTTP 201 Created) means the data was saved.
                    Log.d("HistoryRepo", "✓ Added to FarmHistory");
                    mainHandler.post(callback::onSuccess);
                } else {
                    // An unsuccessful response (like HTTP 400 Bad Request) means something was wrong with the data.
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.e("HistoryRepo", "✗ Failed to add to FarmHistory: " + responseBody);
                    mainHandler.post(() -> callback.onFailure(new IOException("HTTP " + response.code() + ": " + responseBody)));
                }
                response.close(); // Always close the response to free up resources.
            }
        });
    }

    /**
     * Fetches all history entries from the `FarmHistory` table.
     * This uses an HTTP GET request, the standard for retrieving data.
     * @param callback The callback to return the list of history entries or an error.
     */
    public void fetchAllHistory(FetchHistoryCallback callback) {
        // Build the URL to get all entries, ordered by the recorded date descending (newest first).
        String url = HISTORY_URL + "?order=recordedAt.desc";
        Log.d("HistoryRepo", "Fetching all history: " + url);

        // Build the GET request. It's simpler than POST as it doesn't have a body.
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json") // Tells the server we want the response in JSON format.
                .build();

        // Execute the request asynchronously.
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Handle network failures.
                Log.e("HistoryRepo", "Fetch all failed", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("HistoryRepo", "Fetch all response: " + response.code() + " - " + responseBody);

                if (!response.isSuccessful()) {
                    // Handle server errors.
                    mainHandler.post(() -> callback.onFailure(new IOException("HTTP " + response.code() + ": " + responseBody)));
                    return;
                }

                try {
                    // Use Gson to parse the JSON array response into a List of History objects.
                    Type listType = new TypeToken<List<History>>() {}.getType();
                    List<History> historyList = gson.fromJson(responseBody, listType);
                    Log.d("HistoryRepo", "✓ Found " + historyList.size() + " total entries");
                    // Return the successful result on the main thread.
                    mainHandler.post(() -> callback.onSuccess(historyList));
                } catch (Exception e) {
                    // Handle errors during JSON parsing (if the data format is unexpected).
                    Log.e("HistoryRepo", "Parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }

                response.close(); // Important: always close the response.
            }
        });


    }

    public void fetchHistoryByFarmId(long farmId, FetchHistoryCallback callback) {
        String url = HISTORY_URL + "?farmId=eq." + farmId + "&order=recordedAt.desc";
        Log.d("HistoryRepo", "Fetching history for farm: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("HistoryRepo", "Fetch by farmId failed", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("HistoryRepo", "Fetch by farmId response: " + response.code() + " - " + responseBody);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure(new IOException("HTTP " + response.code() + ": " + responseBody)));
                    return;
                }

                try {
                    Type listType = new TypeToken<List<History>>() {}.getType();
                    List<History> historyList = gson.fromJson(responseBody, listType);
                    Log.d("HistoryRepo", "✓ Found " + historyList.size() + " entries for farm " + farmId);
                    mainHandler.post(() -> callback.onSuccess(historyList));
                } catch (Exception e) {
                    Log.e("HistoryRepo", "Parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
                response.close();
            }
        });
    }
}
