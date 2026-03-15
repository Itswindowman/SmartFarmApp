package com.example.smartfarmapp;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class VegetationRepo extends BaseRepo {

    private static final String TAG            = "VegetationRepo";
    private static final String VEGETATION_URL = SUPABASE_URL + "/rest/v1/Vegetationtbl";

    // ── Callback interfaces ───────────────────────────────────────────────────
    public interface FetchVegetationsCallback extends RepoCallBack<List<Vegetation>> {}
    public interface AddVegetationCallback    extends RepoCallBack<Void> {}
    public interface UpdateVegetationCallback extends RepoCallBack<Void> {}

    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetches all Vegetation rows from the database.
     */
    public void fetchVegetations(FetchVegetationsCallback callback) {
        String url = VEGETATION_URL + "?select=*";

        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Log.d(TAG, "Vegetation JSON: " + json);
                Type listType = new TypeToken<List<Vegetation>>() {}.getType();
                List<Vegetation> vegetations = gson.fromJson(json, listType);
                Log.d(TAG, "Parsed vegetations: " + vegetations);
                callback.onSuccess(vegetations);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "fetchVegetations failed", e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * Inserts a new Vegetation row.
     * The {@code id} field on {@code vegetation} must be {@code null} so Supabase
     * generates a new primary key automatically.
     */
    public void addVegetation(Vegetation vegetation, AddVegetationCallback callback) {
        String jsonBody = gson.toJson(vegetation);
        Log.d(TAG, "Adding Vegetation JSON: " + jsonBody);
        // preferMinimal = false → Supabase will echo the created row (useful for getting the new id)
        executePost(TAG, VEGETATION_URL, jsonBody, false, callback);
    }

    /**
     * Updates an existing Vegetation row identified by its id.
     */
    public void updateVegetation(Vegetation vegetation, UpdateVegetationCallback callback) {
        String url      = VEGETATION_URL + "?id=eq." + vegetation.getId();
        String jsonBody = gson.toJson(vegetation);
        Log.d(TAG, "Updating Vegetation JSON: " + jsonBody);
        executePatch(TAG, url, jsonBody, callback);
    }
}