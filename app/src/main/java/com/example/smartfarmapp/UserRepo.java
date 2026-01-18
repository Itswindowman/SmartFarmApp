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
 * Repository class for handling all data operations for the "User" table in Supabase.
 */
public class UserRepo {

    private static final String USER_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/User";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new GsonBuilder().create(); // Assumes camelCase in DB matches Java fields

    // Callback for fetching a user (signing in)
    public interface GetUserCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    // Callback for adding a user (signing up)
    public interface AddUserCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Fetches a user from Supabase based on email and password.
     * @param email The user's email.
     * @param password The user's password.
     * @param callback The callback to be invoked with the result.
     */
    public void getUser(String email, String password, GetUserCallback callback) {
        // Build the query URL: .../User?email=eq.test@example.com&password=eq.12345
        String url = USER_URL + "?email=eq." + email + "&password=eq." + password + "&select=*";

        Request request = new Request.Builder()
                .url(url)
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
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure(new IOException("Unexpected code " + response)));
                    return;
                }

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        mainHandler.post(() -> callback.onFailure(new IOException("Response body is null")));
                        return;
                    }
                    String json = responseBody.string();
                    Type listType = new TypeToken<List<User>>() {}.getType();
                    List<User> users = gson.fromJson(json, listType);

                    // Check if the list is empty (no user found) or not.
                    if (users != null && !users.isEmpty()) {
                        // Success, a user was found.
                        mainHandler.post(() -> callback.onSuccess(users.get(0)));
                    } else {
                        // Failure, no user with these credentials.
                        mainHandler.post(() -> callback.onFailure(new Exception("Invalid email or password")));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }

    /**
     * Adds a new user to the Supabase database.
     * @param user The User object to add.
     * @param callback The callback for success or failure.
     */
    public void addUser(User user, AddUserCallback callback) {
        String jsonBody = gson.toJson(user);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(USER_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                // For a POST, a 201 status code means "Created successfully."
                // A 409 status code means "Conflict" (user with that email/id likely already exists).
                if (response.isSuccessful()) {
                    mainHandler.post(callback::onSuccess);
                } else {
                    mainHandler.post(() -> callback.onFailure(new IOException("Failed to create user. Code: " + response.code())));
                }
                response.close();
            }
        });
    }
}
