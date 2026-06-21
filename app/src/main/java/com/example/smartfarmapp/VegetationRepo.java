package com.example.smartfarmapp;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * VegetationRepo
 * ───────────────
 * Vegetations are PRIVATE per user: every read is scoped to a UserID, and
 * every write must have UserID set on the Vegetation object beforehand.
 *
 * Previously fetchVegetations() returned every row in the table with no
 * filtering at all, so every user saw every vegetation ever created by
 * anyone. That is fixed here — see fetchVegetationsForUser().
 */
public class VegetationRepo extends BaseRepo {

    private static final String TAG            = "VegetationRepo";
    private static final String VEGETATION_URL = SUPABASE_URL + "/rest/v1/Vegetationtbl";

    // ── Callback interfaces ───────────────────────────────────────────────────
    public interface FetchVegetationsCallback extends RepoCallBack<List<Vegetation>> {}
    public interface AddVegetationCallback    extends RepoCallBack<Void> {}
    public interface UpdateVegetationCallback extends RepoCallBack<Void> {}
    public interface DeleteVegetationCallback extends RepoCallBack<Void> {}

    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetches only the Vegetation rows belonging to the given user.
     * Replaces the old fetchVegetations() which had no UserID filter at all.
     *
     * Precondition: userId is valid and callback is not null.
     * Postcondition: Calls callback.onSuccess with this user's vegetations only,
     * or callback.onFailure on error.
     */
    public void fetchVegetationsForUser(long userId, FetchVegetationsCallback callback) {
        String url = VEGETATION_URL + "?UserID=eq." + userId + "&select=*";

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
                Log.e(TAG, "fetchVegetationsForUser failed", e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * Inserts a new Vegetation row owned by a specific user.
     * The {@code id} field on {@code vegetation} must be {@code null} so Supabase
     * generates a new primary key automatically. {@code vegetation.UserID} MUST
     * be set (via setUserID()) before calling this, or the row will be
     * orphaned (UserID = NULL) and invisible to everyone once fetched via
     * fetchVegetationsForUser().
     *
     * Precondition: vegetation and callback are not null. vegetation.id is null,
     * vegetation.getUserID() is non-null and valid.
     * Postcondition: Calls callback.onSuccess(null) if insert is successful, or callback.onFailure on error.
     */
    public void addVegetation(Vegetation vegetation, AddVegetationCallback callback) {
        if (vegetation.getUserID() == null) {
            Log.e(TAG, "addVegetation called without UserID set – aborting to avoid an orphaned row");
            callback.onFailure(new IllegalArgumentException(
                    "Vegetation.UserID must be set before calling addVegetation()"));
            return;
        }
        String jsonBody = gson.toJson(vegetation);
        Log.d(TAG, "Adding Vegetation JSON: " + jsonBody);
        // preferMinimal = false → Supabase will echo the created row (useful for getting the new id)
        executePost(TAG, VEGETATION_URL, jsonBody, false, callback);
    }

    /**
     * Updates an existing Vegetation row identified by its id.
     * Ownership is not re-checked here (id already pins it to one row) — the
     * UI layer is responsible for only ever offering the current user's own
     * vegetations for editing in the first place.
     *
     * Precondition: vegetation and callback are not null. vegetation.getId() must return a valid ID.
     * Postcondition: Calls callback.onSuccess(null) if update is successful, or callback.onFailure on error.
     */
    public void updateVegetation(Vegetation vegetation, UpdateVegetationCallback callback) {
        String url      = VEGETATION_URL + "?id=eq." + vegetation.getId();
        String jsonBody = gson.toJson(vegetation);
        Log.d(TAG, "Updating Vegetation JSON: " + jsonBody);
        executePatch(TAG, url, jsonBody, callback);
    }

    /**
     * Deletes a Vegetation row identified by its id.
     *
     * Precondition: vegetationId is a valid ID and callback is not null.
     * Postcondition: Calls callback.onSuccess(null) if delete is successful, or callback.onFailure on error.
     */
    public void deleteVegetation(long vegetationId, DeleteVegetationCallback callback) {
        String url = VEGETATION_URL + "?id=eq." + vegetationId;
        Log.d(TAG, "Deleting Vegetation ID: " + vegetationId);
        executeDelete(TAG, url, callback);
    }
}