package com.example.smartfarmapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * BaseRepo
 * ─────────
 * The single source of truth for all HTTP communication with Supabase.
 *
 * Child repos (UserRepo, VegetationRepo, SupabaseService, …) inherit:
 *   • Shared config  – SUPABASE_URL, SUPABASE_KEY, httpClient, gson, mainHandler
 *   • buildGetRequest()   – creates an authenticated GET Request
 *   • executeGet()        – sends a GET and delivers the raw JSON string to a callback
 *   • executePost()       – sends a POST with a JSON body
 *   • executePatch()      – sends a PATCH with a JSON body
 *
 * All three execute* methods handle:
 *   - background execution via OkHttp's async enqueue
 *   - posting results back to the main thread via mainHandler
 *   - consistent error reporting through RepoCallBack<String>
 *
 * Child repos only need to parse the JSON string they receive – no HTTP
 * boilerplate required.
 */
public class BaseRepo {

    // ── Supabase config ───────────────────────────────────────────────────────
    protected static final String SUPABASE_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co";
    protected static final String SUPABASE_KEY =
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
                    ".eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0" +
                    ".d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    // ── Shared singletons ─────────────────────────────────────────────────────
    protected static final OkHttpClient httpClient = new OkHttpClient();
    protected static final Gson         gson        = new Gson();
    protected static final Handler      mainHandler = new Handler(Looper.getMainLooper());

    protected static final MediaType JSON_MEDIA_TYPE =
            MediaType.get("application/json; charset=utf-8");

    protected BaseRepo() {}

    // ═════════════════════════════════════════════════════════════════════════
    //  Generic callback – child repos receive a raw JSON String on success
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Typed callback used by all public repo methods.
     * T is the fully-parsed result type (e.g. User, List<Farm>, Void).
     */
    public interface RepoCallBack<T> {
        void onSuccess(T result);
        void onFailure(Exception error);
    }

    /** Internal callback used by the execute* helpers – delivers raw JSON. */
    protected interface RawCallback {
        void onSuccess(String json);
        void onFailure(Exception e);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Request builders
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Builds an authenticated GET {@link Request} for the given URL.
     * Use this when you need to execute the request yourself (e.g. synchronously).
     */
    protected Request buildGetRequest(String url) {
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Accept", "application/json")
                .build();
    }

    /**
     * Builds an authenticated POST {@link Request} with a JSON body.
     *
     * @param preferMinimal when {@code true} adds {@code Prefer: return=minimal}
     *                      so Supabase returns an empty body (saves bandwidth).
     */
    protected Request buildPostRequest(String url, String jsonBody, boolean preferMinimal) {
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        Request.Builder builder = new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(body);
        if (preferMinimal) builder.addHeader("Prefer", "return=minimal");
        return builder.build();
    }

    /**
     * Builds an authenticated PATCH {@link Request} with a JSON body.
     * Always uses {@code Prefer: return=minimal}.
     */
    protected Request buildPatchRequest(String url, String jsonBody) {
        RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
        return new Request.Builder()
                .url(url)
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(body)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Async execute helpers
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Executes a GET request asynchronously.
     * Delivers the raw JSON response body string to {@code callback.onSuccess}.
     *
     * @param tag     log tag identifying the calling repo/method
     * @param url     full Supabase REST URL (with any query parameters)
     * @param callback receives the raw JSON string or an exception
     */
    protected void executeGet(String tag, String url, RawCallback callback) {
        Request request = buildGetRequest(url);
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(tag, "GET failed: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try (ResponseBody body = response.body()) {
                    if (!response.isSuccessful() || body == null) {
                        String msg = "HTTP " + response.code() + " on GET " + url;
                        Log.e(tag, msg);
                        mainHandler.post(() -> callback.onFailure(new IOException(msg)));
                        return;
                    }
                    String json = body.string();
                    Log.d(tag, "GET response: " + json);
                    mainHandler.post(() -> callback.onSuccess(json));
                } catch (Exception e) {
                    Log.e(tag, "GET parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }

    /**
     * Executes a POST request asynchronously.
     * Calls {@code callback.onSuccess(null)} on HTTP 2xx, {@code onFailure} otherwise.
     *
     * @param tag           log tag
     * @param url           target URL
     * @param jsonBody      serialised request payload
     * @param preferMinimal whether to add {@code Prefer: return=minimal}
     * @param callback      result callback
     */
    protected void executePost(String tag, String url, String jsonBody,
                               boolean preferMinimal, RepoCallBack<Void> callback) {
        Request request = buildPostRequest(url, jsonBody, preferMinimal);
        Log.d(tag, "POST to " + url + " body: " + jsonBody);
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(tag, "POST failed: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (response.isSuccessful()) {
                        Log.d(tag, "POST success. Code: " + response.code());
                        mainHandler.post(() -> callback.onSuccess(null));
                    } else {
                        String err = readErrorBody(response);
                        Log.e(tag, "POST failed. Code: " + response.code() + ", Error: " + err);
                        mainHandler.post(() -> callback.onFailure(
                                new IOException("HTTP " + response.code() + ": " + err)));
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    /**
     * Executes a PATCH request asynchronously.
     * Calls {@code callback.onSuccess(null)} on HTTP 2xx, {@code onFailure} otherwise.
     *
     * @param tag      log tag
     * @param url      target URL (must already include the row filter, e.g. {@code ?id=eq.5})
     * @param jsonBody serialised patch payload
     * @param callback result callback
     */
    protected void executePatch(String tag, String url, String jsonBody,
                                RepoCallBack<Void> callback) {
        Request request = buildPatchRequest(url, jsonBody);
        Log.d(tag, "PATCH to " + url + " body: " + jsonBody);
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(tag, "PATCH failed: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    if (response.isSuccessful()) {
                        Log.d(tag, "PATCH success. Code: " + response.code());
                        mainHandler.post(() -> callback.onSuccess(null));
                    } else {
                        String err = readErrorBody(response);
                        Log.e(tag, "PATCH failed. Code: " + response.code() + ", Error: " + err);
                        mainHandler.post(() -> callback.onFailure(
                                new IOException("HTTP " + response.code() + ": " + err)));
                    }
                } finally {
                    response.close();
                }
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Internal utilities
    // ═════════════════════════════════════════════════════════════════════════

    /** Safely reads and returns the error body string without throwing. */
    private String readErrorBody(Response response) {
        try (ResponseBody errorBody = response.body()) {
            return errorBody != null ? errorBody.string() : "(empty body)";
        } catch (IOException e) {
            return "(could not read error body)";
        }
    }
}