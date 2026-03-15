package com.example.smartfarmapp;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * UserVegetationRepo
 * ───────────────────
 * Fetches the active Vegetation profile for a user.
 *
 * Strategy:
 *   1. GET the most-recent UserVegetation row for this user.
 *   2. Use its VegetationID to GET the full Vegetation record.
 */
public class UserVegetationRepo extends BaseRepo {

    private static final String TAG            = "UserVegetationRepo";
    private static final String USER_VEG_URL   = SUPABASE_URL + "/rest/v1/UserVegetation";
    private static final String VEGETATION_URL = SUPABASE_URL + "/rest/v1/Vegetationtbl";

    // ── Inner model ───────────────────────────────────────────────────────────
    /** Minimal model – only the columns we need from UserVegetation. */
    /**
     * Mirrors the UserVegetation table exactly.
     *
     * BUG FIX: The schema primary key is "UserVegID", NOT "UserVegetationID".
     * Gson matches field names to JSON keys, so the wrong name meant this
     * field was always null and the row was silently dropped.
     */
    public static class UserVegetationRow {
        public Long   UserVegID;      // ← fixed: was "UserVegetationID"
        public Long   UserID;
        public Long   VegetationID;
        public String date;
    }

    // ── Callback interface ────────────────────────────────────────────────────
    public interface ActiveVegetationCallback {
        /** Called with the Vegetation object, or {@code null} if none exists for this user. */
        void onSuccess(Vegetation vegetation);
        void onFailure(Exception e);
    }

    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetches the most-recently-assigned Vegetation for the given userId.
     * Delivers {@code null} via {@code onSuccess} if no UserVegetation row exists.
     */
    public void fetchActiveVegetation(int userId, ActiveVegetationCallback callback) {
        String url = USER_VEG_URL + "?UserID=eq." + userId + "&order=date.desc&limit=1";
        Log.d(TAG, "Fetching UserVegetation: " + url);

        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Log.d(TAG, "UserVegetation JSON: " + json);
                Type listType = new TypeToken<List<UserVegetationRow>>() {}.getType();
                List<UserVegetationRow> rows = gson.fromJson(json, listType);

                if (rows == null || rows.isEmpty()) {
                    Log.d(TAG, "No UserVegetation rows for userId=" + userId);
                    callback.onSuccess(null);
                    return;
                }

                long vegId = rows.get(0).VegetationID;
                Log.d(TAG, "Found VegetationID=" + vegId + ", fetching full record…");
                fetchVegetationById(vegId, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "UserVegetation fetch failed", e);
                callback.onFailure(e);
            }
        });
    }

    // ── Private step 2 ───────────────────────────────────────────────────────

    private void fetchVegetationById(long vegetationId, ActiveVegetationCallback callback) {
        String url = VEGETATION_URL + "?id=eq." + vegetationId;

        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Log.d(TAG, "Vegetation JSON: " + json);
                Type listType = new TypeToken<List<Vegetation>>() {}.getType();
                List<Vegetation> list = gson.fromJson(json, listType);

                if (list == null || list.isEmpty()) {
                    Log.w(TAG, "No Vegetation found for id=" + vegetationId);
                    callback.onSuccess(null);
                } else {
                    Vegetation veg = list.get(0);
                    Log.d(TAG, "Active vegetation: " + veg.getName());
                    callback.onSuccess(veg);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Vegetation by ID fetch failed", e);
                callback.onFailure(e);
            }
        });
    }
}