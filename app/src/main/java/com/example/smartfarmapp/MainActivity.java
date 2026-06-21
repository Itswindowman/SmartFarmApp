package com.example.smartfarmapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

/**
 * --- ACTIVITY EXPLANATION ---
 * An Activity is a single, focused thing that the user can do. In modern Android development,
 * you often have a single Activity that acts as a container for multiple Fragments.
 * This `MainActivity` is the main entry point for your app's UI after it launches.
 * Its primary role here is to host the `NavHostFragment`, which manages the navigation
 * between your other Fragments (like `LoginPage` and `MainFragment`).
 */
public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "SmartFarmPrefs";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_USER_ID = "user_id";

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * --- ON-CREATE ---
     * This method is called when the Activity is first created. It's where you should do
     * all of your normal static set up: create views, bind data to lists, etc.
     * This method also provides you with a Bundle containing the activity's previously frozen state, if there was one.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     *
     * Precondition: savedInstanceState is provided by the system.
     * Postcondition: The activity is initialized, the layout is set, network monitoring is started, and navigation is handled based on SharedPreferences.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Always call the superclass's method first. This is required.
        super.onCreate(savedInstanceState);

        // This line connects your Activity to its layout file, `R.layout.activity_main`.
        // The layout file defines the visual structure of your activity's UI.
        // In this case, `activity_main.xml` contains the `NavHostFragment` which controls all your other fragments.
        setContentView(R.layout.activity_main);

        setupConstantMonitoring();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            boolean shouldRemember = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
            int userId = sharedPreferences.getInt(KEY_USER_ID, -1);

            // Edge Case: Check both remember_me AND a valid user_id existence
            if (shouldRemember && userId != -1) {
                // If "Remember Me" is true, navigate to the main fragment.
                navController.navigate(R.id.action_loginPage_to_mainFragment);
            }
        }
    }

    /**
     * Precondition: connectivityManager is not yet initialized.
     * Postcondition: connectivityManager is initialized and a default network callback is registered to show Toasts/Dialogs on connection changes.
     */
    private void setupConstantMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Connection restored
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connection restored", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onLost(Network network) {
                // Connection lost
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("No Internet")
                                .setMessage("Internet connection lost. Some features may not work.")
                                .setPositiveButton("Dismiss", null)
                                .show();
                    }
                });
            }
        };

        // Register the listener
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    /**
     * Precondition: The activity is being destroyed.
     * Postcondition: The network callback is unregistered to avoid memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
