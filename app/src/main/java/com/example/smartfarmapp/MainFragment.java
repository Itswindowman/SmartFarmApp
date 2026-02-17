
package com.example.smartfarmapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;


/**
 * --- FRAGMENT EXPLANATION ---
 * A Fragment is a reusable piece of your app's UI. Think of it like a mini-activity.
 * This MainFragment is the primary screen the user sees after logging in. It's responsible for:
 * - Displaying the latest farm sensor data.
 * - Allowing the user to manage vegetation profiles.
 * - Showing notifications when sensor values are out of range.
 * - Providing access to the history of sensor readings.
 */
public class MainFragment extends Fragment {

    // --- PERMISSION HANDLING ---
    // This is the modern way to ask for permissions in Android.
    // It launches the system dialog to ask the user for permission (in this case, to send notifications).
    // When the user responds, the code inside the lambda (the `isGranted -> { ... }`) is executed.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // If the user grants permission, show a confirmation message.
                    Toast.makeText(getContext(), "Notification permission granted.", Toast.LENGTH_SHORT).show();
                } else {
                    // If the user denies permission, inform them that notifications will be disabled.
                    Toast.makeText(getContext(), "Notifications will not be shown as permission was denied.", Toast.LENGTH_LONG).show();
                }
            });

    // --- UI & DATA-RELATED VARIABLES ---
    private BroadcastReceiver dataUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("MainFragment", "📡 Received broadcast: Data updated!");
            loadFarmData();
        }
    };
    private Button btnHistory; // Button to open the history dialog.
    private HistoryRepo historyRepo; // The repository for managing history data.

    public static final String CHANNEL_ID = "FarmAlerts"; // A unique ID for the notification channel.
    private RecyclerView recyclerView; // The view that displays the list of farm data.
    private FarmAdapter adapter; // The adapter that manages the data for the RecyclerView.
    private List<Farm> farmList; // The list of farm data to be displayed.
    private FloatingActionButton fabAdd; // The floating action button to add/edit vegetation profiles.
    private VegetationRepo vegetationRepo; // The repository for managing vegetation data.
    private TextView tvActiveVegetation; // The text view that displays the name of the active vegetation profile.
    private Button CameraBtn; // Button to save the current state to history.
    private Button LiveCameraBtn;
    // --- STATE VARIABLES ---
    private List<Vegetation> allVegetations = new ArrayList<>(); // A list to hold all available vegetation profiles.
    private Vegetation selectedVegetation = null; // The vegetation profile currently selected in the dialog.
    private boolean isEditMode = false; // A flag to determine if the dialog is in "add" or "edit" mode.

    private int FarmTimer = 1000; // Milisecs
    private Timer refreshTimer;
    private TimerTask refreshTask;

    /**
     * The default constructor for the fragment. Required for the Android framework.
     */
    public MainFragment() {}

    /**
     * --- ON-CREATE ---
     * This method is called when the fragment is first created.
     * It's used for one-time initialization that doesn't involve the UI.
     * @param savedInstanceState Data from a previous state, if available.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Initialize the list and adapter for the RecyclerView.
        farmList = new ArrayList<>();
        adapter = new FarmAdapter(farmList);
        // Initialize the repositories for accessing data.
        vegetationRepo = new VegetationRepo();
        historyRepo = new HistoryRepo();
        // Create the notification channel required for sending notifications on Android 8.0+.
        createNotificationChannel();
    }

    /**
     * --- ON-CREATE-VIEW ---
     * This method is called to create the fragment's UI.
     * It inflates the layout XML file and sets up the views.
     * @param inflater The object used to inflate the layout.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState Data from a previous state, if available.
     * @return The root view of the fragment's layout.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment from the XML file.
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // --- VIEW INITIALIZATION & EVENT LISTENERS ---
        // Find the floating action button and set a click listener to show the "add farm" dialog.
        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> showAddFarmDialog());

        // Find the RecyclerView and set it up with a layout manager and the adapter.
        recyclerView = view.findViewById(R.id.recyclerFarm);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Find the TextView for the active vegetation and set its initial text.
        tvActiveVegetation = view.findViewById(R.id.tvActiveVegetation);
        Vegetation currentActiveProfile = adapter.getActiveVegetation();
        if (currentActiveProfile != null) {
            tvActiveVegetation.setText("Monitoring Profile: " + currentActiveProfile.getName());
        } else {
            tvActiveVegetation.setText("No active profile set");
        }

        // Find the history button and set a click listener to show the history dialog.
        btnHistory = view.findViewById(R.id.btnHistory);
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> {
                showHistoryDialog();
            });
        }

        // Find the camera button and set a click listener to save the current state to history.
        CameraBtn = view.findViewById(R.id.CameraBtn);
        CameraBtn.setOnClickListener(v -> {
            saveCurrentStateToHistory();
        });

        LiveCameraBtn = view.findViewById(R.id.LiveCameraBtn);
        LiveCameraBtn.setOnClickListener(v -> {
            showLiveCameraDialog();
        });



        // --- DATA LOADING & PERMISSIONS ---
        // Ask for notification permission if needed.
        askForNotificationPermission();
        // Load the farm data from the server.
        loadFarmData();
        loadActiveVegetationProfile();

        setupDailyReminderSwitch(view);

        // Return the created view to be displayed on the screen.
        return view;

    }

    private void setupDailyReminderSwitch(View view) {
        Switch switchDailyReminder = view.findViewById(R.id.switchDailyReminder);

        // Load saved state
        boolean isEnabled = SimpleAlarmManager.isDailyReminderEnabled(requireContext());
        switchDailyReminder.setChecked(isEnabled);

        // Handle switch changes
        switchDailyReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SimpleAlarmManager.setDailyReminder(requireContext(), isChecked);
        });
    }

    /**
     * Asks for the POST_NOTIFICATIONS permission on Android 13 (TIRAMISU) and higher.
     * On older versions, this permission is not required.
     */
    private void askForNotificationPermission() {
        // This check is only needed for Android 13 and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if we already have the permission.
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted.
            } else {
                // If not, launch the permission request.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    /**
     * Loads the active vegetation profile from SharedPreferences.
     * SharedPreferences is used for storing small amounts of simple data.
     */
    private void loadActiveVegetationProfile() {
        // Get the SharedPreferences instance for the app.
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);

        // Get the saved JSON string of the active vegetation profile.
        String vegetationJson = sharedPreferences.getString("active_vegetation_profile", null);

        if (vegetationJson != null) {
            // If a profile was saved, convert it back from a JSON string to a Vegetation object.
            Gson gson = new Gson();
            Vegetation savedProfile = gson.fromJson(vegetationJson, Vegetation.class);

            // Set the loaded profile as the active one in the adapter.
            if (savedProfile != null) {
                adapter.setActiveVegetation(savedProfile);
                // Save the active vegetation to SharedPreferences for the service
                if (adapter.getActiveVegetation() != null) {
                    SharedPreferences prefs = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
                    prefs.edit().putString("active_vegetation", vegetationJson).apply();
                    Log.d("MainFragment", "Saved active vegetation to SharedPreferences");
                }
            }
        }
    }

    /**
     * Loads the farm data from the Supabase server.
     */
    private void loadFarmData() {

        Log.d("MainFragment", "─────────────────────────────────────────");
        Log.d("MainFragment", "🔄 loadFarmData() STARTED");
        // Get the SharedPreferences instance to find the user's farm ID.
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);


        // Read the saved farm ID. The default value of -1 indicates it wasn't found.
        int userId = sharedPreferences.getInt("user_id", -1); // Get USER ID
        Log.d("MainFragment", "User ID: " + userId);
        // If the farm ID is not valid, show an error and stop.
        if (userId == -1) {
            Toast.makeText(getContext(), "Error: No Farm ID found for user.", Toast.LENGTH_LONG).show();
            Log.e("MainFragment", "Could not load farm data, userFarmId is -1.");
            return;
        }

        // Create an instance of the Supabase service and fetch the farm data.
        SupabaseService service = new SupabaseService();
        service.fetchFarms(userId, new SupabaseService.FarmCallback() {

            /**
             * This method is called when the farm data is successfully fetched from the server.
             * @param farms The list of farms returned by the server.
             */
            @Override
            public void onSuccess(List<Farm> farms) {
                // Check if we CANNOT update (exit early)
                if (getActivity() == null || !isAdded()) {
                    Log.e("MainFragment", "❌ Cannot update - fragment not attached");
                    return;  // ← Exit early
                }

                // If we get here, we CAN update
                Log.d("MainFragment", "✅ Fragment attached - updating UI");

                getActivity().runOnUiThread(() -> {
                    farmList.clear();
                    farmList.addAll(farms);

                    adapter.notifyDataSetChanged();
                    recyclerView.post(() -> adapter.notifyDataSetChanged());
                    recyclerView.invalidate();
                    recyclerView.requestLayout();

                    Log.d("MainFragment", "✅ UI UPDATE COMPLETED!");
                });
            }



            /**
             * This method is called when there is an error fetching the farm data.
             * @param e The exception that occurred.
             */
            @Override
            public void onFailure(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to load farm data: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    /**
     * Shows the dialog for adding or editing a vegetation profile.
     */
    private void showAddFarmDialog() {
        // Inflate the custom layout for the dialog.
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_add_farm, null);

        // --- DIALOG VIEW INITIALIZATION ---
        // Find all the UI elements in the dialog's layout.
        RadioGroup rgModeSelector = dialogView.findViewById(R.id.rgModeSelector);
        TextInputLayout tilFarmName = dialogView.findViewById(R.id.tilFarmName);
        EditText etFarmName = dialogView.findViewById(R.id.etFarmName);
        Spinner spinnerVegetation = dialogView.findViewById(R.id.spinnerVegetation);
        final EditText etDayTempMin = dialogView.findViewById(R.id.etDayTempMin);
        final EditText etDayTempMax = dialogView.findViewById(R.id.etDayTempMax);
        final EditText etNightTempMin = dialogView.findViewById(R.id.etNightTempMin);
        final EditText etNightTempMax = dialogView.findViewById(R.id.etNightTempMax);
        final EditText etDayGroundMin = dialogView.findViewById(R.id.etDayGroundMin);
        final EditText etDayGroundMax = dialogView.findViewById(R.id.etDayGroundMax);
        final EditText etNightGroundMin = dialogView.findViewById(R.id.etNightGroundMin);
        final EditText etNightGroundMax = dialogView.findViewById(R.id.etNightGroundMax);
        final EditText etDayAirMin = dialogView.findViewById(R.id.etDayAirMin);
        final EditText etDayAirMax = dialogView.findViewById(R.id.etDayAirMax);
        final EditText etNightAirMin = dialogView.findViewById(R.id.etNightAirMin);
        final EditText etNightAirMax = dialogView.findViewById(R.id.etNightAirMax);

        // Create an array of all the EditText fields for easier manipulation.
        final EditText[] allFields = {etDayTempMin, etDayTempMax, etNightTempMin, etNightTempMax,
                etDayGroundMin, etDayGroundMax, etNightGroundMin, etNightGroundMax, etDayAirMin, etDayAirMax, etNightAirMin, etNightAirMax};

        // Fetch the list of existing vegetations from the server to populate the spinner.
        vegetationRepo.fetchVegetations(new VegetationRepo.FetchVegetationsCallback() {
            @Override
            public void onSuccess(List<Vegetation> vegetations) {
                allVegetations = vegetations;
                // Get a list of vegetation names for the spinner.
                List<String> vegetationNames = allVegetations.stream().map(Vegetation::getName).collect(Collectors.toList());
                // Create an adapter for the spinner.
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, vegetationNames);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerVegetation.setAdapter(spinnerAdapter);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Could not load existing vegetations.", Toast.LENGTH_SHORT).show();
            }
        });

        // --- DIALOG BUILDER SETUP ---
        // Create the AlertDialog builder.
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);

        // Set the buttons for the dialog. We set the listeners to null initially
        // so we can override them later to prevent the dialog from closing on button click.
        builder.setNeutralButton("Set Active", null);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        // Create the dialog.
        AlertDialog dialog = builder.create();

        // --- DIALOG EVENT LISTENERS ---
        // Set a listener for when the dialog is shown.
        dialog.setOnShowListener(dialogInterface -> {
            Button btnNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            btnNeutral.setVisibility(View.GONE); // Hide the "Set Active" button by default.

            // Set a listener for the radio group to switch between "add" and "edit" modes.
            rgModeSelector.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.rbAddNew) {
                    isEditMode = false;
                    tilFarmName.setVisibility(View.VISIBLE);
                    spinnerVegetation.setVisibility(View.GONE);
                    btnNeutral.setVisibility(View.GONE);
                    clearForm(allFields, etFarmName);
                } else {
                    isEditMode = true;
                    tilFarmName.setVisibility(View.GONE);
                    spinnerVegetation.setVisibility(View.VISIBLE);
                    btnNeutral.setVisibility(View.VISIBLE);
                    if (!allVegetations.isEmpty()) {
                        spinnerVegetation.setSelection(0);
                        selectedVegetation = allVegetations.get(0);
                        populateForm(selectedVegetation, allFields);
                    }
                }
            });

            // Set a listener for the spinner to update the form when a different vegetation is selected.
            spinnerVegetation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedVegetation = allVegetations.get(position);
                    populateForm(selectedVegetation, allFields);
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectedVegetation = null;
                }
            });

            // Set the click listener for the "Set Active" button.
            btnNeutral.setOnClickListener(v -> {
                if (selectedVegetation != null) {
                    // Update the UI to show the active profile.
                    tvActiveVegetation.setText("Monitoring Profile: " + selectedVegetation.getName());
                    adapter.setActiveVegetation(selectedVegetation);

                    // Save the active profile to SharedPreferences so it's remembered next time the app opens.
                    SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    Gson gson = new Gson();
                    String vegetationJson = gson.toJson(selectedVegetation);
                    editor.putString("active_vegetation_profile", vegetationJson);
                    editor.apply();

                    // Check for notifications with the new active profile.
                    checkForNotifications();

                    Toast.makeText(getContext(), selectedVegetation.getName() + " is now the active monitoring profile.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Please select a vegetation first.", Toast.LENGTH_SHORT).show();
                }
            });

            // Set the click listener for the "Save" button.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                // --- FORM VALIDATION ---
                boolean isNameValid = true;
                if (!isEditMode) {
                    if (TextUtils.isEmpty(etFarmName.getText().toString())) {
                        etFarmName.setError("Name is required");
                        isNameValid = false;
                    }
                } else if (selectedVegetation == null) {
                    Toast.makeText(getContext(), "Please select a vegetation to edit.", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean allNumericFieldsFilled = true;
                for (EditText field : allFields) {
                    if (TextUtils.isEmpty(field.getText().toString())) {
                        field.setError("This field is required");
                        allNumericFieldsFilled = false;
                    }
                }

                if (!isNameValid || !allNumericFieldsFilled) {
                    Toast.makeText(getContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // --- SAVE/UPDATE LOGIC ---
                try {
                    // Create a new Vegetation object or use the selected one.
                    Vegetation vegetationToSave = isEditMode ? selectedVegetation : new Vegetation();
                    if (isEditMode) {
                        vegetationToSave.setId(selectedVegetation.getId());
                    } else {
                        vegetationToSave.setName(etFarmName.getText().toString());
                    }

                    // Set all the properties of the vegetation object from the form fields.
                    vegetationToSave.setDayTempMin(Float.parseFloat(etDayTempMin.getText().toString()));
                    vegetationToSave.setDayTempMax(Float.parseFloat(etDayTempMax.getText().toString()));
                    vegetationToSave.setNightTempMin(Float.parseFloat(etNightTempMin.getText().toString()));
                    vegetationToSave.setNightTempMax(Float.parseFloat(etNightTempMax.getText().toString()));
                    vegetationToSave.setDayGroundHumidMin(Float.parseFloat(etDayGroundMin.getText().toString()));
                    vegetationToSave.setDayGroundHumidMax(Float.parseFloat(etDayGroundMax.getText().toString()));
                    vegetationToSave.setNightGroundHumidMin(Float.parseFloat(etNightGroundMin.getText().toString()));
                    vegetationToSave.setNightGroundHumidMax(Float.parseFloat(etNightGroundMax.getText().toString()));
                    vegetationToSave.setDayAirHumidMin(Float.parseFloat(etDayAirMin.getText().toString()));
                    vegetationToSave.setDayAirHumidMax(Float.parseFloat(etDayAirMax.getText().toString()));
                    vegetationToSave.setNightAirHumidMin(Float.parseFloat(etNightAirMin.getText().toString()));
                    vegetationToSave.setNightAirHumidMax(Float.parseFloat(etNightAirMax.getText().toString()));

                    // Call the appropriate repository method to save or update the vegetation.
                    if (isEditMode) {
                        vegetationRepo.updateVegetation(vegetationToSave, new VegetationRepo.UpdateVegetationCallback() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(getContext(), "Vegetation updated!", Toast.LENGTH_SHORT).show();
                                loadFarmData(); // Reload data to reflect changes.
                                dialog.dismiss();
                            }
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        vegetationRepo.addVegetation(vegetationToSave, new VegetationRepo.AddVegetationCallback() {
                            @Override
                            public void onSuccess(Void result) {
                                Toast.makeText(getContext(), "Vegetation added!", Toast.LENGTH_SHORT).show();
                                loadFarmData(); // Reload data to reflect changes.
                                dialog.dismiss();
                            }
                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(), "Please ensure all numeric fields are valid.", Toast.LENGTH_LONG).show();
                }
            });
        });

        // Show the dialog.
        dialog.show();
    }
    /**
     * Shows the live camera feed in a dialog
     *
     * WHAT'S FIXED:
     * - The fullscreen button now properly toggles without crashing
     * - Better null safety checks
     * - Clearer variable names
     */
    private void showLiveCameraDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView = inflater.inflate(R.layout.dialog_live_camera, null);

        // Initialize views
        WebView webView = dialogView.findViewById(R.id.webView);
        View loadingOverlay = dialogView.findViewById(R.id.loadingOverlay);
        TextView statusText = dialogView.findViewById(R.id.statusText);
        ImageView btnClose = dialogView.findViewById(R.id.btnClose);
        MaterialButton btnFullscreen = dialogView.findViewById(R.id.btnFullscreen);
        MaterialButton btnSnapshot = dialogView.findViewById(R.id.btnSnapshot);
        MaterialButton btnRecord = dialogView.findViewById(R.id.btnRecord);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Make sure dialog window is not null before accessing it
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Load camera stream (replace with your camera URL)
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadingOverlay.setVisibility(View.GONE);
                statusText.setVisibility(View.VISIBLE);
            }
        });
        webView.loadUrl("https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSrOunpvF6cRJnnrnfpFksyRMhWcQyJfivzAQ&s"); // Replace with actual camera URL

        // Button click listeners
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // --- FIXED FULLSCREEN BUTTON ---
        // The crash was caused by trying to cast to ConstraintLayout.LayoutParams
        // without checking the parent type first
        btnFullscreen.setOnClickListener(v -> {
            try {
                ViewGroup.LayoutParams params = webView.getLayoutParams();

                // Check if the parent is actually a ConstraintLayout
                if (params instanceof ConstraintLayout.LayoutParams) {
                    ConstraintLayout.LayoutParams constraintParams = (ConstraintLayout.LayoutParams) params;

                    // Toggle between fullscreen and normal size
                    if (constraintParams.height == ConstraintLayout.LayoutParams.MATCH_PARENT) {
                        // Exit fullscreen - go back to aspect ratio
                        constraintParams.height = 0;
                        constraintParams.dimensionRatio = "H,4:3";
                        btnFullscreen.setText("Fullscreen");
                        Log.d("LiveCamera", "Exited fullscreen mode");
                    } else {
                        // Enter fullscreen - fill the entire height
                        constraintParams.height = ConstraintLayout.LayoutParams.MATCH_PARENT;
                        constraintParams.dimensionRatio = null; // Remove aspect ratio constraint
                        btnFullscreen.setText("Exit Fullscreen");
                        Log.d("LiveCamera", "Entered fullscreen mode");
                    }

                    // Apply the changes
                    webView.setLayoutParams(constraintParams);
                } else {
                    // If the parent is not a ConstraintLayout, log an error
                    Log.e("LiveCamera", "Parent is not ConstraintLayout! It's: " +
                            params.getClass().getSimpleName());
                    Toast.makeText(getContext(), "Fullscreen not supported in this layout",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // Catch any other errors and show a user-friendly message
                Log.e("LiveCamera", "Fullscreen error: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Error toggling fullscreen", Toast.LENGTH_SHORT).show();
            }
        });

        btnSnapshot.setOnClickListener(v -> {
            // Take snapshot logic
            Toast.makeText(getContext(), "Snapshot saved!", Toast.LENGTH_SHORT).show();
        });

        btnRecord.setOnClickListener(v -> {
            // Record video logic
            if (btnRecord.getText().toString().equals("REC")) {
                btnRecord.setText("STOP");
                btnRecord.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.red)));
                Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
            } else {
                btnRecord.setText("REC");
                btnRecord.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.green)));
                Toast.makeText(getContext(), "Recording saved", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();

        // Make dialog fill most of the screen
        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
        }
    }


    /**
     * Populates the form fields with the data from a Vegetation object.
     * @param veg The Vegetation object to get the data from.
     * @param fields The array of EditText fields to populate.
     */
    private void populateForm(Vegetation veg, EditText[] fields) {
        fields[0].setText(String.valueOf(veg.getDayTempMin()));
        fields[1].setText(String.valueOf(veg.getDayTempMax()));
        fields[2].setText(String.valueOf(veg.getNightTempMin()));
        fields[3].setText(String.valueOf(veg.getNightTempMax()));
        fields[4].setText(String.valueOf(veg.getDayGroundHumidMin()));
        fields[5].setText(String.valueOf(veg.getDayGroundHumidMax()));
        fields[6].setText(String.valueOf(veg.getNightGroundHumidMin()));
        fields[7].setText(String.valueOf(veg.getNightGroundHumidMax()));
        fields[8].setText(String.valueOf(veg.getDayAirHumidMin()));
        fields[9].setText(String.valueOf(veg.getDayAirHumidMax()));
        fields[10].setText(String.valueOf(veg.getNightAirHumidMin()));
        fields[11].setText(String.valueOf(veg.getNightAirHumidMax()));
    }

    /**
     * Clears all the fields in the form.
     * @param fields The array of EditText fields to clear.
     * @param nameField The EditText for the vegetation name.
     */
    private void clearForm(EditText[] fields, EditText nameField) {
        nameField.setText("");
        for (EditText field : fields) {
            field.setText("");
        }
    }

    /**
     * Creates a notification channel. This is required on Android 8.0 (Oreo) and above
     * for notifications to be displayed.
     */
    public void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = "Farm Alerts";
            String description = "Notification for when farm sensors are out of range";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = requireActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * Sends a notification to the user's device. when the farm sensors are out of range for the vegetation
     * @param details The details of the out-of-range values.
     * @param activeProfile The active vegetation profile.
     */
    private void sendOutOfRangeNotification(String details, Vegetation activeProfile) {
        int icon = R.drawable.ic_launcher_foreground; // the app icon

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //
            builder = new Notification.Builder(requireContext(), CHANNEL_ID);
        } else {
            builder = new Notification.Builder(requireContext());
        }

        builder.setSmallIcon(icon)
                .setContentTitle("Alert for Profile: " + activeProfile.getName())
                .setContentText(details)
                .setStyle(new Notification.BigTextStyle().bigText(details))
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        notificationManager.notify(1, builder.build());
    }

    /**
     * Checks if any sensor values are out of the allowed range for the active profile.
     * @param farm The farm data to check.
     * @param profile The active vegetation profile.
     * @return A string with the details of the out-of-range values, or an empty string if all values are in range.
     */
    private String getOutOfRangeDetails(Farm farm, Vegetation profile) {
        if(profile == null || farm == null) return "";

        boolean isDay = adapter.isDayTime(farm.getDateTime());
        StringBuilder details = new StringBuilder();

        float tempMin, tempMax, groundHumidMin, groundHumidMax, airHumidMin, airHumidMax;

        if (isDay) {
            tempMin = profile.getDayTempMin();
            tempMax = profile.getDayTempMax();
            groundHumidMin = profile.getDayGroundHumidMin();
            groundHumidMax = profile.getDayGroundHumidMax();
            airHumidMin = profile.getDayAirHumidMin();
            airHumidMax = profile.getDayAirHumidMax();
        } else {
            tempMin = profile.getNightTempMin();
            tempMax = profile.getNightTempMax();
            groundHumidMin = profile.getNightGroundHumidMin();
            groundHumidMax = profile.getNightGroundHumidMax();
            airHumidMin = profile.getNightAirHumidMin();
            airHumidMax = profile.getNightAirHumidMax();
        }

        if (farm.getTemp() < tempMin) {
            details.append(String.format(Locale.US, "Temp is too low by %.1f°C. ", tempMin - farm.getTemp()));
        } else if (farm.getTemp() > tempMax) {
            details.append(String.format(Locale.US, "Temp is too high by %.1f°C. ", farm.getTemp() - tempMax));
        }

        if (farm.getGroundHumid() < groundHumidMin) {
            details.append(String.format(Locale.US, "Ground Humid is too low by %.1f%%. ", groundHumidMin - farm.getGroundHumid()));
        } else if (farm.getGroundHumid() > groundHumidMax) {
            details.append(String.format(Locale.US, "Ground Humid is too high by %.1f%%. ", farm.getGroundHumid() - groundHumidMax));
        }

        if (farm.getAirHumid() < airHumidMin) {
            details.append(String.format(Locale.US, "Air Humid is too low by %.1f%%. ", airHumidMin - farm.getAirHumid()));
        } else if (farm.getAirHumid() > airHumidMax) {
            details.append(String.format(Locale.US, "Air Humid is too high by %.1f%%. ", farm.getAirHumid() - airHumidMax));
        }

        return details.toString().trim();
    }

    /**
     * Checks if a notification should be sent based on the latest farm data and the active profile.
     */
    private void checkForNotifications() {
        Vegetation activeProfile = adapter.getActiveVegetation();
        if (activeProfile != null && !farmList.isEmpty()) {
            Farm latestFarmData = farmList.get(0);
            String details = getOutOfRangeDetails(latestFarmData, activeProfile);
            if(!details.isEmpty()) {
                sendOutOfRangeNotification(details, activeProfile);
            }
        }
    }

    /**
     * Saves the current state of the farm (sensor readings, etc.) to the history.
     */
    private void saveCurrentStateToHistory() {
        if (farmList.isEmpty()) {
            Toast.makeText(getContext(), "No farm data available", Toast.LENGTH_SHORT).show();
            return;
        }

        Farm latestFarm = farmList.get(0);
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("SmartFarmPrefs", Context.MODE_PRIVATE);
        int farmId = latestFarm.getId();
        Vegetation activeVegetation = adapter.getActiveVegetation();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Save to History");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        TextView tvReadings = new TextView(getContext());
        tvReadings.setText("Current Readings:\n" +
                "• Temperature: " + latestFarm.getTemp() + "°C\n" +
                "• Ground Humidity: " + latestFarm.getGroundHumid() + "%\n" +
                "• Air Humidity: " + latestFarm.getAirHumid() + "%");
        tvReadings.setTextSize(16);
        tvReadings.setPadding(0, 0, 0, 20);

        TextView tvFarmState = new TextView(getContext());
        if (activeVegetation != null) {
            String rangeDetails = getOutOfRangeDetails(latestFarm, activeVegetation);
            if (rangeDetails.isEmpty()) {
                tvFarmState.setText("✅ All values within range");
                tvFarmState.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
            } else {
                tvFarmState.setText("⚠️ Some values out of range");
                tvFarmState.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
            }
        } else {
            tvFarmState.setText("No active profile for range checking");
            tvFarmState.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        }
        tvFarmState.setTextSize(14);
        tvFarmState.setPadding(0, 0, 0, 20);

        TextView tvVegetation = new TextView(getContext());
        if (activeVegetation != null) {
            tvVegetation.setText("Active Profile: " + activeVegetation.getName());
            tvVegetation.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark));
        } else {
            tvVegetation.setText("No active vegetation profile");
            tvVegetation.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray));
        }
        tvVegetation.setTextSize(14);
        tvVegetation.setPadding(0, 0, 0, 20);

        EditText etNotes = new EditText(getContext());
        etNotes.setHint("Add your notes here (optional)");
        etNotes.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        etNotes.setMinHeight(100);

        layout.addView(tvReadings);
        layout.addView(tvFarmState);
        layout.addView(tvVegetation);
        layout.addView(etNotes);
        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String userNotes = etNotes.getText().toString().trim();

            History history = new History(
                    (long) farmId,
                    (long) latestFarm.getTemp(),
                    (long) latestFarm.getGroundHumid(),
                    (long) latestFarm.getAirHumid()
            );

            StringBuilder combinedNotes = new StringBuilder();
            if (activeVegetation != null) {
                String rangeDetails = getOutOfRangeDetails(latestFarm, activeVegetation);
                if (rangeDetails.isEmpty()) {
                    combinedNotes.append("✅ All values within range");
                } else {
                    combinedNotes.append("⚠️ ").append(rangeDetails);
                }
                combinedNotes.append(" | ");
                combinedNotes.append("Profile: ").append(activeVegetation.getName());
            } else {
                combinedNotes.append("No active profile set");
            }

            if (!userNotes.isEmpty()) {
                combinedNotes.append(" | Notes: ").append(userNotes);
            }

            history.setNotes(combinedNotes.toString());

            if (activeVegetation != null && activeVegetation.getId() != null) {
                history.setPictureUrl("veg_" + activeVegetation.getId());
            }

            saveHistoryEntry(history);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    /**
     * Saves a history entry to the database.
     * @param history The history entry to save.
     */
    private void saveHistoryEntry(History history) {
        historyRepo.addHistory(history, new HistoryRepo.AddHistoryCallback() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(getContext(), "✓ Saved to history!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("MainFragment", "Save to history failed", e);
            }
        });
    }

    /**
     * Shows the history of sensor readings in a dialog.
     */
    private void showHistoryDialog() {
        // Check if we have farm data
        if (farmList.isEmpty()) {
            Toast.makeText(getContext(), "No farm data available to show history", Toast.LENGTH_SHORT).show();
            return;
        }

        Farm currentFarm = farmList.get(0);
        int currentFarmId = currentFarm.getId();

        // Debug logging
        Log.d("HistoryDialog", "Current farm ID: " + currentFarmId);
        Log.d("HistoryDialog", "Farm UserID: " + currentFarm.getId());
        Log.d("HistoryDialog", "Farm temp: " + currentFarm.getTemp());

        // Show loading dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("History Log");
        builder.setMessage("Loading history for farm ID: " + currentFarmId + "...");
        builder.setCancelable(true);

        AlertDialog loadingDialog = builder.create();
        loadingDialog.show();

        // Use the filtered fetch method
        historyRepo.fetchHistoryByFarmId(currentFarmId, new HistoryRepo.FetchHistoryCallback() {
            @Override
            public void onSuccess(List<History> historyList) {
                loadingDialog.dismiss();

                Log.d("HistoryDialog", "Loaded " + historyList.size() + " history entries for farm " + currentFarmId);

                if (historyList.isEmpty()) {
                    Toast.makeText(getContext(), "No history entries for this farm", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create scrollable text view for history
                TextView textView = new TextView(getContext());
                textView.setPadding(30, 30, 30, 30);
                textView.setTextSize(14);
                textView.setMovementMethod(new ScrollingMovementMethod());

                StringBuilder historyText = new StringBuilder();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());

                for (int i = 0; i < historyList.size(); i++) {
                    History h = historyList.get(i);

                    historyText.append("Entry #").append(i + 1).append("\n");
                    historyText.append("🕒 ").append(sdf.format(h.getRecordedAt())).append("\n\n");
                    historyText.append("📊 Sensor Readings:\n");
                    historyText.append("  • Temperature: ").append(h.getTemperature()).append("°C\n");
                    historyText.append("  • Ground Humidity: ").append(h.getGroundHumidity()).append("%\n");
                    historyText.append("  • Air Humidity: ").append(h.getAirHumidity()).append("%\n\n");

                    if (h.getNotes() != null && !h.getNotes().isEmpty()) {
                        historyText.append("📝 Notes:\n");
                        String[] noteParts = h.getNotes().split("\\|");
                        for (String part : noteParts) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                historyText.append("  • ").append(trimmed).append("\n");
                            }
                        }
                    }

                    // Show which farm this belongs to (for debugging)
                    historyText.append("\n[Farm ID: ").append(h.getFarmId()).append("]\n");
                    historyText.append("\n────────────────────────\n\n");
                }

                textView.setText(historyText.toString());

                // Create result dialog
                AlertDialog.Builder resultBuilder = new AlertDialog.Builder(requireContext());
                resultBuilder.setTitle("Farm History (" + historyList.size() + " entries)");
                resultBuilder.setView(textView);
                resultBuilder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                resultBuilder.setNeutralButton("➕ Save New", (dialog, which) -> {
                    saveCurrentStateToHistory();
                });

                // Optional: Add delete button if you want to manage history
                // resultBuilder.setNegativeButton("Clear History", (dialog, which) -> {
                //     showClearHistoryDialog(currentFarmId);
                // });

                resultBuilder.show();
            }

            @Override
            public void onFailure(Exception e) {
                loadingDialog.dismiss();
                Log.e("HistoryDialog", "Failed to load history: " + e.getMessage(), e);
                Toast.makeText(getContext(), "Failed to load history: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * onResume() - Called when the fragment becomes visible to the user
     *
     * WHAT HAPPENS HERE:
     * 1. We register (activate) the BroadcastReceiver to listen for updates
     * 2. We start the FarmMonitoringService for background monitoring
     * 3. We reload the vegetation profiles to ensure we have the latest data
     */
    @Override
    public void onResume() {
        super.onResume();


        startPeriodicRefresh(); // start the 2-minute timer

        Log.d("MainFragment", "onResume – registering receiver");
        Log.d("MainFragment", "🟢 Fragment resumed - Setting up monitoring");

        // Register BroadcastReceiver with API level check
        IntentFilter filter = new IntentFilter(FarmMonitoringService.ACTION_DATA_UPDATED);
        // FIXED: Different registration based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33+ (Tiramisu)
            requireActivity().registerReceiver(dataUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // For older Android versions, use the simpler method
            requireActivity().registerReceiver(dataUpdateReceiver, filter); // Doesn't really matter because the API is 33+
        }
        Log.d("MainFragment", "✅ BroadcastReceiver registered");

        // Start monitoring service
        Intent serviceIntent = new Intent(requireContext(), FarmMonitoringService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent);
        } else {
            requireContext().startService(serviceIntent);
        }
        Log.d("MainFragment", "✅ Monitoring service started");

        loadFarmData();   // <-- add this line
        loadActiveVegetationProfile();
    }


    /**
     * onPause() - Called when the fragment is no longer visible
     *
     * WHAT HAPPENS HERE:
     * We unregister (deactivate) the BroadcastReceiver to prevent memory leaks
     *
     * IMPORTANT: We do NOT stop the service here! The service keeps running
     * in the background even when the user switches apps. This is intentional!
     */
    @Override
    public void onPause() {
        super.onPause();

        stopPeriodicRefresh(); // stop the timer when fragment is not visible

        Log.d("MainFragment", "onPause – unregistering receiver");
        Log.d("MainFragment", "🟡 Fragment paused - Unregistering receiver");

        // Unregister the BroadcastReceiver
        // This is CRITICAL to avoid memory leaks!
        try {
            requireActivity().unregisterReceiver(dataUpdateReceiver);
            Log.d("MainFragment", "✅ BroadcastReceiver unregistered");
        } catch (IllegalArgumentException e) {
            // This can happen if the receiver was never registered
            // It's safe to ignore
            Log.w("MainFragment", "Receiver was not registered");
        }

        // Note: The service continues running in the background!
        // This allows monitoring to continue even when the user isn't looking at the app
    }
    /**
     * Stops the background monitoring service
     *
     * You might want to call this method when:
     * - User logs out
     * - User disables monitoring in settings
     * - App is being closed
     *
     * For now, you can add a button to call this if you want to manually stop monitoring.
     */
    public void stopMonitoringService() {
        Intent serviceIntent = new Intent(requireContext(), FarmMonitoringService.class);
        requireContext().stopService(serviceIntent);
        Log.d("MainFragment", "🛑 Monitoring service stopped");
        Toast.makeText(getContext(), "Background monitoring stopped", Toast.LENGTH_SHORT).show();
    }

    private void startPeriodicRefresh() {
        // Cancel any existing timer (safety)
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }

        // Create a new Timer
        refreshTimer = new Timer();

        // Define the task
        refreshTask = new TimerTask() {
            @Override
            public void run() {
                // TimerTask runs on a background thread – we must switch to UI thread
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // This will run on the main thread and update the UI
                            loadFarmData();
                        }
                    });
                }
            }
        };

        // Schedule the task: start immediately, then repeat every FarmTimer miliSecs (120,000 ms)
        refreshTimer.schedule(refreshTask, 0, FarmTimer);    }

    private void stopPeriodicRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel(); // cancels the timer and all scheduled tasks
            refreshTimer = null;
            refreshTask = null;    // help garbage collection
        }
    }




}
