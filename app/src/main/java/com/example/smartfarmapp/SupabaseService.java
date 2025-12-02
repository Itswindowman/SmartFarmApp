package com.example.smartfarmapp;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class SupabaseService {
    private static final String SUPABASE_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co/rest/v1/Farm";
    private static final String SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";

    private final OkHttpClient client = new OkHttpClient();
    private final Gson gson = new Gson();

    public interface FarmCallback {
        void onSuccess(List<Farm> farms);
        void onFailure(Exception e);
    }

    public void fetchFarms(FarmCallback callback) {
        Request request = new Request.Builder()
                .url(SUPABASE_URL)
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_API_KEY)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onFailure(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure(new IOException("Unexpected code " + response));
                    return;
                }

                String json = response.body().string();
                Type listType = new TypeToken<List<Farm>>() {}.getType();
                List<Farm> farms = gson.fromJson(json, listType);
                callback.onSuccess(farms);
            }
        });
    }
}