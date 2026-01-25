package com.example.smartfarmapp;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;
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

public class HistoryRepo extends BaseRepo {
    private static final String HISTORY_URL = SUPABASE_URL + "/rest/v1/FarmHistory";

    // Custom Gson for dates
    private static final com.google.gson.Gson customGson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")
            .create();

    // --- CALLBACK INTERFACES ---
    public interface AddHistoryCallback extends RepoCallBack<Void> {}

    public interface FetchHistoryCallback extends RepoCallBack<List<History>> {}

    /**
     * Adds a new history entry to the `FarmHistory` table in Supabase.
     */
    public void addHistory(History history, AddHistoryCallback callback) {
        // Convert the Java `History` object into a JSON string.
        String jsonBody = customGson.toJson(history);
        Log.d("HistoryRepo", "Sending to FarmHistory: " + jsonBody);

        // Create the body of the HTTP request.
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // Build the HTTP request, including the URL, headers (for authentication), and the body.
        Request request = new Request.Builder()
                .url(HISTORY_URL)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        // Execute the request asynchronously (on a background thread).
        httpClient.newCall(request).enqueue(new Callback() {
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
                    mainHandler.post(() -> callback.onSuccess(null));
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
     */
    public void fetchAllHistory(FetchHistoryCallback callback) {
        // Build the URL to get all entries, ordered by the recorded date descending (newest first).
        String url = HISTORY_URL + "?order=recordedAt.desc";
        Log.d("HistoryRepo", "Fetching all history: " + url);

        // Build the GET request. It's simpler than POST as it doesn't have a body.
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        // Execute the request asynchronously.
        httpClient.newCall(request).enqueue(new Callback() {
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
                    List<History> historyList = customGson.fromJson(responseBody, listType);
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
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
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
                    List<History> historyList = customGson.fromJson(responseBody, listType);
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