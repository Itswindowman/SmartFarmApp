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
    /**
     * Checks if the device has an active and functional internet connection.
     * Essential for preventing network-related crashes.
     *
     * Precondition: context is not null.
     * Postcondition: Returns true if there is an active internet connection via WIFI, CELLULAR, or ETHERNET, false otherwise.
     */
    public static boolean isInternetAvailable(Context context) {

        /*
         * 1. ConnectivityManager:
         * Think of this as the "Network Manager" of the Android OS.
         * It is a system service that monitors the state of all network connections
         * (Wi-Fi, Mobile Data, etc.). It doesn't provide the internet; it just
         * reports if the hardware is connected to a network.
         */
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) return false;

        /*
         * 2. Network Object:
         * This represents a specific "physical" connection.
         * getActiveNetwork() identifies which connection the phone is currently
         * using to send data. If this is null, the phone is in Airplane Mode
         * or simply not connected to anything.
         */
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        /*
         * 3. NetworkCapabilities:
         * This is the "ID card" of the network. Just because you are connected to
         * a Wi-Fi router doesn't mean you have internet (the router might be broken).
         * This object tells us what the network can actually DO—its speed, signal
         * strength, and most importantly, if it actually reaches the internet.
         */
        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) return false;

        /*
         * 4. THE FINAL VALIDATION:
         * We check two things to be 100% sure:
         * * A) NET_CAPABILITY_INTERNET:
         * Does this network have a path to the global web?
         * * B) Transport Types (The "Medium"):
         * How is the data moving?
         * - TRANSPORT_WIFI: Over a wireless router.
         * - TRANSPORT_CELLULAR: Over 3G/4G/5G mobile data.
         * - TRANSPORT_ETHERNET: Over a wired cable (used in some smart devices).
         * * Why check both? To ensure the connection is through a standard,
         * reliable source that we know can handle data fetching for our farm.
         */
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
    }
}