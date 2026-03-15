package com.example.smartfarmapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * FarmGalleryRepo
 * ─────────────────
 * Handles the farm photo/video gallery:
 *   1. uploadAndSave()      – reads file → uploads to Storage bucket → inserts row in table
 *   2. fetchGalleryForUser() – fetches all FarmGallery rows for a user, newest-first
 *
 * GET and POST table operations are delegated to the inherited BaseRepo helpers.
 * Only the raw Storage upload (binary, not JSON) stays local because it uses a
 * synchronous execute() call on a background thread and a different content-type.
 *
 * SUPABASE SETUP:
 *   - Storage bucket named exactly: farm-gallery  (set to PUBLIC)
 *   - FarmGallery table columns:    id, UserID, URI, date
 */
public class FarmGalleryRepo extends BaseRepo {

    private static final String TAG = "FarmGalleryRepo";

    private static final String GALLERY_TABLE_URL   = SUPABASE_URL + "/rest/v1/FarmGallery";
    private static final String STORAGE_BUCKET      = "farm-gallery";
    private static final String STORAGE_UPLOAD_BASE = SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET + "/";
    private static final String STORAGE_PUBLIC_BASE = SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET + "/";

    // ── Callback interfaces ───────────────────────────────────────────────────
    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onFailure(Exception e);
    }
    public interface FetchGalleryCallback extends RepoCallBack<List<FarmGallery>> {}
    public interface AddGalleryCallback   extends RepoCallBack<Void> {}

    // ═════════════════════════════════════════════════════════════════════════
    //  Public API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Full pipeline: read file → upload binary to Storage → insert public URL into table.
     * Runs file I/O on a background thread; all callbacks arrive on the main thread.
     */
    public void uploadAndSave(Context context, Uri fileUri, String mimeType,
                              long userId, UploadCallback callback) {
        new Thread(() -> {
            try {
                // 1. Read file bytes
                InputStream is = context.getContentResolver().openInputStream(fileUri);
                if (is == null) {
                    mainHandler.post(() -> callback.onFailure(
                            new IOException("Cannot open file: " + fileUri)));
                    return;
                }
                byte[] bytes = is.readAllBytes();
                is.close();

                // 2. Upload binary to Supabase Storage
                String extension   = mimeType.startsWith("video") ? ".mp4" : ".jpg";
                String storagePath = UUID.randomUUID().toString() + extension;

                uploadBinaryToStorage(bytes, mimeType, storagePath, new UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        // 3. Insert a row in the FarmGallery table
                        insertGalleryRow(publicUrl, userId, new AddGalleryCallback() {
                            @Override public void onSuccess(Void r) {
                                mainHandler.post(() -> callback.onSuccess(publicUrl));
                            }
                            @Override public void onFailure(Exception e) {
                                mainHandler.post(() -> callback.onFailure(e));
                            }
                        });
                    }
                    @Override
                    public void onFailure(Exception e) {
                        mainHandler.post(() -> callback.onFailure(e));
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "uploadAndSave error", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        }).start();
    }

    /**
     * Fetches all FarmGallery rows for a user, newest first.
     */
    public void fetchGalleryForUser(long userId, FetchGalleryCallback callback) {
        String url = GALLERY_TABLE_URL + "?UserID=eq." + userId + "&order=date.desc";

        executeGet(TAG, url, new RawCallback() {
            @Override
            public void onSuccess(String json) {
                Log.d(TAG, "Gallery JSON: " + json);
                Type listType = new TypeToken<List<FarmGallery>>() {}.getType();
                List<FarmGallery> items = gson.fromJson(json, listType);
                callback.onSuccess(items);
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "fetchGalleryForUser failed", e);
                callback.onFailure(e);
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Private helpers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Uploads raw bytes to the Supabase Storage bucket.
     * This is NOT JSON, so we cannot use the inherited executePost() helper.
     * Runs synchronously – must be called from a background thread.
     */
    private void uploadBinaryToStorage(byte[] bytes, String mimeType,
                                       String path, UploadCallback callback) {
        String uploadUrl = STORAGE_UPLOAD_BASE + path;
        Log.d(TAG, "Uploading binary to: " + uploadUrl);

        RequestBody body    = RequestBody.create(bytes, MediaType.parse(mimeType));
        Request     request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .post(body)
                .build();

        try {
            Response response = httpClient.newCall(request).execute();   // synchronous
            if (response.isSuccessful()) {
                String publicUrl = STORAGE_PUBLIC_BASE + path;
                Log.d(TAG, "Upload OK. URL: " + publicUrl);
                callback.onSuccess(publicUrl);
            } else {
                String err = response.body() != null ? response.body().string() : "";
                Log.e(TAG, "Upload failed: " + response.code() + " " + err);
                callback.onFailure(new IOException("Storage HTTP " + response.code() + ": " + err));
            }
            response.close();
        } catch (IOException e) {
            Log.e(TAG, "Upload IO error", e);
            callback.onFailure(e);
        }
    }

    /**
     * Inserts a FarmGallery row via the inherited executePost() helper.
     */
    private void insertGalleryRow(String publicUrl, long userId, AddGalleryCallback callback) {
        FarmGallery item   = new FarmGallery(userId, publicUrl);
        String      json   = gson.toJson(item);
        Log.d(TAG, "Inserting gallery row: " + json);
        // preferMinimal = true → we only need a 201 confirmation
        executePost(TAG, GALLERY_TABLE_URL, json, true, callback);
    }
}