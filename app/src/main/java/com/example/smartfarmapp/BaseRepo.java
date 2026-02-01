package com.example.smartfarmapp;

import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;

public class BaseRepo {
    protected static final String SUPABASE_URL = "https://lqdbdpnqapcrgwdapbba.supabase.co";
    protected static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxxZGJkcG5xYXBjcmd3ZGFwYmJhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjM4OTE0NTEsImV4cCI6MjA3OTQ2NzQ1MX0.d0UCxvHeMxLurzJULgYrYyLdWqrCo4zqaOWW0Ptt1aM";
    protected static final OkHttpClient httpClient = new OkHttpClient();
    protected static final Gson gson = new Gson();
    protected static Handler mainHandler = new Handler(Looper.getMainLooper());


    protected BaseRepo(){
        // Empty constructor
    }

    public interface RepoCallBack<T>{
        void onSuccess(T result);
        void onFailure(Exception error);
    }
}
