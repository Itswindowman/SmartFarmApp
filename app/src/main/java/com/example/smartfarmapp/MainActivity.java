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

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    /**
     * --- ON-CREATE ---
     * This method is called when the Activity is first created. It's where you should do
     * all of your normal static set up: create views, bind data to lists, etc.
     * This method also provides you with a Bundle containing the activity's previously frozen state, if there was one.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
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

            if (shouldRemember) {
                // If "Remember Me" is true, navigate to the main fragment.
                navController.navigate(R.id.action_loginPage_to_mainFragment);
            }
        }
    }

    private void setupConstantMonitoring() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // רץ כשהאינטרנט חוזר
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "החיבור חזר!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onLost(Network network) {
                // רץ כשהאינטרנט אובד
                runOnUiThread(() -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("שים לב")
                            .setMessage("החיבור לאינטרנט אבד.")
                            .setPositiveButton("סגור", null)
                            .show();
                });
            }
        };

        // רישום המאזין
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
