package com.example.smartfarmapp;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SupabaseService extends BaseRepo {
    private static final String FARM_URL = SUPABASE_URL + "/rest/v1/Farm";

    // --- CALLBACK INTERFACE ---
    public interface FarmCallback extends RepoCallBack<List<Farm>> {}

    /**
     * The main public method for this service. It fetches the data for a specific farm.
     */
    public void fetchFarms(int UserId, FarmCallback callback) {
        // Build the full URL with a query parameter to filter by the 'id' column Desending.
        String urlWithFilter = FARM_URL + "?UserID=eq." + UserId + "&order=id.desc";
        Log.d("SupabaseService", "fetchFarms called with URL: " + urlWithFilter);

        // Build the HTTP GET request.
        Request request = new Request.Builder()
                .url(urlWithFilter)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        // --- ASYNCHRONOUS EXECUTION ---
        httpClient.newCall(request).enqueue(new okhttp3.Callback() {
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