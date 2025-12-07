package com.example.smartfarmapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

// --- EXPLANATION ---
// A Fragment represents a reusable portion of your app's UI.
// Think of it as a "mini-activity" that can be combined with other fragments
// inside a main Activity. This MainFragment appears to be the main screen of your app.
public class MainFragment extends Fragment {

    // --- MEMBER VARIABLES ---
    // These are variables that the fragment will need to access in different methods.
    // They are defined here to be accessible throughout the entire class.

    // The RecyclerView is the powerful UI widget that displays a scrollable list of items.
    private RecyclerView recyclerView;

    // The FarmAdapter is a custom class you must create. Its job is to take your
    // list of farm data and "adapt" it so the RecyclerView knows how to display each
    // individual item in the list.
    private FarmAdapter adapter;

    // This is the list that will hold all of your `Farm` objects after they are
    // fetched from the Supabase database.
    private List<Farm> farmList;

    // --- CONSTRUCTOR ---
    // A public, no-argument constructor is required for all fragments.
    // Android uses this to automatically re-create your fragment when needed
    // (for example, after the screen rotates).
    public MainFragment() {
        // Required empty public constructor
    }

    // --- FRAGMENT LIFECYCLE: onCreate ---
    // This method is called when the fragment is first being created.
    // It's used for initialization that doesn't involve the UI, because the
    // UI has not been created yet at this stage.
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Always call the parent class's method first.

        // Initialize the list that will hold your farm data. We create an empty
        // ArrayList here. It will be filled with data later from the database.
        farmList = new ArrayList<>();

        // Initialize the adapter and connect it to your data list. From this point on,
        // the adapter is "watching" the `farmList`. When `farmList` changes, we will
        // tell the adapter so it can update the screen.
        adapter = new FarmAdapter(farmList);
    }

    // --- FRAGMENT LIFECYCLE: onCreateView ---
    // This method is called when it's time for the fragment to create its user interface.
    // This is where you connect your XML layout file (`fragment_main.xml`) to your fragment's code.
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        // "Inflating" the layout means turning your XML file (R.layout.fragment_main)
        // into actual View objects that can be displayed on the screen.
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // --- UI SETUP ---
        // Now that the view is inflated, we can find specific UI elements within it using their ID.


        // Find the FloatingActionButton (the round button) from your XML layout
        FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);

        // Set a listener that waits for the user to click the button.
        // The code inside this block (the lambda `v -> { ... }`) will run every time the button is clicked.
        fabAdd.setOnClickListener(v -> {
            // A Toast is a small popup message that disappears after a few seconds. It's great for testing.
            Toast.makeText(getContext(), "FAB clicked!", Toast.LENGTH_SHORT).show();

            // TODO: This is a placeholder. Here you would add your code to
            // open a new dialog or screen to let the user add a new farm.
        });

        // Find the RecyclerView from your XML layout.
        recyclerView = view.findViewById(R.id.recyclerFarm);

        // Tell the RecyclerView HOW to arrange its items. LinearLayoutManager
        // arranges them in a simple vertical list, like a standard list.
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Connect the RecyclerView to your adapter. The RecyclerView now knows to ask
        // the `adapter` how to create and display each item from the `farmList`.
        recyclerView.setAdapter(adapter);

        // --- DATA LOADING ---
        // Now that the entire UI is set up and ready, we can start fetching the data to display.
        loadFarmData();

        // Finally, we return the fully configured view, so the Android system can draw it on the screen.
        return view;
    }

    /**
     * This method starts the process of fetching farm data from your Supabase database.
     * It runs "asynchronously," meaning the app won't freeze while waiting for the network response.
     */
    private void loadFarmData() {
        // Create an instance of your custom SupabaseService, which handles the complex network call.
        SupabaseService service = new SupabaseService();

        // Call the method to fetch farms. This happens in the background.
        // We provide a "callback" object with two methods: onSuccess and onFailure.
        // The service will call ONE of these two methods when the network request is complete.
        service.fetchFarms(new SupabaseService.FarmCallback() {

            // --- CALLBACK: onSuccess ---
            // This method is called IF the data was fetched from the server successfully.
            // The `farms` parameter will contain the list of data from the database.
            @Override
            public void onSuccess(List<Farm> farms) {
                // --- THREADING: THE MOST IMPORTANT CONCEPT HERE ---
                // Network responses almost always come back on a BACKGROUND thread.
                // However, you are ONLY allowed to update the UI from the MAIN thread.
                // `getActivity().runOnUiThread(...)` is the magic that fixes this. It takes
                // your UI code and makes sure it runs safely on the main thread, preventing a crash.

                // Safety check: ensure the fragment is still attached to an activity before doing anything.
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Clear out any old data that might be in the list from a previous load.
                        farmList.clear();
                        // Add all the new farms that we just received from the database to our list.
                        farmList.addAll(farms);
                        // IMPORTANT: Tell the adapter that the underlying data has changed. The adapter will
                        // then tell the RecyclerView to refresh itself and display the new data.
                        // If you forget this line, the screen will not update!
                        adapter.notifyDataSetChanged();
                    });
                }
            }

            // --- CALLBACK: onFailure ---
            // This method is called IF the network request failed for any reason
            // (e.g., no internet connection, a database error, incorrect URL).
            @Override
            public void onFailure(Exception e) {
                // Print the full error details to the Logcat. This is CRITICAL for debugging!
                // It gives you the technical reason why the request failed.
                e.printStackTrace();

                // Just like onSuccess, we must use runOnUiThread to show a Toast message.
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            // Show a user-friendly error message on the screen so the user knows what happened.
                            // `requireContext()` is a safe way to get the context needed for the Toast.
                            Toast.makeText(requireContext(),
                                    "Failed to load data: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}
