package com.example.smartfarmapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkUtil {

    /**
     * פעולה סטטית לבדיקה רגעית של האינטרנט
     * @return true אם יש אינטרנט פעיל, false אחרת
     */
    public static boolean isInternetAvailable(Context context) {
        // קבלת מנהל הקישוריות של המכשיר
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        // קבלת הרשת הפעילה כרגע (WIFI או נתונים סלולריים)
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        // קבלת היכולות של הרשת שנמצאה
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return false;

        // ********* הבדיקה המרכזית: האם יש חיבור אינטרנט אמיתי *********
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}