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

public class UserRepo extends BaseRepo {
    private static final String USER_URL = SUPABASE_URL + "/rest/v1/User";

    // --- CALLBACK INTERFACES ---
    public interface GetUserCallback extends RepoCallBack<User> {}
    public interface AddUserCallback extends RepoCallBack<Void> {}

    /**
     * Fetches a user from Supabase based on email and password to verify their credentials.
     */
    public void getUser(String email, String password, GetUserCallback callback) {
        // Build the query URL. This is how you ask Supabase for specific data.
        String url = USER_URL + "?email=eq." + email + "&password=eq." + password + "&select=*";

        // Create the HTTP GET request.
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
                // Handle network errors.
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    // Handle server errors.
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                    return;
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        mainHandler.post(() -> callback.onFailure(new IOException("Response body is null")));
                        return;
                    }
                    String json = responseBody.string();
                    // Supabase always returns an array, even for a single result.
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = gson.fromJson(json, listType);

                    // If the returned list is not empty, it means we found a matching user.
                    if (users != null && !users.isEmpty()) {
                        // Success! A user was found.
                        mainHandler.post(() -> callback.onSuccess(users.get(0)));
                    } else {
                        // Failure. The list is empty, so no user matched the credentials.
                        mainHandler.post(() -> callback.onFailure(new Exception("Invalid email or password")));
                    }
                } catch (Exception e) {
                    // Handle JSON parsing errors.
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }

    /**
     * Adds a new user to the Supabase database. This is your sign-up logic.
     */
    public void addUser(User user, AddUserCallback callback) {
        // Convert the user object to a JSON string.
        String jsonBody = gson.toJson(user);
        // Create the request body.
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // Create the HTTP POST request.
        Request request = new Request.Builder()
                .url(USER_URL)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        // Execute the request asynchronously.
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Handle network errors.
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // A successful POST request returns a 201 "Created" status code.
                // A 409 "Conflict" code often means the user (e.g., with that email) already exists.
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    // Report the failure with the HTTP status code for debugging.
                    mainHandler.post(() -> callback.onFailure(new IOException("Failed to create user. Code: " + response.code())));
                }
                response.close(); // Always close the response.
            }
        });
    }
}