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

public class HistoryRepo {
    // CORRECT URL for FarmHistory table
    private static final String HISTORY_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/FarmHistory";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX")
            .create();

    // Callback interfaces remain the same
    public interface AddHistoryCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface FetchHistoryCallback {
        void onSuccess(List<History> historyList);
        void onFailure(Exception e);
    }

    /**
     * Add a new entry to FarmHistory table
     */
    public void addHistory(History history, AddHistoryCallback callback) {
        String jsonBody = gson.toJson(history);
        Log.d("HistoryRepo", "Sending to FarmHistory: " + jsonBody);

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(HISTORY_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("HistoryRepo", "Network error adding to FarmHistory", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("HistoryRepo", "FarmHistory Add Response: " + response.code() + " - " + responseBody);

                if (response.isSuccessful()) {
                    Log.d("HistoryRepo", "✓ Added to FarmHistory");
                    mainHandler.post(callback::onSuccess);
                } else {
                    Log.e("HistoryRepo", "✗ Failed to add to FarmHistory: " + responseBody);
                    mainHandler.post(() -> callback.onFailure(
                            new IOException("HTTP " + response.code() + ": " + responseBody)
                    ));
                }
                response.close();
            }
        });
    }

    /**
     * Fetch all FarmHistory entries for current farm
     */


    public void fetchFarmHistory(Long farmId, FetchHistoryCallback callback) {
        // Fetch history for specific farm, newest first
        String url = HISTORY_URL + "?farmId=eq." + farmId + "&order=recordedAt.desc";
        Log.d("HistoryRepo", "Fetching FarmHistory for farm: " + farmId);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("HistoryRepo", "Network error fetching FarmHistory", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("HistoryRepo", "FarmHistory Fetch Response: " + response.code() + " - " + responseBody);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure(
                            new IOException("HTTP " + response.code() + ": " + responseBody)
                    ));
                    return;
                }

                try {
                    Type listType = new TypeToken<List<History>>() {}.getType();
                    List<History> historyList = gson.fromJson(responseBody, listType);
                    Log.d("HistoryRepo", "✓ Found " + historyList.size() + " FarmHistory entries");
                    mainHandler.post(() -> callback.onSuccess(historyList));
                } catch (Exception e) {
                    Log.e("HistoryRepo", "Error parsing FarmHistory JSON", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }

                response.close();
            }
        });
    }

    public void fetchAllHistory(FetchHistoryCallback callback) {
        // Simple fetch all entries without filtering
        String url = HISTORY_URL + "?order=recordedAt.desc";
        Log.d("HistoryRepo", "Fetching all history: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("HistoryRepo", "Fetch all failed", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d("HistoryRepo", "Fetch all response: " + response.code() + " - " + responseBody);

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure(
                            new IOException("HTTP " + response.code() + ": " + responseBody)
                    ));
                    return;
                }

                try {
                    Type listType = new TypeToken<List<History>>() {}.getType();
                    List<History> historyList = gson.fromJson(responseBody, listType);
                    Log.d("HistoryRepo", "✓ Found " + historyList.size() + " total entries");
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