package com.example.smartfarmapp;

import android.util.Log;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * UserVegetationRepo
 * ───────────────────
 * Manages the link between a User and their personal Vegetation list.
 *
 * Each row in UserVegetation means: "this vegetation is in this user's list."
 * The isActive flag marks which ONE row is currently driving sensor-range
 * checks and notifications (FarmAdapter, MainFragment).
 *
 * A unique partial index in the database (uq_one_active_vegetation_per_user)
 * guarantees at most one isActive=true row per user, so the two-step
 * "deactivate old, activate new" sequence in setActiveVegetation() can never
 * leave two rows active even if a step fails midway — the DB will reject
 * a duplicate active row outright.
 */
public class UserVegetationRepo extends BaseRepo {

    private static final String TAG            = "UserVegetationRepo";
    private static final String USER_VEG_URL   = SUPABASE_URL + "/rest/v1/UserVegetation";
    private static final String VEGETATION_URL = SUPABASE_URL + "/rest/v1/Vegetationtbl";

    // ── Inner model ───────────────────────────────────────────────────────────
    /**
     * Mirrors the UserVegetation table exactly.
     * Primary key is "UserVegID" (not "UserVegetationID").
     */
    public static class UserVegetationRow {
        public Long    UserVegID;
        public Long    UserID;
        public Long    VegetationID;
        public String  date;
        public Boolean isActive;

        public UserVegetationRow() {}

        public UserVegetationRow(Long userID, Long vegetationID, boolean isActive) {
            this.UserID       = userID;
            this.VegetationID = vegetationID;
            this.isActive     = isActive;
        }
    }

    // ── Callback interfaces ───────────────────────────────────────────────────
    public interface ActiveVegetationCallback {
        /** Called with the Vegetation object, or {@code null} if none is active for this user. */
        void onSuccess(Vegetation vegetation);
        void onFailure(Exception e);
    }

    public interface UserVegetationListCallback {
        /** Called with the list of UserVegetationRow entries for this user (may be empty). */
        void onSuccess(List<UserVegetationRow> rows);
        void onFailure(Exception e);
    }

    public interface AddLinkCallback extends RepoCallBack<Void> {}
    public interface SetActiveCallback extends RepoCallBack<Void> {}

    // ═════════════════════════════════════════════════════════════════════════
    //  READ
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetches the currently-active Vegetation for the given userId (isActive = true).
     * Delivers {@code null} via {@code onSuccess} if no active row exists.
     *
     * Precondition: userId is valid and callback is not null.
     * Postcondition: Fetches the active UserVegetationRow (if any), then the corresponding
     * full Vegetation object, and calls callback.
     */
    public void fetchActiveVegetation(int userId, ActiveVegetationCallback callback) {
        String url = USER_VEG_URL + "?UserID=eq." + userId + "&isActive=eq.true&limit=1";
        Log.d(TAG, "Fetching active UserVegetation: " + url);

        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Log.d(TAG, "UserVegetation JSON: " + json);
                Type listType = new TypeToken<List<UserVegetationRow>>() {}.getType();
                List<UserVegetationRow> rows = gson.fromJson(json, listType);

                if (rows == null || rows.isEmpty()) {
                    Log.d(TAG, "No active UserVegetation row for userId=" + userId);
                    callback.onSuccess(null);
                    return;
                }

                long vegId = rows.get(0).VegetationID;
                Log.d(TAG, "Found active VegetationID=" + vegId + ", fetching full record…");
                fetchVegetationById(vegId, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Active UserVegetation fetch failed", e);
                callback.onFailure(e);
            }
        });
    }

    /**
     * Fetches all UserVegetation rows (the user's whole list, active or not) for the given userId.
     *
     * Precondition: userId is valid and callback is not null.
     * Postcondition: Calls callback.onSuccess with the list of this user's UserVegetationRow entries,
     * or callback.onFailure on error.
     */
    public void fetchUserVegetationRows(int userId, UserVegetationListCallback callback) {
        String url = USER_VEG_URL + "?UserID=eq." + userId + "&order=date.desc";
        Log.d(TAG, "Fetching all UserVegetation rows: " + url);

        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Type listType = new TypeToken<List<UserVegetationRow>>() {}.getType();
                List<UserVegetationRow> rows = gson.fromJson(json, listType);
                callback.onSuccess(rows);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "fetchUserVegetationRows failed", e);
                callback.onFailure(e);
            }
        });
    }

    // ── Private step 2 of fetchActiveVegetation ─────────────────────────────

    /**
     * Precondition: vegetationId is a valid ID and callback is not null.
     * Postcondition: Fetches full Vegetation data from Supabase and calls callback.onSuccess with the object or callback.onFailure on error.
     */
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

    // ═════════════════════════════════════════════════════════════════════════
    //  WRITE  (previously missing entirely)
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Links an existing Vegetation row to a user's list. Call this right after
     * VegetationRepo.addVegetation() succeeds, using the new vegetation's id.
     * The new row is inserted with isActive = false; call setActiveVegetation()
     * separately if it should become the active one immediately.
     *
     * Precondition: userId and vegetationId are valid IDs, callback is not null.
     * Postcondition: Inserts a new UserVegetation row (isActive=false) linking the user
     * to the vegetation. Calls callback.onSuccess(null) on success, callback.onFailure on error.
     */
    public void addUserVegetation(long userId, long vegetationId, AddLinkCallback callback) {
        UserVegetationRow row = new UserVegetationRow(userId, vegetationId, false);
        String jsonBody = gson.toJson(row);
        Log.d(TAG, "Linking vegetation to user: " + jsonBody);
        // preferMinimal = true → we only need a 201 confirmation
        executePost(TAG, USER_VEG_URL, jsonBody, true, callback);
    }

    /**
     * Sets the given vegetation as the active one for this user, deactivating
     * any previously active vegetation first. Two-step sequence:
     *   1. PATCH all of this user's rows to isActive=false
     *   2. PATCH the target row to isActive=true
     * Step 2 only runs if step 1 succeeds. The database's partial unique index
     * (uq_one_active_vegetation_per_user) guarantees step 2 can never result in
     * two active rows, even if this method is called concurrently from two places.
     *
     * Precondition: userId is valid, userVegId is the UserVegID of the row to activate
     * (NOT the VegetationID), callback is not null.
     * Postcondition: Exactly one UserVegetation row for this user has isActive=true
     * on success. Calls callback.onSuccess(null) or callback.onFailure on error.
     */
    public void setActiveVegetation(long userId, long userVegId, SetActiveCallback callback) {
        String deactivateUrl = USER_VEG_URL + "?UserID=eq." + userId;
        String deactivateBody = "{\"isActive\": false}";

        Log.d(TAG, "Deactivating all UserVegetation rows for userId=" + userId);
        executePatch(TAG, deactivateUrl, deactivateBody, new RepoCallBack<Void>() {
            @Override
            public void onSuccess(Void unused) {
                String activateUrl  = USER_VEG_URL + "?UserVegID=eq." + userVegId;
                String activateBody = "{\"isActive\": true}";

                Log.d(TAG, "Activating UserVegID=" + userVegId);
                executePatch(TAG, activateUrl, activateBody, callback);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to deactivate existing active vegetation", e);
                callback.onFailure(e);
            }
        });
    }
}