package com.example.smartfarmapp;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * SupabaseService – fetches Farm sensor readings for a given user.
 *
 * Renamed internally to FarmRepo conceptually, but kept as SupabaseService
 * to avoid breaking existing callers.
 */
public class SupabaseService extends BaseRepo {

    private static final String TAG      = "SupabaseService";
    private static final String FARM_URL = SUPABASE_URL + "/rest/v1/Farm";

    // ── Callback interface ────────────────────────────────────────────────────
    public interface FarmCallback extends RepoCallBack<List<Farm>> {}

    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetches all Farm rows for a user, ordered newest-first.
     */
    public void fetchFarms(int userId, FarmCallback callback) {
        String url = FARM_URL + "?UserID=eq." + userId + "&order=id.desc";
        Log.d(TAG, "fetchFarms URL: " + url);

        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Log.d(TAG, "Farm JSON: " + json);
                Type listType = new TypeToken<List<Farm>>() {}.getType();
                List<Farm> farms = gson.fromJson(json, listType);
                Log.d(TAG, "Parsed farms: " + farms);
                callback.onSuccess(farms);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "fetchFarms failed", e);
                callback.onFailure(e);
            }
        });
    }
}