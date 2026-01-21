package com.example.smartfarmapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * --- ACTIVITY EXPLANATION ---
 * An Activity is a single, focused thing that the user can do. In modern Android development,
 * you often have a single Activity that acts as a container for multiple Fragments.
 * This `MainActivity` is the main entry point for your app's UI after it launches.
 * Its primary role here is to host the `NavHostFragment`, which manages the navigation
 * between your other Fragments (like `LoginPage` and `MainFragment`).
 */
public class MainActivity extends AppCompatActivity {

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
    }
}
