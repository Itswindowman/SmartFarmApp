package com.example.smartfarmapp;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class UserRepo extends BaseRepo {

    private static final String TAG      = "UserRepo";
    private static final String USER_URL = SUPABASE_URL + "/rest/v1/User";

    // ── Callback interfaces ───────────────────────────────────────────────────
    public interface GetUserCallback extends RepoCallBack<User> {}
    public interface AddUserCallback extends RepoCallBack<Void> {}

    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetches a user by email + password to verify credentials.
     * Supabase always returns an array, so we take the first element.
     */
    public void getUser(String email, String password, GetUserCallback callback) {
        String url = USER_URL + "?email=eq." + email + "&password=eq." + password + "&select=*";

        // executeGet handles: building auth headers, async enqueue, main-thread delivery
        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Type listType = new TypeToken<List<User>>() {}.getType();
                List<User> users = gson.fromJson(json, listType);

                if (users != null && !users.isEmpty()) {
                    callback.onSuccess(users.get(0));
                } else {
                    callback.onFailure(new Exception("Invalid email or password"));
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "getUser failed", e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * Inserts a new User row into Supabase (sign-up).
     * WARNING: passwords should be hashed before storage in production.
     */
    public void addUser(User user, AddUserCallback callback) {
        String jsonBody = gson.toJson(user);
        // preferMinimal = true → Supabase returns an empty 201, which is all we need
        executePost(TAG, USER_URL, jsonBody, true, callback);
    }
}