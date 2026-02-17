package com.example.smartfarmapp;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import okhttp3.*;

/**
 * UserVegetationRepo
 * ───────────────────
 * Fetches the active vegetation profile for a user from the UserVegetation table.
 *
 * UserVegetation table columns:
 *   UserVegetationID  (primary key)
 *   UserID            (int8)
 *   VegetationID      (int8)
 *   date              (timestamptz)
 *
 * Strategy: get rows for this user ordered by date DESC, take the first,
 * then fetch the full Vegetation record by VegetationID.
 */
public class UserVegetationRepo extends BaseRepo {

    private static final String TAG           = "UserVegetationRepo";
    private static final String USER_VEG_URL  = SUPABASE_URL + "/rest/v1/UserVegetation";
    private static final String VEGETATION_URL = SUPABASE_URL + "/rest/v1/Vegetationtbl";

    /** Minimal model – only the columns we need from UserVegetation. */
    public static class UserVegetationRow {
        public Long UserVegetationID;
        public Long UserID;
        public Long VegetationID;
        public String date;
    }

    public interface ActiveVegetationCallback {
        /** Called with the Vegetation object, or null if no entry exists for this user. */
        void onSuccess(Vegetation vegetation);
        void onFailure(Exception e);
    }

    /**
     * Fetches the most-recently-assigned Vegetation for the given userId.
     * Returns null (via onSuccess) if no UserVegetation row exists.
     */
    public void fetchActiveVegetation(int userId, ActiveVegetationCallback callback) {
        String url = USER_VEG_URL + "?UserID=eq." + userId + "&order=date.desc&limit=1";
        Log.d(TAG, "Fetching UserVegetation: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "UserVegetation fetch failed", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onFailure(new IOException("HTTP " + response.code())));
                        return;
                    }
                    String json = body.string();
                    Log.d(TAG, "UserVegetation JSON: " + json);

                    Type listType = new TypeToken<List<UserVegetationRow>>() {}.getType();
                    List<UserVegetationRow> rows = gson.fromJson(json, listType);

                    if (rows == null || rows.isEmpty()) {
                        Log.d(TAG, "No UserVegetation rows for userId=" + userId);
                        mainHandler.post(() -> callback.onSuccess(null));
                        return;
                    }
                    long vegId = rows.get(0).VegetationID;
                    Log.d(TAG, "Found VegetationID=" + vegId + ", fetching full record…");
                    fetchVegetationById(vegId, callback);
                } catch (Exception e) {
                    Log.e(TAG, "UserVegetation parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }

    private void fetchVegetationById(long vegetationId, ActiveVegetationCallback callback) {
        String url = VEGETATION_URL + "?id=eq." + vegetationId;
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Vegetation by ID fetch failed", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onFailure(new IOException("HTTP " + response.code())));
                        return;
                    }
                    String json = body.string();
                    Log.d(TAG, "Vegetation JSON: " + json);
                    Type listType = new TypeToken<List<Vegetation>>() {}.getType();
                    List<Vegetation> list = gson.fromJson(json, listType);
                    if (list == null || list.isEmpty()) {
                        Log.w(TAG, "No Vegetation found for id=" + vegetationId);
                        mainHandler.post(() -> callback.onSuccess(null));
                    } else {
                        Vegetation veg = list.get(0);
                        Log.d(TAG, "Active vegetation: " + veg.getName());
                        mainHandler.post(() -> callback.onSuccess(veg));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Vegetation parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}