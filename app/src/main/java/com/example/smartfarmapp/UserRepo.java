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
 * This is the Repository for `User` data. It is responsible for all data operations
 * related to the `User` table in your Supabase database.
 * It completely separates the logic of how to fetch or create a user from the UI (the `LoginPage`).
 * The `LoginPage` simply calls methods like `getUser()` or `addUser()` without needing to know
 * about the underlying network requests, URLs, or JSON parsing.
 */
public class UserRepo {

    // The specific URL for the `User` table in your Supabase project.
    private static final String USER_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/User";
    // The public API key for your Supabase project.
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    // --- MEMBER VARIABLES (TOOLS) ---
    private final OkHttpClient client = new OkHttpClient(); // For making network calls.
    private final Handler mainHandler = new Handler(Looper.getMainLooper()); // For returning results to the main UI thread.
    private final Gson gson = new GsonBuilder().create(); // For converting between JSON and Java objects.

    // --- CALLBACK INTERFACES ---
    // Define contracts for asynchronous results.

    // Callback for the sign-in process.
    public interface GetUserCallback {
        void onSuccess(User user); // Called with the User object if login is successful.
        void onFailure(Exception e); // Called if login fails.
    }

    // Callback for the sign-up process.
    public interface AddUserCallback {
        void onSuccess(); // Called if the new user is created successfully.
        void onFailure(Exception e); // Called if creation fails.
    }

    /**
     * Fetches a user from Supabase based on email and password to verify their credentials.
     * This is your sign-in logic.
     * @param email The user's email.
     * @param password The user's password.
     * @param callback The callback to be invoked with the result.
     */
    public void getUser(String email, String password, GetUserCallback callback) {
        // Build the query URL. This is how you ask Supabase for specific data.
        // It translates to: "From the User table, give me all columns (*) for the entry where email equals [email] AND password equals [password]."
        // NOTE: Sending a password in a GET request URL is highly insecure.
        String url = USER_URL + "?email=eq." + email + "&password=eq." + password + "&select=*";

        // Create the HTTP GET request.
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        // Execute the request asynchronously.
        client.newCall(request).enqueue(new Callback() {
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
     * @param user The User object containing the new user's data.
     * @param callback The callback for reporting success or failure.
     */
    public void addUser(User user, AddUserCallback callback) {
        // Convert the user object to a JSON string.
        String jsonBody = gson.toJson(user);
        // Create the request body.
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        // Create the HTTP POST request.
        Request request = new Request.Builder()
                .url(USER_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal") // Tell Supabase we don't need the new object back.
                .post(body) // Specify this is a POST request.
                .build();

        // Execute the request asynchronously.
        client.newCall(request).enqueue(new Callback() {
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
                    mainHandler.post(callback::onSuccess);
                } else {
                    // Report the failure with the HTTP status code for debugging.
                    mainHandler.post(() -> callback.onFailure(new IOException("Failed to create user. Code: " + response.code())));
                }
                response.close(); // Always close the response.
            }
        });
    }
}
