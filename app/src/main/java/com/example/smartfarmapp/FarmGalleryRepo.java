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
import okhttp3.*;

/**
 * FarmGalleryRepo
 * ─────────────────
 * 1. Uploads a photo or video to Supabase Storage bucket "farm-gallery".
 * 2. Inserts the public URL + UserID into the FarmGallery table.
 * 3. Fetches all FarmGallery rows for a user, newest-first.
 *
 * SUPABASE SETUP:
 *   - Create a Storage bucket named exactly:  farm-gallery
 *   - Set the bucket to PUBLIC
 *   - FarmGallery table needs columns: id, UserID, URI, date
 */
public class FarmGalleryRepo extends BaseRepo {

    private static final String TAG = "FarmGalleryRepo";

    private static final String GALLERY_TABLE_URL  = SUPABASE_URL + "/rest/v1/FarmGallery";
    private static final String STORAGE_BUCKET     = "farm-gallery";
    private static final String STORAGE_UPLOAD_BASE = SUPABASE_URL + "/storage/v1/object/" + STORAGE_BUCKET + "/";
    private static final String STORAGE_PUBLIC_BASE  = SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET + "/";

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onFailure(Exception e);
    }
    public interface FetchGalleryCallback extends RepoCallBack<List<FarmGallery>> {}
    public interface AddGalleryCallback   extends RepoCallBack<Void> {}

    /**
     * Full pipeline: read file → upload to Storage → insert URL into table.
     */
    public void uploadAndSave(Context context, Uri fileUri, String mimeType,
                              long userId, UploadCallback callback) {
        new Thread(() -> {
            try {
                InputStream is = context.getContentResolver().openInputStream(fileUri);
                if (is == null) {
                    mainHandler.post(() -> callback.onFailure(new IOException("Cannot open file: " + fileUri)));
                    return;
                }
                byte[] bytes = is.readAllBytes();
                is.close();

                String extension  = mimeType.startsWith("video") ? ".mp4" : ".jpg";
                String storagePath = UUID.randomUUID().toString() + extension;

                uploadToStorage(bytes, mimeType, storagePath, new UploadCallback() {
                    @Override public void onSuccess(String publicUrl) {
                        insertRow(publicUrl, userId, new AddGalleryCallback() {
                            @Override public void onSuccess(Void r) {
                                mainHandler.post(() -> callback.onSuccess(publicUrl));
                            }
                            @Override public void onFailure(Exception e) {
                                mainHandler.post(() -> callback.onFailure(e));
                            }
                        });
                    }
                    @Override public void onFailure(Exception e) {
                        mainHandler.post(() -> callback.onFailure(e));
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "uploadAndSave error", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
        }).start();
    }

    /** Fetches all FarmGallery rows for a user, newest first. */
    public void fetchGalleryForUser(long userId, FetchGalleryCallback callback) {
        String url = GALLERY_TABLE_URL + "?UserID=eq." + userId + "&order=date.desc";
        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "fetchGallery failed", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        mainHandler.post(() -> callback.onFailure(new IOException("HTTP " + response.code())));
                        return;
                    }
                    String json = body.string();
                    Log.d(TAG, "Gallery JSON: " + json);
                    Type listType = new TypeToken<List<FarmGallery>>() {}.getType();
                    List<FarmGallery> items = gson.fromJson(json, listType);
                    mainHandler.post(() -> callback.onSuccess(items));
                } catch (Exception e) {
                    Log.e(TAG, "Gallery parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }

    // Synchronous – already on background thread from uploadAndSave()
    private void uploadToStorage(byte[] bytes, String mimeType, String path, UploadCallback callback) {
        String uploadUrl = STORAGE_UPLOAD_BASE + path;
        Log.d(TAG, "Uploading to: " + uploadUrl);
        RequestBody body = RequestBody.create(bytes, MediaType.parse(mimeType));
        Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", mimeType)
                .addHeader("x-upsert", "true")
                .post(body)
                .build();
        try {
            Response response = httpClient.newCall(request).execute();
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

    private void insertRow(String publicUrl, long userId, AddGalleryCallback callback) {
        FarmGallery item = new FarmGallery(userId, publicUrl);
        String jsonBody  = gson.toJson(item);
        Log.d(TAG, "Inserting row: " + jsonBody);
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(GALLERY_TABLE_URL)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .post(body)
                .build();
        httpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "insertRow failed", e);
                mainHandler.post(() -> callback.onFailure(e));
            }
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "insertRow OK");
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    String err = "";
                    try { if (response.body() != null) err = response.body().string(); } catch (IOException ignored) {}
                    Log.e(TAG, "insertRow HTTP " + response.code() + ": " + err);
                    final String fe = err;
                    mainHandler.post(() -> callback.onFailure(new IOException("HTTP " + response.code() + ": " + fe)));
                }
                response.close();
            }
        });
    }
}